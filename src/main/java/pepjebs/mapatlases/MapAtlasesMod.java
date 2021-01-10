package pepjebs.mapatlases;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapAtlasesMod implements ModInitializer {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final MapAtlasItem MAP_ATLAS = new MapAtlasItem(new Item.Settings().group(ItemGroup.MISC).maxCount(1));

    public static SpecialRecipeSerializer<MapAtlasCreateRecipe> MAP_ATLAS_CREATE_RECIPE;
    public static SpecialRecipeSerializer<MapAtlasesAddRecipe> MAP_ATLAS_ADD_RECIPE;

    public static ScreenHandlerType<MapAtlasesAtlasOverviewScreenHandler> ATLAS_OVERVIEW_HANDLER;

    @Override
    public void onInitialize() {
        // Register special recipes
        MAP_ATLAS_CREATE_RECIPE = Registry.register(Registry.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "crafting_atlas"), new SpecialRecipeSerializer<>(MapAtlasCreateRecipe::new));
        MAP_ATLAS_ADD_RECIPE = Registry.register(Registry.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "adding_atlas"), new SpecialRecipeSerializer<>(MapAtlasesAddRecipe::new));

        ATLAS_OVERVIEW_HANDLER =
                ScreenHandlerRegistry.registerSimple(
                        new Identifier(MOD_ID, "atlas_overview"),
                        (syncId, inv) -> new MapAtlasesAtlasOverviewScreenHandler(syncId));

        // Register items
        Registry.register(Registry.ITEM, new Identifier(MOD_ID,"atlas"), MAP_ATLAS);

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
                MapAtlasesMod.LOGGER.info("Server Sent MapState: " + state.getId());
            }
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
                        while (p == null) {
                            p = state.getPlayerMarkerPacket(map, player.getServerWorld(), player);
                        }
                        PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                        try {
                            p.write(packetByteBuf);
                            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                                    MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                                    packetByteBuf));
                        } catch (IOException e) {
                            e.printStackTrace();
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
