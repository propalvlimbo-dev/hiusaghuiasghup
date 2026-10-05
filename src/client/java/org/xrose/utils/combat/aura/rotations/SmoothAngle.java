package org.xrose.utils.combat.aura.rotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class SmoothAngle extends RotateConstructor {
   public static final SmoothAngle INSTANCE = new SmoothAngle();
   private static final Minecraft mc = Minecraft.getInstance();
   private static final float MIN_SPEED = 0.35F;
   private static final float MAX_SPEED = 0.75F;
   private static final float YAW_LERP = 0.55F;
   private static final float PITCH_LERP = 0.35F;
   private static final int PATTERN_HISTORY_LIMIT = 8;
   private static final int PATTERN_CANDIDATE_ATTEMPTS = 18;
   private float currentSpeed = 0.3F;
   private Entity lastTarget;
   private int currentPointIndex = 0;
   private float prevAttackStrength = 1.0F;
   private final List<Vec3> hitboxPoints = new ArrayList<>();
   private float randomCurveYaw = 0.0F;
   private float randomCurvePitch = 0.0F;
   private float randomCurveTargetYaw = 0.0F;
   private float randomCurveTargetPitch = 0.0F;
   private float appliedPitchNoise = 0.0F;
   private SmoothAngle.RandomPattern currentPattern = SmoothAngle.RandomPattern.defaultPattern();
   private final List<SmoothAngle.RandomPattern> recentPatterns = new ArrayList<>();
   private long lastPatternRetargetTime = 0L;
   private int nextPatternRetargetDelayMs = randomRetargetDelayMs(this.currentPattern);

   public SmoothAngle() {
      super("Smooth");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      if (mc.player == null) {
         return currentAngle;
      }

      if (entity != null && entity instanceof LivingEntity living && living.isAlive()) {
         if (this.lastTarget != entity) {
            this.resetState();
            this.lastTarget = entity;
         }

         this.updateHitboxPoints(living);
         this.detectAttackAndSwitchPoint();
         Vec3 aimPoint = this.hitboxPoints.get(this.currentPointIndex % this.hitboxPoints.size());
         Vec3 playerEye = mc.player.getEyePosition();
         Vec3 dir = aimPoint.subtract(playerEye);
         float yawToTarget = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
         float pitchToTarget = (float)(-Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z))));
         pitchToTarget = Mth.clamp(pitchToTarget, -90.0F, 90.0F);
         float yawDelta = Mth.wrapDegrees(yawToTarget - currentAngle.getYaw());
         float pitchDelta = Mth.wrapDegrees(pitchToTarget - currentAngle.getPitch());
         this.currentSpeed = this.calculateDynamicSpeed(yawDelta, pitchDelta);
         float maxYawStep = Math.max(0.01F, Math.abs(yawDelta) * this.currentSpeed);
         float maxPitchStep = Math.max(0.01F, Math.abs(pitchDelta) * this.currentSpeed);
         yawDelta = Mth.clamp(yawDelta, -maxYawStep, maxYawStep);
         pitchDelta = Mth.clamp(pitchDelta, -maxPitchStep, maxPitchStep);
         float yaw = currentAngle.getYaw() + yawDelta;
         float pitch = currentAngle.getPitch() + pitchDelta;
         float pitchNoise = this.calculatePitchNoise();
         pitch += pitchNoise;
         pitch = Mth.clamp(pitch, -90.0F, 90.0F);
         yaw = lerpAngle(currentAngle.getYaw(), yaw, 0.55F);
         pitch = lerpAngle(currentAngle.getPitch(), pitch, 0.35F);
         pitch = Mth.clamp(pitch, -90.0F, 90.0F);
         return new Angle(yaw, pitch);
      } else {
         this.lastTarget = null;
         this.prevAttackStrength = 1.0F;
         return currentAngle;
      }
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.06, 0.1, 0.06);
   }

   private float calculateDynamicSpeed(float yawDelta, float pitchDelta) {
      float totalDelta = (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
      if (totalDelta < 5.0F) {
         return 0.35F;
      }

      if (totalDelta > 30.0F) {
         return 0.75F;
      }

      float t = (totalDelta - 5.0F) / 25.0F;
      return 0.35F + 0.4F * t;
   }

   private void updateHitboxPoints(LivingEntity living) {
      this.hitboxPoints.clear();
      Vec3 pos = living.position();
      float width = living.getBbWidth();
      float height = living.getBbHeight();
      double halfW = width / 2.0;
      this.hitboxPoints.add(pos.add(0.0, height * 0.9, 0.0));
      this.hitboxPoints.add(pos.add(0.0, height * 0.75, 0.0));
      this.hitboxPoints.add(pos.add(0.0, height * 0.5, 0.0));
      this.hitboxPoints.add(pos.add(-halfW, height * 0.5, 0.0));
      this.hitboxPoints.add(pos.add(halfW, height * 0.5, 0.0));
      this.hitboxPoints.add(pos.add(0.0, height * 0.5, -halfW));
      this.hitboxPoints.add(pos.add(0.0, height * 0.5, halfW));
   }

   private void detectAttackAndSwitchPoint() {
      float attackStrength = mc.player.getAttackStrengthScale(0.0F);
      if (this.prevAttackStrength > 0.9F && attackStrength < 0.3F) {
         this.selectNewPoint();
      }

      this.prevAttackStrength = attackStrength;
   }

   private void selectNewPoint() {
      if (!this.hitboxPoints.isEmpty()) {
         if (this.hitboxPoints.size() == 1) {
            this.currentPointIndex = 0;
         } else {
            int attempts = 0;

            int newIndex;
            do {
               newIndex = ThreadLocalRandom.current().nextInt(this.hitboxPoints.size());
            } while (newIndex == this.currentPointIndex && ++attempts < 10);

            this.currentPointIndex = newIndex;
         }
      }
   }

   private float calculatePitchNoise() {
      long now = System.currentTimeMillis();
      if (now - this.lastPatternRetargetTime > this.nextPatternRetargetDelayMs) {
         this.updatePattern();
         this.lastPatternRetargetTime = now;
      }

      float targetCurveYaw = (float)Math.sin((float)now / this.currentPattern.sinePeriodYawMs()) * this.currentPattern.sineAmplitudeYaw();
      float targetCurvePitch = (float)Math.sin((float)now / this.currentPattern.sinePeriodPitchMs()) * this.currentPattern.sineAmplitudePitch();
      this.randomCurveYaw = this.randomCurveYaw + (targetCurveYaw - this.randomCurveYaw) * this.currentPattern.interpolationFactor();
      this.randomCurvePitch = this.randomCurvePitch + (targetCurvePitch - this.randomCurvePitch) * this.currentPattern.interpolationFactor();
      float noise = this.randomCurvePitch * this.currentPattern.pitchNoiseCurveFactor()
         + (float)Math.sin((float)now / this.currentPattern.pitchNoiseSinePeriodMs()) * this.currentPattern.pitchNoiseSineAmplitude();
      this.appliedPitchNoise = this.appliedPitchNoise + (noise - this.appliedPitchNoise) * 0.45F;
      return Mth.clamp(this.appliedPitchNoise, -this.currentPattern.pitchNoiseClamp(), this.currentPattern.pitchNoiseClamp());
   }

   private void updatePattern() {
      SmoothAngle.RandomPattern nextPattern = this.createMostDistinctPattern();
      this.applyPattern(nextPattern);
      this.rememberPattern(nextPattern);
   }

   private SmoothAngle.RandomPattern createMostDistinctPattern() {
      SmoothAngle.RandomPattern bestPattern = SmoothAngle.RandomPattern.randomPattern();
      double bestScore = this.scorePattern(bestPattern);

      for (int i = 1; i < 18; i++) {
         SmoothAngle.RandomPattern candidate = SmoothAngle.RandomPattern.randomPattern();
         double score = this.scorePattern(candidate);
         if (score > bestScore) {
            bestScore = score;
            bestPattern = candidate;
         }
      }

      return bestPattern;
   }

   private double scorePattern(SmoothAngle.RandomPattern candidate) {
      double minDistance = this.patternDistance(candidate, this.currentPattern);

      for (SmoothAngle.RandomPattern previous : this.recentPatterns) {
         minDistance = Math.min(minDistance, this.patternDistance(candidate, previous));
      }

      return minDistance + ThreadLocalRandom.current().nextDouble(0.0, 0.02);
   }

   private double patternDistance(SmoothAngle.RandomPattern a, SmoothAngle.RandomPattern b) {
      double distance = 0.0;
      distance += Math.abs(a.sineAmplitudeYaw() - b.sineAmplitudeYaw()) / 0.45;
      distance += Math.abs(a.sineAmplitudePitch() - b.sineAmplitudePitch()) / 0.4;
      distance += Math.abs(a.sinePeriodYawMs() - b.sinePeriodYawMs()) / 220.0;
      distance += Math.abs(a.sinePeriodPitchMs() - b.sinePeriodPitchMs()) / 220.0;
      distance += Math.abs(a.pitchNoiseCurveFactor() - b.pitchNoiseCurveFactor()) / 1.0;
      distance += Math.abs(a.pitchNoiseSineAmplitude() - b.pitchNoiseSineAmplitude()) / 0.8;
      distance += Math.abs(a.pitchNoiseSinePeriodMs() - b.pitchNoiseSinePeriodMs()) / 220.0;
      return distance + Math.abs(a.pitchNoiseClamp() - b.pitchNoiseClamp()) / 2.2;
   }

   private void applyPattern(SmoothAngle.RandomPattern pattern) {
      this.currentPattern = pattern;
      this.nextPatternRetargetDelayMs = randomRetargetDelayMs(pattern);
      this.lastPatternRetargetTime = 0L;
      this.randomCurveYaw = 0.0F;
      this.randomCurvePitch = 0.0F;
      this.randomCurveTargetYaw = 0.0F;
      this.randomCurveTargetPitch = 0.0F;
      this.appliedPitchNoise = 0.0F;
   }

   private void rememberPattern(SmoothAngle.RandomPattern pattern) {
      this.recentPatterns.add(0, pattern);

      while (this.recentPatterns.size() > 8) {
         this.recentPatterns.remove(this.recentPatterns.size() - 1);
      }
   }

   private void resetState() {
      this.lastTarget = null;
      this.currentPointIndex = 0;
      this.prevAttackStrength = 1.0F;
      this.randomCurveYaw = 0.0F;
      this.randomCurvePitch = 0.0F;
      this.randomCurveTargetYaw = 0.0F;
      this.randomCurveTargetPitch = 0.0F;
      this.appliedPitchNoise = 0.0F;
      this.recentPatterns.clear();
      this.currentPattern = SmoothAngle.RandomPattern.defaultPattern();
   }

   private static int randomRetargetDelayMs(SmoothAngle.RandomPattern pattern) {
      int minDelay = Math.max(150, pattern.minRetargetDelayMs());
      int maxDelay = Math.max(minDelay + 1, pattern.maxRetargetDelayMs());
      return ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
   }

   private static float lerpAngle(float start, float end, float factor) {
      float delta = Mth.wrapDegrees(end - start);
      return start + delta * factor;
   }

   private record RandomPattern(
      float sineAmplitudeYaw,
      float sineAmplitudePitch,
      float sinePeriodYawMs,
      float sinePeriodPitchMs,
      float interpolationFactor,
      float pitchNoiseCurveFactor,
      float pitchNoiseSineAmplitude,
      float pitchNoiseSinePeriodMs,
      float pitchNoiseClamp,
      int minRetargetDelayMs,
      int maxRetargetDelayMs
   ) {
      private static SmoothAngle.RandomPattern defaultPattern() {
         return new SmoothAngle.RandomPattern(0.22F, 0.2F, 125.0F, 95.0F, 0.45F, 0.55F, 0.32F, 95.0F, 1.4F, 150, 300);
      }

      private static SmoothAngle.RandomPattern randomPattern() {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         int minDelay = random.nextInt(120, 280);
         int maxDelay = random.nextInt(minDelay + 55, 430);
         return new SmoothAngle.RandomPattern(
            random.nextFloat(0.09F, 0.4F),
            random.nextFloat(0.08F, 0.36F),
            random.nextFloat(80.0F, 310.0F),
            random.nextFloat(72.0F, 285.0F),
            random.nextFloat(0.3F, 0.65F),
            random.nextFloat(0.22F, 1.14F),
            random.nextFloat(0.13F, 0.83F),
            random.nextFloat(66.0F, 280.0F),
            random.nextFloat(0.75F, 2.35F),
            minDelay,
            maxDelay
         );
      }
   }
}

