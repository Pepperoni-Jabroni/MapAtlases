package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public class MapAtlasesHUD extends DrawableHelper {

    public static final Identifier MAP_BACKGROUND =
            new Identifier("map_atlases:textures/gui/hud/map_background.png");
    public static final Identifier MAP_FOREGROUND =
            new Identifier("map_atlases:textures/gui/hud/map_foreground.png");
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
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
        // Check the player for an Atlas
        if (atlas.isEmpty()) return false;
        // Check the client has an active map id
        if (MapAtlasesClient.currentMapStateId == null) return false;
        // Check the active map id is in the active atlas
        return atlas.getNbt() != null && atlas.getNbt().contains(MapAtlasItem.MAP_LIST_NBT) &&
                Arrays.stream(atlas.getNbt().getIntArray(MapAtlasItem.MAP_LIST_NBT))
                        .anyMatch(i ->
                                i == MapAtlasesAccessUtils.getMapIntFromString(MapAtlasesClient.currentMapStateId));
    }

    private void renderMapHUD(MatrixStack matrices) {
        // Handle early returns
        if (client.world == null || client.player == null) {
            return;
        }
        String curMapId = MapAtlasesClient.currentMapStateId;
        MapState state = client.world.getMapState(MapAtlasesClient.currentMapStateId);
        if (state == null) return;
        // Update client current map id
        if (currentMapId == null || curMapId.compareTo(currentMapId) != 0) {
            if (currentMapId != null && currentMapId.compareTo("") != 0) {
                client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT, SoundCategory.PLAYERS,
                        MapAtlasesMod.CONFIG.soundScalar, 1.0F, false);
            }
            currentMapId = curMapId;
        }
        // Set zoom-level for map icons
        MapAtlasesClient.setWorldMapZoomLevel(MapAtlasesMod.CONFIG.miniMapIconScale);
        // Draw map background
        int mapBgScaledSize = (int)Math.floor(.2 * client.getWindow().getScaledHeight());
        if (MapAtlasesMod.CONFIG != null) {
            mapBgScaledSize = (int) Math.floor(
                    MapAtlasesMod.CONFIG.forceMiniMapScaling / 100.0 * client.getWindow().getScaledHeight());
        }
        double drawnMapBufferSize = mapBgScaledSize / 20.0;
        int mapDataScaledSize = (int) ((mapBgScaledSize - (2 * drawnMapBufferSize)));
        float mapDataScale = mapDataScaledSize / 128.0f;
        String anchorLocation = "UpperLeft";
        if (MapAtlasesMod.CONFIG != null) {
            anchorLocation = MapAtlasesMod.CONFIG.miniMapAnchoring;
        }
        int x = anchorLocation.contains("Left") ? 0 : client.getWindow().getScaledWidth() - mapBgScaledSize;
        int y = anchorLocation.contains("Lower") ? client.getWindow().getScaledHeight() - mapBgScaledSize : 0;
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
        drawTexture(matrices,x,y,0,0,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize);

        // Draw map data
        VertexConsumerProvider.Immediate vcp;
        vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        matrices.push();
        matrices.translate(x + drawnMapBufferSize, y + drawnMapBufferSize, 0.0);
        matrices.scale(mapDataScale, mapDataScale, -1);
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
        RenderSystem.setShaderTexture(0, MAP_FOREGROUND);
        drawTexture(matrices,x,y,0,0,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize);

        // Draw text data
        float textScaling = MapAtlasesMod.CONFIG.minimapCoordsAndBiomeScale;
        int textHeightOffset = mapBgScaledSize + 4;
        int textWidthOffset = mapBgScaledSize;
        if (anchorLocation.contains("Lower")) {
            textHeightOffset = (int) (-24 * textScaling);
        }
        if (MapAtlasesMod.CONFIG.drawMinimapCoords) {
            drawMapTextCoords(
                    matrices, x, y, textWidthOffset, textHeightOffset,
                    textScaling, new BlockPos(new Vec3i(towardsZero(client.player.getPos().x), towardsZero(client.player.getPos().y), towardsZero(client.player.getPos().z))));
            textHeightOffset += (12 * textScaling);
        }
        if (MapAtlasesMod.CONFIG.drawMinimapBiome) {
            drawMapTextBiome(
                    matrices, x, y, textWidthOffset, textHeightOffset,
                    textScaling, client.player.getBlockPos(), client.world);
        }
    }

    private static int towardsZero(double d) {
        if (d < 0.0)
            return -1 * (int) Math.floor(-1 * d);
        else
            return (int) Math.floor(d);
    }

    public static void drawMapTextCoords(
            MatrixStack matrices,
            int x,
            int y,
            int originOffsetWidth,
            int originOffsetHeight,
            float textScaling,
            BlockPos blockPos
    ) {
        String coordsToDisplay = blockPos.toShortString();
        drawScaledText(matrices, x, y, coordsToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    public static void drawMapTextBiome(
            MatrixStack matrices,
            int x,
            int y,
            int originOffsetWidth,
            int originOffsetHeight,
            float textScaling,
            BlockPos blockPos,
            World world
    ) {
        String biomeToDisplay = getBiomeStringToDisplay(world, blockPos);
        drawScaledText(matrices, x, y, biomeToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    public static void drawScaledText(
            MatrixStack matrices,
            int x,
            int y,
            String text,
            float textScaling,
            int originOffsetWidth,
            int originOffsetHeight
    ) {
        float textWidth = client.textRenderer.getWidth(text) * textScaling;
        float textX = (float) (x + (originOffsetWidth / 2.0) - (textWidth / 2.0));
        float textY = y + originOffsetHeight;
        if (textX + textWidth >= client.getWindow().getScaledWidth()) {
            textX = client.getWindow().getScaledWidth() - textWidth;
        }
        matrices.push();
        matrices.translate(textX, textY, 0);
        matrices.scale(textScaling, textScaling, 1);
        client.textRenderer.draw(matrices, text, 1, 1, Integer.parseInt("595959", 16));
        client.textRenderer.draw(matrices, text, 0, 0, Integer.parseInt("E0E0E0", 16));
        matrices.pop();
    }

    private static String getBiomeStringToDisplay(World world, BlockPos blockPos) {
        if (world == null || world.getBiome(blockPos).getKey().isEmpty())
            return "";
        RegistryKey<Biome> biomeKey = world.getBiome(blockPos).getKey().get();
        return I18n.translate(Util.createTranslationKey("biome", biomeKey.getValue()));
    }

}
