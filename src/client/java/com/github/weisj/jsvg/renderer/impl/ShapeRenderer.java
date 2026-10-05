package com.github.weisj.jsvg.renderer.impl;

import com.github.weisj.jsvg.attributes.MarkerOrientation;
import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.Marker;
import com.github.weisj.jsvg.nodes.ShapeNode;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D.Float;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShapeRenderer {
   private static final boolean DEBUG_MARKERS = false;

   private ShapeRenderer() {
   }

   public static void renderWithPaintOrder(
      @NotNull Output output,
      boolean canBeFilledHint,
      @NotNull PaintOrder paintOrder,
      @NotNull ShapeRenderer.ShapePaintContext shapePaintContext,
      @NotNull ShapeRenderer.PaintShape paintShape,
      @Nullable ShapeRenderer.ShapeMarkerInfo markerInfo
   ) {
      Set<VectorEffect> vectorEffects = shapePaintContext.vectorEffects;
      VectorEffect.applyEffects(shapePaintContext.vectorEffects, output, shapePaintContext.context, shapePaintContext.transform);
      Output.SafeState safeState = output.safeState();
      boolean fillPainted = false;

      for (PaintOrder.Phase phase : paintOrder.phases()) {
         RenderContext phaseContext = RenderContextAccessor.instance().deriveForChildGraphics(shapePaintContext.context);
         switch (phase) {
            case FILL:
               if (canBeFilledHint) {
                  renderShapeFill(phaseContext, output, paintShape);
               }

               fillPainted = true;
               break;
            case STROKE:
               ShapeRenderer.PaintShape strokeShape = null;
               Shape shape = paintShape.shape;
               if (vectorEffects.contains(VectorEffect.NonScalingStroke) && !vectorEffects.contains(VectorEffect.NonScalingSize)) {
                  Shape stroked = VectorEffect.applyNonScalingStroke(output, phaseContext, shapePaintContext.stroke, paintShape.shape);
                  strokeShape = new ShapeRenderer.PaintShape(stroked, stroked.getBounds2D());
               }

               renderShapeStroke(
                  phaseContext,
                  output,
                  new ShapeRenderer.PaintShape(shape, paintShape.bounds),
                  strokeShape,
                  shapePaintContext.stroke,
                  !fillPainted && canBeFilledHint
               );
               break;
            case MARKERS:
               if (markerInfo != null) {
                  renderMarkers(output, phaseContext, paintShape, markerInfo);
               }
         }

         safeState.restore();
      }
   }

   private static void renderMarkers(
      @NotNull Output output, @NotNull RenderContext context, @NotNull ShapeRenderer.PaintShape paintShape, @NotNull ShapeRenderer.ShapeMarkerInfo markerInfo
   ) {
      if (markerInfo.markerStart != null || markerInfo.markerMid != null || markerInfo.markerEnd != null) {
         renderMarkersImpl(output, context, paintShape.shape.getPathIterator(null), markerInfo);
      }
   }

   private static void renderShapeStroke(
      @NotNull RenderContext context,
      @NotNull Output output,
      @NotNull ShapeRenderer.PaintShape paintShape,
      @Nullable ShapeRenderer.PaintShape strokeShape,
      @Nullable Stroke stroke,
      boolean willBeFilledAfterwards
   ) {
      ShapeRenderer.PaintWithOpacity paintWithOpacity = new ShapeRenderer.PaintWithOpacity(context.strokePaint(), context.strokeOpacity());
      if (stroke != null && paintWithOpacity.isVisible(context)) {
         output.applyOpacity(paintWithOpacity.opacity);
         output.setStroke(stroke);
         ShapeRenderer.PaintShape strokeShapeToPaint = computeEffectiveStrokeShape(context, output, paintShape, strokeShape, stroke, willBeFilledAfterwards);
         if (strokeShapeToPaint != null) {
            paintWithOpacity.paint.fillShape(output, context, strokeShapeToPaint.shape, strokeShapeToPaint.bounds);
         } else {
            paintWithOpacity.paint.drawShape(output, context, paintShape.shape, paintShape.bounds);
         }
      }
   }

   private static ShapeRenderer.PaintShape computeEffectiveStrokeShape(
      @NotNull RenderContext context,
      @NotNull Output output,
      @NotNull ShapeRenderer.PaintShape paintShape,
      @Nullable ShapeRenderer.PaintShape strokeShape,
      @NotNull Stroke stroke,
      boolean willBeFilledAfterwards
   ) {
      ShapeRenderer.PaintShape strokeShapeToPaint = strokeShape;
      boolean removeFillArea = output.hasMaskedPaint()
         && willBeFilledAfterwards
         && context.fillPaint().isVisible(context)
         && context.fillOpacity() == 1.0F
         && output.currentOpacity() == 1.0F;
      if (removeFillArea) {
         Area s = new Area(strokeShape != null ? strokeShape.shape : stroke.createStrokedShape(paintShape.shape));
         s.subtract(new Area(paintShape.shape));
         Rectangle2D strokedBounds = paintShape.bounds;
         if (strokedBounds != null) {
            if (stroke instanceof BasicStroke) {
               strokedBounds = GeometryUtil.grow(strokedBounds, ((BasicStroke)stroke).getLineWidth() / 2.0F);
            } else {
               strokedBounds = s.getBounds2D();
            }
         }

         strokeShapeToPaint = new ShapeRenderer.PaintShape(s, strokedBounds);
      }

      return strokeShapeToPaint;
   }

   private static void renderShapeFill(@NotNull RenderContext context, @NotNull Output output, @NotNull ShapeRenderer.PaintShape paintShape) {
      ShapeRenderer.PaintWithOpacity paintWithOpacity = new ShapeRenderer.PaintWithOpacity(context.fillPaint(), context.fillOpacity());
      if (paintWithOpacity.isVisible(context)) {
         output.applyOpacity(paintWithOpacity.opacity);
         paintWithOpacity.paint.fillShape(output, context, paintShape.shape, paintShape.bounds);
      }
   }

   private static void renderMarkersImpl(
      @NotNull Output output, @NotNull RenderContext context, @NotNull PathIterator iterator, @NotNull ShapeRenderer.ShapeMarkerInfo markerInfo
   ) {
      float[] args = new float[6];
      float x = 0.0F;
      float y = 0.0F;
      float xStart = 0.0F;
      float yStart = 0.0F;
      float dxIn = 0.0F;
      float dyIn = 0.0F;
      Marker start = markerInfo.markerStart;
      Marker mid = markerInfo.markerMid;
      Marker end = markerInfo.markerEnd;
      boolean onlyFirst = mid == null && end == null;
      Marker markerToPaint = null;
      MarkerOrientation.MarkerType markerToPaintType = null;

      while (!iterator.isDone()) {
         int type = iterator.currentSegment(args);
         iterator.next();
         Marker nextMarker = iterator.isDone() ? end : mid;
         MarkerOrientation.MarkerType nextMarkerType = iterator.isDone() ? MarkerOrientation.MarkerType.END : MarkerOrientation.MarkerType.MID;
         float xPaint = x;
         float yPaint = y;
         float dx = dxIn;
         float dy = dyIn;
         float dxOut;
         float dyOut;
         switch (type) {
            case 0:
               dxIn = 0.0F;
               dyIn = 0.0F;
               x = xStart = args[0];
               y = yStart = args[1];
               if (markerInfo.shouldPaintStartEndMarkersInMiddle || markerToPaint == null) {
                  nextMarker = start;
                  nextMarkerType = MarkerOrientation.MarkerType.START;
               }

               if (markerToPaint != null) {
                  paintSingleMarker(markerInfo.node, context, output, markerToPaintType, markerToPaint, xPaint, yPaint, 0.0F, 0.0F, dx, dy);
                  if (onlyFirst) {
                     return;
                  }
               }

               markerToPaint = nextMarker;
               markerToPaintType = nextMarkerType;
               continue;
            case 1:
               dxOut = dxIn = args[0] - x;
               dyOut = dyIn = args[1] - y;
               x = args[0];
               y = args[1];
               break;
            case 2:
               dxOut = args[0] - x;
               dyOut = args[1] - y;
               dxIn = args[2] - args[0];
               dyIn = args[3] - args[1];
               x = args[2];
               y = args[3];
               break;
            case 3:
               dxOut = args[0] - x;
               dyOut = args[1] - y;
               dxIn = args[4] - args[2];
               dyIn = args[5] - args[3];
               x = args[4];
               y = args[5];
               break;
            case 4:
               dxOut = dxIn = xStart - x;
               dyOut = dyIn = yStart - y;
               x = xStart;
               y = yStart;
               if (markerInfo.shouldPaintStartEndMarkersInMiddle) {
                  nextMarker = end;
                  nextMarkerType = MarkerOrientation.MarkerType.END;
               }
               break;
            default:
               throw new IllegalStateException();
         }

         paintSingleMarker(markerInfo.node, context, output, markerToPaintType, markerToPaint, xPaint, yPaint, dx, dy, dxOut, dyOut);
         if (onlyFirst) {
            return;
         }

         markerToPaint = nextMarker;
         markerToPaintType = nextMarkerType;
      }

      paintSingleMarker(markerInfo.node, context, output, markerToPaintType, markerToPaint, x, y, dxIn, dyIn, 0.0F, 0.0F);
   }

   public static void paintSingleMarker(
      @NotNull ShapeNode shapeNode,
      @NotNull RenderContext context,
      @NotNull Output output,
      @Nullable MarkerOrientation.MarkerType type,
      @Nullable Marker marker,
      float x,
      float y,
      float dxIn,
      float dyIn,
      float dxOut,
      float dyOut
   ) {
      if (marker != null) {
         assert type != null;
         MarkerOrientation orientation = marker.orientation();
         float rotation = orientation.orientationFor(type, dxIn, dyIn, dxOut, dyOut);
         Output markerOutput = output.createChild();
         RenderContext markerContext = RenderContextAccessor.instance().deriveForChildGraphics(context);
         markerContext.translate(markerOutput, x, y);
         markerContext.rotate(markerOutput, rotation);
         NodeRenderer.renderNode(marker, markerContext, markerOutput, shapeNode);
         markerOutput.dispose();
      }
   }

   private static void paintDebugMarker(@NotNull RenderContext context, @NotNull Graphics2D g, @NotNull Marker marker, float rotation) {
      FloatSize size = marker.size(context);
      Path2D p = new Float();
      p.moveTo(0.0, size.height / 2.0F);
      p.lineTo(size.width, size.height / 2.0F);
      p.moveTo(0.8 * size.width, 0.35F * size.height);
      p.lineTo(size.width, size.height / 2.0F);
      p.lineTo(0.8 * size.width, 0.65F * size.height);
      g.setStroke(new BasicStroke(0.5F));
      g.setColor(Color.MAGENTA.darker().darker());
      g.draw(new java.awt.geom.Rectangle2D.Float(0.0F, 0.0F, size.width, size.height));
      g.draw(p);
      g.rotate(rotation);
      g.setColor(Color.MAGENTA);
      g.draw(new java.awt.geom.Rectangle2D.Float(0.0F, 0.0F, size.width, size.height));
      g.draw(p);
   }

   // $VF: synthetic method
   private static void lambda$paintSingleMarker$0(RenderContext markerContext, Marker marker, float rotation, Graphics2D g) {
      paintDebugMarker(markerContext, g, marker, rotation);
   }

   public static final class PaintShape {
      @NotNull
      private final Shape shape;
      @Nullable
      private final Rectangle2D bounds;

      public PaintShape(@NotNull Shape shape, @Nullable Rectangle2D bounds) {
         this.shape = shape;
         this.bounds = bounds;
      }
   }

   private static final class PaintWithOpacity {
      @NotNull
      private final SVGPaint paint;
      private final float opacity;

      private PaintWithOpacity(@NotNull SVGPaint paint, float opacity) {
         this.paint = paint;
         this.opacity = opacity;
      }

      boolean isVisible(@NotNull RenderContext context) {
         return this.opacity > 0.0F && this.paint.isVisible(context);
      }
   }

   public static final class ShapeMarkerInfo {
      @NotNull
      private final ShapeNode node;
      @Nullable
      private final Marker markerStart;
      @Nullable
      private final Marker markerMid;
      @Nullable
      private final Marker markerEnd;
      private final boolean shouldPaintStartEndMarkersInMiddle;

      public ShapeMarkerInfo(
         @NotNull ShapeNode node,
         @Nullable Marker markerStart,
         @Nullable Marker markerMid,
         @Nullable Marker markerEnd,
         boolean shouldPaintStartEndMarkersInMiddle
      ) {
         this.node = node;
         this.markerStart = markerStart;
         this.markerMid = markerMid;
         this.markerEnd = markerEnd;
         this.shouldPaintStartEndMarkersInMiddle = shouldPaintStartEndMarkersInMiddle;
      }
   }

   public static final class ShapePaintContext {
      @NotNull
      private final RenderContext context;
      @NotNull
      private final Set<VectorEffect> vectorEffects;
      @NotNull
      private final Stroke stroke;
      @Nullable
      private final AffineTransform transform;

      public ShapePaintContext(
         @NotNull RenderContext context, @NotNull Set<VectorEffect> vectorEffects, @NotNull Stroke stroke, @Nullable AffineTransform transform
      ) {
         this.context = context;
         this.vectorEffects = vectorEffects;
         this.stroke = stroke;
         this.transform = transform;
      }
   }
}

