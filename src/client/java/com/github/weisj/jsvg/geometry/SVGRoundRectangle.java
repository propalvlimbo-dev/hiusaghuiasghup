package com.github.weisj.jsvg.geometry;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D.Float;
import org.jetbrains.annotations.NotNull;

public final class SVGRoundRectangle implements SVGShape {
   @NotNull
   private final Float rect = new Float();
   @NotNull
   private final LengthValue x;
   @NotNull
   private final LengthValue y;
   @NotNull
   private final LengthValue w;
   @NotNull
   private final LengthValue h;
   @NotNull
   private final LengthValue rx;
   @NotNull
   private final LengthValue ry;

   public SVGRoundRectangle(
      @NotNull LengthValue x, @NotNull LengthValue y, @NotNull LengthValue w, @NotNull LengthValue h, @NotNull LengthValue rx, @NotNull LengthValue ry
   ) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.rx = rx;
      this.ry = ry;
   }

   private void validateShape(@NotNull MeasureContext measureContext) {
      this.rect
         .setRoundRect(
            this.x.resolve(measureContext),
            this.y.resolve(measureContext),
            this.w.resolve(measureContext),
            this.h.resolve(measureContext),
            Math.max(0.0F, this.rx.resolve(measureContext) * 2.0F),
            Math.max(0.0F, this.ry.resolve(measureContext) * 2.0F)
         );
   }

   @NotNull
   @Override
   public Shape shape(@NotNull RenderContext context, boolean validate) {
      if (validate) {
         this.validateShape(context.measureContext());
      }

      return this.rect;
   }

   @NotNull
   @Override
   public Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
      if (validate) {
         this.validateShape(context.measureContext());
      }

      return this.rect.getBounds2D();
   }

   @Override
   public double pathLength(@NotNull RenderContext context) {
      MeasureContext measureContext = context.measureContext();
      float a = this.rx.resolve(measureContext);
      float b = this.ry.resolve(measureContext);
      double l = 2.0F * (this.w.resolve(measureContext) - 2.0F * a + (this.h.resolve(measureContext) - 2.0F * b));
      return a == b ? l + SVGCircle.circumference(a) : l + SVGEllipse.ellipseCircumference(a, b);
   }

   @Override
   public boolean isClosed(@NotNull RenderContext context) {
      return true;
   }
}

