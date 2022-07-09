package pepjebs.mapatlases.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    private static final MapAtlasesHUD mapAtlasesAtlasHUD = new MapAtlasesHUD();

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void renderHUDActiveAtlasMap(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        mapAtlasesAtlasHUD.render(matrices);
    }
}
