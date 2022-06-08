package pepjebs.mapatlases.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasesAtlasOverviewScreenHandler extends ScreenHandler {

    public Map<Integer, List<Integer>> idsToCenters = new HashMap<>();

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, PlayerInventory _playerInventory, PacketByteBuf buf) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        int numToRead = buf.readInt();
        for (int i = 0; i < numToRead; i++) {
            idsToCenters.put(buf.readInt(), Arrays.asList(buf.readInt(), buf.readInt()));
        }
    }

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, PlayerInventory _playerInventory, Map<Integer, List<Integer>> idsToCenters1) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        idsToCenters = idsToCenters1;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player.getInventory()) != ItemStack.EMPTY;
    }
}
