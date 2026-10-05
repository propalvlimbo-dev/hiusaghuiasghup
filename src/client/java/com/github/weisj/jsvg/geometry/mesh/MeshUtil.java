package com.github.weisj.jsvg.geometry.mesh;

import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class MeshUtil {
   private MeshUtil() {
   }

   @NotNull
   static Float p(float x, float y) {
      return new Float(x, y);
   }
}

