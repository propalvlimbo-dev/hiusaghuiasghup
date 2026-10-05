package com.github.weisj.jsvg.geometry.util;

import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;

final class TransformUtil {
   private TransformUtil() {
   }

   @NotNull
   static AffineTransform interpolate(@NotNull AffineTransform a, @NotNull AffineTransform b, float t) {
      double[] aEntries = new double[6];
      double[] bEntries = new double[6];
      a.getMatrix(aEntries);
      b.getMatrix(bEntries);

      for (int i = 0; i < aEntries.length; i++) {
         aEntries[i] = GeometryUtil.lerp(t, aEntries[i], bEntries[i]);
      }

      return new AffineTransform(aEntries);
   }
}

