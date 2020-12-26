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
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAddRecipe extends SpecialCraftingRecipe {
    public MapAtlasesAddRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        List<ItemStack> itemStacks = MapAtlasesAccessUtils.getItemStacksFromGrid(inv);
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(itemStacks);
        if (atlas.isEmpty()) return false;
        MapState mapState = MapAtlasesAccessUtils.getRandomMapStateFromAtlas(world, atlas);
        // Ensure only correct ingredients are present
        if (itemStacks.size() > 1 && MapAtlasesAccessUtils.isListOnylIngredients(itemStacks)) {
            List<MapState> mapStates = MapAtlasesAccessUtils.getMapStatesFromItemStacks(world, itemStacks);
            // Ensure Filled Maps are all same Scale & Dimension
            if (MapAtlasesAccessUtils.areMapsSameScale(mapState, mapStates) &&
                    MapAtlasesAccessUtils.areMapsSameDimension(mapState, mapStates)) {
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
        List<ItemStack> itemStacks = MapAtlasesAccessUtils.getItemStacksFromGrid(inv);
        // Grab the Atlas in the Grid
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(itemStacks);
        // Get the Map Ids in the Grid
        Set<Integer> mapIds = MapAtlasesAccessUtils.getMapIdsFromItemStacks(world, itemStacks);
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

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_ADD_RECIPE;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }
}
