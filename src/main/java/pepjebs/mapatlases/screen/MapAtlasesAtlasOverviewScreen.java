package pepjebs.mapatlases.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.utils.MapStateIntrfc;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;

public class MapAtlasesAtlasOverviewScreen extends HandledScreen<ScreenHandler> {

    public static final Identifier ATLAS_FOREGROUND =
            new Identifier("map_atlases:textures/gui/screen/atlas_foreground.png");
    public static final Identifier ATLAS_BACKGROUND =
            new Identifier("map_atlases:textures/gui/screen/atlas_background.png");
    private static final int ZOOM_BUCKET = 4;
    private static final int PAN_BUCKET = 25;

    private final ItemStack atlas;
    public Map<Integer, List<Integer>> idsToCenters;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int zoomValue = ZOOM_BUCKET;

    public MapAtlasesAtlasOverviewScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        atlas = ((MapAtlasesAtlasOverviewScreenHandler) handler).atlas;
        idsToCenters = ((MapAtlasesAtlasOverviewScreenHandler) handler).idsToCenters;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawBackground(matrices, delta, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        if (client == null || client.player == null || client.world == null) return;
        // Handle zooming
        int atlasBgScaledSize = (int)Math.floor(.8 * client.getWindow().getScaledHeight());
        if (MapAtlasesMod.CONFIG != null) {
            atlasBgScaledSize = (int)Math.floor(
                    MapAtlasesMod.CONFIG.forceWorldMapScaling/100.0 * client.getWindow().getScaledHeight());
        }
        double drawnMapBufferSize = atlasBgScaledSize / 18.0;
        int atlasDataScaledSize = (int) (atlasBgScaledSize - (2 * drawnMapBufferSize));
        int zoomLevel = round(zoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zoomLevel = Math.max(zoomLevel, 0);
        int zoomLevelDim = (2 * zoomLevel) + 1;
        MapAtlasesClient.setWorldMapZoomLevel(zoomLevelDim);
        // a function of worldMapScaling, zoomLevel, and textureSize
        float mapTextureScale = (float)(atlasDataScaledSize/(128.0*zoomLevelDim));
        // Draw map background
        double y = (height / 2.0)-(atlasBgScaledSize/2.0);
        double x = (width / 2.0)-(atlasBgScaledSize/2.0);
        RenderSystem.setShaderTexture(0, ATLAS_BACKGROUND);
        drawTexture(
                matrices,
                (int) x,
                (int) y,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );
        // Draw maps, putting active map in middle of grid
        if (atlas == null) {
            MapAtlasesMod.LOGGER.warn("atlas == null");
            return;
        }
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(client.world, atlas);
        String activeMapIdStr = MapAtlasesClient.currentMapStateId;
        if (activeMapIdStr == null) {
            MapAtlasesMod.LOGGER.warn("activeMapIdStr == null");
        }
        MapState activeState = client.world.getMapState(activeMapIdStr);
        if (activeState == null) {
            if (!mapInfos.isEmpty()) {
                var info = mapInfos.entrySet().stream().findFirst().get();
                activeMapIdStr = info.getKey();
                activeState = info.getValue();
                MapAtlasesMod.LOGGER.warn("Using back-up active state on client: "+activeMapIdStr);
            } else if(!idsToCenters.isEmpty()) {
                double minDist = Double.MAX_VALUE;
                for (var id : idsToCenters.keySet()) {
                    String idStr = MapAtlasesAccessUtils.getMapStringFromInt(id);
                    MapState ms = MinecraftClient.getInstance().world.getMapState(idStr);
                    if (ms == null) {
                        MapAtlasesMod.LOGGER.warn("Couldn't find: "+idStr);
                        continue;
                    }
                    double distance = MapAtlasesAccessUtils.distanceBetweenMapStateAndPlayer(
                            ms, MinecraftClient.getInstance().player);
                    if (distance < minDist) {
                        minDist = distance;
                        activeMapIdStr = idStr;
                        activeState = ms;
                    }

                }
            }
        }
        if (activeState == null) {
            MapAtlasesMod.LOGGER.warn("Unable to find activeState, returning");
            return;
        }
        int activeMapId = MapAtlasesAccessUtils.getMapIntFromString(activeMapIdStr);
        int atlasScale = (1 << activeState.scale) * 128;
        if (!idsToCenters.containsKey(activeMapId)) {
            if (idsToCenters.isEmpty()) {
                MapAtlasesMod.LOGGER.warn("idsToCenters.isEmpty(), returning");
                return;
            }
            activeMapId = idsToCenters.keySet().stream().findAny().get();
        }
        double mapTextX = x + drawnMapBufferSize;
        double mapTextY = y + drawnMapBufferSize;
        int activeXCenter = idsToCenters.get(activeMapId).get(0) +
                (round(mouseXOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
        int activeZCenter = idsToCenters.get(activeMapId).get(1) +
                (round(mouseYOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
        int centerScreenXCenter = -1;
        int centerScreenZCenter = -1;
        for (int i = zoomLevelDim-1; i >= 0; i--) {
            for (int j = zoomLevelDim-1; j >= 0; j--) {
                int iXIdx = i-(zoomLevelDim/2);
                int jYIdx = j-(zoomLevelDim/2);
                int reqXCenter = activeXCenter + (jYIdx * atlasScale);
                int reqZCenter = activeZCenter + (iXIdx * atlasScale);
                if (i == (zoomLevelDim / 2)  && j == (zoomLevelDim / 2)) {
                    centerScreenXCenter = reqXCenter;
                    centerScreenZCenter = reqZCenter;
                }
                var state = findMapEntryForCenters(mapInfos, reqXCenter, reqZCenter);
                if (state == null) continue;
                if (!mapContainsMeaningfulIcons(state)) {
                    drawMap(matrices,i,j,state,activeMapId,mapTextX,mapTextY,mapTextureScale);
                }
            }
        }
        // draw maps without icons first
        // and then draw maps with icons (to avoid drawing over icons)
        for (int i = zoomLevelDim-1; i >= 0; i--) {
            for (int j = zoomLevelDim-1; j >= 0; j--) {
                int iXIdx = i-(zoomLevelDim/2);
                int jYIdx = j-(zoomLevelDim/2);
                int reqXCenter = activeXCenter + (jYIdx * atlasScale);
                int reqZCenter = activeZCenter + (iXIdx * atlasScale);
                var state = findMapEntryForCenters(mapInfos, reqXCenter, reqZCenter);
                if (state == null) continue;
                if (mapContainsMeaningfulIcons(state)) {
                    drawMap(matrices,i,j,state,activeMapId,mapTextX,mapTextY,mapTextureScale);
                }
            }
        }
        RenderSystem.setShaderTexture(0, ATLAS_FOREGROUND);
        drawTexture(
                matrices,
                (int) x,
                (int) y,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );
        if (mouseX < x + drawnMapBufferSize || mouseY < y + drawnMapBufferSize
                || mouseX > x + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > y + atlasBgScaledSize - drawnMapBufferSize)
            return;
        if (MapAtlasesMod.CONFIG == null || !MapAtlasesMod.CONFIG.drawWorldMapCoords) return;
        BlockPos cursorBlockPos = getBlockPosForCursor(
                mouseX,
                mouseY,
                atlasScale,
                zoomLevelDim,
                centerScreenXCenter,
                centerScreenZCenter,
                atlasBgScaledSize,
                x,
                y,
                drawnMapBufferSize
        );
        int targetHeight = atlasBgScaledSize + 4;
        if (MapAtlasesMod.CONFIG.forceWorldMapScaling >= 95) {
            targetHeight = 8;
        }
        drawMapTextXZCoords(
                matrices, (int) x, (int) y, atlasBgScaledSize,
                targetHeight, 1.0f, cursorBlockPos);
    }

    private BlockPos getBlockPosForCursor(
            int mouseX,
            int mouseY,
            int atlasScale,
            int zoomLevelDim,
            int centerScreenXCenter,
            int centerScreenZCenter,
            int atlasBgScaledSize,
            double x,
            double y,
            double buffer
    ) {
        double atlasMapsRelativeMouseX = mapRangeValueToAnother(
                mouseX, x + buffer, x + atlasBgScaledSize - buffer, -1.0, 1.0);
        double atlasMapsRelativeMouseZ = mapRangeValueToAnother(
                mouseY, y + buffer, y + atlasBgScaledSize - buffer, -1.0, 1.0);
        return new BlockPos(
                Math.floor(atlasMapsRelativeMouseX * zoomLevelDim * (atlasScale / 2.0)) + centerScreenXCenter,
                255,
                Math.floor(atlasMapsRelativeMouseZ * zoomLevelDim * (atlasScale / 2.0)) + centerScreenZCenter);
    }

    private boolean mapContainsMeaningfulIcons(Map.Entry<String, MapState> state) {
        return ((MapStateIntrfc) state.getValue()).getFullIcons().values().stream()
                .anyMatch(p -> p.getType() != MapIcon.Type.PLAYER_OFF_MAP
                    && p.getType() != MapIcon.Type.PLAYER_OFF_LIMITS);
    }

    private Map.Entry<String, MapState> findMapEntryForCenters(
            Map<String, MapState> mapInfos,
            int reqXCenter,
            int reqZCenter
    ) {
        // Get the map for the GUI idx
        return mapInfos.entrySet().stream()
                .filter(m ->
                        idsToCenters.containsKey(MapAtlasesAccessUtils.getMapIntFromString(m.getKey()))
                        && idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(m.getKey())).get(0) == reqXCenter
                        && idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(m.getKey())).get(1) == reqZCenter)
                .findFirst().orElse(null);
    }

    private void drawMap(
            MatrixStack matrices,
            int i,
            int j,
            Map.Entry<String, MapState> state,
            int activeMapId,
            double mapTextX,
            double mapTextY,
            float mapTextureScale
        ) {
        if (state == null || client == null) return;
        // Draw the map
        double curMapTextX = mapTextX + (mapTextureScale * 128 * j);
        double curMapTextY = mapTextY + (mapTextureScale * 128 * i);
        VertexConsumerProvider.Immediate vcp;
        vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        matrices.push();
        matrices.translate(curMapTextX, curMapTextY, 0.0);
        matrices.scale(mapTextureScale, mapTextureScale, -1);
        // Remove the off-map player icons temporarily during render
        Iterator<Map.Entry<String, MapIcon>> it = ((MapStateIntrfc) state.getValue())
                .getFullIcons().entrySet().iterator();
        List<Map.Entry<String, MapIcon>> removed = new ArrayList<>();
        if (state.getKey().compareTo(FilledMapItem.getMapName(activeMapId)) != 0) {
            // Only remove the off-map icon if it's not the active map
            while (it.hasNext()) {
                Map.Entry<String, MapIcon> e = it.next();
                if (e.getValue().getType() == MapIcon.Type.PLAYER_OFF_MAP
                        || e.getValue().getType() == MapIcon.Type.PLAYER_OFF_LIMITS) {
                    it.remove();
                    removed.add(e);
                }
            }
        }
        client.gameRenderer.getMapRenderer()
                .draw(
                        matrices,
                        vcp,
                        activeMapId,
                        state.getValue(),
                        false,
                        Integer.parseInt("F000F0", 16)
                );
        vcp.draw();
        matrices.pop();
        // Re-add the off-map player icons after render
        for (Map.Entry<String, MapIcon> e : removed) {
            ((MapStateIntrfc) state.getValue()).getFullIcons().put(e.getKey(), e.getValue());
        }
    }

    public static void drawMapTextXZCoords(
            MatrixStack matrices,
            int x,
            int y,
            int originOffsetWidth,
            int originOffsetHeight,
            float textScaling,
            BlockPos blockPos
    ) {
        String coordsToDisplay = "X: "+blockPos.getX()+", Z: "+blockPos.getZ();
        MapAtlasesHUD.drawScaledText(
                matrices, x, y, coordsToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            mouseXOffset += deltaX;
            mouseYOffset += deltaY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        zoomValue += -1 * amount;
        zoomValue = Math.max(zoomValue, -1 * ZOOM_BUCKET);
        return true;
    }

    private double mapRangeValueToAnother(
            double input, double inputStart, double inputEnd, double outputStart, double outputEnd) {
        double slope = (outputEnd - outputStart) / (inputEnd - inputStart);
        return outputStart + slope * (input - inputStart);
    }

    private int round(int num, int mod) {
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }
}
