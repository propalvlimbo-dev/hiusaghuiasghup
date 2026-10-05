package org.xrose.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.context.RotationContext;
import org.xrose.feature.impl.player.FreeCamFeature;
import org.xrose.feature.impl.player.FreeLookFeature;
import org.xrose.feature.impl.visual.RemovalsFeature;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @Shadow
   protected abstract void setPosition(double var1, double var3, double var5);

   @Inject(method = "setPosition(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
   private void xrose$freeCamPosition(Vec3 pos, CallbackInfo ci) {
      FreeCamFeature cam = FreeCamFeature.getInstance();
      if (cam != null && cam.isEnabled() && cam.pos != null) {
         Vec3 nextPos = FreeCamFeature.getInterpolatedCameraPosition();
         if (nextPos != null && Double.isFinite(nextPos.x) && Double.isFinite(nextPos.y) && Double.isFinite(nextPos.z)) {
            if (!(nextPos.distanceToSqr(pos) <= 1.0E-8)) {
               this.setPosition(nextPos.x, nextPos.y, nextPos.z);
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void onExtractRenderState(CameraRenderState cameraState, float cameraEntityPartialTick, CallbackInfo ci) {
      if (RemovalsFeature.shouldRemoveBadEffectsVisuals()) {
         cameraState.entityRenderState.doesMobEffectBlockSky = false;
      }
   }

   @Inject(method = "alignWithEntity", at = @At("HEAD"))
   private void onAlignWithEntity(float partialTick, CallbackInfo ci) {
      Camera camera = (Camera)(Object)this;
      Entity entity = camera.entity();
      if (entity != null) {
         RotationContext.applyRenderInterpolation();
         RotationContext.syncFreeLook(entity.getViewYRot(partialTick), entity.getViewXRot(partialTick));
      }
   }

   @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
   private float xrose$freeLookYaw(float original) {
      Float yaw = FreeLookFeature.cameraYaw();
      return yaw != null ? yaw : original;
   }

   @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
   private float xrose$freeLookPitch(float original) {
      Float pitch = FreeLookFeature.cameraPitch();
      return pitch != null ? pitch : original;
   }
}

