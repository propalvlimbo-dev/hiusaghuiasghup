package org.xrose.utils.combat.aura.util;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;

public final class MathUtils {
   private MathUtils() {
   }

   public static float getRandom(float min, float max) {
      if (min == max) {
         return min;
      }

      if (max < min) {
         float tmp = min;
         min = max;
         max = tmp;
      }

      return (float)ThreadLocalRandom.current().nextDouble(min, max);
   }

   public static double computeGcd() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.options != null) {
         double sensitivity = (Double)client.options.sensitivity().get();
         double factor = sensitivity * 0.6 + 0.2;
         return factor * factor * factor * 1.2;
      } else {
         return 0.0;
      }
   }
}

