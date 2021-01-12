package pepjebs.mapatlases.recipe;

import net.minecraft.client.MinecraftClient;
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
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapAtlasCreateRecipe extends SpecialCraftingRecipe {

    public MapAtlasCreateRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        ItemStack filledMap = ItemStack.EMPTY;
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i));
                if (inv.getStack(i).getItem() == Items.FILLED_MAP) {
                    filledMap = inv.getStack(i);
                }
            }
        }
        if (itemStacks.size() == 3) {
            List<Item> items = itemStacks.stream().map(ItemStack::getItem).collect(Collectors.toList());
            boolean hasAllCrafting =
                    items.containsAll(Arrays.asList(Items.FILLED_MAP, Items.SLIME_BALL, Items.BOOK)) ||
                            items.containsAll(Arrays.asList(Items.FILLED_MAP, Items.HONEY_BOTTLE, Items.BOOK));
            if (hasAllCrafting && !filledMap.isEmpty()) {
                MapState state = FilledMapItem.getOrCreateMapState(filledMap, world);
                if (state == null) return false;
                if (MapAtlasesMod.enableMultiDimMaps) {
                    return state.dimension == World.OVERWORLD || state.dimension == World.END
                            || state.dimension == World.NETHER;
                } else {
                    return state.dimension == World.OVERWORLD;
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack craft(CraftingInventory inv) {
        ItemStack mapItemStack = null;
        for(int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isItemEqual(new ItemStack(Items.FILLED_MAP))) {
                mapItemStack = inv.getStack(i);
            }
        }
        if (mapItemStack == null || MinecraftClient.getInstance().world == null) {
            return ItemStack.EMPTY;
        }
        MapState mapState = FilledMapItem.getMapState(mapItemStack, MinecraftClient.getInstance().world);
        if (mapState == null) return ItemStack.EMPTY;
        Item mapAtlasItem;
        if (MapAtlasesMod.enableMultiDimMaps && mapState.dimension == World.END) {
            mapAtlasItem = Registry.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "end_atlas"));
        } else if (MapAtlasesMod.enableMultiDimMaps && mapState.dimension == World.NETHER) {
            mapAtlasItem = Registry.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "nether_atlas"));
        } else {
            mapAtlasItem = Registry.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "atlas"));
        }
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putIntArray("maps", new int[]{MapAtlasesAccessUtils.getMapIntFromState(mapState)});
        ItemStack atlasItemStack = new ItemStack(mapAtlasItem);
        atlasItemStack.setTag(compoundTag);
        return atlasItemStack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CREATE_RECIPE;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 3;
    }
}
