package pepjebs.mapatlases.recipe;

import com.google.common.primitives.Ints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAddRecipe extends SpecialCraftingRecipe {
    public MapAtlasesAddRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i));
            }
        }
        if (itemStacks.size() > 1) {
            List<Item> items = itemStacks.stream().map(ItemStack::getItem).collect(Collectors.toList());
            long filteredItemsCount = items.stream()
                    .filter(i -> i == MapAtlasesMod.MAP_ATLAS || i == Items.MAP || i == Items.FILLED_MAP)
                    .count();
            if (filteredItemsCount == items.size()) {
                return items.stream().filter(i -> i == MapAtlasesMod.MAP_ATLAS).count() == 1;
            }
        }
        return false;
    }

    @Override
    public ItemStack craft(CraftingInventory inv) {
        ClientWorld world = MinecraftClient.getInstance().world;
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i));
            }
        }
        ItemStack atlas = itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).findFirst().get().copy();
        Set<Integer> mapIds = itemStacks.stream()
                .filter(i -> i.isItemEqual(new ItemStack(Items.FILLED_MAP)))
                .map(m -> {
                    MapState mapState = FilledMapItem.getMapState(m, world);
                    return getMapIntFromState(mapState);
                })
                .collect(Collectors.toSet());
        int emptyMapCount = (int)itemStacks.stream().filter(i -> i.isItemEqual(new ItemStack(Items.MAP))).count();
        CompoundTag compoundTag = atlas.getTag();
        Set<Integer> existingMaps = new HashSet<>(Ints.asList(compoundTag.getIntArray("maps")));
        existingMaps.addAll(mapIds);
        compoundTag.putIntArray("maps", existingMaps.stream().mapToInt(i->i).toArray());
        compoundTag.putInt("empty", emptyMapCount + compoundTag.getInt("empty"));
        atlas.setTag(compoundTag);
        return atlas;
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
