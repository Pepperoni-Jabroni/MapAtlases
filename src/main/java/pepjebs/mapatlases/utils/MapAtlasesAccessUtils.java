package pepjebs.mapatlases.utils;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.mixin.plugin.MapAtlasesMixinPlugin;

import java.util.*;

public class MapAtlasesAccessUtils {

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

    public static int getMapIntFromString(String id) {
        if (id == null) {
            MapAtlasesMod.LOGGER.error("Encountered null id when fetching map name. Env: "
                    + FabricLoader.getInstance().getEnvironmentType());
            return 0;
        }
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

    public static ItemStack getAtlasFromPlayerByConfig(PlayerEntity entity) {
        PlayerInventory inventory = entity.getInventory();
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
        if (itemStack == null
                && MapAtlasesMixinPlugin.isTrinketsLoaded()
                && TrinketsApi.getTrinketComponent(entity).isPresent()
                && TrinketsApi.getTrinketComponent(entity).get().getEquipped(MapAtlasesMod.MAP_ATLAS).size() > 0) {
            itemStack = TrinketsApi.getTrinketComponent(entity)
                    .get().getEquipped(MapAtlasesMod.MAP_ATLAS).get(0).getRight();
        }
        return itemStack != null ? itemStack : ItemStack.EMPTY;
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

    public static Map.Entry<String, MapState> getActiveAtlasMapStateServer(World world, ItemStack atlas, ServerPlayerEntity player) {
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(world, atlas);
        Map.Entry<String, MapState> minDistState = null;
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            if (minDistState == null) {
                minDistState = state;
                continue;
            }
            if (Math.hypot(Math.abs(minDistState.getValue().centerX - player.getX()),Math.abs(minDistState.getValue().centerZ - player.getZ()))
                > Math.hypot(Math.abs(state.getValue().centerX - player.getX()),Math.abs(state.getValue().centerZ - player.getZ())) ) {
                minDistState = state;
            }
        }
        return minDistState;
    }

    public static int getEmptyMapCountFromItemStack(ItemStack atlas) {
        NbtCompound tag = atlas.getNbt();
        return tag != null && tag.contains("empty") ? tag.getInt("empty") : 0;
    }

    public static int getMapCountFromItemStack(ItemStack atlas) {
        NbtCompound tag = atlas.getNbt();
        return tag != null && tag.contains("maps") ? tag.getIntArray("maps").length : 0;
    }

    public static int getMapCountToAdd(ItemStack atlas, ItemStack bottomItem) {
        int amountToAdd = bottomItem.getCount();
        int mapCount = MapAtlasesAccessUtils.getMapCountFromItemStack(atlas)
                + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        if (MapAtlasItem.getMaxMapCount() != -1
                && mapCount + bottomItem.getCount() > MapAtlasItem.getMaxMapCount()) {
            amountToAdd = MapAtlasItem.getMaxMapCount() - mapCount;
        }
        return amountToAdd;
    }
}
