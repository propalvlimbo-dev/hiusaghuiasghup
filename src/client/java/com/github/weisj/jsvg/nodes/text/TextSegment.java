package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import org.jetbrains.annotations.NotNull;

interface TextSegment {
   boolean isSegmentVisible(@NotNull RenderContext var1);

   interface RenderableSegment extends TextSegment {
      void prepareSegmentForRendering(@NotNull GlyphCursor var1, @NotNull RenderContext var2, @NotNull TextOutput var3);

      void renderSegmentWithoutLayout(@NotNull GlyphCursor var1, @NotNull RenderContext var2, @NotNull Output var3);

      @NotNull
      TextMetrics computeTextMetrics(@NotNull RenderContext var1, @NotNull TextSegment.RenderableSegment.UseTextLengthForCalculation var2);

      void appendTextShape(@NotNull GlyphCursor var1, @NotNull MutableGlyphRun var2, @NotNull RenderContext var3);

      enum UseTextLengthForCalculation {
         YES,
         NO;

         // $VF: synthetic method
         private static TextSegment.RenderableSegment.UseTextLengthForCalculation[] $values() {
            return new TextSegment.RenderableSegment.UseTextLengthForCalculation[]{YES, NO};
         }
      }
   }
}
