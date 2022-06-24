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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.CartographyTableScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(CartographyTableScreenHandler.class)
public abstract class CartographyTableScreenHandlerMixin extends ScreenHandler {

    @Shadow
    CraftingResultInventory resultInventory;

    protected CartographyTableScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    void antiqueatlas_call(ItemStack atlasTop, ItemStack atlasBottom, ItemStack oldResult, CallbackInfo info) {
        if (atlasTop.getItem() == MapAtlasesMod.MAP_ATLAS && atlasBottom.getItem() == MapAtlasesMod.MAP_ATLAS) {
            ItemStack result = atlasTop.copy();
            result.increment(1);
            this.resultInventory.setStack(CartographyTableScreenHandler.RESULT_SLOT_INDEX, result);

            this.sendContentUpdates();

            info.cancel();
        } else if (atlasTop.getItem() == MapAtlasesMod.MAP_ATLAS && atlasBottom.getItem() == Items.MAP) {
            ItemStack result = atlasTop.copy();
            NbtCompound nbt = result.getNbt() != null ? result.getNbt() : new NbtCompound();
            nbt.putInt("empty", nbt.getInt("empty") + atlasBottom.getCount());
            result.setNbt(nbt);
            this.resultInventory.setStack(CartographyTableScreenHandler.RESULT_SLOT_INDEX, result);

            this.sendContentUpdates();

            info.cancel();
        }
    }

    @Inject(method = "transferSlot", at = @At("HEAD"), cancellable = true)
    void antiqueatlas_transferSlot(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> info) {
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