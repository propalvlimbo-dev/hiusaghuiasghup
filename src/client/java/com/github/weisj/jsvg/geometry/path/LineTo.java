package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class LineTo extends PathCommand {
   private final float x;
   private final float y;

   public LineTo(boolean isRelative, float x, float y) {
      super(isRelative, 2);
      this.x = x;
      this.y = y;
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   @Override
   public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
      Float offset = this.offset(hist);
      path.lineTo(this.x + offset.x, this.y + offset.y);
      hist.setLast(path.getCurrentPoint());
   }

   @Override
   public int nodeCount() {
      return 2;
   }

   @Override
   public String toString() {
      return "L " + this.x + " " + this.y;
   }
}

