package org.xrose.utils.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class SpookyAnkaSmoothMode extends RotateConstructor {
   private final SecureRandom random = new SecureRandom();
   private final float circleSpeed = 2.3F;
   private final float circleRadius = 2.2F;
   private final float shakeIntensity = 0.7F;
   private final float shakeDistance = 18.0F;
   private int pendingDelayTicks = 0;
   private boolean prevCanAttack = false;

   public SpookyAnkaSmoothMode() {
      super("SpookyAnka");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return targetAngle != null ? targetAngle : currentAngle;
      }

      if (entity != null) {
         AuraFeature aura = AuraFeature.getInstance();
         StrikeManager attackHandler = aura.getAttackPerpetrator().getAttackHandler();
         Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle != null ? targetAngle : currentAngle);
         float yawDelta = angleDelta.getYaw();
         float pitchDelta = angleDelta.getPitch();
         float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
         if (rotationDifference < 1.0E-4F) {
            return currentAngle;
         }

         float distance = mc.player.distanceTo(entity);
         float maxRange = aura.attackDistance() + aura.getLookRange().getFloat();
         float distanceFactor = Mth.clamp(distance / Math.max(maxRange, 0.1F), 0.0F, 1.0F);
         float distanceMultiplier = Mth.lerp(distanceFactor, 0.3F, 1.0F);
         boolean canAttack = attackHandler.canAttack(aura.getConfig(), 0);
         if (canAttack && !this.prevCanAttack) {
            this.pendingDelayTicks = this.random.nextInt(3);
         }

         this.prevCanAttack = canAttack;
         float speed;
         if (canAttack) {
            if (this.pendingDelayTicks > 0) {
               speed = this.randomLerp(0.25F, 0.5F);
               this.pendingDelayTicks--;
            } else {
               speed = this.randomLerp(0.6F, 0.93F);
            }
         } else {
            speed = this.random.nextFloat() < 0.6F ? this.randomLerp(0.35F, 0.55F) : this.randomLerp(0.15F, 0.3F);
         }

         speed *= distanceMultiplier;
         float lineYaw = Math.abs(yawDelta / rotationDifference) * 190.0F * distanceMultiplier;
         float linePitch = Math.abs(pitchDelta / rotationDifference) * 190.0F * distanceMultiplier;
         float moveYaw = Mth.clamp(yawDelta, -lineYaw, lineYaw);
         float movePitch = Mth.clamp(pitchDelta, -linePitch, linePitch);
         Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
         float lerpVal = this.randomLerp(speed, speed + 0.7F);
         float jitterYaw = (this.random.nextFloat() - 0.5F) * 0.8F;
         float jitterPitch = (this.random.nextFloat() - 0.5F) * 0.5F;
         moveAngle.setYaw(Mth.lerp(lerpVal, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
         moveAngle.setPitch(Mth.lerp(lerpVal, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);
         this.applyFunTimeOffsets(moveAngle);
         return moveAngle;
      } else {
         float playerYaw = mc.player.getYRot();
         float playerPitch = mc.player.getXRot();
         float yawDelta = Mth.wrapDegrees(playerYaw - currentAngle.getYaw());
         float pitchDelta = playerPitch - currentAngle.getPitch();
         float returnSpeed = 0.7F;
         return new Angle(currentAngle.getYaw() + yawDelta * returnSpeed, currentAngle.getPitch() + pitchDelta * returnSpeed);
      }
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.06, 0.1, 0.06);
   }

   private float randomLerp(float min, float max) {
      return Mth.lerp(this.random.nextFloat(), min, max);
   }

   private void applyFunTimeOffsets(Angle moveAngle) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         float age = mc.player.tickCount;
         float circlePhase = age * 2.3F;
         float circleYawOffset = Mth.sin(circlePhase) * 2.2F;
         float circlePitchOffset = Mth.cos(circlePhase) * 2.2F * 0.6F;
         float shakePhase = age * 0.7F;
         float shakeOffset = Mth.sin(shakePhase) * 18.0F;
         moveAngle.setYaw(moveAngle.getYaw() + circleYawOffset + shakeOffset);
         moveAngle.setPitch(Mth.clamp(moveAngle.getPitch() + circlePitchOffset, -90.0F, 90.0F));
      }
   }
}

