package com.github.weisj.jsvg.geometry;

import com.github.weisj.jsvg.attributes.value.ConstantValue;
import com.github.weisj.jsvg.attributes.value.Value;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;

public class AWTSVGShape<T extends Shape> implements SVGShape {
   public static final Rectangle2D EMPTY_SHAPE = new Rectangle();
   @NotNull
   protected final Value<T> shapeValue;
   private Rectangle2D boundsCache;
   private T shapeCache;
   private double pathLength;

   public AWTSVGShape(@NotNull T shape) {
      this(new ConstantValue<>(shape));
   }

   public AWTSVGShape(@NotNull Value<T> shapeValue) {
      this(shapeValue, Double.NaN);
   }

   private AWTSVGShape(@NotNull Value<T> shapeValue, double pathLength) {
      this.shapeValue = shapeValue;
      this.pathLength = pathLength;
   }

   @NotNull
   @Override
   public T shape(@NotNull RenderContext context, boolean validate) {
      if (this.shapeCache == null || validate) {
         this.shapeCache = this.shapeValue.get(context.measureContext());
      }

      return this.shapeCache;
   }

   @NotNull
   @Override
   public Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
      if (this.boundsCache == null || validate) {
         Shape shape = this.shape(context, validate);
         this.boundsCache = shape.getBounds2D();
      }

      return this.boundsCache;
   }

   @Override
   public double pathLength(@NotNull RenderContext context) {
      if (Double.isNaN(this.pathLength)) {
         this.pathLength = this.computePathLength(context);
      }

      return this.pathLength;
   }

   private double computePathLength(@NotNull RenderContext context) {
      Shape shape = this.shape(context, false);
      if (shape instanceof Rectangle2D) {
         Rectangle2D r = (Rectangle2D)shape;
         return 2.0 * (r.getWidth() + r.getHeight());
      } else if (shape instanceof Ellipse2D) {
         Ellipse2D e = (Ellipse2D)shape;
         double w = e.getWidth();
         double h = e.getHeight();
         return w == h ? Math.PI * w : SVGEllipse.ellipseCircumference(w / 2.0, h / 2.0);
      } else {
         return GeometryUtil.pathLength(shape);
      }
   }

   @Override
   public boolean isClosed(@NotNull RenderContext context) {
      Shape shape = this.shape(context, false);
      if (shape instanceof Rectangle2D) {
         return true;
      } else {
         return shape instanceof Ellipse2D ? true : GeometryUtil.isSingleClosedPath(shape);
      }
   }
}

