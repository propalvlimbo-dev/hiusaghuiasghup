package org.xrose.utils.combat.aura.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.utils.combat.aura.Angle;

public abstract class RotateConstructor {
   private final String name;

   protected RotateConstructor(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle) {
      return this.limitAngleChange(currentAngle, targetAngle, null, null);
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d) {
      return this.limitAngleChange(currentAngle, targetAngle, vec3d, null);
   }

   public abstract Angle limitAngleChange(Angle var1, Angle var2, Vec3 var3, Entity var4);

   public abstract Vec3 randomValue();
}

