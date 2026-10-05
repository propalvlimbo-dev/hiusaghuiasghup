package org.xrose.utils.combat.aura.rotations;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import org.xrose.utils.combat.aura.target.RaycastAngle;

public class USpookyTimeRotations extends RotateConstructor {
   private static final long SUB_STEP_NANO = 11764706L;
   private static final int MAX_SUB_STEPS = 92;
   private static long lastUpdateNano;
   private static long accumulatedSubSteps;
   private static Entity lastTarget;
   private static long spookyLostTraceTime = -1L;
   private static final float[] spookyYawBudget = new float[5];
   private static int spookyBudgetIndex;
   private static long spookyBudgetTime;
   private static float spookyJitterYaw;
   private static long spookyJitterUntil;

   public USpookyTimeRotations() {
      super("SpookyTime");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && entity != null && entity instanceof LivingEntity target) {
         if (lastTarget != entity) {
            this.resetSpookyRotation();
            lastTarget = entity;
         }

         long nanoTime = System.nanoTime();
         if (lastUpdateNano == 0L) {
            lastUpdateNano = nanoTime;
         } else {
            accumulatedSubSteps = accumulatedSubSteps + (nanoTime - lastUpdateNano) / 11764706L;
            lastUpdateNano = nanoTime;
         }

         int steps = (int)Math.min(accumulatedSubSteps, 92L);
         accumulatedSubSteps -= steps;
         if (steps <= 0) {
            steps = 1;
         }

         AuraFeature aura = AuraFeature.getInstance();
         float attackDist = aura.attackDistance();
         float finalDist = attackDist + aura.getLookRange().getFloat() + 10.1F;
         long baseNow = System.currentTimeMillis();
         float convYaw = currentAngle.getYaw();
         float convPitch = currentAngle.getPitch();

         for (int i = 0; i < steps; i++) {
            long now = baseNow + i * 12L;
            float[] result = this.runSpookyStep(client, target, convYaw, convPitch, now, attackDist, finalDist);
            convYaw = result[0];
            convPitch = result[1];
         }

         return new Angle(convYaw, convPitch);
      } else {
         this.resetSpookyRotation();
         return targetAngle != null ? targetAngle : currentAngle;
      }
   }

   private float[] runSpookyStep(Minecraft client, LivingEntity target, float convYaw, float convPitch, long now, float attackDist, float finalDist) {
      Player player = client.player;
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (now >= spookyJitterUntil) {
         double dist = player.distanceTo(target);
         float halfWidth = (float)Math.toDegrees(Math.atan2(target.getBbWidth() / 2.0, Math.max(dist, 0.5)));
         float maxJitter = Math.min(2.5F, halfWidth * 0.5F);
         spookyJitterYaw = (rng.nextFloat() * 2.0F - 1.0F) * maxJitter;
         spookyJitterUntil = now + 100L + rng.nextInt(300);
      }

      Vec3 aim = this.spookyAimVector(player, target);
      float rawYaw = (float)Math.toDegrees(Math.atan2(-aim.x, aim.z)) + spookyJitterYaw;
      Vec3 view = new Angle(convYaw, convPitch).toVector();
      boolean traced = RaycastAngle.rayTrace(view, attackDist + finalDist, target.getBoundingBox());
      boolean tracedSmall = RaycastAngle.rayTrace(view, finalDist, spookyShrunkBox(target));
      float absYawDiff = Math.abs(Mth.wrapDegrees(rawYaw - convYaw));
      double distToTarget = player.distanceTo(target);
      float rand = (float)Math.random();
      float smoothBase;
      if (distToTarget < 2.5) {
         smoothBase = absYawDiff < 8.0F ? 0.34F + rand * 0.07F : 0.47F + rand * 0.08F;
      } else if (distToTarget > 5.5) {
         smoothBase = absYawDiff < 8.0F ? 0.11F + rand * 0.04F : 0.15F + rand * 0.05F;
      } else {
         smoothBase = absYawDiff < 8.0F ? 0.22F + rand * 0.05F : 0.32F + rand * 0.06F;
      }

      if (traced) {
         smoothBase *= tracedSmall ? 0.7F : 0.92F;
         spookyLostTraceTime = -1L;
      } else {
         if (spookyLostTraceTime == -1L) {
            spookyLostTraceTime = now;
         }

         smoothBase *= Math.min(1.0F + (float)(now - spookyLostTraceTime) / 380.0F, 1.9F);
      }

      double yawSpeed = absYawDiff * smoothBase + (Math.random() - 0.5) * 0.05;
      yawSpeed = traced ? Mth.clamp(yawSpeed, 0.12, distToTarget < 3.0 ? 6.2 : 3.3) : Mth.clamp(yawSpeed, 1.3, 14.5);
      if (now != spookyBudgetTime) {
         spookyYawBudget[spookyBudgetIndex] = (float)Math.abs(yawSpeed);
         spookyBudgetIndex = (spookyBudgetIndex + 1) % spookyYawBudget.length;
         spookyBudgetTime = now;
      }

      float spent = 0.0F;

      for (float value : spookyYawBudget) {
         spent += value;
      }

      if (spent > 22.0F && traced) {
         yawSpeed *= 22.0F / spent;
         int last = (spookyBudgetIndex - 1 + spookyYawBudget.length) % spookyYawBudget.length;
         spookyYawBudget[last] = (float)Math.abs(yawSpeed);
      }

      double pitchSpeed = traced ? (tracedSmall ? (Math.random() - 0.5) * 0.025 : 0.2 + Math.random() * 0.45) : 3.5 + Math.random() * 2.0;
      yawSpeed += (Math.random() - 0.5) * 0.06;
      yawSpeed = Math.max(yawSpeed, 0.07);
      float rawYaw2 = rawYaw;
      float rawPitch = (float)Mth.clamp(-Math.toDegrees(Math.atan2(aim.y, Math.hypot(aim.x, aim.z))), -90.0, 90.0);
      rawPitch = convPitch + (rawPitch - convPitch) * 0.74F;
      float targetYaw = convYaw + (float)Math.ceil(Mth.wrapDegrees(rawYaw2) - Mth.wrapDegrees(convYaw));
      float targetPitch = convPitch + (float)Math.ceil(Mth.wrapDegrees(rawPitch) - Mth.wrapDegrees(convPitch));
      float yawDelta = Mth.wrapDegrees(targetYaw - convYaw);
      float pitchDelta = targetPitch - convPitch;
      float clampedYaw = Math.min(Math.abs(yawDelta), (float)Math.abs(yawSpeed));
      float clampedPitch = Math.min(Math.abs(pitchDelta), (float)Math.abs(pitchSpeed));
      float newYaw = convYaw + getFixRotate(Mth.clamp(yawDelta, -clampedYaw, clampedYaw));
      float newPitch = Mth.clamp(convPitch + getFixRotate(Mth.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90.0F, 90.0F);
      return new float[]{newYaw, newPitch};
   }

   private Vec3 spookyAimVector(Player player, LivingEntity target) {
      Vec3 eyes = player.getEyePosition();
      double maxHeight = target.getBbHeight() * 0.55F;
      double aimY = Mth.clamp(eyes.y - target.getY(), 0.0, maxHeight);
      return target.position().add(0.0, aimY, 0.0).subtract(eyes).normalize();
   }

   private static AABB spookyShrunkBox(LivingEntity target) {
      AABB box = target.getBoundingBox();
      double centerX = (box.minX + box.maxX) / 2.0;
      double centerY = (box.minY + box.maxY) / 2.0;
      double centerZ = (box.minZ + box.maxZ) / 2.0;
      double halfX = (box.maxX - box.minX) / 2.0 / 1.6;
      double halfY = (box.maxY - box.minY) / 2.0 / 1.2;
      double halfZ = (box.maxZ - box.minZ) / 2.0 / 1.6;
      return new AABB(centerX - halfX, centerY - halfY, centerZ - halfZ, centerX + halfX, centerY + halfY, centerZ + halfZ);
   }

   private static float getGCD() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.options != null) {
         double var11 = (Double)client.options.sensitivity().get() / 0.15F / 8.0;
         double var9 = Math.cbrt(var11);
         float f1;
         return (f1 = (float)((var9 - 0.2F) / 0.6F * 0.6 + 0.2)) * f1 * f1 * 8.0F;
      } else {
         return 1.0F;
      }
   }

   private static float getGCDValue() {
      return getGCD() * 0.15F;
   }

   private static float getFixRotate(float delta) {
      return Math.round(delta / getGCDValue()) * getGCDValue();
   }

   private void resetSpookyRotation() {
      spookyJitterYaw = 0.0F;
      spookyJitterUntil = 0L;
      spookyLostTraceTime = -1L;
      spookyBudgetIndex = 0;
      spookyBudgetTime = 0L;
      Arrays.fill(spookyYawBudget, 0.0F);
      lastUpdateNano = 0L;
      accumulatedSubSteps = 1L;
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.06, 0.1, 0.06);
   }
}

