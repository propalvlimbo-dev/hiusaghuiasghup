package com.github.weisj.jsvg.geometry;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D.Float;
import org.jetbrains.annotations.NotNull;

public final class SVGLine implements SVGShape {
   @NotNull
   private final Float line = new Float();
   private final LengthValue x1;
   private final LengthValue y1;
   private final LengthValue x2;
   private final LengthValue y2;

   public SVGLine(@NotNull LengthValue x1, @NotNull LengthValue y1, @NotNull LengthValue x2, @NotNull LengthValue y2) {
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
   }

   @Override
   public boolean canBeFilled() {
      return false;
   }

   private void validateShape(@NotNull MeasureContext measureContext) {
      this.line.setLine(this.x1.resolve(measureContext), this.y1.resolve(measureContext), this.x2.resolve(measureContext), this.y2.resolve(measureContext));
   }

   @NotNull
   @Override
   public Shape shape(@NotNull RenderContext context, boolean validate) {
      if (validate) {
         this.validateShape(context.measureContext());
      }

      return this.line;
   }

   @NotNull
   @Override
   public Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
      if (validate) {
         this.validateShape(context.measureContext());
      }

      return this.line.getBounds2D();
   }

   @Override
   public double pathLength(@NotNull RenderContext context) {
      MeasureContext measureContext = context.measureContext();
      return GeometryUtil.lineLength(
         this.x1.resolve(measureContext), this.y1.resolve(measureContext), this.x2.resolve(measureContext), this.y2.resolve(measureContext)
      );
   }

   @Override
   public boolean isClosed(@NotNull RenderContext context) {
      return false;
   }
}

