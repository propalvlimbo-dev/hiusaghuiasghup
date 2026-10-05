package org.xrose.utils.combat.aura.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.StopWatch;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import org.xrose.utils.combat.aura.util.MathUtils;

public class RWAngle extends RotateConstructor {
   public RWAngle() {
      super("ReallyWorld");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      AuraFeature aura = AuraFeature.getInstance();
      if (aura == null) {
         return currentAngle;
      }

      StrikeManager attackHandler = aura.getAttackPerpetrator().getAttackHandler();
      if (attackHandler == null) {
         return currentAngle;
      }

      if (entity != null && !aura.isElytraPredictAimActive()) {
         Vec3 aimPoint = this.brain(entity, 1, 0.5F);
         targetAngle = MathAngle.calculateAngle(aimPoint);
      }

      int count = attackHandler.getCount();
      StopWatch attackTimer = attackHandler.getAttackTimer();
      Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
      float preAttackSpeed = 1.0F;
      float postAttackSpeed = 1.0F;
      float speed = canAttack ? preAttackSpeed : postAttackSpeed;
      float lineYaw = Math.abs(yawDelta / rotationDifference) * 180.0F;
      float linePitch = Math.abs(pitchDelta / rotationDifference) * 180.0F;
      float jitterYaw = canAttack ? 0.0F : (float)(-2.0 * Math.cos(System.currentTimeMillis() / 90.0));
      float jitterPitch = canAttack ? 0.0F : (float)(2.0 * Math.sin(System.currentTimeMillis() / 90.0));
      if (aura.isEnabled() && entity != null) {
         float moveYaw = Mth.clamp(yawDelta, -lineYaw, lineYaw);
         float movePitch = Mth.clamp(pitchDelta, -linePitch, linePitch);
         Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
         moveAngle.setYaw(Mth.lerp(this.randomLerp(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
         moveAngle.setPitch(Mth.lerp(this.randomLerp(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);
         if (count > 0 && count % 50 == 0 && !attackTimer.finished(200.0)) {
            moveAngle.setPitch(Mth.lerp(0.25F, currentAngle.getPitch(), currentAngle.getPitch() - 90.0F) + jitterPitch);
         }

         return moveAngle;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            Angle viewDelta = MathAngle.calculateDelta(currentAngle, new Angle(mc.player.getYRot(), mc.player.getXRot()));
            float returnYaw = viewDelta.getYaw();
            float returnPitch = viewDelta.getPitch();
            float returnDiff = (float)Math.hypot(Math.abs(returnYaw), Math.abs(returnPitch));
            if (returnDiff > 1.0E-4F) {
               float maxYawStep = this.randomLerp(26.0F, 45.0F);
               float maxPitchStep = this.randomLerp(18.0F, 34.0F);
               float stepYaw = Math.abs(returnYaw / returnDiff) * maxYawStep;
               float stepPitch = Math.abs(returnPitch / returnDiff) * maxPitchStep;
               return new Angle(
                  currentAngle.getYaw() + Math.min(Math.max(returnYaw, -stepYaw), stepYaw),
                  currentAngle.getPitch() + Math.min(Math.max(returnPitch, -stepPitch), stepPitch)
               );
            }
         }

         speed = 1.0F;
         jitterYaw = 0.0F;
         jitterPitch = 0.0F;
         return new Angle(currentAngle.getYaw(), currentAngle.getPitch());
      }
   }

   private Vec3 brain(Entity entity, int index, float height) {
      if (!(entity instanceof LivingEntity livingEntity)) {
         return entity.position().add(0.0, entity.getEyeHeight(entity.getPose()), 0.0);
      } else {
         float entityHeight = livingEntity.getBbHeight();
         float standingHeight = entityHeight * 0.85F;
         float offsetY = standingHeight * (height / 100.0F);
         return livingEntity.position().add(0.0, offsetY, 0.0);
      }
   }

   private float randomLerp(float min, float max) {
      return Mth.lerp(MathUtils.getRandom(0.0F, 1.0F), min, max);
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}

