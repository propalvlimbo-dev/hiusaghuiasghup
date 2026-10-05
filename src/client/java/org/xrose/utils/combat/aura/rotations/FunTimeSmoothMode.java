package org.xrose.utils.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class FunTimeSmoothMode extends RotateConstructor {
   private final SecureRandom random = new SecureRandom();

   public FunTimeSmoothMode() {
      super("Funtime");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      AuraFeature aura = AuraFeature.getInstance();
      if (entity != null && aura.getAttackPerpetrator().getAttackHandler().canAttack(aura.getConfig(), 1)) {
         Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
         float yawDelta = angleDelta.getYaw();
         float pitchDelta = angleDelta.getPitch();
         float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
         float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 130.0F;
         float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 130.0F;
         return new Angle(
            currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
            currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
         );
      }

      Angle playerViewAngle = new Angle(mc.player.getYRot(), mc.player.getXRot());
      Angle angleToPlayerView = MathAngle.calculateDelta(currentAngle, playerViewAngle);
      float yawDelta = angleToPlayerView.getYaw();
      float pitchDelta = angleToPlayerView.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      float yaw = (float)(this.randomLerp(12.0F, 24.0F) * Math.sin(System.currentTimeMillis() / 40.0));
      float pitch = (float)(this.randomLerp(4.0F, 12.0F) * Math.cos(System.currentTimeMillis() / 40.0));
      StrikeManager attackHandler = aura.getAttackPerpetrator().getAttackHandler();
      if (AuraFeature.target == null && attackHandler.getAttackTimer().finished(500.0)) {
         yaw = 0.0F;
         pitch = 0.0F;
      }

      float straightLineYaw = Math.abs(yawDelta / rotationDifference) * (!attackHandler.getAttackTimer().finished(435.0) ? 0 : 45);
      float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * (!attackHandler.getAttackTimer().finished(435.0) ? 0 : 45);
      int count = attackHandler.getCount();
      if (count % 40 == 0 && count > 0 && !attackHandler.getAttackTimer().finished(250.0)) {
         pitch = -90.0F;
         if (attackHandler.getAttackTimer().finished(240.0)) {
            mc.player.swing(InteractionHand.MAIN_HAND);
         }
      }

      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw) + yaw,
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch) + pitch
      );
   }

   private float randomLerp(float min, float max) {
      return min + this.random.nextFloat() * (max - min);
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}

