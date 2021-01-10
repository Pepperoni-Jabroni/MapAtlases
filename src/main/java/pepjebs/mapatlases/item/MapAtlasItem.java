package pepjebs.mapatlases.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NetworkSyncedItem;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.List;

public class MapAtlasItem extends Item {

    public static final int MAX_MAP_COUNT = 32;

    public MapAtlasItem(Settings settings) {
        super(settings);
    }

//    @Override
//    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
//        if (!world.isClient && slot < 9) {
//            List<MapState> mapStates = MapAtlasesAccessUtils.getAllMapStatesFromAtlas(world, stack);
//            if (mapStates.isEmpty()) return;
//            for(MapState state : mapStates) {
//                state.update((PlayerEntity) entity, stack);
//                state.getPlayerSyncData((PlayerEntity) entity);
//            }
//        }
//    }

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
            int mapSize = MapAtlasesAccessUtils.getMapCountFromItemStack(stack);
            int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(stack);
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_1", mapSize)
                    .formatted(Formatting.GRAY));
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_2", empties)
                    .formatted(Formatting.GRAY));
            tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale)
                    .formatted(Formatting.GRAY));
            if (mapState.dimension != null) {
                String dimensionName = mapState.dimension.getValue().getPath();
                dimensionName = dimensionName.substring(0, 1).toUpperCase() + dimensionName.substring(1);
                tooltip.add(new TranslatableText("item.map_atlases.atlas.tooltip_4",
                        dimensionName).formatted(Formatting.GRAY));
            }
        }
    }
}
