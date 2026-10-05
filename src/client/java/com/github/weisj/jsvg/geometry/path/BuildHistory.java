package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

public final class BuildHistory {
   @NotNull
   final Float startPoint = new Float();
   @NotNull
   final Float lastPoint = new Float();
   @NotNull
   final Float lastCubicKnot = new Float();
   @NotNull
   final Float lastQuadraticKnot = new Float();

   public void setStartPoint(@NotNull Point2D point) {
      this.startPoint.setLocation(point);
   }

   public void setLast(@NotNull Point2D point) {
      this.lastPoint.setLocation(point);
      this.lastQuadraticKnot.setLocation(point);
      this.lastCubicKnot.setLocation(point);
   }

   public void setLastQuadratic(@NotNull Point2D point, float knotX, float knotY) {
      this.setLastQuadratic(point, new Float(knotX, knotY));
   }

   public void setLastQuadratic(@NotNull Point2D point, @NotNull Point2D knot) {
      this.lastPoint.setLocation(point);
      this.lastQuadraticKnot.setLocation(knot);
      this.lastCubicKnot.setLocation(point);
   }

   public void setLastCubic(@NotNull Point2D current, float x, float y) {
      this.lastPoint.setLocation(current);
      this.lastQuadraticKnot.setLocation(current);
      this.lastCubicKnot.setLocation(x, y);
   }
}

