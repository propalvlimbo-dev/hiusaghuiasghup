package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.attributes.Coordinate;
import com.github.weisj.jsvg.attributes.transform.TransformBox;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.ShapeUtil;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Transformable {
   default boolean shouldTransform() {
      return true;
   }

   @Nullable
   TransformValue transform();

   @NotNull
   Coordinate<LengthValue> transformOrigin();

   TransformBox transformBox();

   @Nullable
   default AffineTransform effectiveTransform(@NotNull RenderContext context, @NotNull ElementBounds bounds) {
      TransformValue transformValue = this.transform();
      if (transformValue == null) {
         return null;
      }

      Coordinate<LengthValue> origin = this.transformOrigin();
      double xOffset = 0.0;
      double yOffset = 0.0;
      MeasureContext measureContext = context.measureContext();
      switch (this.transformBox()) {
         case FillBox:
            Rectangle2D fillBox = bounds.fillBox();
            measureContext = measureContext.derive((float)fillBox.getWidth(), (float)fillBox.getHeight());
            xOffset = fillBox.getX();
            yOffset = fillBox.getY();
            break;
         case StrokeBox:
            Rectangle2D strokeBox = bounds.strokeBox();
            measureContext = measureContext.derive((float)strokeBox.getWidth(), (float)strokeBox.getHeight());
            xOffset = strokeBox.getX();
            yOffset = strokeBox.getY();
         case ViewBox:
      }

      double xOrigin = origin.x().resolve(measureContext) + xOffset;
      double yOrigin = origin.y().resolve(measureContext) + yOffset;
      AffineTransform conjugate = AffineTransform.getTranslateInstance(xOrigin, yOrigin);
      conjugate.concatenate(transformValue.get(measureContext));
      conjugate.translate(-xOrigin, -yOrigin);
      return conjugate;
   }

   default void applyTransform(@NotNull Output output, @NotNull RenderContext context, @NotNull ElementBounds bounds) {
      AffineTransform transform = this.effectiveTransform(context, bounds);
      if (transform != null) {
         output.applyTransform(transform);
         context.userSpaceTransform().concatenate(transform);
      }
   }

   default Shape transformShape(@NotNull Shape shape, @NotNull RenderContext renderContext, @NotNull ElementBounds elementBounds) {
      AffineTransform transform = this.effectiveTransform(renderContext, elementBounds);
      return transform == null ? shape : ShapeUtil.transformShape(shape, transform);
   }
}
