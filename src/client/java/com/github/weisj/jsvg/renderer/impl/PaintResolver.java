package com.github.weisj.jsvg.renderer.impl;

import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.renderer.impl.context.ContextElementAttributes;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PaintResolver {
   private PaintResolver() {
   }

   @NotNull
   public static SVGPaint resolvePaint(@Nullable SVGPaint p, @NotNull PaintContext paintContext, @Nullable ContextElementAttributes contextElementAttributes) {
      if (p == PredefinedPaints.DEFAULT_PAINT) {
         return PredefinedPaints.DEFAULT_PAINT;
      } else if (p == PredefinedPaints.CURRENT_COLOR) {
         return coerceNonNull(paintContext.color);
      } else if (p == PredefinedPaints.CONTEXT_STROKE) {
         return contextElementAttributes == null ? PredefinedPaints.NONE : contextElementAttributes.strokePaint;
      } else if (p == PredefinedPaints.CONTEXT_FILL) {
         return contextElementAttributes == null ? PredefinedPaints.NONE : contextElementAttributes.fillPaint;
      } else {
         return coerceNonNull(p);
      }
   }

   @NotNull
   private static SVGPaint coerceNonNull(@Nullable SVGPaint p) {
      return p != null ? p : PredefinedPaints.DEFAULT_PAINT;
   }
}

