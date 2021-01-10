package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.util.registry.Registry;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class MapAtlasesClient implements ClientModInitializer {

    public static List<MapState> mapStates = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                (client, handler, buf, responseSender) -> {
                        MapAtlasesInitAtlasS2CPacket p = new MapAtlasesInitAtlasS2CPacket();
                        p.read(buf);
                        client.execute(() -> {
                            mapStates.add(p.getMapState());
                        });
                });
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                (client, handler, buf, responseSender) -> {
                    MapUpdateS2CPacket p = new MapUpdateS2CPacket();
                    try {
                        p.read(buf);
                        client.execute(() -> {
                            handler.onMapUpdate(p);
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        ClientTickEvents.END_CLIENT_TICK.register((client -> {
            if (client.player == null) return;
            ItemStack atlas = client.player.inventory.main.stream()
                    .filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
            if (!atlas.isEmpty()) {
                Iterator<MapState> itr = mapStates.iterator();
                while (itr.hasNext()) {
                    MapState state = itr.next();
                    if (client.world != null && client.player != null
                            && client.world.getRegistryKey() == state.dimension) {
                        state.getPlayerSyncData(client.player);
                        client.world.putMapState(state);
                        itr.remove();
                        MapAtlasesMod.LOGGER.info("Client Put MapState: " + state.getId());
                    }
                }
//                List <MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(client.world, atlas);
//                for (MapState state : mapStates) {
//                    state.update(client.player, atlas);
//                    ((FilledMapItem) Items.FILLED_MAP).updateColors(client.world, client.player, state);
//                    client.gameRenderer.getMapRenderer().updateTexture(state);
//                }
            }
        }));
    }
}
