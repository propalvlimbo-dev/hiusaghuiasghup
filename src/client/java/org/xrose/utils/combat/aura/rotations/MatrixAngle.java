package org.xrose.utils.combat.aura.rotations;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.attack.StrikerConstructor;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import org.xrose.utils.combat.aura.target.RaycastAngle;
import org.xrose.utils.combat.aura.util.MathUtils;

public class MatrixAngle extends RotateConstructor {
   public MatrixAngle() {
      super("Matrix");
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

      StrikerConstructor.AttackPerpetratorConfigurable config = aura.getConfig();
      Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      if (rotationDifference < 1.0E-4F) {
         rotationDifference = 1.0E-4F;
      }

      boolean shouldAttack = entity != null && attackHandler.canAttack(config, 2);
      boolean shouldAttack2 = entity != null && attackHandler.canAttack(config, 0);
      boolean shouldAttack3 = entity != null && attackHandler.canAttack(config, 5);
      boolean rayTrace = entity != null && RaycastAngle.rayTrace(aura.attackDistance(), config.getBox());
      float straightLineYaw = Math.abs(yawDelta / rotationDifference) * (shouldAttack2 ? 100.0F : MathUtils.getRandom(25.0F, 50.0F));
      float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * (shouldAttack2 ? 100.0F : MathUtils.getRandom(5.0F, 25.0F));
      if ((float)attackHandler.getAttackTimer().elapsedTime() < MathUtils.getRandom(0.0F, 300.0F)) {
         straightLinePitch = -4.0F;
         straightLineYaw = -4.0F;
      }

      float jitterY = shouldAttack3 ? (!rayTrace && shouldAttack ? 0.0F : MathUtils.getRandom(3.0F, -3.0F)) : 0.0F;
      float jitterX = shouldAttack3 ? (!rayTrace && shouldAttack ? 0.0F : MathUtils.getRandom(3.0F, -3.0F)) : 0.0F;
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw) + jitterX,
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch) + jitterY
      );
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}

