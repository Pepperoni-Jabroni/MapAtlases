package pepjebs.mapatlases.lifecycle;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesActiveStateChangePacket;
import pepjebs.mapatlases.networking.MapAtlasesInitAtlasPacket;
import pepjebs.mapatlases.networking.MapAtlasesOpenGUIPacket;
import pepjebs.mapatlases.networking.MapAtlasesSyncPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class MapAtlasesServerLifecycleEvents {

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final Semaphore mutex = new Semaphore(1);

    // Holds the current MapState ID for each player
    private static final Map<String, String> playerToActiveMapId = new HashMap<>();

    

    public static void openGuiEvent(MapAtlasesOpenGUIPacket payload, ServerPlayNetworking.Context context) {
        context.player().openHandledScreen((MapAtlasItem) payload.atlas().getItem());
    }

    public static void mapAtlasPlayerJoin(
            ServerPlayNetworkHandler serverPlayNetworkHandler,
            PacketSender _responseSender,
            MinecraftServer _server
    ) {
        mapAtlasPlayerJoinImpl(serverPlayNetworkHandler.player);
    }

    public static void mapAtlasPlayerJoinImpl(
            ServerPlayerEntity player
    ) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.getWorld(), atlas);
        for (Map.Entry<String, MapState> info : mapInfos.entrySet()) {
            String mapId = info.getKey();
            MapState state = info.getValue();
            state.update(player, atlas);
            state.getPlayerSyncData(player);
            ServerPlayNetworking.send(player, new MapAtlasesInitAtlasPacket(mapId, state));
        }
    }

    public static void mapAtlasServerTick(MinecraftServer server) {
        ArrayList<String> seenPlayers = new ArrayList<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            var playerName = player.getName().getString();
            seenPlayers.add(playerName);
            if (player.isRemoved() || player.isInTeleportationState() || player.isDisconnected()) continue;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            if (atlas.isEmpty()) continue;
            Map<String, MapState> currentMapInfos =
                    MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(player.getWorld(), atlas);
            Map.Entry<String, MapState> activeInfo = MapAtlasesAccessUtils.getActiveAtlasMapStateServer(
                    currentMapInfos, player);
            // changedMapState has non-null value if player has a new active Map ID
            String changedMapState = relayActiveMapIdToPlayerClient(activeInfo, player);
            if (activeInfo == null) {
                maybeCreateNewMapEntry(player, atlas, 0, MathHelper.floor(player.getX()),
                        MathHelper.floor(player.getZ()));
                continue;
            }
            MapState activeState = activeInfo.getValue();

            int playX = player.getBlockPos().getX();
            int playZ = player.getBlockPos().getZ();
            byte scale = activeState.scale;
            int scaleWidth = (1 << scale) * 128;
            ArrayList<Pair<Integer, Integer>> discoveringEdges = getPlayerDiscoveringMapEdges(
                    activeState.centerX,
                    activeState.centerZ,
                    scaleWidth,
                    playX,
                    playZ
            );

            // Update Map states & colors
            // updateColors is *easily* the most expensive function in the entire server tick
            // As a result, we will only ever call updateColors twice per tick (same as vanilla's limit)
            Map<String, MapState> nearbyExistentMaps = currentMapInfos.entrySet().stream()
                    .filter(e -> discoveringEdges.stream()
                            .anyMatch(edge -> edge.getLeft() == e.getValue().centerX
                                    && edge.getLeft() == e.getValue().centerX))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            for (var mapInfo : currentMapInfos.entrySet()) {
                updateMapDataForPlayer(mapInfo, player, atlas);
            }
            updateMapColorsForPlayer(activeState, player);
            if (!nearbyExistentMaps.isEmpty()) {
                updateMapColorsForPlayer(
                        (MapState) nearbyExistentMaps.values().toArray()[server.getTicks() % nearbyExistentMaps.size()],
                        player);
            }

            // Create new Map entries
            if (MapAtlasesMod.CONFIG != null && !MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill) continue;
            boolean isPlayerOutsideAllMapRegions = MapAtlasesAccessUtils.distanceBetweenMapStateAndPlayer(
                    activeState, player) > scaleWidth;
            if (isPlayerOutsideAllMapRegions) {
                maybeCreateNewMapEntry(player, atlas, scale, MathHelper.floor(player.getX()),
                        MathHelper.floor(player.getZ()));
            }
            discoveringEdges.removeIf(e -> nearbyExistentMaps.values().stream().anyMatch(
                    d -> d.centerX == e.getLeft() && d.centerZ == e.getRight()));
            for (var p : discoveringEdges) {
                maybeCreateNewMapEntry(player, atlas, scale, p.getLeft(), p.getRight());
            }
        }
        // Clean up disconnected players in server tick
        // since when using Disconnect event, the tick will sometimes
        // re-add the Player after they disconnect
        playerToActiveMapId.keySet().removeIf(playerName -> !seenPlayers.contains(playerName));
    }

    private static void updateMapDataForPlayer(
            Map.Entry<String, MapState> mapInfo,
            ServerPlayerEntity player,
            ItemStack atlas
    ) {
        mapInfo.getValue().update(player, atlas);
        relayMapStateSyncToPlayerClient(mapInfo, player);
    }

    private static void updateMapColorsForPlayer(
            MapState state,
            ServerPlayerEntity player) {
        ((FilledMapItem) Items.FILLED_MAP).updateColors(player.getWorld(), player, state);
    }

    public static void relayMapStateSyncToPlayerClient(
            Map.Entry<String, MapState> mapInfo,
            ServerPlayerEntity player
    ) {
        MapIdComponent mapId = MapAtlasesAccessUtils.getMapIdComponentFromString(mapInfo.getKey());
        Packet<?> p = null;
        int tries = 0;
        while (p == null && tries < 10) {
            p = mapInfo.getValue().getPlayerMarkerPacket(mapId, player);
            tries++;
        }
        if (p instanceof MapUpdateS2CPacket p2) {
            ServerPlayNetworking.send(player, new MapAtlasesSyncPacket(p2.mapId(), p2.scale(), p2.locked(), p2.decorations(), p2.updateData()));
        }
    }

    private static String relayActiveMapIdToPlayerClient(
            Map.Entry<String, MapState> activeInfo,
            ServerPlayerEntity player
    ) {
        String playerName = player.getName().getString();
        String changedMapState = null;
        if (activeInfo != null) {
            boolean addingPlayer = !playerToActiveMapId.containsKey(playerName);
            boolean activatingPlayer = playerToActiveMapId.get(playerName) == null;
            // Players that pick up an atlas will need their MapStates initialized
            if (addingPlayer || activatingPlayer) {
                mapAtlasPlayerJoinImpl(player);
            }
            if (addingPlayer || activatingPlayer
                    || activeInfo.getKey().compareTo(playerToActiveMapId.get(playerName)) != 0) {
                changedMapState = playerToActiveMapId.get(playerName);
                playerToActiveMapId.put(playerName, activeInfo.getKey());
                ServerPlayNetworking.send(player,
                new MapAtlasesActiveStateChangePacket(activeInfo.getKey()));
            }
        } else if (playerToActiveMapId.get(playerName) != null){
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            packetByteBuf.writeString("null");
            ServerPlayNetworking.send(player,
            new MapAtlasesActiveStateChangePacket("null"));
            playerToActiveMapId.put(playerName, null);
        }
        return changedMapState;
    }

    private static void maybeCreateNewMapEntry(
            ServerPlayerEntity player,
            ItemStack atlas,
            int scale,
            int destX,
            int destZ
    ) {
        List<Integer> mapIds = new ArrayList<>();
        NbtCompound nbt = atlas.get(DataComponentTypes.CUSTOM_DATA) == null ? null : atlas.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        if (nbt != null) {
            mapIds = Arrays.stream(
                    nbt.getIntArray(MapAtlasItem.MAP_LIST_NBT)).boxed().collect(Collectors.toList());
        } else {
            // If the Atlas is "inactive", give it a pity Empty Map count
            NbtCompound defaultAtlasNbt = new NbtCompound();
            if (MapAtlasesMod.CONFIG != null)
                defaultAtlasNbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, MapAtlasesMod.CONFIG.pityActivationMapCount);
            else
                defaultAtlasNbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, 1);
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, atlas, defaultAtlasNbt);
        }
        int emptyCount = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        boolean bypassEmptyMaps = !MapAtlasesMod.CONFIG.requireEmptyMapsToExpand;
        if (mutex.availablePermits() > 0
                && (emptyCount > 0 || player.isCreative() || bypassEmptyMaps)) {
            try {
                mutex.acquire();

                // Make the new map
                if (!player.isCreative() && !bypassEmptyMaps) {
                    NbtCompound nbt2 = atlas.get(DataComponentTypes.CUSTOM_DATA) == null ? null : atlas.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
                    nbt2.putInt(MapAtlasItem.EMPTY_MAP_NBT, nbt2.getInt(MapAtlasItem.EMPTY_MAP_NBT) - 1);
                    NbtComponent.set(DataComponentTypes.CUSTOM_DATA, atlas, nbt2);
                }
                ItemStack newMap = FilledMapItem.createMap(
                        player.getWorld(),
                        destX,
                        destZ,
                        (byte) scale,
                        true,
                        false);
                mapIds.add(newMap.get(DataComponentTypes.MAP_ID).id());
                NbtCompound nbt3 = atlas.get(DataComponentTypes.CUSTOM_DATA) == null ? null : atlas.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
                nbt3.putIntArray(MapAtlasItem.MAP_LIST_NBT, mapIds);
                NbtComponent.set(DataComponentTypes.CUSTOM_DATA, atlas, nbt3);

                // Play the sound
                player.getWorld().playSound(null, player.getBlockPos(),
                        MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT,
                        SoundCategory.PLAYERS, MapAtlasesMod.CONFIG.soundScalar, 1.0F);
            } catch (InterruptedException e) {
                MapAtlasesMod.LOGGER.warn(e);
            } finally {
                mutex.release();
            }
        }
    }

    private static ArrayList<Pair<Integer, Integer>> getPlayerDiscoveringMapEdges(
            int xCenter,
            int zCenter,
            int width,
            int xPlayer,
            int zPlayer) {
        int halfWidth = width / 2;
        ArrayList<Pair<Integer, Integer>> results = new ArrayList<>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i != 0 || j != 0) {
                    int qI = xCenter;
                    int qJ = zCenter;
                    if (i == -1 && xPlayer - 128 < xCenter - halfWidth) {
                        qI -= width;
                    } else if (i == 1 && xPlayer + 128 > xCenter + halfWidth) {
                        qI += width;
                    }
                    if (j == -1 && zPlayer - 128 < zCenter - halfWidth) {
                        qJ -= width;
                    } else if (j == 1 && zPlayer + 128 > zCenter + halfWidth) {
                        qJ += width;
                    }
                    // Some lambda bullshit
                    int finalQI = qI;
                    int finalQJ = qJ;
                    if ((qI != xCenter || qJ != zCenter) && results.stream()
                            .noneMatch(p -> p.getLeft() == finalQI && p.getRight() == finalQJ)) {
                        results.add(new Pair<>(qI, qJ));
                    }
                }
            }
        }
        return results;
    }
}