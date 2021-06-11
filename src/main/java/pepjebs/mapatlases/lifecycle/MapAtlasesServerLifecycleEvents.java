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
            if (!(player.getInventory().offHand.get(0).getTag() != null && atlas.getTag() != null &&
                    player.getInventory().offHand.get(0).getTag().toString().compareTo(atlas.getTag().toString()) == 0)) {
                int atlasIdx = player.getInventory().main.size();
                for (int i = 0; i < player.getInventory().main.size(); i++) {
                    if (player.getInventory().main.get(i).getItem() == atlas.getItem() &&
                            player.getInventory().main.get(i).getTag() != null &&
                            atlas.getTag() != null &&
                            player.getInventory().main.get(i).getTag().toString().compareTo(atlas.getTag().toString()) == 0) {
                        atlasIdx = i;
                        break;
                    }
                }
                if (atlasIdx < PlayerInventory.getHotbarSize()) {
                    player.getInventory().selectedSlot = atlasIdx;
                    player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(atlasIdx));
                }
            }
            player.openHandledScreen((MapAtlasItem) atlas.getItem());
            player.getServerWorld().playSound(null, player.getBlockPos(),
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
        List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(player.getServerWorld(), atlas);
        for (MapState state : mapStates) {
            state.update(player, atlas);
            state.getPlayerSyncData(player);
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            (new MapAtlasesInitAtlasS2CPacket(state)).write(packetByteBuf);
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                    packetByteBuf));
            MapAtlasesMod.LOGGER.info("Server Sent MapState: " + state.getId());
        }
    }

    public static void mapAtlasServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player.getInventory());
            if (!atlas.isEmpty()) {
                MapState activeState =
                        MapAtlasesAccessUtils.getActiveAtlasMapState(
                                player.getServerWorld(), atlas, player.getName().getString());
                if (activeState != null) {
                    String playerName = player.getName().getString();
                    if (!playerToActiveMapId.containsKey(playerName)
                            || playerToActiveMapId.get(playerName).compareTo(activeState.getId()) != 0) {
                        playerToActiveMapId.put(playerName, activeState.getId());
                        PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                        packetByteBuf.writeString(activeState.getId());
                        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                                MAP_ATLAS_ACTIVE_STATE_CHANGE, packetByteBuf));
                    }
                } else if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas) != 0) {
                    MapAtlasesMod.LOGGER.info("Null active MapState with non-empty Atlas");
                }


                List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(player.getServerWorld(), atlas);

                // Maps are 128x128
                int playX = player.getBlockPos().getX();
                int playZ = player.getBlockPos().getZ();
                int minDist = Integer.MAX_VALUE;
                int scale = -1;

                for (MapState state : mapStates) {
                    state.update(player, atlas);
                    ((FilledMapItem) Items.FILLED_MAP).updateColors(player.getServerWorld(), player, state);
                    ItemStack map = MapAtlasesAccessUtils.createMapItemStackFromStrId(state.getId());
                    Packet<?> p = null;
                    int tries = 0;
                    while (p == null && tries < 10) {
                        p = state.getPlayerMarkerPacket(map, player.getServerWorld(), player);
                        tries++;
                    }
                    if (p != null) {
                        PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                        try {
                            p.write(packetByteBuf);
                            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                                    MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                                    packetByteBuf));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    int mapCX = state.centerX;
                    int mapCZ = state.centerZ;
                    minDist = Math.min(minDist, (int) Math.hypot(playX-mapCX, playZ-mapCZ));
                    scale = state.scale;
                }

                if (MapAtlasesMod.CONFIG != null && !MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill) continue;
                if (atlas.getTag() == null) continue;
                String oldAtlasTagState = atlas.getTag().toString();
                List<Integer> mapIds = Arrays.stream(
                        atlas.getTag().getIntArray("maps")).boxed().collect(Collectors.toList());
                int emptyCount = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
                if (mutex.availablePermits() > 0 && minDist != -1 &&
                        scale != -1 && minDist > (NEW_MAP_CENTER_DISTANCE * (1 << scale)) && emptyCount > 0) {
                    try {
                        mutex.acquire();

                        // Make the new map
                        atlas.getTag().putInt("empty", atlas.getTag().getInt("empty") - 1);
                        ItemStack newMap = FilledMapItem.createMap(
                                player.getServerWorld(),
                                MathHelper.floor(player.getX()),
                                MathHelper.floor(player.getZ()),
                                (byte) scale,
                                true,
                                false);
                        mapIds.add(FilledMapItem.getMapId(newMap));
                        atlas.getTag().putIntArray("maps", mapIds);

                        // Update the reference in the inventory
                        MapAtlasesAccessUtils.setAllMatchingItemStacks(
                                player.getInventory().offHand, 1, MapAtlasesMod.MAP_ATLAS, oldAtlasTagState, atlas);
                        MapAtlasesAccessUtils.setAllMatchingItemStacks(
                                player.getInventory().main, 9, MapAtlasesMod.MAP_ATLAS, oldAtlasTagState, atlas);

                        // Play the sound
                        player.getServerWorld().playSound(null, player.getBlockPos(),
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
