package pepjebs.mapatlases;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.HeldItemChangeS2CPacket;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.state.MapAtlasesOpenGUIC2SPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapAtlasesMod implements ModInitializer {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static MapAtlasItem MAP_ATLAS;
    public static boolean enableMultiDimMaps = false;

    public static SpecialRecipeSerializer<MapAtlasCreateRecipe> MAP_ATLAS_CREATE_RECIPE;
    public static SpecialRecipeSerializer<MapAtlasesAddRecipe> MAP_ATLAS_ADD_RECIPE;

    public static ScreenHandlerType<MapAtlasesAtlasOverviewScreenHandler> ATLAS_OVERVIEW_HANDLER;

    public static KeyBinding displayMapGUIBinding;

    @Override
    public void onInitialize() {
        // Register special recipes
        MAP_ATLAS_CREATE_RECIPE = Registry.register(Registry.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "crafting_atlas"), new SpecialRecipeSerializer<>(MapAtlasCreateRecipe::new));
        MAP_ATLAS_ADD_RECIPE = Registry.register(Registry.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "adding_atlas"), new SpecialRecipeSerializer<>(MapAtlasesAddRecipe::new));

        ATLAS_OVERVIEW_HANDLER =
                ScreenHandlerRegistry.registerExtended(
                        new Identifier(MOD_ID, "atlas_overview"),
                        MapAtlasesAtlasOverviewScreenHandler::new);

        // Register items
        Registry.register(Registry.ITEM, new Identifier(MOD_ID,"atlas"),
                new MapAtlasItem(new Item.Settings().group(ItemGroup.MISC).maxCount(1)));
        if (enableMultiDimMaps) {
            Registry.register(Registry.ITEM, new Identifier(MOD_ID,"end_atlas"),
                    new MapAtlasItem(new Item.Settings().group(ItemGroup.MISC).maxCount(1)));
            Registry.register(Registry.ITEM, new Identifier(MOD_ID,"nether_atlas"),
                    new MapAtlasItem(new Item.Settings().group(ItemGroup.MISC).maxCount(1)));
        }
        MAP_ATLAS = (MapAtlasItem) Registry.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "atlas"));

        // Register Keybind
        displayMapGUIBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
           "key.map_atlases.open_minimap",
           InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.map_atlases.minimap"
        ));

        // Register events/callbacks
        ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, minecraftServer) -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.player;
            ItemStack atlas = player.inventory.main.stream()
                    .filter(is -> is.isItemEqual(new ItemStack(MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
            if (atlas.isEmpty()) return;
            List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(player.getServerWorld(), atlas);
            for (MapState state : mapStates) {
                state.update(player, atlas);
                state.getPlayerSyncData(player);
                PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                (new MapAtlasesInitAtlasS2CPacket(state)).write(packetByteBuf);
                player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                        MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                        packetByteBuf));
                MapAtlasesMod.LOGGER.info("Server Sent MapState: " + state.getId());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(MapAtlasesOpenGUIC2SPacket.MAP_ATLAS_OPEN_GUI,
                (server, player, handler, buf, responseSender) -> {
                    MapAtlasesOpenGUIC2SPacket p = new MapAtlasesOpenGUIC2SPacket();
                    p.read(buf);
                    server.execute(() -> {
                        ItemStack atlas = p.atlas;
                        int atlasIdx = 40;
                        for (int i = 0; i < player.inventory.main.size(); i++) {
                            if (player.inventory.main.get(i).getItem() == atlas.getItem() &&
                                    player.inventory.main.get(i).getTag() != null &&
                                    atlas.getTag() != null &&
                                    player.inventory.main.get(i).getTag().toString().compareTo(atlas.getTag().toString()) == 0) {
                                atlasIdx = i;
                                break;
                            }
                        }
                        if (atlasIdx < 9) {
                            player.inventory.selectedSlot = atlasIdx;
                            player.networkHandler.sendPacket(new HeldItemChangeS2CPacket(atlasIdx));
                            player.openHandledScreen((MapAtlasItem) atlas.getItem());
                        }
                    });
                });
        ServerTickEvents.START_SERVER_TICK.register((server) -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack atlas = player.inventory.main.stream()
                        .filter(is -> is.isItemEqual(new ItemStack(MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
                if (!atlas.isEmpty()) {
                    List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(player.getServerWorld(), atlas);

                    // Maps are 128x128
                    int playX = player.getBlockPos().getX();
                    int playZ = player.getBlockPos().getZ();
                    int minDist = Integer.MAX_VALUE;
                    int scale = -1;

                    for (MapState state : mapStates) {
                        state.update(player, atlas);
                        ((FilledMapItem) Items.FILLED_MAP).updateColors(player.getServerWorld(), player, state);
                        ItemStack map = MapAtlasesAccessUtils.createMapItemStackFromStrId(state.getId());
                        Packet<?> p = null;
                        int tries = 0;
                        while (p == null && tries < 10) {
                            p = state.getPlayerMarkerPacket(map, player.getServerWorld(), player);
                            tries++;
                        }
                        if (p != null) {
                            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                            try {
                                p.write(packetByteBuf);
                                player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                                        MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                                        packetByteBuf));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        int mapCX = state.xCenter;
                        int mapCZ = state.zCenter;
                        minDist = Math.min(minDist, (int) Math.hypot(playX-mapCX, playZ-mapCZ));
                        scale = state.scale;
                    }

                    if (minDist != Integer.MAX_VALUE && scale != -1 && minDist > (80 * (1 << scale))) {
                        int emptyCount = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
                        if (atlas.getTag() != null && emptyCount > 0) {
                            atlas.getTag().putInt("empty", atlas.getTag().getInt("empty") - 1);
                            ItemStack newMap = FilledMapItem.createMap(
                                    player.getServerWorld(),
                                    MathHelper.floor(player.getX()),
                                    MathHelper.floor(player.getZ()),
                                    (byte) scale,
                                    true,
                                    false);
                            List<Integer> mapIds = Arrays.stream(
                                    atlas.getTag().getIntArray("maps")).boxed().collect(Collectors.toList());
                            mapIds.add(FilledMapItem.getMapId(newMap));
                            atlas.getTag().putIntArray("maps", mapIds);
                        }
                    }
                }
            }
        });
    }
}
