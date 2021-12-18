package pepjebs.mapatlases.utils;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {

    public static Map<String, Map.Entry<String, MapState>> previousMapStates = new HashMap<>();

    public static boolean areMapsSameScale(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.scale == testAgainst.scale).count() == newMaps.size();
    }

    public static boolean areMapsSameDimension(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.dimension == testAgainst.dimension).count() == newMaps.size();
    }

    public static MapState getFirstMapStateFromAtlas(World world, ItemStack atlas) {
        return getMapStateByIndexFromAtlas(world, atlas, 0);
    }

    public static MapState getMapStateByIndexFromAtlas(World world, ItemStack atlas, int i) {
        if (atlas.getNbt() == null) return null;
        int[] mapIds = Arrays.stream(atlas.getNbt().getIntArray("maps")).toArray();
        if (i < 0 || i >= mapIds.length) return null;
        ItemStack map = createMapItemStackFromId(mapIds[i]);
        return FilledMapItem.getMapState(FilledMapItem.getMapId(map), world);
    }

    public static ItemStack createMapItemStackFromId(int id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        NbtCompound tag = new NbtCompound();
        tag.putInt("map", id);
        map.setNbt(tag);
        return map;
    }

    public static ItemStack createMapItemStackFromStrId(String id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        NbtCompound tag = new NbtCompound();
        tag.putInt("map", MapAtlasesAccessUtils.getMapIntFromString(id));
        map.setNbt(tag);
        return map;
    }

    public static int getMapIntFromString(String id) {
        return Integer.parseInt(id.substring(4));
    }

    public static Map<String, MapState> getAllMapInfoFromAtlas(World world, ItemStack atlas) {
        if (atlas.getNbt() == null) return new HashMap<>();
        int[] mapIds = Arrays.stream(atlas.getNbt().getIntArray("maps")).toArray();
        Map<String, MapState> mapStates = new HashMap<>();
        for (int mapId : mapIds) {
            String mapName = FilledMapItem.getMapName(mapId);
            MapState state = world.getMapState(mapName);
            if (state == null && world instanceof ServerWorld) {
                ItemStack map = createMapItemStackFromId(mapId);
                state = FilledMapItem.getOrCreateMapState(map, world);
            }
            if (state != null) {
                mapStates.put(mapName, state);
            }
        }
        return mapStates;
    }

    public static List<String> getAllMapIdsFromAtlas(World world, ItemStack atlas) {
        if (atlas.getNbt() == null) return new ArrayList<>();
        String[] mapIds = (String[]) Arrays.stream(atlas.getNbt().getIntArray("maps"))
                .mapToObj(m -> FilledMapItem.getMapName(m)).toArray();
        return List.of(mapIds);
    }

    public static List<MapState> getAllMapStatesFromAtlas(World world, ItemStack atlas) {
        if (atlas.getNbt() == null) return new ArrayList<>();
        int[] mapIds = Arrays.stream(atlas.getNbt().getIntArray("maps")).toArray();
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

    public static ItemStack getAtlasFromPlayerByConfig(PlayerInventory inventory) {
        ItemStack itemStack =  inventory.main.stream()
                .limit(9)
                .filter(i -> i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS)))
                .findFirst().orElse(null);

        if (MapAtlasesMod.CONFIG != null) {
            if(MapAtlasesMod.CONFIG.activationLocation.equals("INVENTORY")) {
                itemStack =  inventory.main.stream()
                        .filter(i -> i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS)))
                        .findFirst().orElse(null);
            } else if (MapAtlasesMod.CONFIG.activationLocation.equals("HANDS")) {
                itemStack = null;
                ItemStack mainHand = inventory.main.get(inventory.selectedSlot);
                if (mainHand.getItem() == MapAtlasesMod.MAP_ATLAS)
                    itemStack = mainHand;
            }
        }
        if (itemStack == null && inventory.offHand.get(0).getItem() == MapAtlasesMod.MAP_ATLAS)
            itemStack = inventory.offHand.get(0);
        return itemStack != null ? itemStack.copy() : ItemStack.EMPTY;
    }

    public static ItemStack getAtlasFromPlayerByHotbar(PlayerInventory inventory) {
        ItemStack itemStack =  inventory.main.stream()
                .limit(9)
                .filter(i -> i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS)))
                .findFirst().orElse(null);

        if (itemStack == null && inventory.offHand.get(0).getItem() == MapAtlasesMod.MAP_ATLAS)
            itemStack = inventory.offHand.get(0);
        return itemStack != null ? itemStack.copy() : ItemStack.EMPTY;
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

    public static Set<Integer> getMapIdsFromItemStacks(World world, List<ItemStack> itemStacks) {
        return itemStacks.stream().map(FilledMapItem::getMapId).collect(Collectors.toSet());
    }

    public static List<ItemStack> getItemStacksFromGrid(CraftingInventory inv) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i).copy());
            }
        }
        return itemStacks;
    }

    public static boolean isListOnlyIngredients(List<ItemStack> itemStacks, List<Item> items) {
        return itemStacks.stream().filter(is -> {
            for (Item i : items) {
                if (i == is.getItem()) return true;
            }
            return false;
        }).count() == itemStacks.size();
    }

    public static Map.Entry<String, MapState> getActiveAtlasMapState(World world, ItemStack atlas, String playerName) {
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(world, atlas);
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            for (Map.Entry<String, MapIcon> entry : ((MapStateIntrfc) state.getValue()).getFullIcons().entrySet()) {
                MapIcon icon = entry.getValue();
                // Entry.getKey is "icon-0" on client
                if (icon.getType() == MapIcon.Type.PLAYER && entry.getKey().compareTo(playerName) == 0) {
                    previousMapStates.put(playerName, state);
                    return state;
                }
            }
        }
        if (previousMapStates.containsKey(playerName)) return previousMapStates.get(playerName);
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            for (Map.Entry<String, MapIcon> entry : ((MapStateIntrfc) state.getValue()).getFullIcons().entrySet()) {
                if (entry.getValue().getType() == MapIcon.Type.PLAYER_OFF_MAP
                        && entry.getKey().compareTo(playerName) == 0) {
                    previousMapStates.put(playerName, state);
                    return state;
                }
            }
        }
        return null;
    }

    public static int getEmptyMapCountFromItemStack(ItemStack atlas) {
        NbtCompound tag = atlas.getNbt();
        return tag != null && tag.contains("empty") ? tag.getInt("empty") : 0;
    }

    public static int getMapCountFromItemStack(ItemStack atlas) {
        NbtCompound tag = atlas.getNbt();
        return tag != null && tag.contains("maps") ? tag.getIntArray("maps").length : 0;
    }

    public static DefaultedList<ItemStack> setAllMatchingItemStacks(
            DefaultedList<ItemStack> itemStacks,
            int size,
            Item searchingItem,
            String searchingTag,
            ItemStack newItemStack) {
        for (int i = 0; i < size; i++) {
            if (itemStacks.get(i).getItem() == searchingItem
                    && itemStacks.get(i)
                    .getOrCreateNbt().toString().compareTo(searchingTag) == 0) {
                itemStacks.set(i, newItemStack);
            }
        }
        return itemStacks;
    }
}
