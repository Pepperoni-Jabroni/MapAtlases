package pepjebs.mapatlases.recipe;

import com.google.common.primitives.Ints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;

public class MapAtlasesAddRecipe extends SpecialCraftingRecipe {
    public MapAtlasesAddRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        List<ItemStack> itemStacks = MapAtlasesAccessUtils.getItemStacksFromGrid(inv);
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(itemStacks);

        // Ensure there's an Atlas
        if (atlas.isEmpty()) return false;
        MapState sampleMap = MapAtlasesAccessUtils.getRandomMapStateFromAtlas(world, atlas);

        // Ensure only correct ingredients are present
        if (!(itemStacks.size() > 1 && MapAtlasesAccessUtils.isListOnylIngredients(itemStacks))) return false;
        List<MapState> mapStates = MapAtlasesAccessUtils.getMapStatesFromItemStacks(world, itemStacks);

        // Ensure we're not trying to add too many Maps
        int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        int mapCount = MapAtlasesAccessUtils.getMapCountFromItemStack(atlas);
        if (empties + mapCount + itemStacks.size() - 1 > MapAtlasItem.MAX_MAP_COUNT) return false;

        // Ensure Filled Maps are all same Scale & Dimension
        if(!(MapAtlasesAccessUtils.areMapsSameScale(sampleMap, mapStates) &&
                MapAtlasesAccessUtils.areMapsSameDimension(sampleMap, mapStates))) return false;

        // Ensure there's only one Atlas
        return itemStacks.stream().filter(i ->
                i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).count() == 1;
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
