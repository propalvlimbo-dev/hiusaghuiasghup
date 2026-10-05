package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class Quadratic extends PathCommand {
   private final float kx;
   private final float ky;
   private final float x;
   private final float y;

   @Override
   public String toString() {
      return "Q " + this.kx + " " + this.ky + " " + this.x + " " + this.y;
   }

   public Quadratic(boolean isRelative, float kx, float ky, float x, float y) {
      super(isRelative, 4);
      this.kx = kx;
      this.ky = ky;
      this.x = x;
      this.y = y;
   }

   @Override
   public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
      Float offset = this.offset(hist);
      path.quadTo(this.kx + offset.x, this.ky + offset.y, this.x + offset.x, this.y + offset.y);
      hist.setLastQuadratic(path.getCurrentPoint(), this.kx + offset.x, this.ky + offset.y);
   }
}

