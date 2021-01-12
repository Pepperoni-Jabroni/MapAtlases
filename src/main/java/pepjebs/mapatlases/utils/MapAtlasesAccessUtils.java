package pepjebs.mapatlases.utils;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {

    public static MapState previousMapState = null;

    public static boolean areMapsSameScale(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.scale == testAgainst.scale).count() == newMaps.size();
    }

    public static boolean areMapsSameDimension(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.dimension == testAgainst.dimension).count() == newMaps.size();
    }

    public static MapState getRandomMapStateFromAtlas(World world, ItemStack atlas) {
        if (atlas.getTag() == null) return null;
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray("maps")).toArray();
        ItemStack map = createMapItemStackFromId(mapIds[0]);
        return FilledMapItem.getMapState(map, world);
    }

    public static ItemStack createMapItemStackFromId(int id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = new CompoundTag();
        tag.putInt("map", id);
        map.setTag(tag);
        return map;
    }

    public static ItemStack createMapItemStackFromStrId(String id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = new CompoundTag();
        tag.putInt("map", Integer.parseInt(id.substring(4)));
        map.setTag(tag);
        return map;
    }

    public static List<MapState> getAllMapStatesFromAtlas(World world, ItemStack atlas) {
        if (atlas.getTag() == null) return new ArrayList<>();
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray("maps")).toArray();
        List<MapState> mapStates = new ArrayList<>();
        for (int mapId : mapIds) {
            MapState state = world.getMapState(FilledMapItem.getMapName(mapId));
            if (state == null && world instanceof ServerWorld) {
                ItemStack map = createMapItemStackFromId(mapId);
                state = FilledMapItem.getOrCreateMapState(map, world);
            }
            if (state != null) {
                mapStates.add(state);
            }
        }
        return mapStates;
    }

    public static ItemStack getAtlasFromItemStacks(List<ItemStack> itemStacks) {
        Optional<ItemStack> item =  itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findFirst();
        return item.orElse(ItemStack.EMPTY).copy();
    }

    public static List<MapState> getMapStatesFromItemStacks(World world, List<ItemStack> itemStacks) {
        return itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(Items.FILLED_MAP)))
                .map(m -> FilledMapItem.getOrCreateMapState(m, world))
                .collect(Collectors.toList());
    }

    public static Set<Integer> getMapIdsFromItemStacks(ClientWorld world, List<ItemStack> itemStacks) {
        return getMapStatesFromItemStacks(world, itemStacks).stream()
                .map(MapAtlasesAccessUtils::getMapIntFromState).collect(Collectors.toSet());
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

    public static MapState getActiveAtlasMapState(World world, ItemStack atlas) {
        List<MapState> mapStates = getAllMapStatesFromAtlas(world, atlas);
        for (MapState state : mapStates) {
            for (Map.Entry<String, MapIcon> entry : state.icons.entrySet()) {
                if (entry.getValue().getType() == MapIcon.Type.PLAYER) {
                    previousMapState = state;
                    return state;
                }
            }
        }
        if (previousMapState != null) return previousMapState;
        for (MapState state : mapStates) {
            for (Map.Entry<String, MapIcon> entry : state.icons.entrySet()) {
                if (entry.getValue().getType() == MapIcon.Type.PLAYER_OFF_MAP) {
                    previousMapState = state;
                    return state;
                }
            }
        }
        return null;
    }

    public static int getEmptyMapCountFromItemStack(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        return tag != null && tag.contains("empty") ? tag.getInt("empty") : 0;
    }

    public static int getMapCountFromItemStack(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        return tag != null && tag.contains("maps") ? tag.getIntArray("maps").length : 0;
    }
}
