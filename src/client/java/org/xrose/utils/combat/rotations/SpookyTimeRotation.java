package org.xrose.utils.combat.rotations;

import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.RotationContext;

public final class SpookyTimeRotation implements AuraRotation {
   private static final SecureRandom SECURE_RANDOM = new SecureRandom();
   private final MouseProfile profile = MouseProfile.DEFAULT;
   private LivingEntity lastTarget = null;
   private float startYaw = 0.0F;
   private float startPitch = 0.0F;
   private float targetYaw = 0.0F;
   private float targetPitch = 0.0F;
   private long startTimeMs = 0L;
   private long durationMs = 1L;
   private float currentJitterYaw = 0.0F;
   private float currentJitterPitch = 0.0F;

   @Override
   public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
      float fromYaw = player.yHeadRot;
      float fromPitch = player.getXRot();
      AABB box = target.getBoundingBox();
      double centerX = (box.minX + box.maxX) * 0.5;
      double centerZ = (box.minZ + box.maxZ) * 0.5;
      double bodyY = box.minY + (box.maxY - box.minY) * 0.4;
      SpookyTimeRotation.Rotation rawGoal = toPoint(player, new Vec3(centerX, bodyY, centerZ));
      long now = System.currentTimeMillis();
      double deltaYaw = Mth.wrapDegrees(rawGoal.yaw() - fromYaw);
      double deltaPitch = rawGoal.pitch() - fromPitch;
      double totalDistance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
      if (this.lastTarget != target || now - this.startTimeMs >= this.durationMs || totalDistance > 45.0) {
         this.lastTarget = target;
         this.startYaw = fromYaw;
         this.startPitch = fromPitch;
         this.startPitch = fromPitch;
         double distanceToTarget = player.distanceTo(target);
         double angularWidth = Math.toDegrees(Math.atan2(box.getXsize(), Math.max(distanceToTarget, 0.1)));
         long fittsTime = KinematicsUtil.calculateFittsLawTime(totalDistance, angularWidth, this.profile.getFittsA(), this.profile.getFittsB());
         this.durationMs = Mth.clamp(fittsTime, attackLikely ? 40L : 70L, 400L);
         this.startTimeMs = now;
         float overshootYaw = 0.0F;
         float overshootPitch = 0.0F;
         if (totalDistance > 35.0 && SECURE_RANDOM.nextDouble() < this.profile.getOvershootProbability()) {
            overshootYaw = (float)((SECURE_RANDOM.nextDouble() - 0.5) * 4.0);
            overshootPitch = (float)((SECURE_RANDOM.nextDouble() - 0.5) * 2.0);
         }

         this.targetYaw = rawGoal.yaw() + overshootYaw;
         this.targetPitch = Mth.clamp(rawGoal.pitch() + overshootPitch, -90.0F, 90.0F);
      }

      double progress = (double)(now - this.startTimeMs) / this.durationMs;
      progress = Mth.clamp(progress, 0.0, 1.0);
      double currentYawUnwrapped = KinematicsUtil.getMinimumJerk(this.startYaw, this.startYaw + Mth.wrapDegrees(this.targetYaw - this.startYaw), progress);
      double currentPitchUnwrapped = KinematicsUtil.getMinimumJerk(this.startPitch, this.targetPitch, progress);
      float targetJitterYaw = legitRandom(-1.2F, 1.2F) * (float)(1.0 - Math.abs(progress - 0.5));
      float targetJitterPitch = legitRandom(-0.8F, 0.8F) * (float)(1.0 - Math.abs(progress - 0.5));
      this.currentJitterYaw = Mth.lerp(0.2F, this.currentJitterYaw, targetJitterYaw);
      this.currentJitterPitch = Mth.lerp(0.2F, this.currentJitterPitch, targetJitterPitch);
      float yaw = Mth.wrapDegrees((float)currentYawUnwrapped + this.currentJitterYaw);
      float pitch = Mth.clamp((float)currentPitchUnwrapped + this.currentJitterPitch, -90.0F, 90.0F);
      SpookyTimeRotation.Rotation corrected = correctRotation(fromYaw, fromPitch, yaw, pitch);
      RotationContext.setRotation(corrected.yaw(), corrected.pitch());
   }

   private static SpookyTimeRotation.Rotation toPoint(LocalPlayer player, Vec3 point) {
      Vec3 eye = player.getEyePosition();
      double dx = point.x - eye.x;
      double dy = point.y - eye.y;
      double dz = point.z - eye.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = Mth.clamp((float)(-Math.toDegrees(Math.atan2(dy, horizontal))), -90.0F, 90.0F);
      return new SpookyTimeRotation.Rotation(yaw, pitch);
   }

   private static float legitRandom(float min, float max) {
      return min + (max - min) * SECURE_RANDOM.nextFloat();
   }

   private static SpookyTimeRotation.Rotation correctRotation(float fromYaw, float fromPitch, float toYaw, float toPitch) {
      float step = gcdStep();
      float deltaYaw = Mth.wrapDegrees(toYaw - fromYaw);
      float deltaPitch = toPitch - fromPitch;
      deltaYaw = Math.round(deltaYaw / step) * step;
      deltaPitch = Math.round(deltaPitch / step) * step;
      return new SpookyTimeRotation.Rotation(fromYaw + deltaYaw, Mth.clamp(fromPitch + deltaPitch, -90.0F, 90.0F));
   }

   private static float gcdStep() {
      double sensitivity = (Double)Minecraft.getInstance().options.sensitivity().get();
      double factor = sensitivity * 0.6 + 0.2;
      return (float)(factor * factor * factor * 1.2);
   }

   private record Rotation(float yaw, float pitch) {
   }
}

