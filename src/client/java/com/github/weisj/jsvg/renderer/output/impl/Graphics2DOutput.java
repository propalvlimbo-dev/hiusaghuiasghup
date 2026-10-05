package com.github.weisj.jsvg.renderer.output.impl;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.AwtSVGPaint;
import com.github.weisj.jsvg.paint.impl.MaskedPaint;
import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Graphics2DOutput implements Output, CurrentColorProvider {
   private final Graphics2D g;

   @NotNull
   public Graphics2D graphics() {
      return this.g;
   }

   public Graphics2DOutput(@NotNull Graphics2D g) {
      this.g = g;
      GraphicsUtil.preparePaint(g.getPaint());
   }

   @Nullable
   @Override
   public SVGPaint currentColor() {
      return new AwtSVGPaint(this.g.getColor());
   }

   @Override
   public void fillShape(@NotNull Shape shape) {
      this.g.fill(shape);
   }

   @Override
   public void drawShape(@NotNull Shape shape) {
      this.g.draw(shape);
   }

   @Override
   public void drawImage(@NotNull BufferedImage image) {
      this.g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null, null);
   }

   @Override
   public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
      GraphicsUtil.safelyDrawImage(this, this.g, image, observer);
   }

   @Override
   public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
      this.g.drawImage(image, at, observer);
   }

   @Override
   public void setPaint(@NotNull Paint paint) {
      GraphicsUtil.safelySetPaint(this, this.g, paint);
   }

   @Override
   public void setPaint(@NotNull Supplier<Paint> paintProvider) {
      this.setPaint(paintProvider.get());
   }

   @Override
   public void setStroke(@NotNull Stroke stroke) {
      this.g.setStroke(stroke);
   }

   @NotNull
   @Override
   public Stroke stroke() {
      return this.g.getStroke();
   }

   @Override
   public void applyClip(@NotNull Shape clipShape) {
      this.g.clip(clipShape);
   }

   @Override
   public void setClip(@Nullable Shape shape) {
      this.g.setClip(shape);
   }

   @Override
   public Optional<Float> contextFontSize() {
      Font f = this.g.getFont();
      return f != null ? Optional.of(f.getSize2D()) : Optional.empty();
   }

   @NotNull
   @Override
   public Output createChild() {
      return new Graphics2DOutput((Graphics2D)this.g.create());
   }

   @Override
   public void dispose() {
      GraphicsUtil.cleanupPaint(this, this.g.getPaint());
      this.g.dispose();
   }

   @Override
   public void debugPaint(@NotNull Consumer<Graphics2D> painter) {
      Graphics2D debugGraphics = (Graphics2D)this.g.create();
      painter.accept(debugGraphics);
      debugGraphics.dispose();
   }

   @NotNull
   @Override
   public Rectangle2D clipBounds() {
      return this.g.getClipBounds();
   }

   @NotNull
   @Override
   public RenderingHints renderingHints() {
      return this.g.getRenderingHints();
   }

   @Nullable
   @Override
   public Object renderingHint(@NotNull Key key) {
      return this.g.getRenderingHint(key);
   }

   @Override
   public void setRenderingHint(@NotNull Key key, @Nullable Object value) {
      this.g.setRenderingHint(key, value);
   }

   @NotNull
   @Override
   public AffineTransform transform() {
      return this.g.getTransform();
   }

   @Override
   public void setTransform(@NotNull AffineTransform affineTransform) {
      this.g.setTransform(affineTransform);
   }

   @Override
   public void applyTransform(@NotNull AffineTransform transform) {
      this.g.transform(transform);
   }

   @Override
   public void rotate(double angle) {
      this.g.rotate(angle);
   }

   @Override
   public void scale(double sx, double sy) {
      this.g.scale(sx, sy);
   }

   @Override
   public void translate(double dx, double dy) {
      this.g.translate(dx, dy);
   }

   @Override
   public float currentOpacity() {
      Composite composite = this.g.getComposite();
      return composite instanceof AlphaComposite ? ((AlphaComposite)composite).getAlpha() : 1.0F;
   }

   @Override
   public void applyOpacity(float opacity) {
      if (!GeometryUtil.approximatelyEqual(opacity, 1.0)) {
         this.g.setComposite(GraphicsUtil.deriveComposite(this.g, opacity));
      }
   }

   @Override
   public boolean hasMaskedPaint() {
      return this.g.getPaint() instanceof MaskedPaint;
   }

   @NotNull
   @Override
   public Output.SafeState safeState() {
      return new GraphicsResetHelper(this.g);
   }

   @Override
   public boolean supportsFilters() {
      return true;
   }

   @Override
   public boolean supportsColors() {
      return true;
   }
}

