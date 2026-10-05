package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.text.GlyphRenderMethod;
import com.github.weisj.jsvg.attributes.text.Side;
import com.github.weisj.jsvg.attributes.text.Spacing;
import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.geometry.util.ReversePathIterator;
import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.ShapeNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.HasGeometryContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.Transformable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasGeometryContextImpl;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import com.github.weisj.jsvg.util.AttributeUtil;
import com.github.weisj.jsvg.util.PathUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D.Float;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories({Category.Graphic, Category.TextContent, Category.TextContentChild})
@PermittedContent(categories = Category.Descriptive, anyOf = {Anchor.class, TextSpan.class, Animate.class, AnimateTransform.class, Set.class}, charData = true)
public final class TextPath extends TextContainer<TextSegment> implements HasGeometryContext.ByDelegate, TextLayoutGroup, CursorContext {
   public static final String TAG = "textpath";
   private static final boolean DEBUG = false;
   private final LayoutGroupSegment<TextSegment, TextPath> asSegment = new LayoutGroupSegment<>(this, this);
   private HasGeometryContext geometryContext;
   private SVGShape pathShape;
   @Nullable
   private Transformable pathShapeTransform;
   private Spacing spacing;
   private GlyphRenderMethod renderMethod;
   private Side side;
   private Length startOffset;

   @NotNull
   @Override
   public String tagName() {
      return "textpath";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.geometryContext = HasGeometryContextImpl.parse(attributeNode);
      this.renderMethod = attributeNode.getEnum("method", GlyphRenderMethod.Align);
      this.side = attributeNode.getEnum("side", Side.Left);
      this.spacing = attributeNode.getEnum("spacing", Spacing.Auto);
      this.startOffset = attributeNode.getLength("startOffset", PercentageDimension.CUSTOM, 0.0F);
      String pathData = attributeNode.getValue("path");
      if (pathData != null) {
         this.pathShape = PathUtil.parseFromPathData(pathData, FillRule.EvenOdd);
         this.pathShapeTransform = null;
      } else {
         String href = attributeNode.getHref();
         ShapeNode shape = attributeNode.getElementByHref(ShapeNode.class, Category.Shape, href, AttributeNode.ElementRelation.GEOMETRY_DATA);
         if (shape != null) {
            this.pathShape = shape.shape();
            this.pathShapeTransform = shape;
         }
      }
   }

   @Override
   protected void doAdd(@NotNull SVGNode node) {
      this.children.add((TextSegment)node);
   }

   @Override
   public void addContent(@NotNull TextContent.Segment content) {
      if (!content.isConstant() || !content.text().isEmpty()) {
         if (!this.children.isEmpty() || !content.isConstant() || !AttributeUtil.isBlank(content.text())) {
            this.children.add(new StringTextSegment(this, this, this.children.size(), content));
         }
      }
   }

   @Override
   protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
      return node instanceof TextSegment;
   }

   @NotNull
   @Override
   public HasGeometryContext geometryContextDelegate() {
      return this.geometryContext;
   }

   @Override
   public boolean isVisible(@NotNull RenderContext context) {
      return this.pathShape != null && super.isVisible(context);
   }

   @NotNull
   @Override
   public List<? extends TextSegment> segments() {
      return this.children();
   }

   @NotNull
   @Override
   public Point2D renderText(@Nullable Point2D start, @NotNull RenderContext context, @NotNull Output output) {
      PathGlyphCursor cursor = this.createCursorWithAnchorAdjustment(context);
      TextOutput textOutput = output.textOutput();
      textOutput.beginText();
      this.asSegment.prepareSegmentForRendering(cursor, context, textOutput);
      this.asSegment.renderSegmentWithoutLayout(cursor, context, output);
      textOutput.endText();
      return GeometryUtil.lastPointOnPath(cursor.pathIterator());
   }

   @NotNull
   @Override
   public Point2D appendGlyphShape(@Nullable Point2D start, @NotNull RenderContext context, @NotNull Path2D shape) {
      PathGlyphCursor cursor = this.createCursorWithAnchorAdjustment(context);
      shape.append(this.glyphShape(cursor, context), false);
      return GeometryUtil.lastPointOnPath(cursor.pathIterator());
   }

   @NotNull
   @Override
   Shape glyphShape(@NotNull RenderContext context) {
      return this.glyphShape(this.createCursorWithAnchorAdjustment(context), context);
   }

   @NotNull
   private Shape glyphShape(PathGlyphCursor cursor, @NotNull RenderContext context) {
      MutableGlyphRun glyphRun = new MutableGlyphRun();
      this.asSegment.appendTextShape(cursor, glyphRun, context);
      return glyphRun.shape();
   }

   @Nullable
   @Override
   public Length fixedLength() {
      return this.textLength.isSpecified() ? this.textLength : null;
   }

   @NotNull
   private PathGlyphCursor createCursorWithAnchorAdjustment(@NotNull RenderContext context) {
      return new PathGlyphCursor(this.createPathIterator(context), this.computeAnchorAdjustedStartOffset(context));
   }

   private float computeAnchorAdjustedStartOffset(@NotNull RenderContext context) {
      float offset = this.computeStartOffset(context);
      TextAnchor textAnchor = this.textAnchor(context);
      switch (textAnchor) {
         case Start:
            return offset;
         case Middle:
            return offset - this.computeTotalTextLength(context) / 2.0F;
         case End:
            return offset - this.computeTotalTextLength(context);
         default:
            throw new IllegalStateException("Unexpected value: " + textAnchor);
      }
   }

   private float computeTotalTextLength(@NotNull RenderContext context) {
      return (float)this.asSegment.computeTextMetrics(context, TextSegment.RenderableSegment.UseTextLengthForCalculation.YES).totalAdjustableLength();
   }

   private float computeStartOffset(@NotNull RenderContext context) {
      float offset = this.startOffset.resolve(context.measureContext());
      if (this.startOffset.unit().isPercentage()) {
         if (this.pathShape.isClosed(context)) {
            offset = (offset % 1.0F + 1.0F) % 1.0F;
         }

         return (float)(offset * this.pathShape.pathLength(context));
      } else {
         return offset;
      }
   }

   private void paintDebugPath(@NotNull RenderContext context, @NotNull Graphics2D g) {
      PathIterator pathIterator = this.createPathIterator(context);
      float startX = 0.0F;
      float startY = 0.0F;
      float curX = 0.0F;
      float curY = 0.0F;
      g.setStroke(new BasicStroke(0.5F));
      float[] cord = new float[2];

      while (!pathIterator.isDone()) {
         switch (pathIterator.currentSegment(cord)) {
            case 0:
               curX = cord[0];
               curY = cord[1];
               startX = curX;
               startY = curY;
               break;
            case 1:
               g.setColor(Color.MAGENTA);
               g.draw(new Float(curX, curY, cord[0], cord[1]));
               g.setColor(Color.RED);
               g.fillRect((int)curX - 2, (int)curY - 2, 4, 4);
               g.fillRect((int)cord[0] - 2, (int)cord[1] - 2, 4, 4);
               curX = cord[0];
               curY = cord[1];
               break;
            case 2:
            case 3:
            default:
               throw new IllegalStateException();
            case 4:
               g.setColor(Color.MAGENTA);
               g.draw(new Float(curX, curY, startX, startY));
               g.setColor(Color.RED);
               g.fillRect((int)curX - 2, (int)curY - 2, 4, 4);
               g.fillRect((int)startX - 2, (int)startY - 2, 4, 4);
               curX = startX;
               curY = startY;
         }

         pathIterator.next();
      }
   }

   @NotNull
   private PathIterator createPathIterator(@NotNull RenderContext context) {
      MeasureContext measureContext = context.measureContext();
      Shape path = this.pathShape.shape(context);
      if (this.pathShapeTransform != null) {
         path = this.pathShapeTransform
            .transformShape(path, context, ElementBounds.fromUntransformedBounds(this, context, path.getBounds2D(), HasShape.Box.BoundingBox));
      }

      float flatness = 0.1F * measureContext.ex();
      switch (this.side) {
         case Left:
            return path.getPathIterator(null, flatness);
         case Right:
            return new ReversePathIterator(path.getPathIterator(null, flatness));
         default:
            throw new IllegalStateException();
      }
   }

   @NotNull
   @Override
   public GlyphCursor createLocalCursor(boolean isInitial, @NotNull GlyphCursor current) {
      return current;
   }

   @Override
   public void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local) {
      current.updateFrom(local);
   }

   // $VF: synthetic method
   private void lambda$renderText$0(RenderContext context, Graphics2D g) {
      this.paintDebugPath(context, g);
   }
}

