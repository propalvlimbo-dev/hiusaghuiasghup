package org.xrose.utils.combat.aura.rotations;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public class UniversalRotation extends RotateConstructor {
   private static final Random RAND = new Random();
   private static LivingEntity trackedTarget;
   private static float currentYaw;
   private static float currentPitch;
   private static float velocityYaw;
   private static float velocityPitch;
   private static double aimPointX;
   private static double aimPointY;
   private static double aimPointZ;
   private static float noiseWalkYaw;
   private static float noiseWalkPitch;
   private static int hitPhase;
   private static int hitTimer;
   private static float pitchBeforeHit;
   private static long firstSeenTime;
   private static int reactionMs;
   private static boolean reactionComplete;
   private static float lastSentYaw;
   private static float lastSentPitch;
   private static float smoothYaw;
   private static float smoothPitch;

   public UniversalRotation() {
      super("Universal");
   }

   public static void onAttack() {
      hitPhase = 1;
      hitTimer = 0;
      pitchBeforeHit = currentPitch;
   }

   public static void resetRotation() {
      trackedTarget = null;
      velocityYaw = 0.0F;
      velocityPitch = 0.0F;
      aimPointX = 0.0;
      aimPointY = 0.0;
      aimPointZ = 0.0;
      noiseWalkYaw = 0.0F;
      noiseWalkPitch = 0.0F;
      hitPhase = 0;
      hitTimer = 0;
      firstSeenTime = 0L;
      reactionComplete = false;
      reactionMs = 0;
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         currentYaw = client.player.getYRot();
         currentPitch = client.player.getXRot();
         lastSentYaw = currentYaw;
         lastSentPitch = currentPitch;
         smoothYaw = currentYaw;
         smoothPitch = currentPitch;
      } else {
         currentYaw = 0.0F;
         currentPitch = 0.0F;
         lastSentYaw = 0.0F;
         lastSentPitch = 0.0F;
         smoothYaw = 0.0F;
         smoothPitch = 0.0F;
      }
   }

   private static float calcGcd() {
      Minecraft client = Minecraft.getInstance();
      double s = (Double)client.options.sensitivity().get() * 0.6 + 0.2;
      return (float)(s * s * s * 1.2);
   }

   private static void pickAimPoint(LivingEntity entity) {
      AABB bb = entity.getBoundingBox();
      double w = bb.maxX - bb.minX;
      double h = bb.maxY - bb.minY;
      double d = bb.maxZ - bb.minZ;
      aimPointX = Mth.clamp(RAND.nextGaussian() * 0.15, -0.5, 0.5) * w * 0.4;
      aimPointY = Mth.clamp(RAND.nextGaussian() * 0.15, -0.5, 0.5) * h * 0.4;
      aimPointZ = Mth.clamp(RAND.nextGaussian() * 0.15, -0.5, 0.5) * d * 0.4;
   }

   private static float measureAngle(LivingEntity entity) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return 0.0F;
      }

      Vec3 eyes = client.player.getEyePosition();
      Vec3 mid = entity.getBoundingBox().getCenter();
      Vec3 delta = mid.subtract(eyes);
      float needYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float needPitch = (float)(-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
      float dYaw = Math.abs(Mth.wrapDegrees(needYaw - client.player.getYRot()));
      float dPitch = Math.abs(needPitch - client.player.getXRot());
      return dYaw + dPitch;
   }

   private static int computeReaction(float angle) {
      int baseDelay = angle > 130.0F ? 70 : (angle > 70.0F ? 45 : (angle > 30.0F ? 22 : 6));
      int variance = angle > 130.0F ? 15 : (angle > 70.0F ? 10 : (angle > 30.0F ? 8 : 3));
      return Math.max(5, baseDelay + (int)(RAND.nextGaussian() * variance));
   }

   private static boolean isMovingForward() {
      Minecraft client = Minecraft.getInstance();
      return client.player != null && client.options.keyUp.isDown();
   }

   private static boolean isOvertakingTarget(LivingEntity target) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && target != null) {
         Vec3 playerPos = client.player.position();
         Vec3 targetPos = target.position();
         Vec3 playerVel = new Vec3(
            client.player.getX() - client.player.xOld, client.player.getY() - client.player.yOld, client.player.getZ() - client.player.zOld
         );
         Vec3 targetVel = new Vec3(target.getX() - target.xOld, target.getY() - target.yOld, target.getZ() - target.zOld);
         Vec3 toTarget = targetPos.subtract(playerPos).normalize();
         double playerSpeedToTarget = playerVel.dot(toTarget);
         double targetSpeedToPlayer = targetVel.dot(toTarget.scale(-1.0));
         double relativeSpeed = playerSpeedToTarget + targetSpeedToPlayer;
         double distance = Math.sqrt(Math.pow(playerPos.x - targetPos.x, 2.0) + Math.pow(playerPos.z - targetPos.z, 2.0));
         return relativeSpeed > 0.05 && distance < 4.0;
      } else {
         return false;
      }
   }

   private static float[] generateNoise(float dist) {
      float scale = Mth.clamp(dist / 4.5F, 0.25F, 1.0F);
      noiseWalkYaw = noiseWalkYaw + (float)(RAND.nextGaussian() * 0.4 * scale);
      noiseWalkPitch = noiseWalkPitch + (float)(RAND.nextGaussian() * 0.3 * scale);
      noiseWalkYaw *= 0.85F;
      noiseWalkPitch *= 0.85F;
      return new float[]{noiseWalkYaw, noiseWalkPitch};
   }

   private static float smoothStep(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   private static float accelCurve(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x);
   }

   private static float springInterp(float current, float target, float vel, float stiffness, float damping, boolean allowOvershoot) {
      float diff = Mth.wrapDegrees(target - current);
      if (allowOvershoot && Math.abs(diff) > 45.0F) {
         stiffness *= 1.2F;
         damping *= 0.8F;
      }

      float acc = diff * stiffness - vel * damping;
      return vel + acc;
   }

   private static float smoothLerp(float from, float to, float alpha) {
      alpha = Mth.clamp(alpha, 0.0F, 1.0F);
      float delta = Mth.wrapDegrees(to - from);
      return from + delta * alpha;
   }

   private static float calculateCurrentAngle(float targetYaw, float targetPitch) {
      float dYaw = Math.abs(Mth.wrapDegrees(targetYaw - currentYaw));
      float dPitch = Math.abs(targetPitch - currentPitch);
      return dYaw + dPitch;
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && entity != null && entity instanceof LivingEntity target) {
         boolean playerFlying = client.player.isFallFlying();
         if (trackedTarget != target) {
            trackedTarget = target;
            currentYaw = client.player.getYRot();
            currentPitch = client.player.getXRot();
            lastSentYaw = currentYaw;
            lastSentPitch = currentPitch;
            smoothYaw = currentYaw;
            smoothPitch = currentPitch;
            velocityYaw = 0.0F;
            velocityPitch = 0.0F;
            pickAimPoint(target);
            hitPhase = 0;
            hitTimer = 0;
            float angleDiff = measureAngle(target);
            reactionMs = computeReaction(angleDiff);
            firstSeenTime = System.currentTimeMillis();
            reactionComplete = false;
         }

         float distance = client.player.distanceTo(target);
         float gcd = calcGcd();
         if (!reactionComplete) {
            long elapsed = System.currentTimeMillis() - firstSeenTime;
            if (elapsed < reactionMs) {
               float jitterY = (float)(RAND.nextGaussian() * 0.15);
               float jitterP = (float)(RAND.nextGaussian() * 0.1);
               float outY = lastSentYaw + jitterY;
               float outP = Mth.clamp(lastSentPitch + jitterP, -89.0F, 89.0F);
               outY -= (outY - lastSentYaw) % gcd;
               outP -= (outP - lastSentPitch) % gcd;
               lastSentYaw = outY;
               lastSentPitch = outP;
               return new Angle(outY, outP);
            }

            reactionComplete = true;
         }

         float[] noise = generateNoise(distance);
         if (hitPhase > 0) {
            hitTimer++;
            int upDuration = 10;
            int downDuration = 7;
            float targetPitchUp = -89.0F;
            if (hitPhase == 1) {
               float t = Mth.clamp((float)hitTimer / upDuration, 0.0F, 1.0F);
               currentPitch = Mth.lerp(accelCurve(t), pitchBeforeHit, targetPitchUp);
               if (hitTimer >= upDuration) {
                  hitPhase = 2;
                  hitTimer = 0;
               }
            } else if (hitPhase == 2) {
               float t = Mth.clamp((float)hitTimer / downDuration, 0.0F, 1.0F);
               currentPitch = Mth.lerp(smoothStep(t), targetPitchUp, pitchBeforeHit);
               if (hitTimer >= downDuration) {
                  hitPhase = 0;
                  hitTimer = 0;
               }
            }

            float outY = currentYaw + noise[0];
            float outP = Mth.clamp(currentPitch + noise[1], -89.0F, 89.0F);
            outY -= (outY - lastSentYaw) % gcd;
            outP -= (outP - lastSentPitch) % gcd;
            lastSentYaw = outY;
            lastSentPitch = outP;
            return new Angle(outY, outP);
         } else {
            if (RAND.nextDouble() < 0.015) {
               pickAimPoint(target);
            }

            float baseYaw;
            float basePitch;
            if (targetAngle != null) {
               baseYaw = targetAngle.getYaw();
               basePitch = Mth.clamp(targetAngle.getPitch(), -89.0F, 89.0F);
            } else {
               Vec3 direction = target.getBoundingBox().getCenter().subtract(client.player.getEyePosition());
               baseYaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
               basePitch = (float)(-Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
            }

            double yawRad = Math.toRadians(baseYaw);
            float safeDistance = Math.max(distance, 1.5F);
            float lateralOffset = (float)(aimPointX * Math.cos(yawRad) + aimPointZ * Math.sin(yawRad));
            float wantYaw = baseYaw + (float)Math.toDegrees(lateralOffset / safeDistance);
            float wantPitch = Mth.clamp(basePitch + (float)Math.toDegrees(aimPointY / safeDistance), -89.0F, 89.0F);
            float diffYaw = Mth.wrapDegrees(wantYaw - currentYaw);
            float diffPitch = wantPitch - currentPitch;
            float speedMultiplier = 1.0F;
            if (playerFlying) {
               float angleToWant = calculateCurrentAngle(wantYaw, wantPitch);
               if (angleToWant > 120.0F) {
                  speedMultiplier = 0.18F;
               } else if (angleToWant > 80.0F) {
                  speedMultiplier = Mth.lerp(smoothStep((angleToWant - 80.0F) / 40.0F), 0.35F, 0.18F);
               } else if (angleToWant > 25.0F) {
                  speedMultiplier = Mth.lerp(smoothStep((angleToWant - 25.0F) / 55.0F), 0.65F, 0.35F);
               } else {
                  speedMultiplier = 0.65F + 0.35F * (1.0F - angleToWant / 25.0F);
               }
            } else if (isMovingForward() || isOvertakingTarget(target)) {
               speedMultiplier = 0.8F;
            }

            float stiffness = (0.092F + (float)Math.abs(RAND.nextGaussian()) * 0.48F) * speedMultiplier;
            float damping = 0.64F + 0.12F * (1.0F - speedMultiplier);
            float totalDiff = (float)Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
            boolean acquiring = System.currentTimeMillis() - firstSeenTime < 600L;
            if (acquiring) {
               stiffness *= 1.6F;
            }

            if (totalDiff > 32.0F) {
               stiffness += 0.024F * speedMultiplier;
            } else if (totalDiff < 4.2F) {
               stiffness *= 0.62F;
            }

            stiffness += Mth.clamp((distance - 1.6F) / 7.5F, 0.0F, 0.045F) * speedMultiplier;
            velocityYaw = springInterp(currentYaw, currentYaw + diffYaw, velocityYaw, stiffness, damping, true);
            velocityPitch = springInterp(currentPitch, wantPitch, velocityPitch, stiffness * 0.87F, damping, false);
            float acquireVelBoost = acquiring ? 1.5F : 1.0F;
            float maxVelYaw = 36.0F * speedMultiplier * acquireVelBoost;
            float maxVelPitch = 52.2F * speedMultiplier * acquireVelBoost;
            velocityYaw = Mth.clamp(velocityYaw, -maxVelYaw, maxVelYaw);
            velocityPitch = Mth.clamp(velocityPitch, -maxVelPitch, maxVelPitch);
            currentYaw = currentYaw + velocityYaw;
            currentPitch = Mth.clamp(currentPitch + velocityPitch, -89.0F, 89.0F);
            float smoothFactor = playerFlying ? 0.3F + speedMultiplier * 0.4F : 0.85F;
            smoothYaw = smoothLerp(smoothYaw, currentYaw, smoothFactor);
            smoothPitch = smoothLerp(smoothPitch, currentPitch, smoothFactor * 0.95F);
            float outY = smoothYaw + noise[0];
            float outP = Mth.clamp(smoothPitch + noise[1], -89.0F, 89.0F);
            outY -= (outY - lastSentYaw) % gcd;
            outP -= (outP - lastSentPitch) % gcd;
            lastSentYaw = outY;
            lastSentPitch = outP;
            return new Angle(outY, outP);
         }
      } else {
         resetRotation();
         return targetAngle != null ? targetAngle : currentAngle;
      }
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}

