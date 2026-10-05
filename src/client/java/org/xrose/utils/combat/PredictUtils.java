package org.xrose.utils.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PredictUtils {
   private PredictUtils() {
   }

   public static Vec3 predict(LivingEntity target, double ticksAhead) {
      if (target.isFallFlying()) {
         Vec3 velocity = target.getDeltaMovement();
         double speed = velocity.length();
         return speed < 0.01 ? target.position() : target.position().add(velocity.scale(ticksAhead * 1.25));
      }

      if (Math.hypot(target.xOld - target.getX(), target.zOld - target.getZ()) * 20.0 <= 1.0 && target.yOld - target.getY() <= 1.0) {
         return target.position();
      }

      float pitch = target.getXRot() + (target.getXRot() - target.xRotO);
      float yaw = target.getYRot() + (target.getYRot() - target.yRotO);
      double deltaLength = new Vec3(target.getX() - target.xOld, target.getY() - target.yOld, target.getZ() - target.zOld).length();
      Vec3 forward = Vec3.directionFromRotation(pitch, yaw).scale(deltaLength * ticksAhead);
      Vec3 rotationVector = Vec3.directionFromRotation(pitch, yaw);
      float pitchRadians = target.getXRot() * (float) (Math.PI / 180.0);
      double horizontalLength = Math.sqrt(rotationVector.x * rotationVector.x + rotationVector.z * rotationVector.z);
      double forwardLength = forward.horizontalDistance();
      boolean falling = target.getDeltaMovement().y <= 0.0;
      double gravity = falling && target.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(target.getGravity(), 0.01) : target.getGravity();
      double cosineSquared = Mth.square(Math.cos(pitchRadians));
      forward = forward.add(0.0, gravity * (-1.0 + cosineSquared * 0.75), 0.0);
      if (forward.y < 0.0 && horizontalLength > 0.0) {
         double verticalAdjustment = forward.y * -0.1 * cosineSquared;
         forward = forward.add(
            rotationVector.x * verticalAdjustment / horizontalLength, verticalAdjustment, rotationVector.z * verticalAdjustment / horizontalLength
         );
      }

      if (pitchRadians < 0.0F && horizontalLength > 0.0) {
         double horizontalAdjustment = forwardLength * -Mth.sin(pitchRadians) * 0.04;
         forward = forward.add(
            -rotationVector.x * horizontalAdjustment / horizontalLength,
            horizontalAdjustment * 2.2F,
            -rotationVector.z * horizontalAdjustment / horizontalLength
         );
      }

      if (horizontalLength > 0.0) {
         forward = forward.add(
            (rotationVector.x / horizontalLength * forwardLength - forward.x) * 0.1,
            0.0,
            (rotationVector.z / horizontalLength * forwardLength - forward.z) * 0.1
         );
      }

      return target.position().add(forward);
   }
}

