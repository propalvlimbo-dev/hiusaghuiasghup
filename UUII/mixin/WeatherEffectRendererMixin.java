package wtf.expensive.client.mixin;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.NoRenderFunction;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void expensive$weather(Vec3 cameraPosition, WeatherRenderState state, CallbackInfo ci) {
        if (Managment.FUNCTION_MANAGER == null) return;
        Function function = Managment.FUNCTION_MANAGER.get("No Render");
        if (function instanceof NoRenderFunction noRender && noRender.isState() && noRender.elements.get(6)) {
            ci.cancel();
        }
    }
}
