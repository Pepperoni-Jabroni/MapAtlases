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
        atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(inventory.main);
        idsToCenters = ((MapAtlasesAtlasOverviewScreenHandler) handler).idsToCenters;
        zoomMapping = new HashMap<Integer, List<Double>>() {{
            // backgroundSize, textureSize, mapTextureTranslate, mapTextureScale, mapTextureOffset
            put(1, Arrays.asList(160.0, 160.0, -1.0, 1.15, 6.0));
            put(3, Arrays.asList(210.0, 70.0, 70.0, 0.5, 3.0));
            put(5, Arrays.asList(200.0, 40.0, 40.0, 0.28, 2.0));
            put(7, Arrays.asList(175.0, 25.0, 25.0, 0.18, 1.5));
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
        int size = zoomingInfo.get(0).intValue();
        int textureSize = zoomingInfo.get(1).intValue();
        int mapTextureTranslate = zoomingInfo.get(2).intValue();
        float mapTextureScale = zoomingInfo.get(3).floatValue();
        double mapTextureOffset = zoomingInfo.get(4).intValue();
        // Draw map background
        double y = 32;
        double x = client.getWindow().getScaledWidth() / 4.0;
        client.getTextureManager().bindTexture(MapAtlasesHUD.MAP_CHKRBRD);
        drawTexture(matrices, (int) x, (int) y,0,0, size, size, textureSize, textureSize);
        // Draw maps, putting active map in middle of grid
        List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(client.world, atlas);
        MapState activeState = MapAtlasesAccessUtils.getActiveAtlasMapState(client.player.world, atlas);
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
                y = 32;
                x = client.getWindow().getScaledWidth() / 4.0;
                // Get the map for the GUI idx
                int reqXCenter = activeXCenter + (j * (1 << activeState.scale) * 128);
                int reqZCenter = activeZCenter + (i * (1 << activeState.scale) * 128);
                MapState state = mapStates.stream()
                        .filter(m -> idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(0) == reqXCenter
                                && idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(1) == reqZCenter)
                        .findFirst().orElse(null);
                if (state == null) continue;
                // Draw the map
                x += (mapTextureTranslate * (j + loopEnd - 1));
                y += (mapTextureTranslate * (i + loopEnd - 1));
                x += mapTextureOffset;
                y += mapTextureOffset;
                VertexConsumerProvider.Immediate vcp;
                vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                matrices.push();
                matrices.translate(x, y, 0.0);
                matrices.scale(mapTextureScale, mapTextureScale, 0);
                // Remove the off-map player icons temporarily during render
                Iterator<Map.Entry<String, MapIcon>> it = state.icons.entrySet().iterator();
                List<Map.Entry<String, MapIcon>> removed = new ArrayList<>();
                while (it.hasNext()) {
                    Map.Entry<String, MapIcon> e = it.next();
                    if (e.getValue().getType() == MapIcon.Type.PLAYER_OFF_MAP
                            || e.getValue().getType() == MapIcon.Type.PLAYER_OFF_LIMITS) {
                        it.remove();
                        removed.add(e);
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
