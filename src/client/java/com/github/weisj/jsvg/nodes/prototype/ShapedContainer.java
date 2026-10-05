package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D.Float;
import org.jetbrains.annotations.NotNull;

public interface ShapedContainer<E> extends Container<E>, HasShape {
   @NotNull
   @Override
   default Shape untransformedElementShape(@NotNull RenderContext context, HasShape.Box box) {
      Path2D shape = new Float();

      for (E child : this.children()) {
         if (child instanceof HasShape) {
            RenderContext childContext = NodeRenderer.setupRenderContext(child, context);
            Shape childShape = ((HasShape)child).elementShape(childContext, box);
            shape.append(childShape, false);
         }
      }

      return shape;
   }

   @NotNull
   @Override
   default Rectangle2D untransformedElementBounds(@NotNull RenderContext context, HasShape.Box box) {
      Rectangle2D bounds = null;

      for (E child : this.children()) {
         if (child instanceof HasShape) {
            RenderContext childContext = NodeRenderer.setupRenderContext(child, context);
            Rectangle2D childBounds = ((HasShape)child).elementBounds(childContext, box);
            if (GeometryUtil.isValidRect(childBounds) && !childBounds.isEmpty()) {
               if (bounds == null) {
                  bounds = childBounds;
               } else {
                  Rectangle2D.union(bounds, childBounds, bounds);
               }
            }
         }
      }

      return bounds == null ? new java.awt.geom.Rectangle2D.Float(java.lang.Float.NEGATIVE_INFINITY, java.lang.Float.NEGATIVE_INFINITY, 0.0F, 0.0F) : bounds;
   }
}
