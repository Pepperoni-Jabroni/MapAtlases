package pepjebs.mapatlases.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.List;

public class MapAtlasItem extends Item {
    public MapAtlasItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        if (world != null && world.isClient) {
            MapState mapState = MapAtlasesAccessUtils.getRandomMapStateFromAtlas(world, stack);
            CompoundTag tag = stack.getTag();
            int[] mapIds = tag.getIntArray("maps");
            int empties = tag.getInt("empty");
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_1", ArrayUtils.getLength(mapIds))
                    .formatted(Formatting.GRAY));
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_2", empties)
                    .formatted(Formatting.GRAY));
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale)
                    .formatted(Formatting.GRAY));
            if (mapState.dimension != null) {
                tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_4", mapState.dimension.getValue())
                        .formatted(Formatting.GRAY));
            }
        }
    }
}
