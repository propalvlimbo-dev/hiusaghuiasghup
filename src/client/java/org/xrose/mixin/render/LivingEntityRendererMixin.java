package org.xrose.mixin.render;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.feature.impl.visual.SeeInvisibleFeature;
import org.xrose.utils.combat.aura.AngleConnection;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   private static final Set<LivingEntityRenderState> seeInvisible$originallyInvisible = Collections.newSetFromMap(new WeakHashMap<>());

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void updateRenderStateAura(LivingEntity entity, S state, float tickDelta, CallbackInfo ci) {
      if (SeeInvisibleFeature.isActive() && state.isInvisibleToPlayer) {
         seeInvisible$originallyInvisible.add(state);
         state.isInvisibleToPlayer = false;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player != null && entity == client.player) {
         AngleConnection controller = AngleConnection.INSTANCE;
         if (controller.getFakeAngle() != null) {
            float prevHeadYaw = controller.getPreviousFakeRotation().getYaw();
            float currHeadYaw = controller.getFakeRotation().getYaw();
            float prevPitch = controller.getPreviousFakeRotation().getPitch();
            float currPitch = controller.getFakeRotation().getPitch();
            float prevBodyYaw = controller.getPreviousFakeBodyYaw();
            float currBodyYaw = controller.getFakeBodyYaw();
            float headYaw = Mth.rotLerp(tickDelta, prevHeadYaw, currHeadYaw);
            float pitch = Mth.clamp(Mth.lerp(tickDelta, prevPitch, currPitch), -90.0F, 90.0F);
            float bodyYaw = Mth.rotLerp(tickDelta, prevBodyYaw, currBodyYaw);
            float maxHeadRotation = 52.0F;
            float headBodyDiff = Mth.wrapDegrees(headYaw - bodyYaw);
            if (Math.abs(headBodyDiff) > maxHeadRotation) {
               bodyYaw = headYaw - Mth.sign(headBodyDiff) * maxHeadRotation;
            }

            state.bodyRot = bodyYaw;
            state.yRot = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -maxHeadRotation, maxHeadRotation);
            state.xRot = pitch;
         } else {
            if (controller.getCurrentAngle() != null) {
               float headYaw = Mth.rotLerp(tickDelta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw());
               float pitch = Mth.lerp(tickDelta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch());
               float maxHeadRotation = 52.0F;
               state.yRot = Mth.clamp(Mth.wrapDegrees(headYaw - state.bodyRot), -maxHeadRotation, maxHeadRotation);
               state.xRot = Mth.clamp(pitch, -90.0F, 90.0F);
            }
         }
      }
   }

   @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
   private void seeInvisible$forceTranslucent(S renderState, boolean visible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
      if (seeInvisible$originallyInvisible.contains(renderState) && SeeInvisibleFeature.isActive()) {
         LivingEntityRenderer self = (LivingEntityRenderer)(Object)this;
         cir.setReturnValue(RenderTypes.entityTranslucent(self.getTextureLocation(renderState)));
      }
   }

   @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
   private void seeInvisible$applyAlpha(S renderState, CallbackInfoReturnable<Integer> cir) {
      if (seeInvisible$originallyInvisible.contains(renderState) && SeeInvisibleFeature.isActive()) {
         Integer original = (Integer)cir.getReturnValue();
         int rgb = original != null ? original : -1;
         int a = (int)(SeeInvisibleFeature.getAlpha() * 255.0F);
         int r = rgb & 0xFF;
         int g = rgb >> 8 & 0xFF;
         int b = rgb >> 16 & 0xFF;
         cir.setReturnValue(a << 24 | b << 16 | g << 8 | r);
      }
   }
}

