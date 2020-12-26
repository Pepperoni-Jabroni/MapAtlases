package pepjebs.mapatlases;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;

public class MapAtlasesMod implements ModInitializer {

    public static final String MOD_ID = "map_atlases";

    public static final MapAtlasItem MAP_ATLAS = new MapAtlasItem(new Item.Settings().group(ItemGroup.MISC).maxCount(1));

    public static SpecialRecipeSerializer<MapAtlasCreateRecipe> MAP_ATLAS_RECIPE_SERIALIZER;

    @Override
    public void onInitialize() {
        MAP_ATLAS_RECIPE_SERIALIZER = Registry.register(Registry.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "crafting_atlas"), new SpecialRecipeSerializer<>(MapAtlasCreateRecipe::new));
        Registry.register(Registry.ITEM, new Identifier(MOD_ID,"atlas"), MAP_ATLAS);
    }
}
