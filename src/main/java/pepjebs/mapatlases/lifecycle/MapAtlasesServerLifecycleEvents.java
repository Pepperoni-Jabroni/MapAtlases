package pepjebs.mapatlases.lifecycle;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.networking.MapAtlasesOpenGUIC2SPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class MapAtlasesServerLifecycleEvents {

    public static final Identifier MAP_ATLAS_ACTIVE_STATE_CHANGE = new Identifier(
            MapAtlasesMod.MOD_ID, "active_state_change");

    // Value minimum of 64, since maps are 128
    private static final int NEW_MAP_CENTER_DISTANCE = 90;

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final Semaphore mutex = new Semaphore(1);

    // Holds the current MapState ID for each player
    private static final Map<String, String> playerToActiveMapId = new HashMap<>();

    public static void openGuiEvent(
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler _handler,
            PacketByteBuf buf,
            PacketSender _responseSender) {
        MapAtlasesOpenGUIC2SPacket p = new MapAtlasesOpenGUIC2SPacket();
        p.read(buf);
        server.execute(() -> {
            ItemStack atlas = p.atlas;
            player.openHandledScreen((MapAtlasItem) atlas.getItem());
            player.getWorld().playSound(null, player.getBlockPos(),
                    MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        });
    }

    public static void mapAtlasPlayerJoin(
            ServerPlayNetworkHandler serverPlayNetworkHandler,
            PacketSender _responseSender,
            MinecraftServer _server
    ) {
        ServerPlayerEntity player = serverPlayNetworkHandler.player;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player.getInventory());
        if (atlas.isEmpty()) return;
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.world, atlas);
        for (Map.Entry<String, MapState> info : mapInfos.entrySet()) {
            String mapId = info.getKey();
            MapState state = info.getValue();
            state.update(player, atlas);
            state.getPlayerSyncData(player);
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            (new MapAtlasesInitAtlasS2CPacket(mapId, state)).write(packetByteBuf);
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                    packetByteBuf));
            MapAtlasesMod.LOGGER.info("Server Sent MapState: " + mapId);
        }
    }

    public static void mapAtlasServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player.getInventory());
            if (!atlas.isEmpty()) {
                Map.Entry<String, MapState> activeInfo =
                        MapAtlasesAccessUtils.getActiveAtlasMapState(
                                player.getWorld(), atlas, player.getName().getString());
                if (activeInfo != null) {
                    String playerName = player.getName().getString();
                    if (!playerToActiveMapId.containsKey(playerName)
                            || playerToActiveMapId.get(playerName).compareTo(activeInfo.getKey()) != 0) {
                        playerToActiveMapId.put(playerName, activeInfo.getKey());
                        PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                        packetByteBuf.writeString(activeInfo.getKey());
                        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                                MAP_ATLAS_ACTIVE_STATE_CHANGE, packetByteBuf));
                    }
                } else if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas) != 0) {
                    MapAtlasesMod.LOGGER.info("Null active MapState with non-empty Atlas");
                }


                Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.world, atlas);

                // Maps are 128x128
                int playX = player.getBlockPos().getX();
                int playZ = player.getBlockPos().getZ();
                int minDist = Integer.MAX_VALUE;
                int scale = -1;

                for (Map.Entry<String, MapState> info : mapInfos.entrySet()) {
                    MapState state = info.getValue();
                    state.update(player, atlas);
                    ((FilledMapItem) Items.FILLED_MAP).updateColors(player.getWorld(), player, state);
                    int mapId = MapAtlasesAccessUtils.getMapIntFromString(info.getKey());
                    Packet<?> p = null;
                    int tries = 0;
                    while (p == null && tries < 10) {
                        p = state.getPlayerMarkerPacket(mapId, player);
                        tries++;
                    }
                    if (p != null) {
                        PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                        p.write(packetByteBuf);
                        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                                MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                                packetByteBuf));
                    }

                    int mapCX = state.centerX;
                    int mapCZ = state.centerZ;
                    minDist = Math.min(minDist, (int) Math.hypot(playX-mapCX, playZ-mapCZ));
                    scale = state.scale;
                }

                if (MapAtlasesMod.CONFIG != null && !MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill) continue;
                if (atlas.getNbt() == null) continue;
                String oldAtlasTagState = atlas.getNbt().toString();
                List<Integer> mapIds = Arrays.stream(
                        atlas.getNbt().getIntArray("maps")).boxed().collect(Collectors.toList());
                int emptyCount = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
                if (mutex.availablePermits() > 0 && minDist != -1 &&
                        scale != -1 && minDist > (NEW_MAP_CENTER_DISTANCE * (1 << scale)) && emptyCount > 0) {
                    try {
                        mutex.acquire();

                        // Make the new map
                        atlas.getNbt().putInt("empty", atlas.getNbt().getInt("empty") - 1);
                        ItemStack newMap = FilledMapItem.createMap(
                                player.getWorld(),
                                MathHelper.floor(player.getX()),
                                MathHelper.floor(player.getZ()),
                                (byte) scale,
                                true,
                                false);
                        mapIds.add(FilledMapItem.getMapId(newMap));
                        atlas.getNbt().putIntArray("maps", mapIds);

                        // Update the reference in the inventory
                        MapAtlasesAccessUtils.setAllMatchingItemStacks(
                                player.getInventory().offHand, 1, MapAtlasesMod.MAP_ATLAS, oldAtlasTagState, atlas);
                        MapAtlasesAccessUtils.setAllMatchingItemStacks(
                                player.getInventory().main, 9, MapAtlasesMod.MAP_ATLAS, oldAtlasTagState, atlas);

                        // Play the sound
                        player.getWorld().playSound(null, player.getBlockPos(),
                                MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT,
                                SoundCategory.PLAYERS, 1.0F, 1.0F);
                    } catch (InterruptedException e) {
                        MapAtlasesMod.LOGGER.warn(e);
                    } finally {
                        mutex.release();
                    }
                }
            }
        }
    }
}
