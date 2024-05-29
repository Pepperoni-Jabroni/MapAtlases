package pepjebs.mapatlases.lifecycle;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.networking.MapAtlasesInitAtlasPacket;
import pepjebs.mapatlases.networking.MapAtlasesOpenGUIPacket;
import pepjebs.mapatlases.networking.MapAtlasesSyncPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasesClientLifecycleEvents {

    public static void mapAtlasClientTick(MinecraftClient client) {
        while (MapAtlasesClient.displayMapGUIBinding.wasPressed()) {
            if (client.world == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            if (atlas.isEmpty()) return;
            MapAtlasesOpenGUIPacket p = new MapAtlasesOpenGUIPacket(atlas);
            ClientPlayNetworking.send(p);
        }
    }

    

    public static void mapAtlasClientInit(MapAtlasesInitAtlasPacket packet, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        client.execute(() -> {
            if (client.world == null || client.player == null) {
                return;
            }
            MapState state = packet.mapState;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            state.update(client.player, atlas);
            state.getPlayerSyncData(client.player);
            client.world.putClientsideMapState(MapAtlasesAccessUtils.getMapIdComponentFromString(packet.mapId), state);
        });
    }

    

    public static void mapAtlasClientSync(MapAtlasesSyncPacket packet, ClientPlayNetworking.Context context) {
        try {
            MapUpdateS2CPacket p = new MapUpdateS2CPacket(packet.mapId(), packet.scale(), packet.locked(), packet.decorations(), packet.updateData());
            MinecraftClient client = context.client();
            client.execute(() -> {
                client.getNetworkHandler().onMapUpdate(p);
            });
        } catch (ArrayIndexOutOfBoundsException e) {
            MapAtlasesMod.LOGGER.error("Bad Minecraft MapUpdate packet sent to client by server");
            MapAtlasesMod.LOGGER.error(e);
        }
    }
}
