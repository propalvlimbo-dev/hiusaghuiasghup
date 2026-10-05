package org.xrose.utils.combat.aura.util;

import net.minecraft.world.phys.Vec3;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;

public final class AngleUtil {
   private AngleUtil() {
   }

   public static Angle calculateDelta(Angle start, Angle end) {
      return MathAngle.calculateDelta(start, end);
   }

   public static Vec3 getRandom() {
      return new Vec3(0.06, 0.1, 0.06);
   }
}

