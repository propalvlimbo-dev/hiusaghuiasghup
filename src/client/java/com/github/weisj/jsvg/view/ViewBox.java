package com.github.weisj.jsvg.view;

import com.github.weisj.jsvg.geometry.size.Length;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import org.jetbrains.annotations.NotNull;

public final class ViewBox extends Float {
   public ViewBox(float @NotNull [] viewBox) {
      super(viewBox[0], viewBox[1], viewBox[2], viewBox[3]);
   }

   public ViewBox(float x, float y, float w, float h) {
      super(x, y, w, h);
   }

   public ViewBox(float w, float h) {
      this(0.0F, 0.0F, w, h);
   }

   public ViewBox(@NotNull FloatSize size) {
      this(size.width, size.height);
   }

   public ViewBox(@NotNull Point2D location, @NotNull FloatSize size) {
      this((float)location.getX(), (float)location.getY(), size.width, size.height);
   }

   public ViewBox(@NotNull Rectangle2D bounds) {
      this((float)bounds.getX(), (float)bounds.getY(), (float)bounds.getWidth(), (float)bounds.getHeight());
   }

   public void setSize(@NotNull FloatSize size) {
      this.width = size.width;
      this.height = size.height;
   }

   @NotNull
   public FloatSize size() {
      return new FloatSize(this.width, this.height);
   }

   @NotNull
   public Point2D.Float location() {
      return new Point2D.Float(this.x, this.y);
   }

   public boolean hasSpecifiedX() {
      return Length.isSpecified(this.x);
   }

   public boolean hasSpecifiedY() {
      return Length.isSpecified(this.y);
   }

   public boolean hasSpecifiedWidth() {
      return Length.isSpecified(this.width);
   }

   public boolean hasSpecifiedHeight() {
      return Length.isSpecified(this.height);
   }

   @NotNull
   @Override
   public String toString() {
      return "ViewBox[" + this.x + "," + this.y + "," + this.width + "," + this.height + "]";
   }
}

