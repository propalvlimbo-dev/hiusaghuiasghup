package com.github.weisj.jsvg.attributes.filter;

import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.filter.FilterLayoutContext;
import java.awt.geom.Rectangle2D;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

public final class LayoutBounds {
   @NotNull
   private final LayoutBounds.Data data;
   @NotNull
   private final BiFunction<LayoutBounds.Data, LayoutBounds.ComputeFlags, LayoutBounds.Data> transformer;
   private final LayoutBounds.ComputeFlags additionalFlags;

   public LayoutBounds(@NotNull Rectangle2D bounds, @NotNull FloatInsets clipBoundsEscapeInsets) {
      this.data = new LayoutBounds.Data(bounds, clipBoundsEscapeInsets);
      this.transformer = (d, f) -> d;
      this.additionalFlags = new LayoutBounds.ComputeFlags(false);
   }

   private LayoutBounds(
      @NotNull LayoutBounds.Data data,
      @NotNull BiFunction<LayoutBounds.Data, LayoutBounds.ComputeFlags, LayoutBounds.Data> transformer,
      @NotNull LayoutBounds.ComputeFlags flags
   ) {
      this.data = data;
      this.transformer = transformer;
      this.additionalFlags = flags;
   }

   @NotNull
   public LayoutBounds transform(@NotNull BiFunction<LayoutBounds.Data, LayoutBounds.ComputeFlags, LayoutBounds.Data> newTransformer) {
      return new LayoutBounds(this.data, (data, flags) -> {
         LayoutBounds.Data newData = this.transformer.apply(data, flags);
         return newTransformer.apply(newData, flags);
      }, this.additionalFlags);
   }

   @NotNull
   public LayoutBounds withFlags(@NotNull LayoutBounds.ComputeFlags flags) {
      return new LayoutBounds(this.data, this.transformer, this.additionalFlags.or(flags));
   }

   @NotNull
   public LayoutBounds.Data resolve(@NotNull LayoutBounds.ComputeFlags flags) {
      return this.transformer.apply(this.data, flags.or(this.additionalFlags));
   }

   @NotNull
   public LayoutBounds union(@NotNull LayoutBounds other) {
      return this.transform(
         (data, flags) -> {
            LayoutBounds.Data otherData = other.resolve(flags);
            return new LayoutBounds.Data(
               data.bounds.createUnion(otherData.bounds), GeometryUtil.max(data.clipBoundsEscapeInsets, otherData.clipBoundsEscapeInsets)
            );
         }
      );
   }

   @NotNull
   public LayoutBounds grow(float horizontal, float vertical, @NotNull FilterLayoutContext context) {
      return this.transform((data, flags) -> {
         FloatInsets insets = data.clipBoundsEscapeInsets;
         Rectangle2D clipBounds = context.clipBounds();
         FloatInsets growInsets = new FloatInsets(vertical, horizontal, vertical, horizontal);
         Rectangle2D newBounds = GeometryUtil.grow(data.bounds, growInsets);
         FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, newBounds), growInsets);
         return new LayoutBounds.Data(newBounds, GeometryUtil.max(insets, ins));
      });
   }

   @NotNull
   public LayoutBounds translate(float dx, float dy, @NotNull FilterLayoutContext context) {
      return this.transform((data, flags) -> {
         FloatInsets insets = data.clipBoundsEscapeInsets;
         FloatInsets offsetInsets = new FloatInsets(Math.max(dy, 0.0F), Math.max(dx, 0.0F), Math.max(-dy, 0.0F), Math.max(-dx, 0.0F));
         Rectangle2D newBounds = GeometryUtil.grow(data.bounds, offsetInsets);
         Rectangle2D clipBounds = context.clipBounds();
         FloatInsets ins = GeometryUtil.max(GeometryUtil.overhangInsets(clipBounds, data.bounds), offsetInsets);
         return new LayoutBounds.Data(newBounds, GeometryUtil.max(insets, ins));
      });
   }

   @Override
   public String toString() {
      return "LayoutBounds{data=" + this.data + ", transformer=" + this.transformer + '}';
   }

   public static class ComputeFlags {
      @NotNull
      public static final LayoutBounds.ComputeFlags INITIAL = new LayoutBounds.ComputeFlags(false);
      public final boolean operatesOnWholeFilterRegion;

      public ComputeFlags(boolean operatesOnWholeFilterRegion) {
         this.operatesOnWholeFilterRegion = operatesOnWholeFilterRegion;
      }

      @NotNull
      public LayoutBounds.ComputeFlags or(@NotNull LayoutBounds.ComputeFlags other) {
         return new LayoutBounds.ComputeFlags(this.operatesOnWholeFilterRegion || other.operatesOnWholeFilterRegion);
      }
   }

   public static final class Data {
      @NotNull
      private final Rectangle2D bounds;
      @NotNull
      private final FloatInsets clipBoundsEscapeInsets;

      private Data(@NotNull Rectangle2D bounds, @NotNull FloatInsets clipBoundsEscapeInsets) {
         this.bounds = bounds;
         this.clipBoundsEscapeInsets = clipBoundsEscapeInsets;
      }

      @NotNull
      public FloatInsets clipBoundsEscapeInsets() {
         return this.clipBoundsEscapeInsets;
      }

      @NotNull
      public Rectangle2D bounds() {
         return this.bounds;
      }

      @Override
      public String toString() {
         return "Data{bounds=" + GeometryUtil.compactRepresentation(this.bounds) + ", clipBoundsEscapeInsets=" + this.clipBoundsEscapeInsets + '}';
      }
   }
}

