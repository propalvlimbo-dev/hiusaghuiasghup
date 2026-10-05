package com.github.weisj.jsvg.geometry.path;

import com.github.weisj.jsvg.geometry.mesh.Bezier;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class LineToBezier implements BezierPathCommand {
   private final boolean relative;
   private final float x;
   private final float y;

   public LineToBezier(boolean relative, float x, float y) {
      this.relative = relative;
      this.x = x;
      this.y = y;
   }

   @NotNull
   @Override
   public Bezier createBezier(@NotNull Float start) {
      return this.relative ? Bezier.straightLine(start, new Float(start.x + this.x, start.y + this.y)) : Bezier.straightLine(start, new Float(this.x, this.y));
   }
}

