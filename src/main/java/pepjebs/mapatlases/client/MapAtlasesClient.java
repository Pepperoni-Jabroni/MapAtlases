package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreen;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;

import java.io.IOException;
import java.util.stream.Collectors;

public class MapAtlasesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, MapAtlasesAtlasOverviewScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                (client, handler, buf, responseSender) -> {
                    MapAtlasesInitAtlasS2CPacket p = new MapAtlasesInitAtlasS2CPacket();
                    p.read(buf);
                    client.execute(() -> {
                        if (client.world == null) return;
                        MapState state = p.getMapState();
                        ItemStack atlas = client.player.inventory.main.stream()
                                .filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
                        state.update(client.player, atlas);
                        state.getPlayerSyncData(client.player);
                        client.world.putMapState(state);
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
    }
}
