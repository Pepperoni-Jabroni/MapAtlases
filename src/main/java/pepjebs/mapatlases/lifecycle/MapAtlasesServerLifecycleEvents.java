package pepjebs.mapatlases.lifecycle;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.networking.MapAtlasesOpenGUIC2SPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
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
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof MapAtlasItem) {
                Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.world, stack);
                for (Map.Entry<String, MapState> info : mapInfos.entrySet()) {
                    String mapId = info.getKey();
                    MapState state = info.getValue();
                    state.update(player, stack);
                    state.getPlayerSyncData(player);
                    PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                    (new MapAtlasesInitAtlasS2CPacket(mapId, state)).write(packetByteBuf);
                    player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                            MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                            packetByteBuf));
                    MapAtlasesMod.LOGGER.info("Server Sent MapState: " + mapId);
                }
            }
        }
    }

    // @TODO: Fix Trinkets slot not displaying user map icon
    // The active map state selection still works though,
    // so its likely a client-side issue
    public static void mapAtlasServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            if (!atlas.isEmpty()) {
                Map.Entry<String, MapState> activeInfo =
                        MapAtlasesAccessUtils.getActiveAtlasMapStateServer(
                                player.getWorld(), atlas, player);
                String changedMapState = null;
                if (activeInfo != null) {
                    String playerName = player.getName().getString();
                    if (!playerToActiveMapId.containsKey(playerName)
                            || playerToActiveMapId.get(playerName).compareTo(activeInfo.getKey()) != 0) {
                        changedMapState = playerToActiveMapId.get(playerName);
                        playerToActiveMapId.put(playerName, activeInfo.getKey());
                        PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                        packetByteBuf.writeString(activeInfo.getKey());
                        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                                MAP_ATLAS_ACTIVE_STATE_CHANGE, packetByteBuf));
                    }
                } else if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas) != 0) {
                    MapAtlasesMod.LOGGER.info("Null active MapState with non-empty Atlas");
                }



                // Maps are 128x128
                int playX = player.getBlockPos().getX();
                int playZ = player.getBlockPos().getZ();
                boolean isPlayerOutsideAllMapRegions = true;
                int scale = 0;
                ArrayList<Pair<Integer, Integer>> discoveringEdges = new ArrayList<>();
                if (activeInfo != null) {
                    discoveringEdges = MapAtlasesAccessUtils.getPlayerDiscoveringMapEdges(
                            activeInfo.getValue().centerX,
                            activeInfo.getValue().centerZ,
                            (1 << activeInfo.getValue().scale) * 128,
                            playX,
                            playZ,
                            128
                    );
                }

                Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.world, atlas);
                for (Map.Entry<String, MapState> info : mapInfos.entrySet()) {
                    MapState state = info.getValue();
                    // Only update active (based on discovery radius) map states
                    if (discoveringEdges.stream().noneMatch(p -> p.getLeft()==state.centerX && p.getRight() == state.centerZ)
                        && (activeInfo == null
                        || !(activeInfo.getValue().centerX==state.centerX && activeInfo.getValue().centerZ==state.centerZ))
                        && (changedMapState == null || info.getKey().compareTo(changedMapState) != 0)
                    )
                        continue;
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
                    isPlayerOutsideAllMapRegions = isPlayerOutsideAllMapRegions &&
                            MapAtlasesAccessUtils.isPlayerOutsideSquareRegion(
                                state.centerX,
                                state.centerZ,
                                (1 << state.scale) * 128,
                                playX,
                                playZ,
                                0
                            );
                    scale = state.scale;
                }

                if (MapAtlasesMod.CONFIG != null && !MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill) continue;
                if (atlas.getNbt() == null) {
                    NbtCompound nbt = new NbtCompound();
                    nbt.putInt("empty", 9);
                    atlas.setNbt(nbt);
                }
                if (isPlayerOutsideAllMapRegions) {
                    maybeCreateNewMapEntry(player, atlas, scale, MathHelper.floor(player.getX()),
                            MathHelper.floor(player.getZ()));
                }
                discoveringEdges.removeIf(p -> mapInfos.values().stream().anyMatch(i -> p.getLeft() == i.centerX
                        && p.getRight() == i.centerZ));
                for (var p : discoveringEdges) {
                    maybeCreateNewMapEntry(player, atlas, scale, p.getLeft(), p.getRight());
                }
            }
        }
    }

    private static void maybeCreateNewMapEntry(
            ServerPlayerEntity player,
            ItemStack atlas,
            int scale,
            int destX,
            int destZ
    ) {
        String oldAtlasTagState = atlas.getNbt().toString();
        List<Integer> mapIds = Arrays.stream(
                atlas.getNbt().getIntArray("maps")).boxed().collect(Collectors.toList());
        int emptyCount = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        // Only allow Map creation in the Overworld
        if (mutex.availablePermits() > 0 && emptyCount > 0 && player.world.getRegistryKey() == World.OVERWORLD) {
            try {
                mutex.acquire();

                // Make the new map
                MapAtlasesMod.LOGGER.info("Creating map for "+destX+", "+destZ);
                atlas.getNbt().putInt("empty", atlas.getNbt().getInt("empty") - 1);
                ItemStack newMap = FilledMapItem.createMap(
                        player.getWorld(),
                        destX,
                        destZ,
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
