package org.xrose.utils.combat.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.RotationContext;

public final class DroidRotation implements AuraRotation {
   private static final float MAX_YAW_SPEED = 24.0F;
   private static final float MAX_PITCH_SPEED = 12.0F;
   private static final long RAMP_DURATION_MS = 400L;
   private float lastYawSpeed;
   private float lastPitchSpeed;
   private long rayLostTimestamp = -1L;

   @Override
   public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
      if (target != null && target.isAlive()) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         double jitter = random.nextDouble(0.0, 1.0);
         Vec3 eye = player.getEyePosition();
         Vec3 delta = targetEyePos.subtract(eye);
         double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
         float idealYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
         float idealPitch = Mth.clamp((float)(-Math.toDegrees(Math.atan2(delta.y, horizontal))), -90.0F, 90.0F);
         float currentYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
         float currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
         float deltaYaw = Mth.wrapDegrees(idealYaw - currentYaw);
         float deltaPitch = idealPitch - currentPitch;
         float serverYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
         float serverPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
         boolean rayHitsTarget = this.isRayOnTarget(player, target, serverYaw, serverPitch);
         boolean rayHitsSmallBox = rayHitsTarget && this.isRayOnSmallHitbox(player, target, serverYaw, serverPitch);
         float yawSpeed;
         float pitchSpeed;
         if (rayHitsTarget) {
            this.rayLostTimestamp = -1L;
            if (player.distanceTo(target) > 0.5F) {
               if (!rayHitsSmallBox) {
                  yawSpeed = randomLerp(random, 0.6F, 0.8F);
                  pitchSpeed = randomLerp(random, 0.2F, 0.6F);
               } else {
                  yawSpeed = randomLerp(random, -0.02F, 0.02F);
                  pitchSpeed = randomLerp(random, -0.01F, 0.01F);
               }
            } else {
               yawSpeed = 0.0F;
               pitchSpeed = 0.0F;
            }
         } else {
            long now = System.currentTimeMillis();
            if (this.rayLostTimestamp == -1L) {
               this.rayLostTimestamp = now;
            }

            long elapsed = now - this.rayLostTimestamp;
            if (elapsed < 400L) {
               double yawProgress = elapsed / 400.0;
               double pitchProgress = elapsed / 600.0;
               float maxYaw = 24.0F;
               float maxPitch = 12.0F;
               if (player.distanceTo(target) < 0.95F) {
                  yawSpeed = randomLerp(random, 8.0F, 12.0F);
               } else {
                  yawSpeed = lerp(0.0F, maxYaw, (float)yawProgress);
               }

               pitchSpeed = lerp(0.0F, maxPitch, (float)Math.min(pitchProgress, 1.0));
               this.lastYawSpeed = yawSpeed;
               this.lastPitchSpeed = pitchSpeed;
            } else {
               yawSpeed = this.lastYawSpeed + (float)jitter;
               pitchSpeed = this.lastPitchSpeed + (float)jitter;
            }
         }

         float stepYaw = Mth.clamp(deltaYaw, -Math.abs(yawSpeed), Math.abs(yawSpeed));
         float stepPitch = Mth.clamp(deltaPitch, -Math.abs(pitchSpeed), Math.abs(pitchSpeed));
         float returnYawSpeed = 8.0F + (float)jitter;
         float returnPitchSpeed = 4.0F + (float)jitter;
         if (rayHitsTarget && Math.abs(deltaYaw) < 2.0F && Math.abs(deltaPitch) < 2.0F) {
            stepYaw *= 0.3F;
            stepPitch *= 0.3F;
         }

         RotationContext.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -90.0F, 90.0F));
      } else {
         this.reset();
         RotationContext.clear();
      }
   }

   @Override
   public void reset() {
      this.rayLostTimestamp = -1L;
      this.lastYawSpeed = 0.0F;
      this.lastPitchSpeed = 0.0F;
   }

   private boolean isRayOnTarget(LocalPlayer player, LivingEntity target, float yaw, float pitch) {
      Vec3 eyeVec = player.getEyePosition();
      Vec3 lookVec = Vec3.directionFromRotation(pitch, yaw);
      double distance = player.distanceTo(target) + 2.0;
      Vec3 endVec = eyeVec.add(lookVec.scale(distance));
      AABB aabb = target.getBoundingBox();
      return aabb.contains(eyeVec) || aabb.clip(eyeVec, endVec).isPresent();
   }

   private boolean isRayOnSmallHitbox(LocalPlayer player, LivingEntity target, float yaw, float pitch) {
      Vec3 eyeVec = player.getEyePosition();
      Vec3 lookVec = Vec3.directionFromRotation(pitch, yaw);
      double distance = player.distanceTo(target) + 2.0;
      Vec3 endVec = eyeVec.add(lookVec.scale(distance));
      AABB originalBB = target.getBoundingBox();
      double shrinkFactor = 1.6;
      double shrinkFactorY = 1.2;
      double centerX = (originalBB.minX + originalBB.maxX) / 2.0;
      double centerY = (originalBB.minY + originalBB.maxY) / 2.0;
      double centerZ = (originalBB.minZ + originalBB.maxZ) / 2.0;
      double halfX = (originalBB.maxX - originalBB.minX) / 2.0 / shrinkFactor;
      double halfY = (originalBB.maxY - originalBB.minY) / 2.0 / shrinkFactorY;
      double halfZ = (originalBB.maxZ - originalBB.minZ) / 2.0 / shrinkFactor;
      AABB smallBB = new AABB(centerX - halfX, centerY - halfY, centerZ - halfZ, centerX + halfX, centerY + halfY, centerZ + halfZ);
      return smallBB.contains(eyeVec) || smallBB.clip(eyeVec, endVec).isPresent();
   }

   private static float lerp(float start, float end, float t) {
      return start + t * (end - start);
   }

   private static float randomLerp(ThreadLocalRandom random, float min, float max) {
      return min + random.nextFloat() * (max - min);
   }
}

