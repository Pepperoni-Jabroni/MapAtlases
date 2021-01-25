package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientLifecycleEvents;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreen;
import pepjebs.mapatlases.networking.MapAtlasesInitAtlasS2CPacket;

public class MapAtlasesClient implements ClientModInitializer {

    public static KeyBinding displayMapGUIBinding;

    @Override
    public void onInitializeClient() {
        // Register client screen
        ScreenRegistry.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, MapAtlasesAtlasOverviewScreen::new);

        // Register client events
        ClientTickEvents.END_CLIENT_TICK.register(MapAtlasesClientLifecycleEvents::mapAtlasClientTick);
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                MapAtlasesClientLifecycleEvents::mapAtlasClientInit);
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                MapAtlasesClientLifecycleEvents::mapAtlasClientSync);

        // Register Keybind
        displayMapGUIBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.map_atlases.open_minimap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.map_atlases.minimap"
        ));
    }
}
