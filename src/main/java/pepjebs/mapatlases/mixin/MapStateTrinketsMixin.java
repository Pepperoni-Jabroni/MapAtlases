package pepjebs.mapatlases.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import pepjebs.mapatlases.MapAtlasesMod;

@Mixin(value = MapState.class, priority = 1100)
public class MapStateTrinketsMixin {
    /*
    @Redirect(
            method = "update(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;contains(Lnet/minecraft/item/ItemStack;)Z")
    )
    private boolean containsProxy(PlayerInventory inventory, ItemStack stack) {
        return inventory.contains(stack) || (TrinketsApi.getTrinketComponent(inventory.player).isPresent()
                && TrinketsApi.getTrinketComponent(inventory.player).get()
                    .getEquipped(MapAtlasesMod.MAP_ATLAS).size() > 0);
    }
     */
    @ModifyExpressionValue(method = "update(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;contains(Ljava/util/function/Predicate;)Z"))
    private boolean containsProxy(boolean original, PlayerEntity player, ItemStack stack) {
        PlayerInventory inventory = player.getInventory();
        return inventory.contains(stack) || (TrinketsApi.getTrinketComponent(inventory.player).isPresent()
                && TrinketsApi.getTrinketComponent(inventory.player).get()
                    .getEquipped(MapAtlasesMod.MAP_ATLAS).size() > 0);
    }
}
