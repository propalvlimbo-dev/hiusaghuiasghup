package com.github.weisj.jsvg.renderer.output.impl;

import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NullOutput implements Output, Output.SafeState {
   @Override
   public void fillShape(@NotNull Shape shape) {
   }

   @Override
   public void drawShape(@NotNull Shape shape) {
   }

   @Override
   public void drawImage(@NotNull BufferedImage image) {
   }

   @Override
   public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
   }

   @Override
   public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
   }

   @Override
   public void setPaint(@NotNull Paint paint) {
   }

   @Override
   public void setPaint(@NotNull Supplier<Paint> paintProvider) {
   }

   @Override
   public void setStroke(@NotNull Stroke stroke) {
   }

   @NotNull
   @Override
   public Stroke stroke() {
      return new BasicStroke();
   }

   @Override
   public void applyClip(@NotNull Shape clipShape) {
   }

   @Override
   public void setClip(@Nullable Shape shape) {
   }

   @Override
   public Optional<Float> contextFontSize() {
      return Optional.empty();
   }

   @NotNull
   @Override
   public Output createChild() {
      return this;
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
      return new Double();
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
      return new AffineTransform();
   }

   @Override
   public void setTransform(@NotNull AffineTransform affineTransform) {
   }

   @Override
   public void applyTransform(@NotNull AffineTransform transform) {
   }

   @Override
   public void rotate(double angle) {
   }

   @Override
   public void scale(double sx, double sy) {
   }

   @Override
   public void translate(double dx, double dy) {
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
      return this;
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
   public void restore() {
   }
}

