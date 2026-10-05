package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;

public interface HasShape extends SVGNode {
   @NotNull
   default Shape elementShape(@NotNull RenderContext context, HasShape.Box box) {
      Shape shape = this.untransformedElementShape(context, box);
      return this instanceof Transformable
         ? ((Transformable)this).transformShape(shape, context, ElementBounds.fromUntransformedBounds(this, context, shape.getBounds2D(), box))
         : shape;
   }

   @NotNull
   Shape untransformedElementShape(@NotNull RenderContext var1, HasShape.Box var2);

   @NotNull
   default Rectangle2D elementBounds(@NotNull RenderContext context, HasShape.Box box) {
      Rectangle2D shape = this.untransformedElementBounds(context, box);
      if (!GeometryUtil.isValidRect(shape)) {
         return shape;
      } else {
         return this instanceof Transformable
            ? ((Transformable)this).transformShape(shape, context, ElementBounds.fromUntransformedBounds(this, context, shape, box)).getBounds2D()
            : shape;
      }
   }

   @NotNull
   Rectangle2D untransformedElementBounds(@NotNull RenderContext var1, HasShape.Box var2);

   enum Box {
      BoundingBox,
      StrokeBox;

      // $VF: synthetic method
      private static HasShape.Box[] $values() {
         return new HasShape.Box[]{BoundingBox, StrokeBox};
      }
   }
}
