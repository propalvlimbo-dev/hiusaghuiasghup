package org.xrose.utils.combat.rotations;

import java.security.SecureRandom;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.RotationContext;
import org.xrose.utils.combat.StopWatch;

public final class FunTimeRotation implements AuraRotation {
   private static final int ATTACK_TURN_SPEED = 130;
   private static final int RETURN_TURN_SPEED = 45;
   private static final long IDLE_ZERO_MS = 500L;
   private static final long STRAIGHT_LINE_MS = 435L;
   private static final long SWING_PITCH_MS = 250L;
   private static final long SWING_MS = 240L;
   private static final int SWING_EVERY_ATTACKS = 40;
   private final SecureRandom random = new SecureRandom();
   private final StopWatch attackTimer = new StopWatch();
   private int count = 0;

   @Override
   public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
      if (player != null) {
         FunTimeRotation.Angle currentAngle = currentAngle(player);
         FunTimeRotation.Angle targetAngle = aimAngle(player, targetEyePos);
         if (attackLikely) {
            FunTimeRotation.Angle angleDelta = delta(currentAngle, targetAngle);
            float yawDelta = angleDelta.yaw;
            float pitchDelta = angleDelta.pitch;
            float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
            float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 130.0F;
            float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 130.0F;
            RotationContext.setRotation(
               currentAngle.yaw + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
               currentAngle.pitch + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
            );
         } else {
            FunTimeRotation.Angle playerViewAngle = new FunTimeRotation.Angle(player.getYRot(), player.getXRot());
            FunTimeRotation.Angle deltaToView = delta(currentAngle, playerViewAngle);
            float yawDelta = deltaToView.yaw;
            float pitchDelta = deltaToView.pitch;
            float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
            float yaw = (float)(this.randomLerp(12.0F, 24.0F) * Math.sin(System.currentTimeMillis() / 40.0));
            float pitch = (float)(this.randomLerp(4.0F, 12.0F) * Math.cos(System.currentTimeMillis() / 40.0));
            if (target == null && this.attackTimer.finished(500.0)) {
               yaw = 0.0F;
               pitch = 0.0F;
            }

            float straightLineYaw = Math.abs(yawDelta / rotationDifference) * (!this.attackTimer.finished(435.0) ? 0 : 45);
            float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * (!this.attackTimer.finished(435.0) ? 0 : 45);
            if (this.count % 40 == 0 && this.count > 0 && !this.attackTimer.finished(250.0)) {
               pitch = -90.0F;
               if (this.attackTimer.finished(240.0)) {
                  player.swing(InteractionHand.MAIN_HAND);
               }
            }

            RotationContext.setRotation(
               currentAngle.yaw + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw) + yaw,
               currentAngle.pitch + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch) + pitch
            );
         }
      }
   }

   @Override
   public void onAttack() {
      this.attackTimer.reset();
      this.count++;
   }

   @Override
   public void reset() {
      this.count = 0;
      this.attackTimer.reset();
   }

   private static FunTimeRotation.Angle currentAngle(LocalPlayer player) {
      return RotationContext.isActive()
         ? new FunTimeRotation.Angle(RotationContext.getServerYaw(), RotationContext.getServerPitch())
         : new FunTimeRotation.Angle(player.getYRot(), player.getXRot());
   }

   private static FunTimeRotation.Angle aimAngle(LocalPlayer player, Vec3 targetEyePos) {
      Vec3 delta = targetEyePos.subtract(player.getEyePosition());
      return new FunTimeRotation.Angle(
         (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F, (float)(-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))))
      );
   }

   private static FunTimeRotation.Angle delta(FunTimeRotation.Angle start, FunTimeRotation.Angle end) {
      return new FunTimeRotation.Angle(wrapDegrees(end.yaw - start.yaw), wrapDegrees(end.pitch - start.pitch));
   }

   private static float wrapDegrees(float value) {
      float wrapped = value % 360.0F;
      if (wrapped >= 180.0F) {
         wrapped -= 360.0F;
      }

      if (wrapped < -180.0F) {
         wrapped += 360.0F;
      }

      return wrapped;
   }

   private float randomLerp(float min, float max) {
      return min + this.random.nextFloat() * (max - min);
   }

   private record Angle(float yaw, float pitch) {
   }
}

