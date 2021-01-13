package pepjebs.mapatlases.lifecycle;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.state.MapAtlasesOpenGUIC2SPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.io.IOException;

public class MapAtlasesClientLifecycleEvents {

    public static void mapAtlasClientTick(MinecraftClient client) {
        while (MapAtlasesMod.displayMapGUIBinding.wasPressed()) {
            if (client.world == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(client.player.inventory.main);
            if (atlas.isEmpty()) return;
            MapAtlasesOpenGUIC2SPacket p = new MapAtlasesOpenGUIC2SPacket(atlas);
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            p.write(packetByteBuf);
            client.world.sendPacket(
                    new CustomPayloadC2SPacket(MapAtlasesOpenGUIC2SPacket.MAP_ATLAS_OPEN_GUI, packetByteBuf));
        }
    }

    public static void mapAtlasClientInit(
            MinecraftClient client,
            ClientPlayNetworkHandler _handler,
            PacketByteBuf buf,
            PacketSender _sender) {
        MapAtlasesInitAtlasS2CPacket p = new MapAtlasesInitAtlasS2CPacket();
        p.read(buf);
        client.execute(() -> {
            if (client.world == null || client.player == null) return;
            MapState state = p.getMapState();
            ItemStack atlas = client.player.inventory.main.stream()
                    .filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
            state.update(client.player, atlas);
            state.getPlayerSyncData(client.player);
            client.world.putMapState(state);
        });
    }

    public static void mapAtlasClientSync(
            MinecraftClient client,
            ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender _sender) {
        MapUpdateS2CPacket p = new MapUpdateS2CPacket();
        try {
            p.read(buf);
            client.execute(() -> {
                handler.onMapUpdate(p);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
