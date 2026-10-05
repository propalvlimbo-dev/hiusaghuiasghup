package org.xrose.utils.combat.aura.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class SnapAngle extends RotateConstructor {
   public SnapAngle() {
      super("Snap");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      AuraFeature aura = AuraFeature.getInstance();
      Minecraft client = Minecraft.getInstance();
      if (aura != null && client.player != null) {
         StrikeManager attackHandler = aura.getAttackPerpetrator().getAttackHandler();
         return currentAngle;
      } else {
         return currentAngle;
      }
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }

   private Angle step(Angle currentAngle, Angle targetAngle, float speed) {
      Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = delta.getYaw();
      float pitchDelta = delta.getPitch();
      float difference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      if (difference < 1.0E-4F) {
         difference = 1.0E-4F;
      }

      float straightLineYaw = Math.abs(yawDelta / difference) * speed;
      float straightLinePitch = Math.abs(pitchDelta / difference) * speed;
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
      );
   }
}

