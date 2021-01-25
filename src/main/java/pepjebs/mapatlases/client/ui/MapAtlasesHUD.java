package pepjebs.mapatlases.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.MapRenderer;
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
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Map;

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
        PlayerInventory inv = client.player.inventory;
        // Check config disable
        if (MapAtlasesMod.CONFIG != null && !MapAtlasesMod.CONFIG.drawMiniMapHUD) return ItemStack.EMPTY;
        // Check the hot-bar for an Atlas
        return MapAtlasesAccessUtils.getAtlasFromPlayer(client.player.inventory);
    }

    private void renderMapHUDFromItemStack(MatrixStack matrices, ItemStack atlas) {
        if (client.world == null || MinecraftClient.getInstance().player == null) {
            MapAtlasesMod.LOGGER.warn("renderMapHUDFromItemStack: Current map id - null (client.world)");
            return;
        }
        MapState state = client.world.getMapState(MapAtlasesClient.currentMapStateId);
        if (state == null) {
            if (currentMapId != null) {
                MapAtlasesMod.LOGGER.warn("renderMapHUDFromItemStack: Current map id - null (state)");
                currentMapId = null;
            }
            return;
        }
        if (currentMapId == null || state.getId().compareTo(currentMapId) != 0) {
            if (currentMapId != null && currentMapId.compareTo("") != 0) {
                client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
            }
            currentMapId = state.getId();
        }
        // Draw map background
        int mapScaling = 64;
        if (MapAtlasesMod.CONFIG != null) {
            mapScaling = MapAtlasesMod.CONFIG.forceMiniMapScaling;
        }
        int y = 0;
        int x = client.getWindow().getScaledWidth()-mapScaling;
        client.getTextureManager().bindTexture(MAP_CHKRBRD);
        drawTexture(matrices,x,y,0,0,mapScaling,mapScaling, mapScaling, mapScaling);

        // Draw map data
        x += 4;
        y += 4;
        VertexConsumerProvider.Immediate vcp;
        vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        matrices.push();
        matrices.translate(x, y, 0.0);
        // Prepare yourself for some magic numbers
        matrices.scale((float) mapScaling / 142, (float) mapScaling / 142, 0);
        mapRenderer.draw(matrices, vcp, state, false, Integer.parseInt("0000000011110000", 2));
        vcp.draw();
        matrices.pop();
    }
}
