package org.xrose.utils.combat.aura.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class FTNewAngle extends RotateConstructor {
   public static final FTNewAngle INSTANCE = new FTNewAngle();
   private long noiseTimestamp = -1L;
   private boolean active;

   public FTNewAngle() {
      super("FT-New");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return currentAngle;
      }

      AuraFeature aura = AuraFeature.getInstance();
      StrikeManager handler = aura != null ? aura.getAttackPerpetrator().getAttackHandler() : null;
      boolean hasTarget = entity != null && entity.isAlive();
      this.active = hasTarget;
      if (hasTarget && handler != null) {
         boolean canCrit = handler.canAttack(aura.getConfig(), 2);
         boolean recentAttack = !handler.getAttackTimer().finished(535.0);
         if (canCrit) {
            this.noiseTimestamp = -1L;
            return this.clampLerpTo(currentAngle, targetAngle, 130.0F, 130.0F, 0.85F);
         } else {
            Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotDiff = Math.max((float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);
            float yawNoise = (float)(this.rand(4.0F, 15.0F) * Math.sin(System.currentTimeMillis() / 95.0));
            float pitchNoise = (float)(this.rand(6.0F, 7.0F) * Math.cos(System.currentTimeMillis() / 45.0));
            this.noiseTimestamp = -1L;
            float yawBudget = recentAttack ? 0.0F : Math.abs(yawDelta / rotDiff) * 45.0F;
            float pitchBudget = recentAttack ? 0.0F : Math.abs(pitchDelta / rotDiff) * 45.0F;
            float newYaw = currentAngle.getYaw() + Mth.clamp(yawDelta, -yawBudget, yawBudget) + yawNoise;
            float newPitch = currentAngle.getPitch() + Mth.clamp(pitchDelta, -pitchBudget, pitchBudget) + pitchNoise;
            return new Angle(this.lerp(0.85F, currentAngle.getYaw(), newYaw), Mth.clamp(this.lerp(0.85F, currentAngle.getPitch(), newPitch), -90.0F, 90.0F));
         }
      } else {
         Angle cameraAngle = MathAngle.cameraAngle();
         Angle resetDelta = MathAngle.calculateDelta(currentAngle, cameraAngle);
         float yawDelta = resetDelta.getYaw();
         float pitchDelta = resetDelta.getPitch();
         float rotDiff = Math.max((float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);
         if (this.noiseTimestamp < 0L) {
            this.noiseTimestamp = System.currentTimeMillis();
         }

         float fade = 1.0F - Mth.clamp((float)(System.currentTimeMillis() - this.noiseTimestamp) / 1000.0F, 0.0F, 1.0F);
         float yawNoise = (float)(this.rand(4.0F, 15.0F) * Math.sin(System.currentTimeMillis() / 95.0)) * fade;
         float pitchNoise = (float)(this.rand(6.0F, 7.0F) * Math.cos(System.currentTimeMillis() / 45.0)) * fade;
         float yawBudget = Math.abs(yawDelta / rotDiff) * 45.0F;
         float pitchBudget = Math.abs(pitchDelta / rotDiff) * 45.0F;
         float newYaw = currentAngle.getYaw() + Mth.clamp(yawDelta, -yawBudget, yawBudget) + yawNoise;
         float newPitch = currentAngle.getPitch() + Mth.clamp(pitchDelta, -pitchBudget, pitchBudget) + pitchNoise;
         return new Angle(this.lerp(0.85F, currentAngle.getYaw(), newYaw), Mth.clamp(this.lerp(0.85F, currentAngle.getPitch(), newPitch), -90.0F, 90.0F));
      }
   }

   private Angle clampLerpTo(Angle from, Angle to, float yawMax, float pitchMax, float factor) {
      Angle delta = MathAngle.calculateDelta(from, to);
      float yawDelta = delta.getYaw();
      float pitchDelta = delta.getPitch();
      float rotDiff = Math.max((float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);
      float clampedYaw = Math.abs(yawDelta / rotDiff) * yawMax;
      float clampedPitch = Math.abs(pitchDelta / rotDiff) * pitchMax;
      return new Angle(
         this.lerp(factor, from.getYaw(), from.getYaw() + Mth.clamp(yawDelta, -clampedYaw, clampedYaw)),
         Mth.clamp(this.lerp(factor, from.getPitch(), from.getPitch() + Mth.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90.0F, 90.0F)
      );
   }

   private float lerp(float f, float a, float b) {
      return a + f * (b - a);
   }

   private float rand(float min, float max) {
      return (float)ThreadLocalRandom.current().nextDouble(min, max);
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.05, 0.1, 0.02);
   }
}

