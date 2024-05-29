package pepjebs.mapatlases.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.utils.MapStateIntrfc;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import static pepjebs.mapatlases.utils.MapAtlasesAccessUtils.getMapStateDimKey;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

// TODO: If the atlas world map scaling changes, MAX_TAB_DISP needs to change too
// TODO: Map Icon Selectors don't look right at non-default scaling
public class MapAtlasesAtlasOverviewScreen extends HandledScreen<ScreenHandler> {

    public static final Identifier ATLAS_FOREGROUND =
            new Identifier("map_atlases:textures/gui/screen/atlas_foreground.png");
    public static final Identifier ATLAS_BACKGROUND =
            new Identifier("map_atlases:textures/gui/screen/atlas_background.png");
    public static final Identifier PAGE_SELECTED =
            new Identifier("map_atlases:textures/gui/screen/page_selected.png");
    public static final Identifier PAGE_UNSELECTED =
            new Identifier("map_atlases:textures/gui/screen/page_unselected.png");
    public static final Identifier PAGE_OVERWORLD =
            new Identifier("map_atlases:textures/gui/screen/overworld_atlas_page.png");
    public static final Identifier PAGE_NETHER =
            new Identifier("map_atlases:textures/gui/screen/nether_atlas_page.png");
    public static final Identifier PAGE_END =
            new Identifier("map_atlases:textures/gui/screen/end_atlas_page.png");
    public static final Identifier PAGE_OTHER =
            new Identifier("map_atlases:textures/gui/screen/unknown_atlas_page.png");
    public static final Identifier MAP_ICON_TEXTURE = new Identifier("textures/map/map_icons.png");
    private static final RenderLayer MAP_ICONS = RenderLayer.getText(MAP_ICON_TEXTURE);
    private static final int ZOOM_BUCKET = 4;
    private static final int PAN_BUCKET = 25;
    private static final int MAX_TAB_DISP = 7;

    private final ItemStack atlas;
    public final String centerMapId;
    public Map<Integer, Pair<String,List<Integer>>> idsToCenters;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int currentXCenter;
    private int currentZCenter;
    private double rawMouseXMoved = 0;
    private double rawMouseYMoved = 0;
    private int zoomValue = ZOOM_BUCKET;
    private String currentWorldSelected;
    private final String initialWorldSelected;
    private final int atlasScale;
    private int mapIconSelectorOffset = 0;
    private int dimSelectorOffset = 0;

    public MapAtlasesAtlasOverviewScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        atlas = ((MapAtlasesAtlasOverviewScreenHandler) handler).atlas;
        idsToCenters = ((MapAtlasesAtlasOverviewScreenHandler) handler).idsToCenters;
        centerMapId = ((MapAtlasesAtlasOverviewScreenHandler) handler).centerMapId;
        atlasScale = ((MapAtlasesAtlasOverviewScreenHandler) handler).atlasScale;
        var coords = idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(centerMapId)).getSecond();
        currentXCenter = coords.get(0);
        currentZCenter = coords.get(1);
        currentWorldSelected = MapAtlasesAccessUtils.getPlayerDimKey(inventory.player);
        initialWorldSelected = MapAtlasesAccessUtils.getPlayerDimKey(inventory.player);
        // Play open sound
        inventory.player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT, MapAtlasesMod.CONFIG.soundScalar, 1.0F);
    }

    private record MapInfo(int id, String dimension, int centerX, int centerZ, MapState state) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        drawBackground(context, delta, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        if (client == null || client.player == null || client.world == null) return;
        var matrices = context.getMatrices();

        // Handle zooming
        int atlasBgScaledSize = getAtlasBgScaledSize();
        double drawnMapBufferSize = atlasBgScaledSize / 18.0;
        int atlasDataScaledSize = (int) (atlasBgScaledSize - (2 * drawnMapBufferSize));
        int zoomLevelDim = getZoomLevelDim();
        MapAtlasesClient.setWorldMapZoomLevel(zoomLevelDim * MapAtlasesMod.CONFIG.worldMapIconScale);
        float mapTextureScale = (float)(atlasDataScaledSize/(128.0*zoomLevelDim));
        Vector2i centerTile = BlockPosToMapTile(currentXCenter, currentZCenter, atlasScale);
        Rect2i tileRegion = new Rect2i(
            centerTile.x - (zoomLevelDim / 2),
            centerTile.y - (zoomLevelDim / 2),
            zoomLevelDim,
            zoomLevelDim
        );

        // Draw map background
        double y = (height / 2.0)-(atlasBgScaledSize/2.0);
        double x = (width / 2.0)-(atlasBgScaledSize/2.0);
        RenderSystem.setShaderTexture(0, ATLAS_BACKGROUND);
        context.drawTexture(
                ATLAS_BACKGROUND,
                (int) x,
                (int) y,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );

        // Draw selectors
        drawDimensionSelectors(context, x, y, atlasBgScaledSize);
        drawMapIconSelectors(context, x, y, atlasBgScaledSize);

        // Draw maps, putting active map in middle of grid
        if (atlas == null) {
            return;
        }
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(client.world, atlas);
        MapInfo[][] mapGrid = CreateMapGrid(mapInfos, currentWorldSelected, tileRegion);
        double mapTextX = x + drawnMapBufferSize;
        double mapTextY = y + drawnMapBufferSize;
        for (int gridX=zoomLevelDim-1; gridX>=0; --gridX)
        for (int gridY=zoomLevelDim-1; gridY>=0; --gridY)
        {
            MapInfo info = mapGrid[gridX][gridY];
            if (info != null && !mapContainsMeaningfulIcons(info.state)) {
                boolean drawPlayerIcons = initialWorldSelected.equals(info.dimension);
                drawMap(matrices,mapGrid,gridX,gridY,mapTextX,mapTextY,mapTextureScale, drawPlayerIcons);
            }
        }
        // draw maps without icons first
        // and then draw maps with icons (to avoid drawing over icons)
        for (int gridX=zoomLevelDim-1; gridX>=0; --gridX)
        for (int gridY=zoomLevelDim-1; gridY>=0; --gridY)
        {
            MapInfo info = mapGrid[gridX][gridY];
            if (info != null && mapContainsMeaningfulIcons(info.state)) {
                boolean drawPlayerIcons = initialWorldSelected.equals(info.dimension);
                drawMap(matrices,mapGrid,gridX,gridY,mapTextX,mapTextY,mapTextureScale, drawPlayerIcons);
            }
        }

        MapAtlasesClient.setWorldMapZoomLevel(1);

        // Draw foreground
        RenderSystem.setShaderTexture(0, ATLAS_FOREGROUND);
        context.drawTexture(
                ATLAS_FOREGROUND,
                (int) x,
                (int) y,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );

        // Draw tooltips if necessary
        drawDimensionTooltip(context, x, y, atlasBgScaledSize);
        drawMapIconTooltip(context, x, y, atlasBgScaledSize);

        // Draw world map coords
        if (mouseX < x + drawnMapBufferSize || mouseY < y + drawnMapBufferSize
                || mouseX > x + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > y + atlasBgScaledSize - drawnMapBufferSize)
            return;
        if (MapAtlasesMod.CONFIG == null || !MapAtlasesMod.CONFIG.drawWorldMapCoords) return;
        BlockPos cursorBlockPos = getBlockPosForCursor(
                mouseX,
                mouseY,
                zoomLevelDim,
                currentXCenter,
                currentZCenter,
                atlasBgScaledSize,
                x,
                y,
                drawnMapBufferSize
        );
        int targetHeight = atlasBgScaledSize + 4;
        if (MapAtlasesMod.CONFIG.forceWorldMapScaling >= 95) {
            targetHeight = 8;
        }
        float textScaling = MapAtlasesMod.CONFIG.worldMapCoordsScale;
        drawMapTextXZCoords(context, (int) x, (int) y, atlasBgScaledSize, targetHeight, textScaling, cursorBlockPos);
    }

    // ================== Mouse Functions ==================

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            mouseXOffset += deltaX;
            mouseYOffset += deltaY;
            int targetXCenter = currentXCenter + (round(mouseXOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
            int targetZCenter = currentZCenter + (round(mouseYOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
            if (targetXCenter != currentXCenter || targetZCenter != currentZCenter) {
                currentXCenter = targetXCenter;
                currentZCenter = targetZCenter;
                mouseXOffset = 0;
                mouseYOffset = 0;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Handle dim selector scroll
        var dims =
                idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
        int atlasBgScaledSize = getAtlasBgScaledSize();
        double x = (width / 2.0)-(atlasBgScaledSize/2.0);
        double y = (height / 2.0)-(atlasBgScaledSize/2.0);
        int scaledWidth = calcScaledWidth(100);
        int targetX = (int) x + (int) (29.5/32.0 * atlasBgScaledSize);
        if (mouseX >= targetX && mouseX <= targetX + scaledWidth) {
            dimSelectorOffset =
                    Math.max(0, Math.min(dims.size() - MAX_TAB_DISP, dimSelectorOffset + (verticalAmount > 0 ? -1 : 1)));
            return true;
        }
        // Handle map icon selector scroll
        var mapList = getMapIconList();
        targetX = (int) x - (int) (1.0/16 * atlasBgScaledSize);
        if (mouseX >= targetX && mouseX <= targetX + scaledWidth) {
            mapIconSelectorOffset =
                    Math.max(0, Math.min(mapList.size() - MAX_TAB_DISP, mapIconSelectorOffset + (verticalAmount > 0 ? -1 : 1)));
            return true;
        }
        // Handle world map zooming
        double drawnMapBufferSize = atlasBgScaledSize / 18.0;
        if (mouseX < x + drawnMapBufferSize || mouseY < y + drawnMapBufferSize
                || mouseX > x + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > y + atlasBgScaledSize - drawnMapBufferSize)
            return true;
        zoomValue += (int) (-1 * verticalAmount);
        zoomValue = Math.max(zoomValue, -1 * ZOOM_BUCKET);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        rawMouseXMoved = mouseX;
        rawMouseYMoved = mouseY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (client == null || client.player == null) return false;
        if (button == 0) {
            var dims =
                    idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
            int atlasBgScaledSize = getAtlasBgScaledSize();
            double x = (width / 2.0)-(atlasBgScaledSize/2.0);
            double y = (height / 2.0)-(atlasBgScaledSize/2.0);
            int scaledWidth = calcScaledWidth(100);
            for (int i = 0; i < MAX_TAB_DISP; i++) {
                int targetX = (int) x + (int) (29.5/32.0 * atlasBgScaledSize);
                int targetY = (int) y +
                        (int) (i * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize);
                if (mouseX >= targetX && mouseX <= targetX + scaledWidth
                        && mouseY >= targetY && mouseY <= targetY + scaledWidth) {
                    int targetIdx = dimSelectorOffset + i;
                    if (targetIdx >= dims.size()) {
                        continue;
                    }
                    String newDim = dims.get(targetIdx);
                    currentWorldSelected = newDim;
                    // Set center map coords
                    int[] coords = getCenterMapCoordsForDimension(newDim);
                    currentXCenter = coords[0];
                    currentZCenter = coords[1];
                    // Reset offset & zoom
                    mouseXOffset = 0;
                    mouseYOffset = 0;
                    zoomValue = ZOOM_BUCKET;
                }
            }
            var mapList = getMapIconList();
            for (int k = 0; k < MAX_TAB_DISP; k++) {
                int targetX = (int) x - (int) (1.0/16 * atlasBgScaledSize);
                int targetY = (int) y + (int) (k * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize);
                if (mouseX >= targetX && mouseX <= targetX + scaledWidth
                        && mouseY >= targetY && mouseY <= targetY + scaledWidth) {
                    int targetIdx = mapIconSelectorOffset + k;
                    if (targetIdx >= mapList.size()) {
                        continue;
                    }
                    String key = mapList.get(targetIdx).getKey();
                    var stateIdStr = key.split("/")[0];
                    var centers =
                            idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(stateIdStr)).getSecond();
                    // Set center map coords
                    currentXCenter = centers.get(0);
                    currentZCenter = centers.get(1);
                    // Reset offset & zoom
                    mouseXOffset = 0;
                    mouseYOffset = 0;
                    zoomValue = ZOOM_BUCKET;
                }
            }
        }
        return true;
    }

    // ================== Drawing Utils ==================

    public static void drawMapTextXZCoords(
            DrawContext context,
            int x,
            int y,
            int originOffsetWidth,
            int originOffsetHeight,
            float textScaling,
            BlockPos blockPos
    ) {
        String coordsToDisplay = "X: "+blockPos.getX()+", Z: "+blockPos.getZ();
        MapAtlasesHUD.drawScaledText(
                context, x, y, coordsToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    private void drawMap(
            MatrixStack matrices,
            MapInfo[][] mapGrid,
            int gridX,
            int gridY,
            double mapTextX,
            double mapTextY,
            float mapTextureScale,
            boolean drawPlayerIcons
    ) {
        MapInfo info = mapGrid[gridX][gridY];
        if (info == null || client == null) return;
        // Draw the map
        double curMapTextX = mapTextX + (mapTextureScale * 128 * gridX);
        double curMapTextY = mapTextY + (mapTextureScale * 128 * gridY);
        VertexConsumerProvider.Immediate vcp;
        vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        matrices.push();
        matrices.translate(curMapTextX, curMapTextY, 0.0);
        matrices.scale(mapTextureScale, mapTextureScale, -1);
        // Remove the off-map player icons temporarily during render
        Iterator<Map.Entry<String, MapDecoration>> it = ((MapStateIntrfc) info.state)
                .getFullIcons().entrySet().iterator();
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        Optional<Integer> playerIconMapId = getPlayerIconMapId(mapGrid);
        if (playerIconMapId.isEmpty() || info.id != playerIconMapId.get()) {
            // Only remove the off-map icon if it's not the active map or its not the active dimension
            while (it.hasNext()) {
                Map.Entry<String, MapDecoration> e = it.next();
                if (!isMeaningfulMapIcon(e.getValue().type())
                        || (e.getValue().type() == MapDecorationTypes.PLAYER && !drawPlayerIcons)) {
                    it.remove();
                    removed.add(e);
                }
            }
        }
        client.gameRenderer.getMapRenderer()
                .draw(
                        matrices,
                        vcp,
                        MapAtlasesAccessUtils.getMapIdComponentFromString(info.id() + ""),
                        info.state,
                        false,
                        0xF000F0
                );
        vcp.draw();
        matrices.pop();
        // Re-add the off-map player icons after render
        for (Map.Entry<String, MapDecoration> e : removed) {
            ((MapStateIntrfc) info.state).getFullIcons().put(e.getKey(), e.getValue());
        }
    }

    // ================== Other Util Fns ==================

    private int[] getCenterMapCoordsForDimension(String dim) {
        var dimIdsToCenters = getDimIdsToCenters(dim);
        int centerMap;
        if (dim.compareTo(initialWorldSelected) == 0) {
            centerMap = MapAtlasesAccessUtils.getMapIntFromString(centerMapId);
        } else {
            centerMap = dimIdsToCenters.keySet().stream()
                    .filter(mapId -> {
                        if (client == null || client.world == null) return false;
                        var state = client.world.getMapState(MapAtlasesAccessUtils.getMapIdComponentFromString(mapId + ""));
                        if (state == null) {
                            return false;
                        }
                        return !((MapStateIntrfc) state).getFullIcons().entrySet().stream()
                                .filter(e -> e.getValue().isAlwaysRendered())
                                .collect(Collectors.toSet())
                                .isEmpty();
                    })
                    .findAny().orElseGet(() -> dimIdsToCenters.keySet().stream().findAny().orElseThrow());
        }
        var entry = dimIdsToCenters.get(centerMap).getSecond();
        return new int[]{entry.get(0), entry.get(1)};
    }

    private int getAtlasBgScaledSize() {
        if (client == null) return 16;
        if (MapAtlasesMod.CONFIG != null) {
            return (int) Math.floor(
                    MapAtlasesMod.CONFIG.forceWorldMapScaling/100.0 * client.getWindow().getScaledHeight());
        }
        return (int) Math.floor(.8 * client.getWindow().getScaledHeight());
    }

    private Optional<Integer> getPlayerIconMapId(MapInfo[][] mapGrid) {
        if (currentWorldSelected.compareTo(initialWorldSelected) != 0) {
            return Optional.empty();
        }
        int zoomLevelDim = getZoomLevelDim();
        Optional<Integer> returnVal = Optional.empty();
        double minSquaredDist = Double.MAX_VALUE;
        for (int gridX=0; gridX<zoomLevelDim; ++gridX)
        for (int gridY=0; gridY<zoomLevelDim; ++gridY)
        {
            var info = mapGrid[gridX][gridY];
            if (info == null) {
                continue;
            }
            double distX = info.centerX - client.player.getX();
            double distZ = info.centerZ - client.player.getZ();
            double squaredDist = (distX*distX) + (distZ*distZ);
            if (squaredDist < minSquaredDist) {
                returnVal = Optional.of(info.id);
                minSquaredDist = squaredDist;
            }
        }
        return returnVal;
    }

    private BlockPos getBlockPosForCursor(
            int mouseX,
            int mouseY,
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
                (int) (Math.floor(atlasMapsRelativeMouseX * zoomLevelDim * (atlasScale / 2.0)) + centerScreenXCenter),
                255,
                (int) (Math.floor(atlasMapsRelativeMouseZ * zoomLevelDim * (atlasScale / 2.0)) + centerScreenZCenter));
    }

    private boolean mapContainsMeaningfulIcons(MapState state) {
        return ((MapStateIntrfc) state).getFullIcons().values().stream()
                .anyMatch(i -> this.isMeaningfulMapIcon(i.type()));
    }

    private boolean isMeaningfulMapIcon(RegistryEntry<MapDecorationType> type) {
        return type != MapDecorationTypes.PLAYER_OFF_MAP && type != MapDecorationTypes.PLAYER_OFF_LIMITS;
    }

    private Map.Entry<String, MapState> findMapEntryForCenters(
            Map<String, MapState> mapInfos,
            String reqDimension,
            int reqXCenter,
            int reqZCenter
    ) {
        return mapInfos.entrySet().stream()
                .filter(infoEntry -> {
                    var mapId = MapAtlasesAccessUtils.getMapIntFromString(infoEntry.getKey());
                    var dimAndCenters = idsToCenters.get(mapId);
                    return idsToCenters.containsKey(mapId)
                            && dimAndCenters.getFirst().compareTo(reqDimension) == 0
                            && dimAndCenters.getSecond().get(0) == reqXCenter
                            && dimAndCenters.getSecond().get(1) == reqZCenter;
                        }
                )
                .findFirst().orElse(null);
    }

    /**
     * Computes the position of a map on a grid, such that (0,0) corresponds to
     * the map at the center of the world.
     * @param x,z The block position at the center of the map.
     * @param mapSize The size of the map measured in blocks.
     * @return The position of the map on the grid.
     */
    static private Vector2i BlockPosToMapTile(int x, int z, int mapSize){
        int origin = (mapSize/2) - 64; //The blockpos at the center of the (0,0) map.
        Vector2i tile = new Vector2i();
        tile.x = (x-origin) / mapSize;
        tile.y = (z-origin) / mapSize;
        return tile;
    }

    /**
     * Find all maps that will be renderered, and their corresponding position
     * on the grid.
     * @param region The dimensions of the grid, and its position in the atlas.
     */
    private MapInfo[][] CreateMapGrid(
        Map<String, MapState> mapInfos,
        String dimension,
        Rect2i region
    ) {
        MapInfo[][] result = new MapInfo[region.getWidth()][region.getHeight()];

        for (var entry : mapInfos.entrySet()){
            int id = MapAtlasesAccessUtils.getMapIntFromString(entry.getKey());
            var coords = idsToCenters.get(id);
            if (!dimension.equals(coords.getFirst()))
                continue;

            var blockpos = idsToCenters.get(id).getSecond();
            Vector2i tile = BlockPosToMapTile(blockpos.get(0), blockpos.get(1), atlasScale);
            tile.x -= region.getX();
            tile.y -= region.getY();

            if (tile.x >= 0
            &&  tile.y >= 0
            &&  tile.x < region.getWidth()
            &&  tile.y < region.getHeight()
            ){
                result[tile.x][tile.y] = new MapInfo(
                    id,
                    dimension,
                    blockpos.get(0),
                    blockpos.get(1),
                    entry.getValue()
                );
            }
        }

        return result;
    }

    private int getZoomLevelDim() {
        int zoomLevel = round(zoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zoomLevel = Math.max(zoomLevel, 0);
        return (2 * zoomLevel) + 1;
    }

    private Map<Integer, Pair<String, List<Integer>>> getDimIdsToCenters(String worldKey) {
        return idsToCenters.entrySet().stream()
                .filter(t -> t.getValue().getFirst().compareTo(worldKey) == 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (m1, m2) -> m1));
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

    private int calcScaledWidth(int rawWidth) {
        if (client == null) return 0;
        return rawWidth * client.getWindow().getScaledHeight() / 1080;
    }

    private String firstCharCapitalize(String source) {
        char[] array = source.toLowerCase(Locale.ROOT).toCharArray();
        array[0] = Character.toUpperCase(array[0]);
        for (int j = 1; j < array.length; j++) {
            if (Character.isWhitespace(array[j - 1])) {
                array[j] = Character.toUpperCase(array[j]);
            }
        }
        return new String(array);
    }

    // ================== Dimension Selectors ==================

    private void drawDimensionTooltip(
            DrawContext context,
            double x,
            double y,
            int atlasBgScaledSize
    ) {
        var dimensions =
                idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
        int scaledWidth = calcScaledWidth(100);
        for (int i = 0; i < MAX_TAB_DISP; i++) {
            if (rawMouseXMoved >= (x + (int) (29.5/32.0 * atlasBgScaledSize))
                    && rawMouseXMoved <= (x + (int) (29.5/32.0 * atlasBgScaledSize) + scaledWidth)
                    && rawMouseYMoved >= (y + (int) (i * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize))
                    && rawMouseYMoved <= (y + (int) (i * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize)) + scaledWidth) {
                int targetIdx = dimSelectorOffset + i;
                if (targetIdx >= dimensions.size()) {
                    continue;
                }
                Identifier dimRegistry = new Identifier(dimensions.get(targetIdx));
                String dimName;
                if (dimRegistry.getNamespace().compareTo("minecraft") == 0) {
                    dimName = dimRegistry.getPath().toString().replace("_", " ");
                } else {
                    dimName = dimRegistry.toString().toString().replace("_", " ").replace(":", " ");
                }
                dimName = firstCharCapitalize(dimName);
                assert client != null;
                context.drawTooltip(client.textRenderer, Text.of(dimName), (int) rawMouseXMoved, (int) rawMouseYMoved);
                return;
            }
        }
    }

    private void drawDimensionSelectors(
            DrawContext context,
            double x,
            double y,
            int atlasBgScaledSize
    ) {
        var dimensions =
                idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
        int scaledWidth;
        for (int i = 0; i < MAX_TAB_DISP; i++) {
            int targetIdx = dimSelectorOffset + i;
            if (targetIdx >= dimensions.size()) {
                continue;
            }
            var dim = dimensions.get(targetIdx);
            scaledWidth = calcScaledWidth(100);
            // Draw selector
            Identifier selectionPage = (dim.compareTo(currentWorldSelected) == 0) ? PAGE_SELECTED : PAGE_UNSELECTED;
            RenderSystem.setShaderTexture(0, selectionPage);
            context.drawTexture(
                    selectionPage,
                    (int) x + (int) (29.5/32.0 * atlasBgScaledSize),
                    (int) y + (int) (i * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );
            // Draw Icon
            Identifier dimensionPage;
            if (dim.compareTo("minecraft:overworld") == 0) {
                dimensionPage = PAGE_OVERWORLD;
            } else if (dim.compareTo("minecraft:the_nether") == 0) {
                dimensionPage = PAGE_NETHER;
            } else if (dim.compareTo("minecraft:the_end") == 0) {
                dimensionPage = PAGE_END;
            } else {
                dimensionPage = PAGE_OTHER;
            }
            RenderSystem.setShaderTexture(0, dimensionPage);
            scaledWidth = calcScaledWidth(75);
            context.drawTexture(
                    dimensionPage,
                    (int) x + (int) (30.0/32.0 * atlasBgScaledSize),
                    (int) y + (int) (i * (4/32.0 * atlasBgScaledSize)) + (int) (4.0/64.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );
        }
    }

    // ================== Map Icon Selectors ==================

    private List<Map.Entry<String, MapDecoration>> getMapIconList() {
        var mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(client.world, atlas);
        // Map of <"MapStateId/MapIconId": MapIcon>
        Map<String, MapDecoration> mapIcons = new HashMap<>();
        for (var state : mapInfos.entrySet())
        if  (state != null && currentWorldSelected.equals(getMapStateDimKey(state.getValue())))
        {
            var fullIcons = ((MapStateIntrfc) state.getValue()).getFullIcons();
            var stateString = state.getKey();
            var keptIcons = fullIcons.entrySet().stream()
                    .filter(t -> t.getValue().isAlwaysRendered())
                    .map(t -> new AbstractMap.SimpleEntry<>(stateString + "/" + t.getKey(), t.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            mapIcons.putAll(keptIcons);
        }
        return mapIcons.entrySet().stream().toList();
    }

    private void drawMapIconSelectors(DrawContext context, double x, double y, int atlasBgScaledSize) {
        if (client == null) return;
        int scaledWidth = calcScaledWidth(100);
        List<Entry<String, MapDecoration>> mapList = getMapIconList();
        for (int k = 0; k < MAX_TAB_DISP; k++) {
            int targetIdx = mapIconSelectorOffset + k;
            if (targetIdx >= mapList.size()) {
                continue;
            }
            var matrices = context.getMatrices();
            Entry<String, MapDecoration> mapIconE = mapList.get(targetIdx);
            // Draw selector
            String mapIdStr = mapIconE.getKey().split("/")[0];
            var centers = idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(mapIdStr)).getSecond();
            if (currentXCenter == centers.get(0) && currentZCenter == centers.get(1)) {
                RenderSystem.setShaderTexture(0, PAGE_SELECTED);
            } else {
                RenderSystem.setShaderTexture(0, PAGE_UNSELECTED);
            }
            drawTextureFlippedX(
                    context,
                    (int) x - (int) (1.0/16 * atlasBgScaledSize),
                    (int) y + (int) (k * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );

            // Draw map Icon
            MapDecoration mapIcon = mapIconE.getValue();
            matrices.push();
            matrices.translate(
                    x,
                    (int) y + (int) (k * (4/32.0 * atlasBgScaledSize)) + (int) (1.75/16.0 * atlasBgScaledSize),
                    1
            );
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(mapIcon.rotation() * 360) / 16.0F));
            matrices.scale((0.25f * scaledWidth) ,(0.25f * scaledWidth), 1);
            matrices.translate(-0.125D, 0.125D, -1.0D);
            byte b = mapIcon.getTypeId();
            float g = (float)(b % 16 + 0) / 16.0F;
            float h = (float)(b / 16 + 0) / 16.0F;
            float l = (float)(b % 16 + 1) / 16.0F;
            float m = (float)(b / 16 + 1) / 16.0F;
            Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
            int light = 0xF000F0;
            VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            VertexConsumer vertexConsumer2 = vcp.getBuffer(MAP_ICONS);
            vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, (float)k * 0.001F)
                    .color(255, 255, 255, 255).texture(g, h).light(light).next();
            vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, (float)k * 0.002F)
                    .color(255, 255, 255, 255).texture(l, h).light(light).next();
            vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, (float)k * 0.003F)
                    .color(255, 255, 255, 255).texture(l, m).light(light).next();
            vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, (float)k * 0.004F)
                    .color(255, 255, 255, 255).texture(g, m).light(light).next();
            vcp.draw();
            matrices.pop();
        }
    }

    private void drawTextureFlippedX(DrawContext context, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        var matrices = context.getMatrices();
        matrices.push();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        float u0 = (u + (float)width) / (float)textureWidth;
        float u1 = (u + 0.0F) / (float)textureWidth;
        float v0 = (v + 0.0F) / (float)textureHeight;
        float v1 = (v + (float)height) / (float)textureHeight;
        bufferBuilder.vertex(matrices.peek().getPositionMatrix(), (float)x, (float)y + height,  0.00001F).texture(u0, v1).next();
        bufferBuilder.vertex(matrices.peek().getPositionMatrix(), (float)x + width, (float)y + height,  0.00002F).texture(u1, v1).next();
        bufferBuilder.vertex(matrices.peek().getPositionMatrix(), (float)x + width, (float)y,  0.00003F).texture(u1, v0).next();
        bufferBuilder.vertex(matrices.peek().getPositionMatrix(), (float)x, (float)y,  0.00004F).texture(u0, v0).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        matrices.pop();
    }

    private void drawMapIconTooltip(
            DrawContext context,
            double x,
            double y,
            int atlasBgScaledSize
    ) {
        int scaledWidth = calcScaledWidth(100);
        List<Entry<String, MapDecoration>> mapList = getMapIconList();
        for (int k = 0; k < MAX_TAB_DISP; k++) {
            int targetIdx = mapIconSelectorOffset + k;
            if (targetIdx >= mapList.size()) {
                continue;
            }
            Entry<String, MapDecoration> entry = mapList.get(targetIdx);
            String stateIdStr = entry.getKey().split("/")[0];
            var stateId = MapAtlasesAccessUtils.getMapIntFromString(stateIdStr);
            var dimAndCenters = idsToCenters.get(stateId);
            var mapState = client.world.getMapState(MapAtlasesAccessUtils.getMapIdComponentFromString(stateIdStr));
            if (mapState == null) continue;
            MapDecoration mapIcon = entry.getValue();
            var mapIconText = mapIcon.name().isEmpty()
                    ? MutableText.of(new PlainTextContent.Literal(
                            firstCharCapitalize(mapIcon.type().value().assetId().toString().replace("_", " "))))
                    : mapIcon.name().get();
            if (rawMouseXMoved >= (int) x - (int) (1.0/16 * atlasBgScaledSize)
                    && rawMouseXMoved <= (int) x - (int) (1.0/16 * atlasBgScaledSize) + scaledWidth
                    && rawMouseYMoved >= (int) y + (int) (k * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize)
                    && rawMouseYMoved <= (int) y + (int) (k * (4/32.0 * atlasBgScaledSize)) + (int) (1.0/16.0 * atlasBgScaledSize) + scaledWidth) {
                // draw text
                PlainTextContent.Literal coordsText = new PlainTextContent.Literal(
                        "X: " + (int) (dimAndCenters.getSecond().get(0) - (atlasScale / 2.0d) + ((atlasScale / 2.0d) * ((mapIcon.x() + 128) / 128.0d)))
                                + ", Z: "
                                + (int) (dimAndCenters.getSecond().get(1) - (atlasScale / 2.0d) + ((atlasScale / 2.0d) * ((mapIcon.z() + 128) / 128.0d)))
                );
                MutableText formattedCoords = MutableText.of(coordsText).formatted(Formatting.GRAY);
                context.drawTooltip(client.textRenderer, List.of(mapIconText, formattedCoords), (int) rawMouseXMoved, (int) rawMouseYMoved);
                return;
            }
        }
    }
}