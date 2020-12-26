package pepjebs.mapatlases.recipe;

import com.google.common.primitives.Ints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAddRecipe extends SpecialCraftingRecipe {
    public MapAtlasesAddRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        List<ItemStack> itemStacks = getItemStacksFromGrid(inv);
        ItemStack atlas = getAtlasFromItemStacks(itemStacks);
        if (atlas.isEmpty()) return false;
        MapState mapState = getRandomMapStateFromAtlas(world, atlas);
        // Ensure only correct ingredients are present
        if (itemStacks.size() > 1 && isListOnylIngredients(itemStacks)) {
            List<MapState> mapStates = getMapStatesFromItemStacks(world, itemStacks);
            // Ensure Filled Maps are all same Scale & Dimension
            if (areMapsSameScale(mapState, mapStates) && areMapsSameDimension(mapState, mapStates)) {
                // Ensure there's only one Atlas
                return itemStacks.stream().filter(i ->
                        i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).count() == 1;
            }
        }
        return false;
    }

    @Override
    public ItemStack craft(CraftingInventory inv) {
        ClientWorld world = MinecraftClient.getInstance().world;
        List<ItemStack> itemStacks = getItemStacksFromGrid(inv);
        // Grab the Atlas in the Grid
        ItemStack atlas = getAtlasFromItemStacks(itemStacks);
        // Get the Map Ids in the Grid
        Set<Integer> mapIds = getMapIdsFromItemStacks(world, itemStacks);
        // Set NBT Data
        int emptyMapCount = (int)itemStacks.stream().filter(i -> i.isItemEqual(new ItemStack(Items.MAP))).count();
        CompoundTag compoundTag = atlas.getTag();
        Set<Integer> existingMaps = new HashSet<>(Ints.asList(compoundTag.getIntArray("maps")));
        existingMaps.addAll(mapIds);
        compoundTag.putIntArray("maps", existingMaps.stream().mapToInt(i->i).toArray());
        compoundTag.putInt("empty", emptyMapCount + compoundTag.getInt("empty"));
        atlas.setTag(compoundTag);
        return atlas;
    }

    private boolean areMapsSameScale(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.scale == testAgainst.scale).count() == newMaps.size();
    }

    private boolean areMapsSameDimension(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.dimension == testAgainst.dimension).count() == newMaps.size();
    }

    private MapState getRandomMapStateFromAtlas(World world, ItemStack atlas) {
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray("maps")).toArray();
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = new CompoundTag();
        tag.putInt("map", mapIds[0]);
        map.setTag(tag);
        return FilledMapItem.getMapState(map, world);
    }

    private ItemStack getAtlasFromItemStacks(List<ItemStack> itemStacks) {
        Optional<ItemStack> item =  itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findFirst();
        return item.orElse(ItemStack.EMPTY).copy();
    }

    private List<MapState> getMapStatesFromItemStacks(World world, List<ItemStack> itemStacks) {
        return itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(Items.FILLED_MAP)))
                .map(m -> FilledMapItem.getMapState(m, world))
                .collect(Collectors.toList());
    }

    private Set<Integer> getMapIdsFromItemStacks(ClientWorld world, List<ItemStack> itemStacks) {
        return getMapStatesFromItemStacks(world, itemStacks).stream()
                .map(m -> getMapIntFromState(m)).collect(Collectors.toSet());
    }

    private List<ItemStack> getItemStacksFromGrid(CraftingInventory inv) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i));
            }
        }
        return itemStacks;
    }

    private boolean isListOnylIngredients(List<ItemStack> itemStacks) {
        return itemStacks.stream().filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))
                || is.isItemEqual(new ItemStack(Items.MAP))
                || is.isItemEqual(new ItemStack(Items.FILLED_MAP))).count() == itemStacks.size();
    }

    private boolean areNewMapsSameScale(ClientWorld world, Set<Integer> existingMapIds, Set<Integer> newMapIds) {
        MapState existingMapState = existingMapIds.stream().findFirst().map(i -> {
            ItemStack map = new ItemStack(Items.FILLED_MAP);
            CompoundTag mapData = new CompoundTag();
            mapData.putInt("map", i);
            map.setTag(mapData);
            return FilledMapItem.getMapState(map, world);
        }).get();
        Set<MapState> newMapStates = newMapIds.stream().map(i -> {
            ItemStack map = new ItemStack(Items.FILLED_MAP);
            CompoundTag mapData = new CompoundTag();
            mapData.putInt("map", i);
            map.setTag(mapData);
            return FilledMapItem.getMapState(map, world);
        }).collect(Collectors.toSet());
        return newMapStates.stream().filter(m -> m.scale == existingMapState.scale).count() == newMapStates.size();
    }

    private int getMapIntFromState(MapState mapState) {
        String mapId = mapState.getId();
        return Integer.parseInt(mapId.substring(4));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_ADD_RECIPE;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }
}
