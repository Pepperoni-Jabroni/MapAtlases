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
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

@Environment(EnvType.CLIENT)
public class MapAtlasesHUD extends DrawableHelper {

    public static final Identifier MAP_BACKGROUND =
            new Identifier("map_atlases:textures/gui/hud/map_background.png");
    private static MinecraftClient client;
    private static MapRenderer mapRenderer;
    private static String currentMapId = "";

    public MapAtlasesHUD() {
        client = MinecraftClient.getInstance();
        mapRenderer = client.gameRenderer.getMapRenderer();
    }

    public void render(MatrixStack matrices) {
        if (shouldDraw(client)) {
            renderMapHUD(matrices);
        }
    }

    private boolean shouldDraw(MinecraftClient client) {
        if (client.player == null) return false;
        // Check config disable
        if (MapAtlasesMod.CONFIG != null && !MapAtlasesMod.CONFIG.drawMiniMapHUD) return false;
        // Check F3 menu displayed
        if (client.options.debugEnabled) return false;
        // Check the hot-bar for an Atlas
        return MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player) != ItemStack.EMPTY;
    }

    private void renderMapHUD(MatrixStack matrices) {
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
            mapScaling = (int) Math.floor(MapAtlasesMod.CONFIG.forceMiniMapScaling / 100.0 * client.getWindow().getScaledHeight());
        }
        String anchorLocation = "UpperLeft";
        if (MapAtlasesMod.CONFIG != null) {
            anchorLocation = MapAtlasesMod.CONFIG.miniMapAnchoring;
        }
        int x = anchorLocation.contains("Left") ? 0 : client.getWindow().getScaledWidth()-mapScaling;
        int y = anchorLocation.contains("Lower") ? client.getWindow().getScaledHeight()-mapScaling : 0;
        if (MapAtlasesMod.CONFIG != null) {
            x += MapAtlasesMod.CONFIG.miniMapHorizontalOffset;
            y += MapAtlasesMod.CONFIG.miniMapVerticalOffset;
        }
        if (anchorLocation.contentEquals("UpperRight")) {
            boolean hasBeneficial =
                    client.player.getStatusEffects().stream().anyMatch(p -> p.getEffectType().isBeneficial());
            boolean hasNegative =
                    client.player.getStatusEffects().stream().anyMatch(p -> !p.getEffectType().isBeneficial());
            int offsetForEffects = 26;

            if (MapAtlasesMod.CONFIG != null) {
                offsetForEffects = MapAtlasesMod.CONFIG.activePotionVerticalOffset;
            }
            if (hasNegative && y < 2 * offsetForEffects) {
                y += (2  * offsetForEffects - y);
            } else if (hasBeneficial && y < offsetForEffects) {
                y += (offsetForEffects - y);
            }
        }
        RenderSystem.setShaderTexture(0, MAP_BACKGROUND);
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
}
