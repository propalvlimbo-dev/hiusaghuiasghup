package org.xrose.utils.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class FTAngle extends RotateConstructor {
   public static final FTAngle INSTANCE = new FTAngle();
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final long LOOK_DELAY_MS = 3500L;
   private static final int AIR_MISS_INTERVAL = 31;
   private static final long AIR_MISS_LOOK_MS = 250L;
   private static final long AIR_MISS_SWING_MS = 238L;
   private int lastAirMissSwingCount = -1;

   public FTAngle() {
      super("FunTime");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      AuraFeature aura = AuraFeature.getInstance();
      StrikeManager attackHandler = aura == null ? null : aura.getAttackPerpetrator().getAttackHandler();
      if (attackHandler == null) {
         return FunTimeSnapAngle.INSTANCE.limitAngleChange(currentAngle, targetAngle, vec3d, entity);
      }

      long attackTimer = attackHandler.getAttackTimer().elapsedTime();
      int count = attackHandler.getCount();
      Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      if (rotationDifference < 1.0E-4F) {
         rotationDifference = 1.0E-4F;
      }

      long elapsed = attackTimer;
      boolean airMiss = count > 0 && count % 31 == 0 && elapsed < 250L;
      if (airMiss) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && elapsed >= 238L && this.lastAirMissSwingCount != count) {
            client.player.swing(InteractionHand.MAIN_HAND);
            this.lastAirMissSwingCount = count;
         }

         float missYaw = currentAngle.getYaw() + Mth.clamp(yawDelta, -22.0F, 22.0F);
         return new Angle(missYaw, -85.0F);
      } else {
         return entity != null
            ? this.applyRecordedTracking(currentAngle, yawDelta, pitchDelta, rotationDifference, entity, aura, attackHandler, attackTimer)
            : this.applyRecordedDelay(currentAngle, yawDelta, pitchDelta, rotationDifference, attackTimer, count);
      }
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.05, 0.1, 0.02);
   }

   private Angle applyRecordedTracking(
      Angle currentAngle,
      float yawDelta,
      float pitchDelta,
      float rotationDifference,
      Entity entity,
      AuraFeature aura,
      StrikeManager attackHandler,
      long attackTimer
   ) {
      Minecraft client = Minecraft.getInstance();
      int count = attackHandler.getCount();
      boolean attackZone = aura != null && client.player != null && client.player.distanceTo(entity) <= aura.attackDistance();
      boolean attackNow = aura != null && attackHandler.canAttack(aura.getConfig(), 0);
      boolean attackSoon = aura != null && attackHandler.canAttack(aura.getConfig(), 1);
      boolean recentAttack = attackTimer < 180L;
      float yawBudget = this.rand(18.0F, 28.0F);
      float pitchBudget = this.rand(2.8F, 6.2F);
      if (attackSoon) {
         yawBudget = Math.max(yawBudget, this.rand(34.0F, 52.0F));
         pitchBudget = Math.max(pitchBudget, this.rand(4.2F, 7.8F));
      }

      if (recentAttack) {
         yawBudget = Math.max(yawBudget, this.rand(44.0F, 72.0F));
         pitchBudget = Math.max(pitchBudget, this.rand(5.4F, 10.0F));
      }

      if (Math.abs(yawDelta) > 40.0F) {
         yawBudget += this.rand(10.0F, 18.0F);
      }

      if (Math.abs(yawDelta) > 75.0F) {
         yawBudget += this.rand(12.0F, 24.0F);
      }

      if (Math.abs(pitchDelta) > 20.0F) {
         pitchBudget += this.rand(1.4F, 3.2F);
      }

      if (Math.abs(pitchDelta) > 35.0F) {
         pitchBudget += this.rand(1.6F, 3.8F);
      }

      float moveYaw = Mth.clamp(yawDelta, -this.axisBudget(yawDelta, rotationDifference, yawBudget), this.axisBudget(yawDelta, rotationDifference, yawBudget));
      float movePitch = Mth.clamp(
         pitchDelta, -this.axisBudget(pitchDelta, rotationDifference, pitchBudget), this.axisBudget(pitchDelta, rotationDifference, pitchBudget)
      );
      float blend = attackNow ? 1.0F : (attackSoon ? this.rand(0.88F, 0.97F) : (recentAttack ? this.rand(0.74F, 0.88F) : this.rand(0.56F, 0.74F)));
      if (attackZone && !attackSoon && !recentAttack) {
         blend = Math.max(blend, this.rand(0.68F, 0.82F));
      }

      float shakeScale = attackZone ? 1.25F : 0.9F;
      if (attackSoon) {
         shakeScale = Math.max(shakeScale, 1.4F);
      }

      if (recentAttack) {
         shakeScale = Math.max(shakeScale, 1.55F);
      }

      float shakeYaw = this.trackShakeYaw(attackTimer, count, shakeScale, Math.abs(yawDelta));
      float shakePitch = this.trackShakePitch(attackTimer, count, shakeScale, Math.abs(pitchDelta));
      if (Math.abs(yawDelta) < 4.0F) {
         shakeYaw *= 0.35F;
      }

      if (Math.abs(pitchDelta) < 2.5F) {
         shakePitch *= 0.25F;
      }

      float newYaw = Mth.lerp(blend, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + shakeYaw;
      float newPitch = Mth.lerp(blend, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + shakePitch;
      return new Angle(newYaw, Mth.clamp(newPitch, -90.0F, 90.0F));
   }

   private Angle applyRecordedDelay(Angle currentAngle, float yawDelta, float pitchDelta, float rotationDifference, long attackTimer, int count) {
      long elapsed = attackTimer;

      Angle oscillation = switch (count % 4) {
         case 0 -> new Angle((float)Math.cos((float)elapsed / 40.0F + count % 6), (float)Math.sin((float)elapsed / 40.0F + count % 6));
         case 1 -> new Angle((float)Math.sin((float)elapsed / 40.0F + count % 6), (float)Math.cos((float)elapsed / 40.0F + count % 6));
         case 2 -> new Angle((float)Math.sin((float)elapsed / 40.0F + count % 6), (float)(-Math.cos((float)elapsed / 40.0F + count % 6)));
         default -> new Angle((float)(-Math.cos((float)elapsed / 40.0F + count % 6)), (float)Math.sin((float)elapsed / 40.0F + count % 6));
      };
      float holdProgress = Mth.clamp((float)elapsed / 3500.0F, 0.0F, 1.0F);
      float holdScale = attackTimer >= 3500L ? 0.0F : 1.0F - holdProgress * 0.55F;
      float idleYaw = holdScale > 0.0F ? this.rand(12.0F, 22.0F) * oscillation.getYaw() * holdScale : 0.0F;
      float pitchWave = this.rand(0.35F, 1.35F) * (float)Math.cos(System.currentTimeMillis() / 420.0 + count);
      float idlePitch = holdScale > 0.0F ? (this.rand(2.2F, 5.8F) * oscillation.getPitch() + pitchWave) * holdScale : 0.0F;
      float yawBudget = attackTimer < 180L
         ? this.rand(0.0F, 3.5F)
         : (attackTimer < 600L ? this.rand(4.0F, 10.0F) : (attackTimer >= 3500L ? this.rand(12.0F, 28.0F) : this.rand(6.0F, 14.0F)));
      float pitchBudget = attackTimer < 180L
         ? this.rand(0.0F, 1.0F)
         : (attackTimer < 600L ? this.rand(1.2F, 3.0F) : (attackTimer >= 3500L ? this.rand(3.0F, 6.8F) : this.rand(1.5F, 4.2F)));
      float moveYaw = Mth.clamp(yawDelta, -this.axisBudget(yawDelta, rotationDifference, yawBudget), this.axisBudget(yawDelta, rotationDifference, yawBudget));
      float movePitch = Mth.clamp(
         pitchDelta, -this.axisBudget(pitchDelta, rotationDifference, pitchBudget), this.axisBudget(pitchDelta, rotationDifference, pitchBudget)
      );
      float returnBlend = attackTimer < 180L
         ? 0.0F
         : (attackTimer < 600L ? this.rand(0.08F, 0.22F) : (attackTimer >= 3500L ? this.rand(0.54F, 0.78F) : this.rand(0.2F, 0.42F)));
      float newYaw = Mth.lerp(returnBlend, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + idleYaw;
      float newPitch = Mth.lerp(returnBlend, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + idlePitch;
      return new Angle(newYaw, Mth.clamp(newPitch, -90.0F, 90.0F));
   }

   private float trackShakeYaw(long elapsed, int count, float scale, float absYawDelta) {
      float shake = (float)Math.sin((float)elapsed / 38.0F + count * 0.37F) * this.rand(0.45F, 1.25F)
         + (float)Math.cos((float)elapsed / 71.0F + count * 0.18F) * this.rand(0.18F, 0.55F);
      if (this.chance(absYawDelta > 24.0F ? 0.22F : 0.08F)) {
         shake += this.rand(-1.55F, 1.55F);
      }

      return shake * scale;
   }

   private float trackShakePitch(long elapsed, int count, float scale, float absPitchDelta) {
      float shake = (float)Math.sin((float)elapsed / 52.0F + count * 0.21F) * this.rand(0.1F, 0.42F)
         + (float)Math.cos((float)elapsed / 93.0F + count * 0.11F) * this.rand(0.08F, 0.28F);
      if (this.chance(absPitchDelta > 8.0F ? 0.18F : 0.06F)) {
         shake += this.rand(-0.55F, 0.55F);
      }

      return shake * scale;
   }

   private float axisBudget(float axisDelta, float rotationDifference, float budget) {
      return Math.abs(axisDelta / rotationDifference) * budget;
   }

   private boolean chance(float probability) {
      return RANDOM.nextFloat() < probability;
   }

   private float rand(float min, float max) {
      return Mth.lerp(RANDOM.nextFloat(), min, max);
   }
}

