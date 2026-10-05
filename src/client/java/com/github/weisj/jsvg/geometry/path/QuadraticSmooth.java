package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class QuadraticSmooth extends PathCommand {
   private final float x;
   private final float y;

   public QuadraticSmooth(boolean isRelative, float x, float y) {
      super(isRelative, 4);
      this.x = x;
      this.y = y;
   }

   @Override
   public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
      Float offset = this.offset(hist);
      Float knot = this.lastKnotReflectionQuadratic(hist);
      path.quadTo(knot.x, knot.y, this.x + offset.x, this.y + offset.y);
      hist.setLastQuadratic(path.getCurrentPoint(), knot);
   }

   @Override
   public String toString() {
      return "T " + this.x + " " + this.y;
   }
}

