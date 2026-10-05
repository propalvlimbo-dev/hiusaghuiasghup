package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;

public final class FilterLayoutContext {
   @NotNull
   private final ChannelStorage<LayoutBounds> resultChannels = new ChannelStorage<>();
   @NotNull
   private final UnitType primitiveUnits;
   @NotNull
   private final Rectangle2D elementBounds;
   @NotNull
   private final Rectangle2D clipBounds;

   public FilterLayoutContext(@NotNull UnitType primitiveUnits, @NotNull Rectangle2D elementBounds, @NotNull Rectangle2D clipBounds) {
      this.primitiveUnits = primitiveUnits;
      this.elementBounds = elementBounds;
      this.clipBounds = clipBounds;
   }

   @NotNull
   public UnitType primitiveUnits() {
      return this.primitiveUnits;
   }

   @NotNull
   public Rectangle2D elementBounds() {
      return this.elementBounds;
   }

   @NotNull
   public Rectangle2D filterPrimitiveRegion(@NotNull MeasureContext context, @NotNull FilterPrimitive filterPrimitive) {
      return this.primitiveUnits
         .computeViewBounds(context, this.elementBounds, filterPrimitive.x(), filterPrimitive.y(), filterPrimitive.width(), filterPrimitive.height());
   }

   @NotNull
   public ChannelStorage<LayoutBounds> resultChannels() {
      return this.resultChannels;
   }

   @NotNull
   public Rectangle2D clipBounds() {
      return this.clipBounds;
   }
}

