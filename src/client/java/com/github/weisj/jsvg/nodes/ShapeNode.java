package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.FontSize;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.prototype.HasFontContext;
import com.github.weisj.jsvg.nodes.prototype.HasPaintContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.HasVectorEffects;
import com.github.weisj.jsvg.nodes.prototype.Instantiator;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ShapeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public abstract class ShapeNode extends RenderableSVGNode implements HasShape, HasPaintContext, HasFontContext, HasVectorEffects, Instantiator {
   private PaintContext paintContext;
   private FontSize fontSize;
   private Length fontSizeAdjust;
   private Length pathLength;
   private SVGShape shape;
   private Marker markerStart;
   private Marker markerMid;
   private Marker markerEnd;
   private Set<VectorEffect> vectorEffects;

   @NotNull
   public PaintContext paintContext() {
      return this.paintContext;
   }

   @NotNull
   @Override
   public Mutator<MeasurableFontSpec> fontSpec() {
      return s -> s.withFontSize(this.fontSize, this.fontSizeAdjust);
   }

   @NotNull
   public SVGShape shape() {
      return this.shape;
   }

   @NotNull
   @Override
   public Set<VectorEffect> vectorEffects() {
      return this.vectorEffects;
   }

   @Override
   public final void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.paintContext = PaintContext.parse(attributeNode);
      this.fontSize = FontParser.parseFontSize(attributeNode);
      this.fontSizeAdjust = FontParser.parseSizeAdjust(attributeNode);
      this.shape = this.buildShape(attributeNode);
      this.pathLength = attributeNode.getLength("pathLength", PercentageDimension.NONE, Length.UNSPECIFIED);
      Marker template = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker"), AttributeNode.ElementRelation.TEMPLATE);
      this.markerStart = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-start"), AttributeNode.ElementRelation.TEMPLATE);
      if (this.markerStart == null) {
         this.markerStart = template;
      }

      this.markerMid = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-mid"), AttributeNode.ElementRelation.TEMPLATE);
      if (this.markerMid == null) {
         this.markerMid = template;
      }

      this.markerEnd = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-end"), AttributeNode.ElementRelation.TEMPLATE);
      if (this.markerEnd == null) {
         this.markerEnd = template;
      }

      this.vectorEffects = VectorEffect.parse(attributeNode);
   }

   @NotNull
   protected abstract SVGShape buildShape(@NotNull AttributeNode var1);

   @NotNull
   @Override
   public Shape untransformedElementShape(@NotNull RenderContext context, HasShape.Box box) {
      Shape realShape = this.shape.shape(context);
      switch (box) {
         case BoundingBox:
            return realShape;
         case StrokeBox:
            Area area = new Area(realShape);
            area.add(new Area(this.computeEffectiveStroke(context).createStrokedShape(realShape)));
            return area;
         default:
            throw new IllegalStateException("Unexpected value: " + box);
      }
   }

   @NotNull
   @Override
   public Rectangle2D untransformedElementBounds(@NotNull RenderContext context, HasShape.Box box) {
      Rectangle2D bounds = this.shape.bounds(context, true);
      switch (box) {
         case BoundingBox:
            return bounds;
         case StrokeBox:
            LengthValue strokeWidth = RenderContextAccessor.instance().strokeContext(context).strokeWidth;
            if (strokeWidth != null) {
               float stroke = strokeWidth.resolve(context.measureContext());
               if (stroke > 0.0F) {
                  bounds = GeometryUtil.grow(bounds, stroke);
               }
            }

            return bounds;
         default:
            throw new IllegalStateException("Unexpected value: " + box);
      }
   }

   @Override
   public boolean canInstantiate(@NotNull SVGNode node) {
      return node instanceof Marker;
   }

   @NotNull
   private Stroke computeEffectiveStroke(@NotNull RenderContext context) {
      MeasureContext measureContext = context.measureContext();
      float pathLengthFactor = 1.0F;
      if (this.pathLength.isSpecified()) {
         double effectiveLength = this.pathLength.resolve(measureContext);
         double actualLength = this.shape.pathLength(context);
         pathLengthFactor = (float)(actualLength / effectiveLength);
      }

      return context.stroke(pathLengthFactor);
   }

   @Override
   public final void render(@NotNull RenderContext context, @NotNull Output output) {
      Shape paintShape = this.shape.shape(context);
      Rectangle2D bounds = this.shape.usesOptimizedBoundsCalculation() ? this.shape.bounds(context, false) : null;
      Stroke effectiveStroke = this.computeEffectiveStroke(context);
      ShapeRenderer.renderWithPaintOrder(
         output,
         this.shape.canBeFilled(),
         RenderContextAccessor.instance().paintOrder(context),
         new ShapeRenderer.ShapePaintContext(context, this.vectorEffects(), effectiveStroke, GeometryUtil.toAwtTransform(context, this.transform())),
         new ShapeRenderer.PaintShape(paintShape, bounds),
         new ShapeRenderer.ShapeMarkerInfo(this, this.markerStart, this.markerMid, this.markerEnd, this.shouldPaintStartEndMarkersInMiddle())
      );
   }

   protected boolean shouldPaintStartEndMarkersInMiddle() {
      return true;
   }
}

