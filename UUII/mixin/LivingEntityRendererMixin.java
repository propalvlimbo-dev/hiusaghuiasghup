package wtf.expensive.client.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.HitColor;
import wtf.expensive.client.util.render.ColorUtil;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void expensive$hitColor(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (!state.hasRedOverlay || Managment.FUNCTION_MANAGER == null || Managment.STYLE_MANAGER == null) return;
        Function function = Managment.FUNCTION_MANAGER.get("HitColor");
        if (function instanceof HitColor hitColor && hitColor.isState()) {
            float intensity = hitColor.intensity.getValue().floatValue();
            cir.setReturnValue(ColorUtil.interpolate(0xFFFFFFFF, Managment.STYLE_MANAGER.getColor(5), intensity));
        }
    }
}
