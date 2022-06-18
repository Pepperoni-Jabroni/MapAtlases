/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 *      /hunternif/mc/impl/atlas/mixin/prod/MixinCartographyTableHandlerSlot.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;

@Mixin(targets = "net.minecraft.screen.CartographyTableScreenHandler$3")
class MixinCartographyTableScreenHandlerFirstSlot {

    @Inject(method = "canInsert", at = @At("RETURN"), cancellable = true)
    void antiqueatlas_canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.getItem() == MapAtlasesMod.MAP_ATLAS || info.getReturnValueZ());
    }
}

@Mixin(targets = "net.minecraft.screen.CartographyTableScreenHandler$4")
class MixinCartographyTableScreenHandlerSecondSlot {

    @Inject(method = "canInsert", at = @At("RETURN"), cancellable = true)
    void antiqueatlas_canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.getItem() == MapAtlasesMod.MAP_ATLAS || info.getReturnValueZ());
    }
}