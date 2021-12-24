package pepjebs.mapatlases.screen;

import com.mojang.blaze3d.systems.RenderSystem;
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
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.mixin.MapRendererMixin;
import pepjebs.mapatlases.utils.MapStateIntrfc;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;

public class MapAtlasesAtlasOverviewScreen extends HandledScreen<ScreenHandler> {

    private static final int ZOOM_BUCKET = 4;
    private static final int PAN_BUCKET = 25;
    public static final ThreadLocal<Integer> worldMapZoomLevel = new ThreadLocal<>();

    private final ItemStack atlas;
    public Map<Integer, List<Integer>> idsToCenters;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int zoomValue = ZOOM_BUCKET;

    public MapAtlasesAtlasOverviewScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(inventory);
        idsToCenters = ((MapAtlasesAtlasOverviewScreenHandler) handler).idsToCenters;
    }

    public static int getWorldMapZoomLevel() {
        if (worldMapZoomLevel.get() == null) return 1;
        return worldMapZoomLevel.get();
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
        int worldMapScaling = MapAtlasesMod.CONFIG.forceWorldMapScaling;
        // zoomLevel can be any of 0,1,2,3
        int zoomLevel = round(zoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zoomLevel = Math.max(zoomLevel, 0);
        zoomLevel = Math.min(zoomLevel, 3);
        // zoomLevelDim can be any of 1,3,5,7
        int zoomLevelDim = (2 * zoomLevel) + 1;
        worldMapZoomLevel.set(zoomLevelDim);
        // a function of worldMapScaling, zoomLevel, and textureSize
        float mapTextureScale = (float)((worldMapScaling-(worldMapScaling/8.0))/(128.0*zoomLevelDim));
        // Draw map background
        double y = (height / 2.0)-(worldMapScaling/2.0);
        double x = (width / 2.0)-(worldMapScaling/2.0);
        RenderSystem.setShaderTexture(0, MapAtlasesHUD.MAP_CHKRBRD);
        drawTexture(
                matrices,
                (int) x,
                (int) y,
                0,
                0,
                worldMapScaling,
                worldMapScaling,
                worldMapScaling,
                worldMapScaling
        );
        // Draw maps, putting active map in middle of grid
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(client.world, atlas);
        MapState activeState = client.world.getMapState(MapAtlasesClient.currentMapStateId);
        ItemStack activeFilledMap = MapAtlasesAccessUtils.createMapItemStackFromStrId(MapAtlasesClient.currentMapStateId);
        if (activeState == null) {
            if (!mapInfos.isEmpty())
                activeState = mapInfos.entrySet().stream().findFirst().get().getValue();
            else
                return;
        }
        int activeMapId = FilledMapItem.getMapId(activeFilledMap);
        if (!idsToCenters.containsKey(activeMapId)) {
            MapAtlasesMod.LOGGER.warn("Client didn't have idsToCenters entry.");
            if (idsToCenters.isEmpty())
                return;
            activeMapId = idsToCenters.keySet().stream().findAny().get();
        }
        int activeXCenter = idsToCenters.get(activeMapId).get(0);
        int activeZCenter = idsToCenters.get(activeMapId).get(1);
        activeXCenter = activeXCenter +
                (round(mouseXOffset, PAN_BUCKET) / PAN_BUCKET * (1 << activeState.scale) * -128);
        activeZCenter = activeZCenter +
                (round(mouseYOffset, PAN_BUCKET) / PAN_BUCKET * (1 << activeState.scale) * -128);
        double mapTextY = y+(worldMapScaling/18.0);
        double mapTextX = x+(worldMapScaling/18.0);
        for (int i = zoomLevelDim-1; i >= 0; i--) {
            for (int j = zoomLevelDim-1; j >= 0; j--) {
                // Get the map for the GUI idx
                int iXIdx = i-(zoomLevelDim/2);
                int jYIdx = j-(zoomLevelDim/2);
                int reqXCenter = activeXCenter + (jYIdx * (1 << activeState.scale) * 128);
                int reqZCenter = activeZCenter + (iXIdx * (1 << activeState.scale) * 128);
                Map.Entry<String, MapState> state = mapInfos.entrySet().stream()
                        .filter(m -> idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(m.getKey())).get(0) == reqXCenter
                                && idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(m.getKey())).get(1) == reqZCenter)
                        .findFirst().orElse(null);
                if (state == null) continue;
                // Draw the map
                double curMapTextX = mapTextX + (mapTextureScale * 128 * j);
                double curMapTextY = mapTextY + (mapTextureScale * 128 * i);
                VertexConsumerProvider.Immediate vcp;
                vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                matrices.push();
                matrices.translate(curMapTextX, curMapTextY, 0.0);
                matrices.scale(mapTextureScale, mapTextureScale, 0);
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
                                Integer.parseInt("0000000011110000", 2)
                        );
                vcp.draw();
                matrices.pop();
                // Re-add the off-map player icons after render
                for (Map.Entry<String, MapIcon> e : removed) {
                    ((MapStateIntrfc) state.getValue()).getFullIcons().put(e.getKey(), e.getValue());
                }
            }
        }
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
        zoomValue = Math.min(zoomValue, 4 * ZOOM_BUCKET);
        return true;
    }

    private int round(int num, int mod) {
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }
}
