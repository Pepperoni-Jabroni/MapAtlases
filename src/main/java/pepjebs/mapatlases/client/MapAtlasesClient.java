package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.util.registry.Registry;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;

import java.util.ArrayList;
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
        ClientTickEvents.START_CLIENT_TICK.register((client -> {
            for (MapState state : mapStates) {
                if (client.world != null && client.player != null &&
                        client.world.getRegistryKey() == state.dimension &&
                        client.world.getMapState(state.getId()) == null) {
                    ItemStack atlas = client.player.inventory.main.stream()
                            .filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
                    state.getPlayerSyncData(client.player);
                    state.update(client.player, atlas);
                    client.gameRenderer.getMapRenderer().updateTexture(state);
                    client.world.putMapState(state);
                    MapAtlasesMod.LOGGER.info("Client Put mapState: " + state.getId());
                }
            }
        }));
    }
}
