package pepjebs.mapatlases.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.item.MapAtlasItem;

@Mixin(LecternBlock.class)
public class LecternBlockMixin extends Block {

    public LecternBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "<init>(Lnet/minecraft/block/AbstractBlock$Settings;)V", at = @At(value = "RETURN"))
    public void lecternAtlasConstructorMixin(AbstractBlock.Settings settings, CallbackInfo ci) {
        this.setDefaultState(this.getDefaultState().with(MapAtlasItem.HAS_ATLAS, false));
    }

    @Inject(
            method = "appendProperties(Lnet/minecraft/state/StateManager$Builder;)V",
            at = @At(value = "RETURN")
    )
    public void injectAtlasProperty(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(new Property[]{MapAtlasItem.HAS_ATLAS});
    }
}
