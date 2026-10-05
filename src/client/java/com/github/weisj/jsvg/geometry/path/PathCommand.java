package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

public abstract class PathCommand {
   private final boolean isRelative;
   private final int nodeCount;

   protected PathCommand(int nodeCount) {
      this(false, nodeCount);
   }

   protected PathCommand(boolean isRelative, int nodeCount) {
      this.isRelative = isRelative;
      this.nodeCount = nodeCount;
   }

   protected Float offset(@NotNull BuildHistory hist) {
      return this.isRelative() ? new Float(hist.lastPoint.x, hist.lastPoint.y) : new Float(0.0F, 0.0F);
   }

   @NotNull
   protected Float lastKnotReflectionCubic(@NotNull BuildHistory hist) {
      return this.lastKnotReflection(hist.lastPoint, hist.lastCubicKnot);
   }

   @NotNull
   protected Float lastKnotReflectionQuadratic(@NotNull BuildHistory hist) {
      return this.lastKnotReflection(hist.lastPoint, hist.lastQuadraticKnot);
   }

   @NotNull
   private Float lastKnotReflection(@NotNull Float point, @NotNull Float knot) {
      float kx = point.x * 2.0F - knot.x;
      float ky = point.y * 2.0F - knot.y;
      return new Float(kx, ky);
   }

   public boolean isRelative() {
      return this.isRelative;
   }

   public abstract void appendPath(@NotNull Path2D var1, @NotNull BuildHistory var2);

   public int nodeCount() {
      return this.nodeCount;
   }
}

