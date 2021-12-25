package pepjebs.mapatlases.mixin;

import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pepjebs.mapatlases.client.MapAtlasesClient;

@Mixin(MapRenderer.MapTexture.class)
public class MapRendererMixin {

    @Redirect(method = "draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void scaleProxy(MatrixStack matrices, float x, float y, float z) {
        int multiplier = MapAtlasesClient.getWorldMapZoomLevel();
        matrices.scale(x*multiplier, y*multiplier, z);
    }
}
