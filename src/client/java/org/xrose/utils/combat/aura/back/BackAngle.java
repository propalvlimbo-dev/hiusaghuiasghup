package org.xrose.utils.combat.aura.back;

import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class BackAngle extends RotateConstructor {
   private final float speed;
   private final Random random = new Random();
   private float lastYawJitter;
   private float lastPitchJitter;

   public BackAngle(float speed) {
      super("UseBack");
      this.speed = Mth.clamp(speed, 0.05F, 1.0F);
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
      float absYaw = Math.abs(delta.getYaw());
      float absPitch = Math.abs(delta.getPitch());
      float adaptiveSpeed = this.speed;
      if (absYaw < 5.0F || absPitch < 5.0F) {
         adaptiveSpeed = Mth.clamp(this.speed * 1.2F, 0.05F, 0.98F);
      }

      float randomFactor = 1.0F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
      adaptiveSpeed = Mth.clamp(adaptiveSpeed * randomFactor, 0.1F, 0.98F);
      float yawStep = delta.getYaw() * adaptiveSpeed;
      float pitchStep = delta.getPitch() * adaptiveSpeed;
      this.lastYawJitter = Mth.lerp(0.4F, this.lastYawJitter, (this.random.nextFloat() - 0.5F) * 0.6F);
      this.lastPitchJitter = Mth.lerp(0.4F, this.lastPitchJitter, (this.random.nextFloat() - 0.5F) * 0.4F);
      yawStep += this.lastYawJitter;
      pitchStep += this.lastPitchJitter;
      if (Math.abs(yawStep) < 0.01F) {
         yawStep = delta.getYaw();
      }

      if (Math.abs(pitchStep) < 0.01F) {
         pitchStep = delta.getPitch();
      }

      return new Angle(currentAngle.getYaw() + yawStep, Mth.clamp(currentAngle.getPitch() + pitchStep, -89.0F, 90.0F));
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}

