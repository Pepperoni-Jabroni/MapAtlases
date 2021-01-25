package pepjebs.mapatlases.screen;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAtlasOverviewScreen extends HandledScreen<ScreenHandler> {

    private static final int ZOOM_BUCKET = 4;
    private static final int PAN_BUCKET = 25;

    private final ItemStack atlas;
    public Map<Integer, List<Integer>> idsToCenters;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int zoomValue = ZOOM_BUCKET;

    private Map<Integer, List<Double>> zoomMapping;

    public MapAtlasesAtlasOverviewScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        atlas = MapAtlasesAccessUtils.getAtlasFromPlayer(inventory);
        idsToCenters = ((MapAtlasesAtlasOverviewScreenHandler) handler).idsToCenters;
        zoomMapping = new HashMap<Integer, List<Double>>() {{
            // mapTextureTranslate, mapTextureScale
            put(1, Arrays.asList(1.0, 1.29));
            put(3, Arrays.asList(55.0, 0.43));
            put(5, Arrays.asList(33.0, 0.26));
            put(7, Arrays.asList(24.0, 0.19));
        }};
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawBackground(matrices, delta, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        if (client == null || client.player == null) return;
        // Handle zooming
        int zoomLevel = round(zoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zoomLevel = Math.max(zoomLevel, 0);
        zoomLevel = Math.min(zoomLevel, zoomMapping.size() - 1);
        int loopBegin = -1 * zoomLevel;
        int loopEnd = zoomLevel + 1;
        zoomLevel = (2 * zoomLevel) + 1;
        List<Double> zoomingInfo = zoomMapping.get(zoomLevel);
        int mapTextureTranslate = zoomingInfo.get(0).intValue();
        float mapTextureScale = zoomingInfo.get(1).floatValue();
        // Draw map background
        double y = (height - backgroundHeight) / 2.0;
        double x = (width - backgroundWidth) / 2.0;
        client.getTextureManager().bindTexture(MapAtlasesHUD.MAP_CHKRBRD);
        drawTexture(matrices, (int) x, (int) y,0,0, 180, 180, 180, 180);
        // Draw maps, putting active map in middle of grid
        List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(client.world, atlas);
        MapState activeState = client.world.getMapState(MapAtlasesClient.currentMapStateId);
        if (activeState == null) {
            if (!mapStates.isEmpty())
                activeState = mapStates.get(0);
            else
                return;
        }
        int activeMapId = MapAtlasesAccessUtils.getMapIntFromState(activeState);
        int activeXCenter = idsToCenters.get(activeMapId).get(0);
        int activeZCenter = idsToCenters.get(activeMapId).get(1);
        activeXCenter = activeXCenter +
                (round(mouseXOffset, PAN_BUCKET) / PAN_BUCKET * (1 << activeState.scale) * -128);
        activeZCenter = activeZCenter +
                (round(mouseYOffset, PAN_BUCKET) / PAN_BUCKET * (1 << activeState.scale) * -128);
        for (int i = loopBegin; i < loopEnd; i++) {
            for (int j = loopBegin; j < loopEnd; j++) {
                double mapTextY = (height - backgroundHeight) / 2.0 + 8;
                double mapTextX = (width - backgroundWidth) / 2.0 + 8;
                // Get the map for the GUI idx
                int reqXCenter = activeXCenter + (j * (1 << activeState.scale) * 128);
                int reqZCenter = activeZCenter + (i * (1 << activeState.scale) * 128);
                MapState state = mapStates.stream()
                        .filter(m -> idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(0) == reqXCenter
                                && idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(1) == reqZCenter)
                        .findFirst().orElse(null);
                if (state == null) continue;
                // Draw the map
                mapTextX += (mapTextureTranslate * (j + loopEnd - 1));
                mapTextY += (mapTextureTranslate * (i + loopEnd - 1));
                VertexConsumerProvider.Immediate vcp;
                vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                matrices.push();
                matrices.translate(mapTextX, mapTextY, 0.0);
                matrices.scale(mapTextureScale, mapTextureScale, 0);
                // Remove the off-map player icons temporarily during render
                Iterator<Map.Entry<String, MapIcon>> it = state.icons.entrySet().iterator();
                List<Map.Entry<String, MapIcon>> removed = new ArrayList<>();
                if (state.getId().compareTo(activeState.getId()) != 0) {
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
                        .draw(matrices, vcp, state, false, Integer.parseInt("0000000011110000", 2));
                vcp.draw();
                matrices.pop();
                // Re-add the off-map player icons after render
                for (Map.Entry<String, MapIcon> e : removed) {
                    state.icons.put(e.getKey(), e.getValue());
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
        zoomValue = Math.min(zoomValue, zoomMapping.size() * ZOOM_BUCKET);
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
