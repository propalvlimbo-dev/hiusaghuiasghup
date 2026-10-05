package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LinearTextLayoutGroup implements TextLayoutGroup {
   private final LinearTextContainer<?> parent;
   @NotNull
   private final List<TextSegment> segments;
   private final TextSegment.RenderableSegment asSegment;

   LinearTextLayoutGroup(@NotNull LinearTextContainer<?> parent) {
      this(parent, new ArrayList<>());
   }

   LinearTextLayoutGroup(@NotNull LinearTextContainer<?> parent, @NotNull List<TextSegment> segments) {
      this.parent = parent;
      this.segments = segments;
      this.asSegment = createSegment(parent, this);
   }

   private static <E> TextSegment.RenderableSegment createSegment(@NotNull LinearTextContainer<E> parent, @NotNull TextLayoutGroup group) {
      return new LayoutGroupSegment<>(parent, group);
   }

   @NotNull
   public TextSegment.RenderableSegment asSegment() {
      return this.asSegment;
   }

   @NotNull
   @Override
   public List<TextSegment> segments() {
      return this.segments;
   }

   @Nullable
   @Override
   public Length fixedLength() {
      return this.parent.textLength.isSpecified() ? this.parent.textLength : null;
   }

   @NotNull
   private GlyphCursor createCursor(@Nullable Point2D start) {
      GlyphCursor cursor = this.parent.createLocalCursor(start == null, new GlyphCursor(0.0F, 0.0F, new AffineTransform()));
      if (start != null) {
         cursor.x = (float)start.getX();
         cursor.y = (float)start.getY();
      }

      return cursor;
   }

   @NotNull
   @Override
   public Point2D renderText(@Nullable Point2D start, @NotNull RenderContext context, @NotNull Output output) {
      GlyphCursor cursor = this.createCursor(start);
      TextOutput textOutput = output.textOutput();
      textOutput.beginText();
      this.asSegment().prepareSegmentForRendering(cursor, context, textOutput);
      double offset = start == null ? this.textAnchorOffset(this.parent.textAnchor(context), cursor.completeGlyphRunMetrics) : 0.0;
      context.translate(output, -offset, 0.0);
      this.asSegment().renderSegmentWithoutLayout(cursor, context, output);
      context.translate(output, offset, 0.0);
      textOutput.endText();
      return cursor.currentLocation(context.measureContext());
   }

   @NotNull
   @Override
   public Point2D appendGlyphShape(@Nullable Point2D start, @NotNull RenderContext context, @NotNull Path2D shape) {
      MutableGlyphRun glyphRun = new MutableGlyphRun();
      GlyphCursor cursor = this.createCursor(start);
      this.asSegment().appendTextShape(cursor, glyphRun, context);
      double offset = this.textAnchorOffset(this.parent.textAnchor(context), glyphRun.metrics());
      if (GeometryUtil.approximatelyEqual(offset, 0.0)) {
         shape.append(glyphRun.shape(), false);
      } else {
         shape.append(glyphRun.shape().createTransformedShape(AffineTransform.getTranslateInstance(-offset, 0.0)), false);
      }

      return cursor.currentLocation(context.measureContext());
   }

   private double textAnchorOffset(@NotNull TextAnchor textAnchor, @NotNull AbstractGlyphRun.Metrics metrics) {
      switch (textAnchor) {
         case Start:
            return 0.0;
         case Middle:
            return metrics.layoutBounds.getWidth() / 2.0;
         case End:
            return metrics.layoutBounds.getWidth();
         default:
            throw new IllegalStateException("Unexpected value: " + textAnchor);
      }
   }
}

