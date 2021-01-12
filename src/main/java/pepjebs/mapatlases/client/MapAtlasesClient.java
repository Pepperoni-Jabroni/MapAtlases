package pepjebs.mapatlases.client;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreen;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.state.MapAtlasesOpenGUIC2SPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.io.IOException;

public class MapAtlasesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, MapAtlasesAtlasOverviewScreen::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
        });

        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                (client, handler, buf, responseSender) -> {
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
