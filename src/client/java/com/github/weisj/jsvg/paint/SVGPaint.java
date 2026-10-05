package com.github.weisj.jsvg.paint;

import com.github.weisj.jsvg.animation.value.AnimatedColor;
import com.github.weisj.jsvg.animation.value.AnimatedPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SVGPaint {
   @Nullable
   static SVGPaint derive(@Nullable SVGPaint current, @Nullable SVGPaint other) {
      if (other == null) {
         return current;
      } else if (current == null) {
         return other;
      } else if (other instanceof AnimatedPaint) {
         return ((AnimatedPaint)other).derive(current);
      } else {
         return other instanceof AnimatedColor ? ((AnimatedColor)other).derive(current) : other;
      }
   }

   void fillShape(@NotNull Output var1, @NotNull RenderContext var2, @NotNull Shape var3, @Nullable Rectangle2D var4);

   void drawShape(@NotNull Output var1, @NotNull RenderContext var2, @NotNull Shape var3, @Nullable Rectangle2D var4);

   default boolean isVisible(@NotNull RenderContext context) {
      return this != PredefinedPaints.NONE;
   }
}
