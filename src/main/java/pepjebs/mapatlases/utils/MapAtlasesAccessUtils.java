package pepjebs.mapatlases.utils;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {

    public static boolean areMapsSameScale(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.scale == testAgainst.scale).count() == newMaps.size();
    }

    public static boolean areMapsSameDimension(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.dimension == testAgainst.dimension).count() == newMaps.size();
    }

    public static MapState getRandomMapStateFromAtlas(World world, ItemStack atlas) {
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray("maps")).toArray();
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = new CompoundTag();
        tag.putInt("map", mapIds[0]);
        map.setTag(tag);
        return FilledMapItem.getMapState(map, world);
    }

    public static ItemStack getAtlasFromItemStacks(List<ItemStack> itemStacks) {
        Optional<ItemStack> item =  itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findFirst();
        return item.orElse(ItemStack.EMPTY).copy();
    }

    public static List<MapState> getMapStatesFromItemStacks(World world, List<ItemStack> itemStacks) {
        return itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(Items.FILLED_MAP)))
                .map(m -> FilledMapItem.getMapState(m, world))
                .collect(Collectors.toList());
    }

    public static Set<Integer> getMapIdsFromItemStacks(ClientWorld world, List<ItemStack> itemStacks) {
        return getMapStatesFromItemStacks(world, itemStacks).stream()
                .map(m -> getMapIntFromState(m)).collect(Collectors.toSet());
    }

    public static List<ItemStack> getItemStacksFromGrid(CraftingInventory inv) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i));
            }
        }
        return itemStacks;
    }

    public static boolean isListOnylIngredients(List<ItemStack> itemStacks) {
        return itemStacks.stream().filter(is -> is.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))
                || is.isItemEqual(new ItemStack(Items.MAP))
                || is.isItemEqual(new ItemStack(Items.FILLED_MAP))).count() == itemStacks.size();
    }

    public static int getMapIntFromState(MapState mapState) {
        String mapId = mapState.getId();
        return Integer.parseInt(mapId.substring(4));
    }
}
