package pepjebs.mapatlases.screen;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.List;
import java.util.Map;

public class MapAtlasesAtlasOverviewScreen extends HandledScreen<ScreenHandler> {

    private static final Identifier MAP_CHKRBRD =
            new Identifier("minecraft:textures/map/map_background_checkerboard.png");

    private final ItemStack atlas;
    public Map<Integer, List<Integer>> idsToCenters;
    private boolean printOnce = false;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;

    public MapAtlasesAtlasOverviewScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(inventory.main);
        idsToCenters = ((MapAtlasesAtlasOverviewScreenHandler) handler).idsToCenters;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawBackground(matrices, delta, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        if (client == null || client.player == null) return;
        // Draw map background as 3x3 grid of maps
        int y = 32;
        int x = (int) (client.getWindow().getScaledWidth() / 4.0);
        client.getTextureManager().bindTexture(MAP_CHKRBRD);
        drawTexture(matrices,x,y,0,0,210,210, 70, 70);
        // Draw maps, putting active map in middle of grid
        List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(client.world, atlas);
        MapState activeState = MapAtlasesAccessUtils.getActiveAtlasMapState(client.player.world, atlas);
        if (activeState == null) return;
        int activeMapId = MapAtlasesAccessUtils.getMapIntFromState(activeState);
        int activeXCenter = idsToCenters.get(activeMapId).get(0);
        int activeZCenter = idsToCenters.get(activeMapId).get(1);
        activeXCenter = activeXCenter + (round(mouseXOffset, 50) / 50 * (1 << activeState.scale) * -128);
        activeZCenter = activeZCenter + (round(mouseYOffset, 50) / 50 * (1 << activeState.scale) * -128);
        if (!printOnce) MapAtlasesMod.LOGGER.info(idsToCenters);
        for (int i = -1; i < 2; i++) {
            if (!printOnce) MapAtlasesMod.LOGGER.info("Column: " + i);
            for (int j = -1; j < 2; j++) {
                y = 32;
                x = (int) (client.getWindow().getScaledWidth() / 4.0);
                if (!printOnce) MapAtlasesMod.LOGGER.info("Row: " + j);
                // Get the map for the GUI idx
                int reqXCenter = activeXCenter + (j * (1 << activeState.scale) * 128);
                int reqZCenter = activeZCenter + (i * (1 << activeState.scale) * 128);
                if (!printOnce) MapAtlasesMod.LOGGER.info(reqXCenter + ", " + reqZCenter);
                MapState state = mapStates.stream()
                        .filter(m -> idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(0) == reqXCenter
                                && idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(1) == reqZCenter)
                        .findFirst().orElse(null);
                if (state == null) continue;
                // Draw the map
                x += (70 * (j + 1));
                y += (70 * (i + 1));
                x += 3;
                y += 3;
                if (!printOnce) MapAtlasesMod.LOGGER.info(state.getId() + ": " + x + ", " + y);
                VertexConsumerProvider.Immediate vcp;
                vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                matrices.push();
                matrices.translate(x, y, 0.0);
                matrices.scale(0.5f, 0.5f, 0);
                client.gameRenderer.getMapRenderer()
                        .draw(matrices, vcp, state, false, Integer.parseInt("0000000011110000", 2));
                vcp.draw();
                matrices.pop();
            }
        }
        printOnce = true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            mouseXOffset += deltaX;
            mouseYOffset += deltaY;
            MapAtlasesMod.LOGGER.info(mouseXOffset + ", " + mouseYOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private int round(int num, int mod) {
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }
}
