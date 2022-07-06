package pepjebs.mapatlases.mixin;

import com.google.common.collect.Maps;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapStateIntrfc;

import java.util.Map;

@Mixin(MapState.class)
public class MapStateMixin implements MapStateIntrfc {
    @Shadow
    Map<String, MapIcon> icons = Maps.newLinkedHashMap();

    public Map<String, MapIcon> getFullIcons(){
        return icons;
    }

    @Redirect(
            method = "update(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;contains(Lnet/minecraft/item/ItemStack;)Z")
    )
    private boolean containsProxy(PlayerInventory inventory, ItemStack stack) {
        return inventory.contains(stack) || (FabricLoader.getInstance().isModLoaded("trinkets")
                && TrinketsApi.getTrinketComponent(inventory.player).isPresent()
                && TrinketsApi.getTrinketComponent(inventory.player).get()
                    .getEquipped(MapAtlasesMod.MAP_ATLAS).size() > 0);
    }
}
