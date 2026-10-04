package wtf.expensive.client.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.FullBrightFunction;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void expensive$fullBright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (Managment.FUNCTION_MANAGER == null) return;
        Function function = Managment.FUNCTION_MANAGER.get("FullBright");
        if (function instanceof FullBrightFunction fullBright && fullBright.isState() && fullBright.type.is("Gamma")) {
            state.brightness = 1f;
            state.blockFactor = 1f;
            state.skyFactor = 1f;
            state.darknessEffectScale = 0f;
            state.nightVisionEffectIntensity = 1f;
            state.needsUpdate = true;
        }
    }
}
