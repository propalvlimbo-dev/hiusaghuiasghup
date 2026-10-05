package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import java.util.List;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;

class LayoutGroupSegment<E, T extends TextContainer<E> & CursorContext> implements TextSegment.RenderableSegment {
   @NotNull
   private final T parent;
   @NotNull
   private final TextLayoutGroup group;

   LayoutGroupSegment(@NotNull T parent, @NotNull TextLayoutGroup group) {
      this.parent = parent;
      this.group = group;
   }

   @Override
   public boolean isSegmentVisible(@NotNull RenderContext currentContext) {
      return this.parent.isVisible(currentContext);
   }

   @Override
   public void prepareSegmentForRendering(@NotNull GlyphCursor cursor, @NotNull RenderContext context, @NotNull TextOutput textOutput) {
      SVGFont font = RenderContextAccessor.instance().font(context);
      GlyphCursor localCursor = this.parent.createLocalCursor(false, cursor);
      localCursor.setAdvancement(this.localGlyphAdvancement(context, cursor));
      forEachSegment(
         this.group.segments(),
         context,
         (seg, ctx) -> GlyphRenderer.prepareGlyphRun(seg, localCursor, font, ctx, textOutput),
         (seg, ctx) -> seg.prepareSegmentForRendering(localCursor, ctx, textOutput)
      );
      this.parent.cleanUpLocalCursor(cursor, localCursor);
   }

   @NotNull
   private GlyphAdvancement localGlyphAdvancement(@NotNull RenderContext context, @NotNull GlyphCursor cursor) {
      Length length = this.group.fixedLength();
      return length != null
         ? new GlyphAdvancement(
            this.computeTextMetrics(context, TextSegment.RenderableSegment.UseTextLengthForCalculation.NO),
            length.resolve(context.measureContext()),
            this.parent.lengthAdjust
         )
         : cursor.advancement();
   }

   @Override
   public void renderSegmentWithoutLayout(@NotNull GlyphCursor cursor, @NotNull RenderContext context, @NotNull Output output) {
      forEachSegment(this.group.segments(), context, (seg, ctx) -> {
         if (!(this.parent instanceof Renderable) || ((Renderable)this.parent).isVisible(ctx)) {
            GlyphRenderer.renderGlyphRun(output, RenderContextAccessor.instance().paintOrder(context), this.parent.vectorEffects(), seg);
         }
      }, (seg, ctx) -> seg.renderSegmentWithoutLayout(cursor, ctx, output));
   }

   @NotNull
   @Override
   public TextMetrics computeTextMetrics(@NotNull RenderContext context, @NotNull TextSegment.RenderableSegment.UseTextLengthForCalculation flag) {
      return TextMetrics.computeTextMetrics(this.group, context, flag);
   }

   @Override
   public void appendTextShape(@NotNull GlyphCursor cursor, @NotNull MutableGlyphRun glyphRun, @NotNull RenderContext context) {
      SVGFont font = RenderContextAccessor.instance().font(context);
      GlyphCursor localCursor = this.parent.createLocalCursor(false, cursor);
      localCursor.setAdvancement(this.localGlyphAdvancement(context, cursor));
      forEachSegment(
         this.group.segments(),
         context,
         (seg, ctx) -> glyphRun.append(GlyphRenderer.layoutGlyphRun(seg, localCursor, font, ctx, NullTextOutput.INSTANCE)),
         (seg, ctx) -> seg.appendTextShape(localCursor, glyphRun, ctx)
      );
      this.parent.cleanUpLocalCursor(cursor, localCursor);
   }

   static void forEachSegment(
      @NotNull List<? extends TextSegment> segments,
      @NotNull RenderContext context,
      @NotNull BiConsumer<StringTextSegment, RenderContext> onStringTextSegment,
      @NotNull BiConsumer<TextSegment.RenderableSegment, RenderContext> onRenderableSegment
   ) {
      for (TextSegment segment : segments) {
         RenderContext currentContext = context;
         if (segment instanceof TextContainer) {
            currentContext = NodeRenderer.setupRenderContext(segment, context);
         }

         if (segment instanceof StringTextSegment) {
            onStringTextSegment.accept((StringTextSegment)segment, currentContext);
         } else {
            if (!(segment instanceof TextSegment.RenderableSegment)) {
               throw new IllegalStateException("Unexpected segment " + segment);
            }

            onRenderableSegment.accept((TextSegment.RenderableSegment)segment, currentContext);
         }
      }
   }
}

