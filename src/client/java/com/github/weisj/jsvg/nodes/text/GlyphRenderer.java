package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ShapeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.FontRenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import com.github.weisj.jsvg.util.ShapeUtil;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D.Float;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

final class GlyphRenderer {
   private static final boolean DEBUG = false;

   private GlyphRenderer() {
   }

   static void prepareGlyphRun(
      @NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font, @NotNull RenderContext context, @NotNull TextOutput textOutput
   ) {
      GlyphRun glyphRun = layoutGlyphRun(segment, cursor, font, context, textOutput);
      cursor.completeGlyphRunMetrics.union(glyphRun.metrics());
      segment.currentGlyphRun = glyphRun;
      segment.currentRenderContext = context;
   }

   static void renderGlyphRun(
      @NotNull Output output, @NotNull PaintOrder paintOrder, @NotNull Set<VectorEffect> vectorEffects, @NotNull StringTextSegment segment
   ) {
      RenderContext context = segment.currentRenderContext;
      assert context != null;
      GlyphRun glyphRun = segment.currentGlyphRun;
      assert glyphRun != null;
      AbstractGlyphRun.Metrics metrics = glyphRun.metrics();
      Stroke stroke = context.stroke(1.0F);
      ShapeRenderer.renderWithPaintOrder(
         output,
         true,
         paintOrder,
         new ShapeRenderer.ShapePaintContext(context, vectorEffects, stroke, null),
         new ShapeRenderer.PaintShape(glyphRun.shape(), metrics.paintBounds),
         null
      );
      SVGFont font = RenderContextAccessor.instance().font(context);
      Output.SafeState safeState = output.safeState();

      for (AbstractGlyphRun.PaintableEmoji emoji : glyphRun.emojis()) {
         emoji.render(output, font);
         safeState.restore();
      }

      segment.currentRenderContext = null;
      segment.currentGlyphRun = null;
   }

   @NotNull
   static GlyphRun layoutGlyphRun(
      @NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font, @NotNull RenderContext context, @NotNull TextOutput textOutput
   ) {
      boolean segmentVisible = segment.isSegmentVisible(context);
      MeasureContext measure = context.measureContext();
      FontRenderContext fontRenderContext = RenderContextAccessor.instance().fontRenderContext(context);
      float letterSpacing = fontRenderContext.letterSpacing().resolve(measure);
      Path2D glyphPath = new Float();
      java.awt.geom.Point2D.Float layoutStart = cursor.currentLocation(measure);
      List<AbstractGlyphRun.PaintableEmoji> emojis = null;
      boolean isLastSegment = segment.isLastSegmentInParent();
      boolean shouldSkipLastSpacing = isLastSegment && cursor.advancement().shouldSkipLastSpacing();
      textOutput.glyphRunBreak();
      List<String> codepoints = segment.codepoints();
      int i = 0;

      for (int count = codepoints.size(); i < count; i++) {
         String codepoint = codepoints.get(i);
         boolean lastCodepoint = i == count - 1;
         Glyph glyph = font.codepointGlyph(codepoint);
         if (i > 0 && !cursor.isCurrentGlyphAutoLayout()) {
            textOutput.glyphRunBreak();
         }

         AffineTransform glyphTransform = cursor.advance(measure, glyph);
         boolean skipSpacing = lastCodepoint && shouldSkipLastSpacing;
         if (!skipSpacing) {
            cursor.advanceSpacing(letterSpacing);
         }

         if (segmentVisible && cursor.shouldRenderCurrentGlyph()) {
            if (glyphTransform == null) {
               break;
            }

            if (glyph.isRendered()) {
               float baselineOffset = computeBaselineOffset(font, fontRenderContext);
               glyphTransform.translate(0.0, -baselineOffset);
               if (glyph instanceof EmojiGlyph) {
                  if (emojis == null) {
                     emojis = new ArrayList<>();
                  }

                  emojis.add(new AbstractGlyphRun.PaintableEmoji((EmojiGlyph)glyph, new AffineTransform(glyphTransform)));
               } else {
                  Shape glyphOutline = glyph.glyphOutline();
                  Shape renderPath = ShapeUtil.transformShape(glyphOutline, glyphTransform);
                  glyphPath.append(renderPath, false);
               }
            }

            textOutput.codepoint(codepoint, glyphTransform, context);
         }
      }

      Rectangle2D paintBounds = glyphPath.getBounds2D();
      java.awt.geom.Point2D.Float layoutEnd = cursor.currentLocation(measure);
      Rectangle2D layoutBounds = new Double(layoutStart.x, paintBounds.getY(), layoutEnd.x - layoutStart.x, paintBounds.getHeight());
      return new GlyphRun(glyphPath, new AbstractGlyphRun.Metrics(paintBounds, layoutBounds), emojis != null ? emojis : Collections.emptyList());
   }

   private static float computeBaselineOffset(@NotNull SVGFont font, @NotNull FontRenderContext fontRenderContext) {
      switch (fontRenderContext.dominantBaseline()) {
         case Auto:
         case Alphabetic:
         default:
            return font.romanBaseline();
         case Hanging:
            return font.hangingBaseline();
         case Central:
            return font.centerBaseline();
         case Middle:
            return font.middleBaseline();
         case Mathematical:
            return font.mathematicalBaseline();
         case Ideographic:
         case TextAfterEdge:
         case TextBottom:
            return font.textUnderBaseline();
         case TextBeforeEdge:
         case TextTop:
            return font.textOverBaseline();
      }
   }
}

