package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

@Environment(EnvType.CLIENT)
public class MapAtlasesHUD extends DrawableHelper {

    public static final Identifier MAP_CHKRBRD =
            new Identifier("minecraft:textures/map/map_background_checkerboard.png");
    private static MinecraftClient client;
    private static MapRenderer mapRenderer;
    private static String currentMapId = "";

    public MapAtlasesHUD() {
        client = MinecraftClient.getInstance();
        mapRenderer = client.gameRenderer.getMapRenderer();
    }

    public void render(MatrixStack matrices) {
        ItemStack atlas;
        if (!(atlas = shouldDraw(client)).isEmpty()) {
            renderMapHUDFromItemStack(matrices, atlas);
        }
    }

    private ItemStack shouldDraw(MinecraftClient client) {
        // Forcibly only render on Overworld since player trackers don't disappear from Overworld
        // in other dimensions in vanilla MC
        if (client.player == null || client.player.world.getRegistryKey() != World.OVERWORLD) return ItemStack.EMPTY;
        PlayerInventory inv = client.player.getInventory();
        // Check config disable
        if (MapAtlasesMod.CONFIG != null && !MapAtlasesMod.CONFIG.drawMiniMapHUD) return ItemStack.EMPTY;
        // Check F3 menu displayed
        if (client.options.debugEnabled) return ItemStack.EMPTY;
        // Check the hot-bar for an Atlas
        return MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
    }

    private void renderMapHUDFromItemStack(MatrixStack matrices, ItemStack atlas) {
        // Handle early returns
        if (client.world == null || client.player == null) {
            MapAtlasesMod.LOGGER.warn("renderMapHUDFromItemStack: Current map id - null (client.world)");
            return;
        }
        String curMapId = MapAtlasesClient.currentMapStateId;
        MapState state = client.world.getMapState(MapAtlasesClient.currentMapStateId);
        if (curMapId == null || state == null) {
            if (currentMapId != null) {
                MapAtlasesMod.LOGGER.warn("renderMapHUDFromItemStack: Current map id - null (state)");
                currentMapId = null;
            }
            return;
        }
        // Update client current map id
        if (currentMapId == null || curMapId.compareTo(currentMapId) != 0) {
            if (currentMapId != null && currentMapId.compareTo("") != 0) {
                client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
            }
            currentMapId = curMapId;
        }
        // Set zoom-level for map icons
        MapAtlasesClient.setWorldMapZoomLevel(1);
        // Draw map background
        int mapScaling = (int)Math.floor(.2 * client.getWindow().getScaledHeight());
        if (MapAtlasesMod.CONFIG != null) {
            mapScaling = (int)Math.floor(MapAtlasesMod.CONFIG.forceMiniMapScaling/100.0 * client.getWindow().getScaledHeight());
        }
        int y = 0;
        if (!client.player.getStatusEffects().isEmpty()) {
            y = 26;
        }
        if (MapAtlasesMod.CONFIG.miniMapAnchoring.contains("Lower")) {
            y = client.getWindow().getScaledHeight() - mapScaling;
        }
        y += MapAtlasesMod.CONFIG.miniMapVerticalOffset;
        int x = client.getWindow().getScaledWidth()-mapScaling;
        if (MapAtlasesMod.CONFIG.miniMapAnchoring.contains("Left")) {
            x = 0;
        }
        x += MapAtlasesMod.CONFIG.miniMapHorizontalOffset;
        RenderSystem.setShaderTexture(0, MAP_CHKRBRD);
        drawTexture(matrices,x,y,0,0,mapScaling,mapScaling, mapScaling, mapScaling);

        // Draw map data
        x += (mapScaling / 16) - (mapScaling / 64);
        y += (mapScaling / 16) - (mapScaling / 64);
        VertexConsumerProvider.Immediate vcp;
        vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        matrices.push();
        matrices.translate(x, y, 0.0);
        // Prepare yourself for some magic numbers
        matrices.scale((float) mapScaling / 142, (float) mapScaling / 142, -1);
        mapRenderer.draw(
                matrices,
                vcp,
                MapAtlasesAccessUtils.getMapIntFromString(curMapId),
                state,
                false,
                Integer.parseInt("F000F0", 16)
        );
        vcp.draw();
        matrices.pop();
    }

    private double mapRange(double a1, double a2, double b1, double b2, double s){
        return b1 + ((s - a1)*(b2 - b1))/(a2 - a1);
    }
}
