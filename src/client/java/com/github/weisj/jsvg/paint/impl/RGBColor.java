package com.github.weisj.jsvg.paint.impl;

import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.util.ColorUtil;
import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class RGBColor implements Paint, ColorValue {
   public static final RGBColor INHERITED = new RGBColor(0, 0, 0, 0);
   public static final RGBColor DEFAULT = new RGBColor(0, 0, 0, 255);
   private final int r;
   private final int g;
   private final int b;
   private final int a;
   private Color color;

   public RGBColor(int r, int g, int b, int a) {
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
   }

   public RGBColor(@NotNull Color color) {
      this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
      this.color = color;
   }

   @NotNull
   public static RGBColor interpolate(float t, @NotNull RGBColor a, @NotNull RGBColor b) {
      return new RGBColor(
         Math.round(a.r + (b.r - a.r) * t), Math.round(a.g + (b.g - a.g) * t), Math.round(a.b + (b.b - a.b) * t), Math.round(a.a + (b.a - a.a) * t)
      );
   }

   @NotNull
   public static RGBColor saxpy(float t, @NotNull RGBColor a, @NotNull RGBColor b) {
      return new RGBColor(Math.round(a.r + t * b.r), Math.round(a.g + t * b.g), Math.round(a.b + t * b.b), Math.round(a.a + t * b.a));
   }

   @NotNull
   public static RGBColor add(@NotNull RGBColor a, @NotNull RGBColor b) {
      return new RGBColor(a.r + b.r, a.g + b.g, a.b + b.b, a.a + b.a);
   }

   @NotNull
   public Color toColor() {
      if (this.color == null) {
         this.color = new Color(ColorUtil.clampColor(this.r), ColorUtil.clampColor(this.g), ColorUtil.clampColor(this.b), ColorUtil.clampColor(this.a));
      }

      return this.color;
   }

   public boolean isVisible() {
      return this.a > 0;
   }

   public static boolean isVisible(@NotNull Color c) {
      return c.getAlpha() > 0;
   }

   @Contract(pure = true)
   @NotNull
   @Override
   public String toString() {
      return "ColorValue{r=" + this.r + ", g=" + this.g + ", b=" + this.b + ", a=" + this.a + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RGBColor that = (RGBColor)o;
         return this.r == that.r && this.g == that.g && this.b == that.b && this.a == that.a;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.r, this.g, this.b, this.a);
   }

   @Override
   public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
      return this.toColor().createContext(cm, deviceBounds, userBounds, xform, hints);
   }

   @Override
   public int getTransparency() {
      return this.toColor().getTransparency();
   }

   @NotNull
   @Override
   public Color get(@NotNull MeasureContext context) {
      return this.toColor();
   }
}

