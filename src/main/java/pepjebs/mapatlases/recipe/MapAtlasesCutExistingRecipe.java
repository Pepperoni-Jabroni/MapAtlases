package pepjebs.mapatlases.recipe;

import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MapAtlasesCutExistingRecipe extends SpecialCraftingRecipe {

    public MapAtlasesCutExistingRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack shears = ItemStack.EMPTY;
        int size = 0;
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                size++;
                if (inv.getStack(i).getItem() == MapAtlasesMod.MAP_ATLAS) {
                    atlas = inv.getStack(i);
                } else if (inv.getStack(i).getItem() == Items.SHEARS) {
                    shears = inv.getStack(i);
                }
            }
        }
        return !atlas.isEmpty() && !shears.isEmpty() && size == 2;
    }

    @Override
    public ItemStack craft(CraftingInventory inv) {
        ItemStack atlas = ItemStack.EMPTY;
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                if (inv.getStack(i).getItem() == MapAtlasesMod.MAP_ATLAS) {
                    atlas = inv.getStack(i);
                }
            }
        }
        if (atlas.getTag() == null) return ItemStack.EMPTY;
        if (MapAtlasesAccessUtils.getMapCountFromItemStack(atlas) > 1) {
            List<Integer> mapIds = Arrays.stream(atlas.getTag()
                    .getIntArray("maps")).boxed().collect(Collectors.toList());
            if (mapIds.size() > 0) {
                int lastId = mapIds.remove(mapIds.size() - 1);
                return MapAtlasesAccessUtils.createMapItemStackFromId(lastId);
            }
        }
        if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas) > 0) {
            return new ItemStack(Items.MAP);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public DefaultedList<ItemStack> getRemainingStacks(CraftingInventory inv) {
        DefaultedList<ItemStack> list = DefaultedList.of();
        for(int i = 0; i < inv.size(); i++) {
            ItemStack cur = inv.getStack(i);
            if (cur.getItem() == Items.SHEARS) {
                cur.damage(1, new Random(), null);
            } else if (cur.getItem() == MapAtlasesMod.MAP_ATLAS && cur.getTag() != null) {
                boolean didRemoveFilled = false;
                if (MapAtlasesAccessUtils.getMapCountFromItemStack(cur) > 1) {
                    List<Integer> mapIds = Arrays.stream(cur.getTag()
                            .getIntArray("maps")).boxed().collect(Collectors.toList());
                    if (mapIds.size() > 0) {
                        mapIds.remove(mapIds.size() - 1);
                        cur.getTag().putIntArray("maps", mapIds);
                        didRemoveFilled = true;
                    }

                }
                if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(cur) > 0 && !didRemoveFilled) {
                    cur.getTag().putInt("empty", cur.getTag().getInt("empty") - 1);
                }
            }
            list.add(cur.copy());
        }
        return list;
    }

    @Override
    public boolean fits(int width, int height) {
        return width + height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CUT_RECIPE;
    }
}
