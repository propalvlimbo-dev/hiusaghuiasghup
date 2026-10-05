package org.xrose.utils.combat.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.RotationContext;

public final class GrimRotation implements AuraRotation {
   private static final float MAX_YAW_SPEED = 32.0F;
   private static final float MAX_PITCH_SPEED = 20.0F;
   private float smoothedYaw;
   private float smoothedPitch;
   private boolean initialized;

   @Override
   public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
      if (target != null && target.isAlive()) {
         Vec3 eye = player.getEyePosition();
         Vec3 delta = targetEyePos.subtract(eye);
         double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
         float idealYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
         float idealPitch = Mth.clamp((float)(-Math.toDegrees(Math.atan2(delta.y, horizontal))), -90.0F, 90.0F);
         float currentYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
         float currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
         if (!this.initialized) {
            this.smoothedYaw = currentYaw;
            this.smoothedPitch = currentPitch;
            this.initialized = true;
         }

         float deltaYaw = Mth.wrapDegrees(idealYaw - this.smoothedYaw);
         float deltaPitch = idealPitch - this.smoothedPitch;
         double dist = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
         float speedFactor = (float)Math.min(1.0, Math.max(0.15, cubicEase(Math.min(1.0, dist / 45.0))));
         float stepYaw = Mth.clamp(deltaYaw * speedFactor, -32.0F, 32.0F);
         float stepPitch = Mth.clamp(deltaPitch * speedFactor, -20.0F, 20.0F);
         ThreadLocalRandom random = ThreadLocalRandom.current();
         stepYaw += (float)random.nextDouble(-0.15, 0.15);
         stepPitch += (float)random.nextDouble(-0.1, 0.1);
         this.smoothedYaw = Mth.wrapDegrees(this.smoothedYaw + stepYaw);
         this.smoothedPitch = Mth.clamp(this.smoothedPitch + stepPitch, -90.0F, 90.0F);
         RotationContext.setRotation(this.smoothedYaw, this.smoothedPitch);
      } else {
         this.reset();
         RotationContext.clear();
      }
   }

   @Override
   public void reset() {
      this.initialized = false;
   }

   private static double cubicEase(double t) {
      return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
   }

   private static float getGcdStep() {
      double sensitivity = (Double)Minecraft.getInstance().options.sensitivity().get();
      double factor = sensitivity * 0.6 + 0.2;
      return (float)(factor * factor * factor * 1.2);
   }
}

