package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.Graphics2DOutput;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.SystemUtil;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
import java.util.List;
import org.jetbrains.annotations.NotNull;

class AbstractGlyphRun<T extends Shape> {
   @NotNull
   private final T shape;
   @NotNull
   private final AbstractGlyphRun.Metrics metrics;
   @NotNull
   private final List<AbstractGlyphRun.PaintableEmoji> emojis;

   public AbstractGlyphRun(@NotNull T shape, @NotNull AbstractGlyphRun.Metrics metrics, @NotNull List<AbstractGlyphRun.PaintableEmoji> emojis) {
      this.shape = shape;
      this.metrics = metrics;
      this.emojis = emojis;
   }

   @NotNull
   public T shape() {
      return this.shape;
   }

   @NotNull
   public AbstractGlyphRun.Metrics metrics() {
      return this.metrics;
   }

   @NotNull
   public List<AbstractGlyphRun.PaintableEmoji> emojis() {
      return this.emojis;
   }

   static class Metrics {
      @NotNull
      final Rectangle2D paintBounds;
      @NotNull
      final Rectangle2D layoutBounds;

      Metrics(@NotNull Rectangle2D paintBounds, @NotNull Rectangle2D layoutBounds) {
         this.paintBounds = paintBounds;
         this.layoutBounds = layoutBounds;
      }

      @NotNull
      static AbstractGlyphRun.Metrics createDefault() {
         return new AbstractGlyphRun.Metrics(
            new Float(java.lang.Float.NaN, java.lang.Float.NaN, 0.0F, 0.0F), new Float(java.lang.Float.NaN, java.lang.Float.NaN, 0.0F, 0.0F)
         );
      }

      void union(@NotNull AbstractGlyphRun.Metrics metrics) {
         if (Length.isUnspecified((float)this.paintBounds.getX())) {
            this.paintBounds.setRect(metrics.paintBounds);
            this.layoutBounds.setRect(metrics.layoutBounds);
         } else {
            Rectangle2D.union(this.paintBounds, metrics.paintBounds, this.paintBounds);
            Rectangle2D.union(this.layoutBounds, metrics.layoutBounds, this.layoutBounds);
         }
      }

      @NotNull
      AbstractGlyphRun.Metrics copy() {
         return new AbstractGlyphRun.Metrics(this.paintBounds, this.layoutBounds);
      }
   }

   public static class PaintableEmoji {
      @NotNull
      private final EmojiGlyph glyph;
      @NotNull
      private final AffineTransform transform;

      PaintableEmoji(@NotNull EmojiGlyph glyph, @NotNull AffineTransform transform) {
         this.glyph = glyph;
         this.transform = transform;
      }

      public void render(@NotNull Output output, @NotNull SVGFont font) {
         output.applyTransform(this.transform);
         int fontSize = font.size();
         int maxFontSize = SystemUtil.isMacOS ? 100 : Integer.MAX_VALUE;
         if (output.transform().getScaleY() * fontSize > maxFontSize) {
            float baselinePosition = 0.9F;
            if (this.glyph.largeBitmap == null) {
               BufferedImage bitmap = ImageUtil.createCompatibleTransparentImage(maxFontSize, maxFontSize);
               Graphics g = bitmap.getGraphics();
               g.setFont(g.getFont().deriveFont((float)maxFontSize));
               g.drawString(this.glyph.codepoint(), 0, (int)(baselinePosition * maxFontSize));
               g.dispose();
               this.glyph.largeBitmap = bitmap;
            }

            output.scale((double)fontSize / maxFontSize, (double)fontSize / maxFontSize);
            output.translate(0.0, -((int)(baselinePosition * maxFontSize)));
            output.drawImage(this.glyph.largeBitmap);
         } else if (output instanceof Graphics2DOutput) {
            Graphics2D g = ((Graphics2DOutput)output).graphics();
            g.setFont(g.getFont().deriveFont((float)fontSize));
            g.drawString(this.glyph.codepoint(), 0, 0);
         }
      }
   }
}

