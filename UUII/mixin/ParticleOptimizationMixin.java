package wtf.expensive.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;

@Mixin(ParticleEngine.class)
public abstract class ParticleOptimizationMixin {
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void expensive$skipParticles(ParticlesRenderState state, Frustum frustum, Camera camera,
                                         float partialTick, CallbackInfo ci) {
        if (Managment.FUNCTION_MANAGER != null && Managment.FUNCTION_MANAGER.optimization.isState()
                && Managment.FUNCTION_MANAGER.optimization.options.get(1)) {
            ci.cancel();
        }
    }
}
