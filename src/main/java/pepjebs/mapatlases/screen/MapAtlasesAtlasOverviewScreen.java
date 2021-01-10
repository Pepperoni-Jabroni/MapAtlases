package pepjebs.mapatlases.screen;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.IntLists;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapAtlasesAtlasOverviewScreen extends HandledScreen<ScreenHandler> {

    private static final Identifier MAP_CHKRBRD =
            new Identifier("minecraft:textures/map/map_background_checkerboard.png");

    private ItemStack atlas;
    public Map<Integer, List<Integer>> idsToCenters;

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
        // Draw map background as 5x5 grid of maps
        int y = 32;
        int x = (int) (client.getWindow().getScaledWidth() / 4.0);
        client.getTextureManager().bindTexture(MAP_CHKRBRD);
        drawTexture(matrices,x,y,0,0,200,200, 40, 40);
        // Draw maps, putting active map in middle of grid
        List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(client.world, atlas);
        MapState activeState = MapAtlasesAccessUtils.getActiveAtlasMapState(client.player.world, atlas);
        if (activeState == null) return;
        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                // Get the map for the GUI idx
                int reqXCenter = idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(activeState)).get(0) + (j * (1 << activeState.scale) * 128);
                int reqZCenter = idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(activeState)).get(1) + (i * (1 << activeState.scale) * 128);
                MapState state = mapStates.stream()
                        .filter(m -> idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(0) == reqXCenter
                                && idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromState(m)).get(1) == reqZCenter)
                        .findFirst().orElse(null);
                if (state == null) continue;
                // Draw the map
                x += (40 * (j + 2));
                y += (40 * (i + 2));
                x += 4;
                y += 4;
                VertexConsumerProvider.Immediate vcp;
                vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                matrices.push();
                matrices.translate(x, y, 0.0);
                matrices.scale(0.25f, 0.25f, 0);
                client.gameRenderer.getMapRenderer()
                        .draw(matrices, vcp, state, false, Integer.parseInt("0000000011110000", 2));
                vcp.draw();
                matrices.pop();
            }
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {

    }
}
