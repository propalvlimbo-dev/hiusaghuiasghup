package com.github.weisj.jsvg.renderer.impl;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.HasFilter;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;

public class ElementBounds {
   @NotNull
   private final SVGNode node;
   private final RenderContext context;
   private Rectangle2D boundingBox;
   private Rectangle2D strokeBox;
   private Rectangle2D geometryBox;

   public ElementBounds(@NotNull SVGNode node, RenderContext context) {
      this.node = node;
      this.context = context;
   }

   @NotNull
   public static ElementBounds fromUntransformedBounds(@NotNull SVGNode node, @NotNull RenderContext context, @NotNull Rectangle2D bounds, HasShape.Box box) {
      ElementBounds elementBounds = new ElementBounds(node, context);
      switch (box) {
         case BoundingBox:
            elementBounds.boundingBox = bounds;
            break;
         case StrokeBox:
            elementBounds.strokeBox = bounds;
            break;
         default:
            throw new IllegalStateException("Unexpected value: " + box);
      }

      return elementBounds;
   }

   @NotNull
   public Rectangle2D boundingBox() {
      if (this.boundingBox == null) {
         this.boundingBox = elementBounds(this.node, this.context, HasShape.Box.BoundingBox);
      }

      return this.boundingBox;
   }

   @NotNull
   public Rectangle2D geometryBox() {
      if (this.geometryBox == null) {
         this.geometryBox = this.strokeBox();
         if (this.node instanceof HasFilter) {
            this.geometryBox = this.filterBounds((HasFilter)this.node, this.context, this.geometryBox);
         }
      }

      return this.geometryBox;
   }

   @NotNull
   public Rectangle2D strokeBox() {
      if (this.strokeBox == null) {
         this.strokeBox = elementBounds(this.node, this.context, HasShape.Box.StrokeBox);
      }

      return this.strokeBox;
   }

   @NotNull
   public Rectangle2D fillBox() {
      return this.boundingBox();
   }

   @NotNull
   private static Rectangle2D elementBounds(@NotNull SVGNode node, @NotNull RenderContext context, HasShape.Box box) {
      Rectangle2D elementBounds;
      if (node instanceof HasShape) {
         elementBounds = ((HasShape)node).untransformedElementBounds(context, box);
      } else {
         MeasureContext measureContext = context.measureContext();
         elementBounds = new ViewBox(measureContext.viewWidth(), measureContext.viewHeight());
      }

      return elementBounds;
   }

   @NotNull
   private Rectangle2D filterBounds(@NotNull HasFilter node, @NotNull RenderContext context, @NotNull Rectangle2D elementBounds) {
      Filter filter = node.filter();
      if (filter == null) {
         return elementBounds;
      }

      Filter.FilterBounds filterBounds = filter.createFilterBounds(null, context, this);
      return filterBounds == null ? elementBounds : elementBounds.createUnion(filterBounds.effectiveFilterArea());
   }
}

