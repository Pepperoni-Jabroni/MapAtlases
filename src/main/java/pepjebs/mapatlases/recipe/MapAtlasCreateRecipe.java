package pepjebs.mapatlases.recipe;

import joptsimple.util.RegexMatcher;
import net.minecraft.block.Blocks;
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
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;

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
        ArrayList<ItemStack> items = new ArrayList<>();
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                items.add(inv.getStack(i));
            }
        }
        if (items.size() == 3) {
            List<Item> items1 = items.stream().map(ItemStack::getItem).collect(Collectors.toList());
            return items1.containsAll(Arrays.asList(Items.FILLED_MAP, Items.CARTOGRAPHY_TABLE, Items.BOOK));
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
        if (mapItemStack == null) {
            return ItemStack.EMPTY;
        }
        MapState mapState = FilledMapItem.getMapState(mapItemStack, MinecraftClient.getInstance().world);
        MapAtlasItem mapAtlasItem = MapAtlasesMod.MAP_ATLAS;
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putIntArray("maps", new int[]{getMapIntFromState(mapState)});
        ItemStack atlasItemStack = new ItemStack(mapAtlasItem);
        atlasItemStack.setTag(compoundTag);
        return atlasItemStack;
    }

    private int getMapIntFromState(MapState mapState) {
        String mapId = mapState.getId();
        return Integer.parseInt(mapId.substring(4));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_RECIPE_SERIALIZER;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 3;
    }
}
