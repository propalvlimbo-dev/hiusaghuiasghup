package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class Cubic extends PathCommand {
   private final float k1x;
   private final float k1y;
   private final float k2x;
   private final float k2y;
   private final float x;
   private final float y;

   public Cubic(boolean isRelative, float k1x, float k1y, float k2x, float k2y, float x, float y) {
      super(isRelative, 6);
      this.k1x = k1x;
      this.k1y = k1y;
      this.k2x = k2x;
      this.k2y = k2y;
      this.x = x;
      this.y = y;
   }

   @Override
   public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
      Float offset = this.offset(hist);
      path.curveTo(this.k1x + offset.x, this.k1y + offset.y, this.k2x + offset.x, this.k2y + offset.y, this.x + offset.x, this.y + offset.y);
      hist.setLastCubic(path.getCurrentPoint(), this.k2x + offset.x, this.k2y + offset.y);
   }

   @Override
   public String toString() {
      return "C " + this.k1x + " " + this.k1y + " " + this.k2x + " " + this.k2y + " " + this.x + " " + this.y;
   }
}

