package com.github.weisj.jsvg.renderer.output.impl;

import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.ShapeUtil;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShapeOutput implements Output {
   @NotNull
   private final Area accumulatorShape;
   @NotNull
   private AffineTransform currentTransform;
   @NotNull
   private Stroke currentStroke;
   @Nullable
   private Shape currentClip;

   public ShapeOutput(@NotNull Area area) {
      this.accumulatorShape = area;
      this.currentStroke = new BasicStroke();
      this.currentTransform = new AffineTransform();
      this.currentClip = null;
   }

   private ShapeOutput(@NotNull ShapeOutput parent) {
      this.accumulatorShape = parent.accumulatorShape;
      this.currentStroke = parent.currentStroke;
      this.currentTransform = new AffineTransform(parent.currentTransform);
      this.currentClip = parent.currentClip != null ? new Area(parent.currentClip) : null;
   }

   private void addShape(@NotNull Shape shape) {
      Shape s = this.currentClip != null ? ShapeUtil.intersect(this.currentClip, shape, true, false) : shape;
      this.accumulatorShape.add(new Area(s));
   }

   private void append(@NotNull Shape shape, @NotNull AffineTransform transform) {
      AffineTransform at = new AffineTransform(this.currentTransform);
      at.concatenate(transform);
      this.addShape(ShapeUtil.transformShape(shape, at));
   }

   private void append(@NotNull Shape shape) {
      this.addShape(ShapeUtil.transformShape(shape, this.currentTransform));
   }

   @Override
   public void fillShape(@NotNull Shape shape) {
      this.append(shape);
   }

   @Override
   public void drawShape(@NotNull Shape shape) {
      this.append(this.currentStroke.createStrokedShape(shape));
   }

   @Override
   public void drawImage(@NotNull BufferedImage image) {
      this.append(new Float(0.0F, 0.0F, image.getWidth(), image.getHeight()));
   }

   @Override
   public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
      this.append(new Float(0.0F, 0.0F, image.getWidth(null), image.getHeight(null)));
   }

   @Override
   public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
      this.append(new Float(0.0F, 0.0F, image.getWidth(null), image.getHeight(null)), at);
   }

   @Override
   public void setPaint(@NotNull Paint paint) {
   }

   @Override
   public void setPaint(@NotNull Supplier<Paint> paintProvider) {
   }

   @Override
   public void setStroke(@NotNull Stroke stroke) {
      this.currentStroke = stroke;
   }

   @NotNull
   @Override
   public Stroke stroke() {
      return this.currentStroke;
   }

   @Override
   public void applyClip(@NotNull Shape clipShape) {
      Shape transformedShape = ShapeUtil.transformShape(clipShape, this.currentTransform);
      if (this.currentClip != null) {
         this.currentClip = ShapeUtil.intersect(this.currentClip, transformedShape, true, false);
      } else {
         this.currentClip = transformedShape;
      }
   }

   @Override
   public void setClip(@Nullable Shape shape) {
      this.currentClip = shape != null ? ShapeUtil.transformShape(shape, this.currentTransform) : null;
   }

   @Override
   public Optional<java.lang.Float> contextFontSize() {
      return Optional.empty();
   }

   @NotNull
   @Override
   public Output createChild() {
      return new ShapeOutput(this);
   }

   @Override
   public void dispose() {
   }

   @Override
   public void debugPaint(@NotNull Consumer<Graphics2D> painter) {
   }

   @NotNull
   @Override
   public Rectangle2D clipBounds() {
      float veryLargeNumber = 8.5070587E37F;
      return this.currentClip != null
         ? this.currentClip.getBounds2D()
         : new Float(-veryLargeNumber, -veryLargeNumber, 2.0F * veryLargeNumber, 2.0F * veryLargeNumber);
   }

   @Nullable
   @Override
   public RenderingHints renderingHints() {
      return null;
   }

   @Nullable
   @Override
   public Object renderingHint(@NotNull Key key) {
      return null;
   }

   @Override
   public void setRenderingHint(@NotNull Key key, @Nullable Object value) {
   }

   @NotNull
   @Override
   public AffineTransform transform() {
      return new AffineTransform(this.currentTransform);
   }

   @Override
   public void setTransform(@NotNull AffineTransform affineTransform) {
      this.currentTransform = new AffineTransform(affineTransform);
   }

   @Override
   public void applyTransform(@NotNull AffineTransform transform) {
      this.currentTransform.concatenate(transform);
   }

   @Override
   public void rotate(double angle) {
      this.currentTransform.rotate(angle);
   }

   @Override
   public void scale(double sx, double sy) {
      this.currentTransform.scale(sx, sy);
   }

   @Override
   public void translate(double dx, double dy) {
      this.currentTransform.translate(dx, dy);
   }

   @Override
   public float currentOpacity() {
      return 1.0F;
   }

   @Override
   public void applyOpacity(float opacity) {
   }

   @NotNull
   @Override
   public Output.SafeState safeState() {
      return new ShapeOutput.ShapeOutputSafeState(this);
   }

   @Override
   public boolean supportsFilters() {
      return false;
   }

   @Override
   public boolean supportsColors() {
      return false;
   }

   @Override
   public boolean isSoftClippingEnabled() {
      return false;
   }

   private static class ShapeOutputSafeState implements Output.SafeState {
      @NotNull
      private final ShapeOutput shapeOutput;
      @NotNull
      private final Stroke oldStroke;
      @NotNull
      private final AffineTransform oldTransform;
      @Nullable
      private final Area oldClip;

      private ShapeOutputSafeState(@NotNull ShapeOutput shapeOutput) {
         this.shapeOutput = shapeOutput;
         this.oldStroke = shapeOutput.stroke();
         this.oldTransform = shapeOutput.transform();
         this.oldClip = shapeOutput.currentClip != null ? new Area(shapeOutput.currentClip) : null;
      }

      @Override
      public void restore() {
         this.shapeOutput.currentStroke = this.oldStroke;
         this.shapeOutput.currentTransform = this.oldTransform;
         this.shapeOutput.currentClip = this.oldClip;
      }
   }
}

