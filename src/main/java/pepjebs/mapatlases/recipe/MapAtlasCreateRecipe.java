package pepjebs.mapatlases.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapAtlasCreateRecipe extends SpecialCraftingRecipe {

    private World world = null;

    public MapAtlasCreateRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        this.world = world;
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
            List<Item> items = itemStacks.stream().map(ItemStack::getItem).toList();
            boolean hasAllCrafting =
                    items.containsAll(Arrays.asList(Items.FILLED_MAP, Items.SLIME_BALL, Items.BOOK)) ||
                            items.containsAll(Arrays.asList(Items.FILLED_MAP, Items.HONEY_BOTTLE, Items.BOOK));
            if (hasAllCrafting && !filledMap.isEmpty()) {
                MapState state = FilledMapItem.getOrCreateMapState(filledMap, world);
                return state != null;
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
        if (mapItemStack == null || world == null || mapItemStack.getNbt() == null) {
            return ItemStack.EMPTY;
        }
        MapState mapState = FilledMapItem.getMapState(mapItemStack.getNbt().getInt("map"), world);
        if (mapState == null) return ItemStack.EMPTY;
        Item mapAtlasItem;
        if (MapAtlasesMod.enableMultiDimMaps && mapState.dimension == World.END) {
            mapAtlasItem = Registry.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "end_atlas"));
        } else if (MapAtlasesMod.enableMultiDimMaps && mapState.dimension == World.NETHER) {
            mapAtlasItem = Registry.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "nether_atlas"));
        } else {
            mapAtlasItem = Registry.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "atlas"));
        }
        NbtCompound compoundTag = new NbtCompound();
        Integer mapId = FilledMapItem.getMapId(mapItemStack);
        if (mapId == null) {
            MapAtlasesMod.LOGGER.warn("MapAtlasCreateRecipe found null Map ID from Filled Map");
            compoundTag.putIntArray("maps", new int[]{});
        }
        else
            compoundTag.putIntArray("maps", new int[]{mapId});
        ItemStack atlasItemStack = new ItemStack(mapAtlasItem);
        atlasItemStack.setNbt(compoundTag);
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
