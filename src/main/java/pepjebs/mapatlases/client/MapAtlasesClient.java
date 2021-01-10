package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;

import java.io.IOException;

public class MapAtlasesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
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
