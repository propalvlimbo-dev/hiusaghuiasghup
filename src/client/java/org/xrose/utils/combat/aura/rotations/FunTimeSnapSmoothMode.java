package org.xrose.utils.combat.aura.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public final class FunTimeSnapSmoothMode extends RotateConstructor {
   private long smoothbackShakeStartMs = -1L;

   public FunTimeSnapSmoothMode() {
      super("FunTimeSnap");
   }

   @Override
   public Angle limitAngleChange(Angle current, Angle target, Vec3 aimPoint, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      AuraFeature aura = AuraFeature.getInstance();
      if (aura != null && mc.player != null) {
         StrikeManager attackHandler = aura.getAttackPerpetrator().getAttackHandler();
         boolean canAttack = attackHandler != null && aura.getConfig() != null && attackHandler.canAttack(aura.getConfig(), 4);
         if (aura.isEnabled() && aura.getTarget() != null && entity != null && canAttack) {
            this.smoothbackShakeStartMs = -1L;
            Angle delta = MathAngle.calculateDelta(current, target);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotationDifference = this.safeRotationDifference(yawDelta, pitchDelta);
            float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 110.0F;
            float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 110.0F;
            float desiredYaw = Mth.lerp(0.55F, current.getYaw(), current.getYaw() + Mth.clamp(yawDelta, -straightLineYaw, straightLineYaw));
            float desiredPitch = Mth.lerp(0.55F, current.getPitch(), current.getPitch() + Mth.clamp(pitchDelta, -straightLinePitch, straightLinePitch));
            return this.animate(current, desiredYaw, desiredPitch);
         }

         Angle playerView = new Angle(mc.player.getYRot(), mc.player.getXRot());
         Angle delta = MathAngle.calculateDelta(current, playerView);
         float yawDelta = delta.getYaw();
         float pitchDelta = delta.getPitch();
         float rotationDifference = this.safeRotationDifference(yawDelta, pitchDelta);
         if (rotationDifference < 1.0F) {
            this.smoothbackShakeStartMs = -1L;
            return playerView;
         }

         long now = System.currentTimeMillis();
         float yawShake = (float)(FunTimeSnapSmoothMode.FTMathUtil.smoothRandom(-12.0F, 12.0F, 14.0F) * Math.sin(now / 70.0));
         float pitchShake = (float)(FunTimeSnapSmoothMode.FTMathUtil.smoothRandom(-6.0F, 6.0F, 14.0F) * Math.cos(now / 60.0));
         if (aura.isEnabled() && aura.getTarget() != null) {
            this.smoothbackShakeStartMs = -1L;
         } else {
            if (this.smoothbackShakeStartMs < 0L) {
               this.smoothbackShakeStartMs = now;
            }

            float shakeFade = 1.0F - Mth.clamp((float)(now - this.smoothbackShakeStartMs) / 1000.0F, 0.0F, 1.0F);
            yawShake *= shakeFade;
            pitchShake *= shakeFade;
         }

         boolean attackCooldownFinished = attackHandler == null || attackHandler.getAttackTimer().finished(535.0);
         float speed = attackCooldownFinished ? 45.0F : 0.0F;
         float straightLineYaw = Math.abs(yawDelta / rotationDifference) * speed;
         float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * speed;
         float desiredYaw = Mth.lerp(0.85F, current.getYaw(), current.getYaw() + Mth.clamp(yawDelta, -straightLineYaw, straightLineYaw) + yawShake);
         float desiredPitch = Mth.lerp(
            0.85F, current.getPitch(), current.getPitch() + Mth.clamp(pitchDelta, -straightLinePitch, straightLinePitch) + pitchShake
         );
         return this.animate(current, desiredYaw, desiredPitch);
      } else {
         return current;
      }
   }

   private Angle animate(Angle current, float targetYaw, float targetPitch) {
      return FunTimeSnapSmoothMode.Animator.back(FunTimeSnapSmoothMode.MathUtils.getRandom(6.0F, 7.0F), FunTimeSnapSmoothMode.class)
         .apply(current, targetYaw, Mth.clamp(targetPitch, -90.0F, 90.0F));
   }

   private float safeRotationDifference(float yawDelta, float pitchDelta) {
      float diff = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      return diff == 0.0F ? 1.0E-4F : diff;
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }

   public static final class Animator {
      public static FunTimeSnapSmoothMode.Animator.AngleAnimator back(float speed, Class<?> clazz) {
         return new FunTimeSnapSmoothMode.Animator.AngleAnimator(speed);
      }

      public record AngleAnimator(float speed) {
         public Angle apply(Angle current, float targetYaw, float targetPitch) {
            return new Angle(targetYaw, targetPitch);
         }
      }
   }

   public static final class FTMathUtil {
      public static float smoothRandom(float min, float max, float step) {
         return org.xrose.utils.combat.aura.util.MathUtils.getRandom(min, max);
      }
   }

   public static final class MathUtils {
      public static float getRandom(float min, float max) {
         return org.xrose.utils.combat.aura.util.MathUtils.getRandom(min, max);
      }
   }
}

