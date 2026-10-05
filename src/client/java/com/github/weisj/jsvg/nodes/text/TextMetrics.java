package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import org.jetbrains.annotations.NotNull;

public final class TextMetrics {
   private final double letterSpacingLength;
   private final double glyphLength;
   private final double fixedGlyphLength;
   private final int glyphCount;
   private final int controllableLetterSpacingCount;

   public TextMetrics(double letterSpacingLength, double visibleCodepointLength, int glyphCount, double fixedGlyphLength, int controllableLetterSpacingCount) {
      this.letterSpacingLength = letterSpacingLength;
      this.glyphLength = visibleCodepointLength;
      this.glyphCount = glyphCount;
      this.fixedGlyphLength = fixedGlyphLength;
      this.controllableLetterSpacingCount = controllableLetterSpacingCount;
   }

   @NotNull
   static TextMetrics computeTextMetrics(
      @NotNull TextLayoutGroup layoutGroup, @NotNull RenderContext context, @NotNull TextSegment.RenderableSegment.UseTextLengthForCalculation flag
   ) {
      if (flag == TextSegment.RenderableSegment.UseTextLengthForCalculation.YES) {
         Length fixedLength = layoutGroup.fixedLength();
         if (fixedLength != null) {
            return new TextMetrics(0.0, 0.0, 0, fixedLength.resolve(context.measureContext()), 0);
         }
      }

      RenderContextAccessor.Accessor accessor = RenderContextAccessor.instance();
      SVGFont font = accessor.font(context);
      float letterSpacing = accessor.fontRenderContext(context).letterSpacing().resolve(context.measureContext());
      TextMetrics.IntermediateTextMetrics metrics = new TextMetrics.IntermediateTextMetrics();
      int index = 0;

      for (TextSegment segment : layoutGroup.segments()) {
         RenderContext currentContext = context;
         if (segment instanceof Renderable) {
            currentContext = NodeRenderer.setupRenderContext(segment, context);
         }

         if (segment instanceof StringTextSegment) {
            StringTextSegment stringTextSegment = (StringTextSegment)segment;
            accumulateSegmentMetrics(layoutGroup, metrics, stringTextSegment, font, letterSpacing, index);
         } else {
            if (!(segment instanceof TextSegment.RenderableSegment)) {
               throw new IllegalStateException("Unexpected segment " + segment);
            }

            accumulateRenderableSegmentMetrics((TextSegment.RenderableSegment)segment, metrics, currentContext);
         }

         index++;
      }

      return new TextMetrics(
         metrics.letterSpacingLength, metrics.glyphLength, metrics.glyphCount, metrics.fixedGlyphLength, metrics.controllableLetterSpacingCount
      );
   }

   private static void accumulateRenderableSegmentMetrics(
      @NotNull TextSegment.RenderableSegment segment, @NotNull TextMetrics.IntermediateTextMetrics metrics, @NotNull RenderContext currentContext
   ) {
      TextMetrics textMetrics = segment.computeTextMetrics(currentContext, TextSegment.RenderableSegment.UseTextLengthForCalculation.YES);
      metrics.letterSpacingLength = metrics.letterSpacingLength + textMetrics.letterSpacingLength();
      metrics.glyphLength = metrics.glyphLength + textMetrics.glyphLength();
      metrics.glyphCount = metrics.glyphCount + textMetrics.glyphCount();
      metrics.fixedGlyphLength = metrics.fixedGlyphLength + textMetrics.fixedGlyphLength();
      metrics.controllableLetterSpacingCount = metrics.controllableLetterSpacingCount + textMetrics.controllableLetterSpacingCount();
   }

   private static void accumulateSegmentMetrics(
      @NotNull TextLayoutGroup layoutGroup,
      @NotNull TextMetrics.IntermediateTextMetrics metrics,
      @NotNull StringTextSegment segment,
      @NotNull SVGFont font,
      float letterSpacing,
      int index
   ) {
      int glyphCount = segment.codepoints().size();
      boolean lastSegment = index == layoutGroup.segments().size() - 1;
      int whiteSpaceCount = lastSegment ? glyphCount - 1 : glyphCount;
      metrics.glyphCount += glyphCount;
      metrics.letterSpacingLength += whiteSpaceCount * letterSpacing;
      metrics.controllableLetterSpacingCount += whiteSpaceCount;

      for (String codepoint : segment.codepoints()) {
         metrics.glyphLength = metrics.glyphLength + font.codepointGlyph(codepoint).advance();
      }
   }

   public double letterSpacingLength() {
      return this.letterSpacingLength;
   }

   public double glyphLength() {
      return this.glyphLength;
   }

   public double fixedGlyphLength() {
      return this.fixedGlyphLength;
   }

   public double totalAdjustableLength() {
      return this.glyphLength() + this.letterSpacingLength();
   }

   public int glyphCount() {
      return this.glyphCount;
   }

   public int controllableLetterSpacingCount() {
      return this.controllableLetterSpacingCount;
   }

   @Override
   public String toString() {
      return "TextMetrics{whiteSpaceLength="
         + this.letterSpacingLength
         + ", glyphLength="
         + this.glyphLength
         + ", glyphCount="
         + this.glyphCount
         + ", fixedGlyphLength="
         + this.fixedGlyphLength
         + '}';
   }

   private static final class IntermediateTextMetrics {
      double letterSpacingLength = 0.0;
      double glyphLength = 0.0;
      double fixedGlyphLength = 0.0;
      int glyphCount = 0;
      int controllableLetterSpacingCount = 0;

      private IntermediateTextMetrics() {
      }
   }
}

