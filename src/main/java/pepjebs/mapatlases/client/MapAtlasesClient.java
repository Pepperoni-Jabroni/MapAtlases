package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.registry.Registry;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;

import java.util.stream.Collectors;

public class MapAtlasesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                (client, handler, buf, responseSender) -> {
                        MapAtlasesInitAtlasS2CPacket p = new MapAtlasesInitAtlasS2CPacket();
                        p.read(buf);
                        client.execute(() -> {
                            if (client.world == null) return;
                            MapState state = p.getMapState();
                            ItemStack atlas = client.player.inventory.main.stream()
                                    .filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
                            state.getPlayerSyncData(client.player);
                            MapAtlasesMod.LOGGER.info("Client updateTrackers #1: " + state.updateTrackers.size());
                            state.update(client.player, atlas);
                            client.world.putMapState(state);

                            // client.player.inventory.main is all air here
                            MapAtlasesMod.LOGGER.info("Client client.player.inventory.main.size: " + client.player.inventory.main.size());
                            MapAtlasesMod.LOGGER.info("Client client.player.inventory.main.set: " + client.player.inventory.main.stream().map(is -> Registry.ITEM.getId(is.getItem())).collect(Collectors.toSet()));
                            MapAtlasesMod.LOGGER.info("Client client.player.inventory.contains: " + client.player.inventory.contains(atlas));
                            MapAtlasesMod.LOGGER.info("Client atlas.getItem.getName: " + atlas.getItem().getName());
                            MapAtlasesMod.LOGGER.info("Client showIcons: " + state.showIcons);
                            MapAtlasesMod.LOGGER.info("Client updateTrackers #2: " + state.updateTrackers.size());
                            MapAtlasesMod.LOGGER.info("Client client.player.removed: " + client.player.removed);
                            MapAtlasesMod.LOGGER.info("Client client.player.world.getRegistryKey: " + client.player.world.getRegistryKey());
                            MapAtlasesMod.LOGGER.info("Client state.dimension: " + state.dimension);
                            MapAtlasesMod.LOGGER.info("Client client.player.removed: " + client.player.removed);
                            MapAtlasesMod.LOGGER.info("Client Map Icons: " + state.icons.values().stream().map(i -> i.getType()).collect(Collectors.toList()));
                            MapAtlasesMod.LOGGER.info("Client Received MapState: " + p.getMapState().getId());
                        });
                });
    }
}
