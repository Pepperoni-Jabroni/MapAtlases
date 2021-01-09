package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;

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
                            client.world.putMapState(p.getMapState());
                            MapAtlasesMod.LOGGER.info("Received MapState: " + p.getMapState().getId());
                        });
                });
    }
}
