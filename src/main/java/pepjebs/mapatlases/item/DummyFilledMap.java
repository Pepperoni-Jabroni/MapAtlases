package pepjebs.mapatlases.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DummyFilledMap extends Item {

    public DummyFilledMap(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.dummy_filled_map.dummy"))
                .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
        tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.dummy_filled_map.desc"))
                .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
    }
}
