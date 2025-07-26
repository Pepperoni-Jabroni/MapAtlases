package pepjebs.mapatlases.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasesAtlasOverviewScreenHandler extends ScreenHandler {

    public ItemStack atlas = ItemStack.EMPTY;
    public String centerMapId = "";
    public int atlasScale = 128;
    public Map<Integer, Pair<String,List<Integer>>> idsToCenters = new HashMap<>();

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, PlayerInventory _playerInventory, MapAtlasesAtlasOverviewScreenData data) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        atlas = data.atlas();
        centerMapId = data.centerMapId();
        atlasScale = data.atlasScale();
        idsToCenters = data.idsToCenters();
    }

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, PlayerInventory _playerInventory,
                                                Map<Integer, Pair<String,List<Integer>>> idsToCenters1,
                                                ItemStack atlas1,
                                                String centerMapId1,
                                                int atlasScale1) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        idsToCenters = idsToCenters1;
        atlas = atlas1;
        centerMapId = centerMapId1;
        atlasScale = atlasScale1;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {return true;}
}
