package pepjebs.mapatlases.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NetworkSyncedItem;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.List;

public class MapAtlasItem extends NetworkSyncedItem {
    public MapAtlasItem(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public Packet<?> createSyncPacket(ItemStack stack, World world, PlayerEntity player) {
        return super.createSyncPacket(stack, world, player);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        if (world != null && world.isClient) {
            MapState mapState = MapAtlasesAccessUtils.getRandomMapStateFromAtlas(world, stack);
            if (mapState == null) {
                tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_err")
                        .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
                return;
            }
            CompoundTag tag = stack.getTag();
            int mapSize = tag != null ? tag.getIntArray("maps").length : 0;
            int empties = tag != null ? tag.getIntArray("empty").length : 0;
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_1", mapSize)
                    .formatted(Formatting.GRAY));
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_2", empties)
                    .formatted(Formatting.GRAY));
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale)
                    .formatted(Formatting.GRAY));
            if (mapState.dimension != null) {
                tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_4",
                        mapState.dimension.getValue().getPath()).formatted(Formatting.GRAY));
            }
        }
    }
}
