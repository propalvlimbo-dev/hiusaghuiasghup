package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.combat.AuraFunction;

@Mixin(LivingEntityRenderer.class)
public abstract class SilentRotationRenderMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN"))
    private void expensive$applySilentRotation(LivingEntity entity, LivingEntityRenderState state,
                                               float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity != minecraft.player || Managment.FUNCTION_MANAGER == null) {
            return;
        }
        Function function = Managment.FUNCTION_MANAGER.get("Aura");
        if (!(function instanceof AuraFunction aura) || !aura.isState() || aura.target == null) {
            return;
        }
        float headYaw = aura.rotate.x;
        state.bodyRot = Mth.rotLerp(0.5f, state.bodyRot, headYaw);
        state.yRot = Mth.wrapDegrees(headYaw - state.bodyRot);
        state.xRot = aura.rotate.y;
    }
}
