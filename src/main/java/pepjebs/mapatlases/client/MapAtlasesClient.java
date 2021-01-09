package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;

import java.util.stream.Collectors;

public class MapAtlasesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                (client, handler, buf, responseSender) -> {
                        MapAtlasesMod.LOGGER.info("Received MapState packet...");
                        MapAtlasesInitAtlasS2CPacket p = new MapAtlasesInitAtlasS2CPacket();
                        p.read(buf);
                        client.execute(() -> {
                            MapAtlasesMod.LOGGER.info(client.world);
                            if (client.world == null) return;
                            MapState state = p.getMapState();
                            ItemStack atlas = client.player.inventory.main.stream()
                                    .filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
                            state.update(client.player, atlas);
                            state.getPlayerSyncData(client.player);
                            MapAtlasesMod.LOGGER.info(state.icons.values().stream().map(i -> i.getType()).collect(Collectors.toList()));
                            client.world.putMapState(state);
                            MapAtlasesMod.LOGGER.info("Received MapState: " + p.getMapState().getId());
                        });
                });
    }
}
