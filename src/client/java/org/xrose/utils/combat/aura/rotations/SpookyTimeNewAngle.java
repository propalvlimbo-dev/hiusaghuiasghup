package org.xrose.utils.combat.aura.rotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class SpookyTimeNewAngle extends RotateConstructor {
   public static final SpookyTimeNewAngle INSTANCE = new SpookyTimeNewAngle();
   private static final float AIM_STRENGTH = 1850.0F;
   private static final float AIM_STRENGTH_MAX = 100.0F;
   private static final boolean RANDOMIZATION = true;
   private static final float RANDOMIZATION_STRENGTH = 1.5F;
   private static final float RANDOMIZATION_INTERPOLATION = 0.45F;
   private static final int PATTERN_HISTORY_LIMIT = 8;
   private static final int PATTERN_CANDIDATE_ATTEMPTS = 18;
   private Entity lastTarget;
   private float frozenTargetPitch = 0.0F;
   private SpookyTimeNewAngle.RandomPattern currentPattern = SpookyTimeNewAngle.RandomPattern.defaultPattern();
   private long currentPatternId = 0L;
   private final List<SpookyTimeNewAngle.RandomPattern> recentPatterns = new ArrayList<>();
   private long lastRandomRetargetTime = 0L;
   private int nextRandomRetargetDelayMs = randomRetargetDelayMs(this.currentPattern);
   private float randomCurveYaw = 0.0F;
   private float randomCurvePitch = 0.0F;
   private float randomCurveTargetYaw = 0.0F;
   private float randomCurveTargetPitch = 0.0F;
   private float randomOffsetYaw = 0.0F;
   private float randomOffsetPitch = 0.0F;
   private float appliedPitchNoise = 0.0F;

   public SpookyTimeNewAngle() {
      super("SpookyDuel");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft client = Minecraft.getInstance();
      AuraFeature aura = AuraFeature.getInstance();
      if (client.player != null && entity != null) {
         if (this.lastTarget != entity) {
            this.resetRandomState();
            this.lastTarget = entity;
         }

         double deltaSeconds = this.getDeltaSeconds();
         float targetYaw = targetAngle.getYaw();
         float targetPitch = targetAngle.getPitch();
         if (client.player.getAttackStrengthScale(0.0F) > 0.75F) {
            this.frozenTargetPitch = targetPitch;
         }

         float yawDiff = Mth.wrapDegrees(targetYaw - currentAngle.getYaw());
         float maxYawStep = this.getMaxAssistStep(deltaSeconds, false, Math.abs(yawDiff));
         yawDiff = Mth.clamp(yawDiff, -maxYawStep, maxYawStep);
         float yaw = currentAngle.getYaw() + yawDiff;
         float pitch = this.frozenTargetPitch;
         float targetPitchNoise = this.getRandomPitchNoise();
         float maxNoiseStep = Math.max(0.02F, (float)deltaSeconds * 36.0F);
         float randomPitchAssist = Mth.clamp(targetPitchNoise - this.appliedPitchNoise, -maxNoiseStep, maxNoiseStep);
         this.appliedPitchNoise += randomPitchAssist;
         pitch += this.appliedPitchNoise;
         yaw = lerpAngle(currentAngle.getYaw(), yaw, 0.55F);
         pitch = lerpAngle(currentAngle.getPitch(), pitch, 0.25F);
         pitch = Mth.clamp(pitch, -90.0F, 90.0F);
         return new Angle(yaw, pitch);
      } else {
         this.lastTarget = null;
         return targetAngle != null ? targetAngle : currentAngle;
      }
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.06, 0.1, 0.06);
   }

   private float getMaxAssistStep(double deltaSeconds, boolean pitch, float difference) {
      float speedFactor = Mth.clamp(12.025F, 0.01F, 18.0F);
      float baseSpeed = pitch ? 90.0F + speedFactor * 560.0F : speedFactor * 980.0F;
      float closeFactor = Mth.clamp(difference / (pitch ? 8.0F : 10.0F), pitch ? 0.05F : 0.06F, 1.0F);
      return Math.max(0.01F, (float)deltaSeconds * baseSpeed * closeFactor);
   }

   private float getRandomPitchNoise() {
      long now = System.currentTimeMillis();
      float strength = 1.5F;
      if (strength <= 1.0E-4F) {
         return 0.0F;
      }

      float interpolationDamp = 0.685F;
      float pitchNoise = this.randomCurvePitch * this.currentPattern.pitchNoiseCurveFactor()
         + (float)Math.sin((float)now / this.currentPattern.pitchNoiseSinePeriodMs())
            * this.currentPattern.pitchNoiseSineAmplitude()
            * strength
            * interpolationDamp;
      return Mth.clamp(pitchNoise, -this.currentPattern.pitchNoiseClamp(), this.currentPattern.pitchNoiseClamp());
   }

   public long generateRandomPattern() {
      SpookyTimeNewAngle.RandomPattern nextPattern = this.createMostDistinctPattern();
      this.applyPattern(nextPattern);
      this.rememberPattern(nextPattern);
      this.currentPatternId++;
      return this.currentPatternId;
   }

   public long getCurrentPatternId() {
      return this.currentPatternId;
   }

   private double getDeltaSeconds() {
      return 0.05;
   }

   private SpookyTimeNewAngle.RandomPattern createMostDistinctPattern() {
      SpookyTimeNewAngle.RandomPattern bestPattern = SpookyTimeNewAngle.RandomPattern.randomPattern();
      double bestScore = this.scorePattern(bestPattern);

      for (int i = 1; i < 18; i++) {
         SpookyTimeNewAngle.RandomPattern candidate = SpookyTimeNewAngle.RandomPattern.randomPattern();
         double score = this.scorePattern(candidate);
         if (score > bestScore) {
            bestScore = score;
            bestPattern = candidate;
         }
      }

      return bestPattern;
   }

   private double scorePattern(SpookyTimeNewAngle.RandomPattern candidate) {
      double minDistance = this.patternDistance(candidate, this.currentPattern);

      for (SpookyTimeNewAngle.RandomPattern previous : this.recentPatterns) {
         minDistance = Math.min(minDistance, this.patternDistance(candidate, previous));
      }

      return minDistance + ThreadLocalRandom.current().nextDouble(0.0, 0.02);
   }

   private double patternDistance(SpookyTimeNewAngle.RandomPattern a, SpookyTimeNewAngle.RandomPattern b) {
      double distance = 0.0;
      distance += Math.abs(a.maxCurveYaw() - b.maxCurveYaw()) / 3.5;
      distance += Math.abs(a.maxCurvePitch() - b.maxCurvePitch()) / 2.6;
      distance += Math.abs(a.sineAmplitudeYaw() - b.sineAmplitudeYaw()) / 0.45;
      distance += Math.abs(a.sineAmplitudePitch() - b.sineAmplitudePitch()) / 0.4;
      distance += Math.abs(a.sinePeriodYawMs() - b.sinePeriodYawMs()) / 220.0;
      distance += Math.abs(a.sinePeriodPitchMs() - b.sinePeriodPitchMs()) / 220.0;
      distance += Math.abs(a.yawStepPerSecond() - b.yawStepPerSecond()) / 12.0;
      distance += Math.abs(a.pitchStepPerSecond() - b.pitchStepPerSecond()) / 6.0;
      distance += Math.abs(a.minRetargetDelayMs() - b.minRetargetDelayMs()) / 280.0;
      distance += Math.abs(a.maxRetargetDelayMs() - b.maxRetargetDelayMs()) / 420.0;
      distance += Math.abs(a.pitchNoiseCurveFactor() - b.pitchNoiseCurveFactor()) / 1.0;
      distance += Math.abs(a.pitchNoiseSineAmplitude() - b.pitchNoiseSineAmplitude()) / 0.8;
      distance += Math.abs(a.pitchNoiseSinePeriodMs() - b.pitchNoiseSinePeriodMs()) / 220.0;
      return distance + Math.abs(a.pitchNoiseClamp() - b.pitchNoiseClamp()) / 2.2;
   }

   private void applyPattern(SpookyTimeNewAngle.RandomPattern pattern) {
      this.currentPattern = pattern;
      this.nextRandomRetargetDelayMs = randomRetargetDelayMs(pattern);
      this.lastRandomRetargetTime = 0L;
      this.randomCurveYaw = 0.0F;
      this.randomCurvePitch = 0.0F;
      this.randomCurveTargetYaw = 0.0F;
      this.randomCurveTargetPitch = 0.0F;
      this.randomOffsetYaw = 0.0F;
      this.randomOffsetPitch = 0.0F;
      this.appliedPitchNoise = 0.0F;
   }

   private void rememberPattern(SpookyTimeNewAngle.RandomPattern pattern) {
      this.recentPatterns.add(0, pattern);

      while (this.recentPatterns.size() > 8) {
         this.recentPatterns.remove(this.recentPatterns.size() - 1);
      }
   }

   private void resetRandomState() {
      this.randomOffsetYaw = 0.0F;
      this.randomOffsetPitch = 0.0F;
      this.randomCurveTargetYaw = 0.0F;
      this.randomCurveTargetPitch = 0.0F;
      this.appliedPitchNoise = 0.0F;
      this.lastRandomRetargetTime = 0L;
   }

   private static int randomRetargetDelayMs(SpookyTimeNewAngle.RandomPattern pattern) {
      int minDelay = Math.max(30, pattern.minRetargetDelayMs());
      int maxDelay = Math.max(minDelay + 1, pattern.maxRetargetDelayMs());
      return ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
   }

   private static float lerpAngle(float start, float end, float factor) {
      float delta = Mth.wrapDegrees(end - start);
      return start + delta * factor;
   }

   private record RandomPattern(
      float maxCurveYaw,
      float maxCurvePitch,
      float sineAmplitudeYaw,
      float sineAmplitudePitch,
      float sinePeriodYawMs,
      float sinePeriodPitchMs,
      float yawStepPerSecond,
      float pitchStepPerSecond,
      int minRetargetDelayMs,
      int maxRetargetDelayMs,
      float pitchNoiseCurveFactor,
      float pitchNoiseSineAmplitude,
      float pitchNoiseSinePeriodMs,
      float pitchNoiseClamp
   ) {
      private static SpookyTimeNewAngle.RandomPattern defaultPattern() {
         return new SpookyTimeNewAngle.RandomPattern(2.0F, 1.6F, 0.22F, 0.2F, 125.0F, 95.0F, 7.5F, 2.8F, 150, 300, 0.55F, 0.32F, 95.0F, 1.4F);
      }

      private static SpookyTimeNewAngle.RandomPattern randomPattern() {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         int minDelay = random.nextInt(90, 280);
         int maxDelay = random.nextInt(minDelay + 55, 430);
         return new SpookyTimeNewAngle.RandomPattern(
            random.nextFloat(1.05F, 3.35F),
            random.nextFloat(0.75F, 2.35F),
            random.nextFloat(0.09F, 0.4F),
            random.nextFloat(0.08F, 0.36F),
            random.nextFloat(80.0F, 310.0F),
            random.nextFloat(72.0F, 285.0F),
            random.nextFloat(4.3F, 15.5F),
            random.nextFloat(1.7F, 8.2F),
            minDelay,
            maxDelay,
            random.nextFloat(0.22F, 1.14F),
            random.nextFloat(0.13F, 0.83F),
            random.nextFloat(66.0F, 280.0F),
            random.nextFloat(0.75F, 2.35F)
         );
      }
   }
}

