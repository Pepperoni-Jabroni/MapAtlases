package pepjebs.mapatlases;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.impl.networking.server.ServerPlayNetworkHandlerExtensions;
import net.fabricmc.fabric.mixin.container.ServerPlayerEntityAccessor;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.state.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.List;
import java.util.stream.Collectors;

public class MapAtlasesMod implements ModInitializer {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final MapAtlasItem MAP_ATLAS = new MapAtlasItem(new Item.Settings().group(ItemGroup.MISC).maxCount(1));

    public static SpecialRecipeSerializer<MapAtlasCreateRecipe> MAP_ATLAS_CREATE_RECIPE;
    public static SpecialRecipeSerializer<MapAtlasesAddRecipe> MAP_ATLAS_ADD_RECIPE;

    @Override
    public void onInitialize() {
        // Register special recipes
        MAP_ATLAS_CREATE_RECIPE = Registry.register(Registry.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "crafting_atlas"), new SpecialRecipeSerializer<>(MapAtlasCreateRecipe::new));
        MAP_ATLAS_ADD_RECIPE = Registry.register(Registry.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "adding_atlas"), new SpecialRecipeSerializer<>(MapAtlasesAddRecipe::new));

        // Register items
        Registry.register(Registry.ITEM, new Identifier(MOD_ID,"atlas"), MAP_ATLAS);

        // Register events/callbacks
        ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, minecraftServer) -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.player;
            World serverWorld = player.world;
            ItemStack atlas = player.inventory.main.stream()
                    .filter(is -> is.isItemEqual(new ItemStack(MAP_ATLAS))).findAny().orElse(ItemStack.EMPTY);
            if (atlas.isEmpty()) return;
            List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(serverWorld, atlas);
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
    }
}
