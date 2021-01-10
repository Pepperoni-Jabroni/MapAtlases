package pepjebs.mapatlases.screen;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class MapAtlasesAtlasOverviewScreen extends HandledScreen<ScreenHandler> {

    private static final Identifier MAP_CHKRBRD = new Identifier("minecraft:textures/map/map_background_checkerboard.png");

    public MapAtlasesAtlasOverviewScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawBackground(matrices, delta, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        // Draw map background
        int y = 32;
        int x = (int) (client.getWindow().getScaledWidth() / 4.0);
        client.getTextureManager().bindTexture(MAP_CHKRBRD);
        drawTexture(matrices,x,y,0,0,200,200, 50, 50);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {

    }
}
