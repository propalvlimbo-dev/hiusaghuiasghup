package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import org.jetbrains.annotations.NotNull;

final class Horizontal extends PathCommand {
   private final float x;

   public Horizontal(boolean isRelative, float x) {
      super(isRelative, 2);
      this.x = x;
   }

   @Override
   public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
      float xOff = this.isRelative() ? hist.lastPoint.x : 0.0F;
      float yOff = hist.lastPoint.y;
      path.lineTo(this.x + xOff, yOff);
      hist.setLast(path.getCurrentPoint());
   }

   @Override
   public String toString() {
      return "H " + this.x;
   }
}

