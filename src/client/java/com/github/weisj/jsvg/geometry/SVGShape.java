package com.github.weisj.jsvg.geometry;

import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;

public interface SVGShape {
   default boolean canBeFilled() {
      return true;
   }

   @NotNull
   default Shape shape(@NotNull RenderContext context) {
      return this.shape(context, true);
   }

   @NotNull
   Shape shape(@NotNull RenderContext var1, boolean var2);

   @NotNull
   Rectangle2D bounds(@NotNull RenderContext var1, boolean var2);

   double pathLength(@NotNull RenderContext var1);

   default boolean usesOptimizedBoundsCalculation() {
      return true;
   }

   boolean isClosed(@NotNull RenderContext var1);
}
