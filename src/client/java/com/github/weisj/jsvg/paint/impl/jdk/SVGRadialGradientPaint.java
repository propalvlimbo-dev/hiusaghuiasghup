package com.github.weisj.jsvg.paint.impl.jdk;

import java.awt.Color;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.ColorModel;
import org.jetbrains.annotations.NotNull;

public final class SVGRadialGradientPaint extends SVGMultipleGradientPaint {
   @NotNull
   private final Point2D focus;
   @NotNull
   private final Point2D center;
   private final float radius;
   private final float focusRadius;

   public SVGRadialGradientPaint(
      @NotNull Point2D center,
      float radius,
      @NotNull Point2D focus,
      float focusRadius,
      float[] fractions,
      Color[] colors,
      CycleMethod cycleMethod,
      ColorSpaceType colorSpace,
      @NotNull AffineTransform gradientTransform
   ) {
      super(fractions, colors, cycleMethod, colorSpace, gradientTransform);
      if (radius <= 0.0F) {
         throw new IllegalArgumentException("Radius must be greater than zero");
      }

      if (focusRadius < 0.0F) {
         throw new IllegalArgumentException("Radius must be greater than zero");
      }

      this.center = new Double(center.getX(), center.getY());
      this.focus = new Double(focus.getX(), focus.getY());
      this.radius = radius;
      this.focusRadius = focusRadius;
   }

   @Override
   public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform transform, RenderingHints hints) {
      transform = new AffineTransform(transform);
      transform.concatenate(this.gradientTransform);
      return new SVGRadialGradientPaintContext(
         this,
         transform,
         (float)this.center.getX(),
         (float)this.center.getY(),
         this.radius,
         (float)this.focus.getX(),
         (float)this.focus.getY(),
         this.focusRadius,
         this.fractions,
         this.colors,
         this.cycleMethod,
         this.colorSpace
      );
   }
}

