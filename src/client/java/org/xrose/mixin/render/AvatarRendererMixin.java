package org.xrose.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.feature.impl.visual.NameTagsFeature;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.render.ClientCape;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("TAIL")
   )
   private void updateRenderStateAura(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && avatar.getUUID() == client.player.getUUID()) {
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

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("TAIL")
   )
   private void forceCapeVisible(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
      if (ClientCape.shouldForceCape(avatar.getUUID())) {
         state.showCape = true;
      }
   }

   @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
   private void hideVanillaNameTag(Avatar avatar, double distanceSqr, CallbackInfoReturnable<Boolean> cir) {
      if (NameTagsFeature.shouldHideVanillaTag()) {
         cir.setReturnValue(false);
      }
   }
}

