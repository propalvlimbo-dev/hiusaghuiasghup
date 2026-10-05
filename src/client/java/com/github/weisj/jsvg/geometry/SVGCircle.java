package com.github.weisj.jsvg.geometry;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D.Float;
import org.jetbrains.annotations.NotNull;

public final class SVGCircle implements SVGShape {
   @NotNull
   private final Float circle = new Float();
   @NotNull
   private final LengthValue cx;
   @NotNull
   private final LengthValue cy;
   @NotNull
   private final LengthValue r;

   public SVGCircle(@NotNull LengthValue cx, @NotNull LengthValue cy, @NotNull LengthValue r) {
      this.cx = cx;
      this.cy = cy;
      this.r = r;
   }

   private void validateShape(@NotNull MeasureContext measureContext) {
      float x = this.cx.resolve(measureContext);
      float y = this.cy.resolve(measureContext);
      float rr = this.r.resolve(measureContext);
      this.circle.setFrame(x - rr, y - rr, 2.0F * rr, 2.0F * rr);
   }

   @NotNull
   @Override
   public Shape shape(@NotNull RenderContext context, boolean validate) {
      if (validate) {
         this.validateShape(context.measureContext());
      }

      return this.circle;
   }

   @NotNull
   @Override
   public Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
      if (validate) {
         this.validateShape(context.measureContext());
      }

      return this.circle.getBounds2D();
   }

   @Override
   public double pathLength(@NotNull RenderContext context) {
      return circumference(this.r.resolve(context.measureContext()));
   }

   @Override
   public boolean isClosed(@NotNull RenderContext context) {
      return true;
   }

   static double circumference(double radius) {
      return (Math.PI * 2) * radius;
   }
}

