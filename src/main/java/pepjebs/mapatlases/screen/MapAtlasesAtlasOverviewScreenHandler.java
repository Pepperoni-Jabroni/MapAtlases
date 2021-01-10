package pepjebs.mapatlases.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasesAtlasOverviewScreenHandler extends ScreenHandler {
    public MapAtlasesAtlasOverviewScreenHandler(int syncId) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return MapAtlasesAccessUtils.getAtlasFromItemStacks(player.inventory.main) != ItemStack.EMPTY;
    }
}
