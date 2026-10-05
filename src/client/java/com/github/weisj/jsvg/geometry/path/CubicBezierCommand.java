package com.github.weisj.jsvg.geometry.path;

import com.github.weisj.jsvg.geometry.mesh.Bezier;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class CubicBezierCommand implements BezierPathCommand {
   private final boolean relative;
   private final float bx;
   private final float by;
   private final float cx;
   private final float cy;
   private final float dx;
   private final float dy;

   public CubicBezierCommand(boolean relative, float bx, float by, float cx, float cy, float dx, float dy) {
      this.relative = relative;
      this.bx = bx;
      this.by = by;
      this.cx = cx;
      this.cy = cy;
      this.dx = dx;
      this.dy = dy;
   }

   @NotNull
   @Override
   public Bezier createBezier(@NotNull Float start) {
      return this.relative
         ? new Bezier(
            start,
            new Float(start.x + this.bx, start.y + this.by),
            new Float(start.x + this.cx, start.y + this.cy),
            new Float(start.x + this.dx, start.y + this.dy)
         )
         : new Bezier(start, new Float(this.bx, this.by), new Float(this.cx, this.cy), new Float(this.dx, this.dy));
   }

   @Override
   public String toString() {
      return "CubicBezierCommand{relative="
         + this.relative
         + ", bx="
         + this.bx
         + ", by="
         + this.by
         + ", cx="
         + this.cx
         + ", cy="
         + this.cy
         + ", dx="
         + this.dx
         + ", dy="
         + this.dy
         + '}';
   }
}

