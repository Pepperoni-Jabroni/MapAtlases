package pepjebs.mapatlases.item;

import net.minecraft.client.item.TooltipType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;

public class DummyFilledMap extends Item {

    public DummyFilledMap(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("item.map_atlases.dummy_filled_map.dummy")
                .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.map_atlases.dummy_filled_map.desc")
                .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
    }
}
