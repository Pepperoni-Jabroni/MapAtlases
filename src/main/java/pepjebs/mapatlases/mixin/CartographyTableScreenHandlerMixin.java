/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 *      /hunternif/mc/impl/atlas/mixin/MixinCartographyTableScreenHandler.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Shadow;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Mixin(CartographyTableScreenHandler.class)
public abstract class CartographyTableScreenHandlerMixin extends ScreenHandler {

    @Shadow
    CraftingResultInventory resultInventory;

    @Shadow
    ScreenHandlerContext context;

    protected CartographyTableScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    void mapAtlasUpdateResult(ItemStack atlas, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && bottomItem.getItem() == MapAtlasesMod.MAP_ATLAS) {
            final int[] allMapIds = Stream.of(Arrays.stream(MapAtlasesAccessUtils.getMapIdsFromItemStack(atlas)),
                    Arrays.stream(MapAtlasesAccessUtils.getMapIdsFromItemStack(bottomItem)))
                    .flatMapToInt(x -> x)
                    .distinct()
                    .toArray();
            this.context.run((world, blockPos) -> {
                // Ensure duplicate X,Z maps are removed (as adding duplicate maps is bad practice)
                Map<String, Pair<Integer, MapState>> uniqueXZMapIds =
                                Arrays.stream(allMapIds)
                                        .mapToObj(mId -> new Pair<>(mId, world.getMapState("map_" + mId)))
                                        .filter(m -> m.getRight() != null)
                                        .collect(Collectors.toMap(
                                                m -> m.getRight().centerX + ":" + m.getRight().centerZ
                                                        + ":"  + m.getRight().dimension,
                                                m -> m,
                                                (m1, m2) -> m1));
                int[] filteredMapIds = uniqueXZMapIds.values().stream().mapToInt(Pair::getLeft).toArray();
                ItemStack result = new ItemStack(MapAtlasesMod.MAP_ATLAS);
                NbtCompound mergedNbt = new NbtCompound();
                int halfEmptyCount = (int) Math.ceil((MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas)
                        + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(bottomItem)) / 2.0);
                mergedNbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, halfEmptyCount);
                mergedNbt.putIntArray(MapAtlasItem.MAP_LIST_NBT, filteredMapIds);
                result.setNbt(mergedNbt);

                result.increment(1);
                this.resultInventory.setStack(CartographyTableScreenHandler.RESULT_SLOT_INDEX, result);
            });

            this.sendContentUpdates();

            info.cancel();
        } else if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && bottomItem.getItem() == Items.MAP) {
            ItemStack result = atlas.copy();
            NbtCompound nbt = result.getNbt() != null ? result.getNbt() : new NbtCompound();
            int amountToAdd = MapAtlasesAccessUtils.getMapCountToAdd(atlas, bottomItem);
            nbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, nbt.getInt(MapAtlasItem.EMPTY_MAP_NBT) + amountToAdd);
            result.setNbt(nbt);
            this.resultInventory.setStack(CartographyTableScreenHandler.RESULT_SLOT_INDEX, result);

            this.sendContentUpdates();

            info.cancel();
        }
    }

    @Inject(method = "transferSlot", at = @At("HEAD"), cancellable = true)
    void mapAtlasTransferSlot(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> info) {
        if (index >= 0 && index <= 2) return;

        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();

            if (stack.getItem() != MapAtlasesMod.MAP_ATLAS) return;

            boolean result = this.insertItem(stack, 0, 2, false);

            if (!result) {
                info.setReturnValue(ItemStack.EMPTY);
            }
        }
    }

}