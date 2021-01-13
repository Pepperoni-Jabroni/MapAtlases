package pepjebs.mapatlases;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.*;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerLifecycleEvents;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.mapatlases.state.MapAtlasesOpenGUIC2SPacket;

public class MapAtlasesMod implements ModInitializer {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static MapAtlasItem MAP_ATLAS;
    public static boolean enableMultiDimMaps = false;

    public static SpecialRecipeSerializer<MapAtlasCreateRecipe> MAP_ATLAS_CREATE_RECIPE;
    public static SpecialRecipeSerializer<MapAtlasesAddRecipe> MAP_ATLAS_ADD_RECIPE;

    public static ScreenHandlerType<MapAtlasesAtlasOverviewScreenHandler> ATLAS_OVERVIEW_HANDLER;

    private static final Identifier ATLAS_OPEN_SOUND_ID = new Identifier(MOD_ID, "atlas_open");
    public static SoundEvent ATLAS_OPEN_SOUND_EVENT = new SoundEvent(ATLAS_OPEN_SOUND_ID);
    private static final Identifier ATLAS_PAGE_TURN_SOUND_ID = new Identifier(MOD_ID, "atlas_page_turn");
    public static SoundEvent ATLAS_PAGE_TURN_SOUND_EVENT = new SoundEvent(ATLAS_PAGE_TURN_SOUND_ID);
    private static final Identifier ATLAS_CREATE_MAP_SOUND_ID = new Identifier(MOD_ID, "atlas_create_map");
    public static SoundEvent ATLAS_CREATE_MAP_SOUND_EVENT = new SoundEvent(ATLAS_CREATE_MAP_SOUND_ID);

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

        // Register sounds
        Registry.register(Registry.SOUND_EVENT, ATLAS_OPEN_SOUND_ID, ATLAS_OPEN_SOUND_EVENT);
        Registry.register(Registry.SOUND_EVENT, ATLAS_PAGE_TURN_SOUND_ID, ATLAS_PAGE_TURN_SOUND_EVENT);
        Registry.register(Registry.SOUND_EVENT, ATLAS_CREATE_MAP_SOUND_ID, ATLAS_CREATE_MAP_SOUND_EVENT);

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
        ServerPlayConnectionEvents.JOIN.register(MapAtlasesServerLifecycleEvents::mapAtlasPlayerJoin);
        ServerPlayNetworking.registerGlobalReceiver(MapAtlasesOpenGUIC2SPacket.MAP_ATLAS_OPEN_GUI,
                MapAtlasesServerLifecycleEvents::openGuiEvent);
        ServerTickEvents.START_SERVER_TICK.register(MapAtlasesServerLifecycleEvents::mapAtlasServerTick);
    }
}
