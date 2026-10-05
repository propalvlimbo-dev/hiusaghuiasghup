package com.github.weisj.jsvg.renderer.output.impl;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.ImageUtil;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GraphicsUtil {
   private static final Logger LOGGER = LogFactory.createLogger(GraphicsUtil.class);

   private GraphicsUtil() {
   }

   public static void safelySetPaint(@NotNull Output output, @NotNull Graphics2D g, @NotNull Paint paint) {
      g.setPaint(exchangePaint(output, g.getPaint(), paint, true));
   }

   public static void cleanupPaint(@NotNull Output output, @NotNull Paint paint) {
      if (paint instanceof GraphicsUtil.WrappingPaint) {
         cleanupPaint(output, ((GraphicsUtil.WrappingPaint)paint).paint());
      }

      if (paint instanceof GraphicsUtil.DisposablePaint) {
         ((GraphicsUtil.DisposablePaint)paint).cleanupIfNeeded(output);
      }
   }

   public static void preparePaint(@NotNull Paint paint) {
      if (paint instanceof GraphicsUtil.WrappingPaint) {
         preparePaint(((GraphicsUtil.WrappingPaint)paint).paint());
      }
   }

   @NotNull
   public static Paint exchangePaint(@NotNull Output output, @NotNull Paint current, @NotNull Paint paint, boolean doCleanUp) {
      if (paint instanceof GraphicsUtil.WrappingPaint) {
         GraphicsUtil.WrappingPaint wrappingPaint = (GraphicsUtil.WrappingPaint)paint;
         wrappingPaint.safelySetPaint(output, current, doCleanUp);
         return paint;
      }

      if (current instanceof GraphicsUtil.WrappingPaint) {
         GraphicsUtil.WrappingPaint wrappingPaint = (GraphicsUtil.WrappingPaint)current;
         wrappingPaint.safelySetPaint(output, paint, doCleanUp);
         return current;
      }

      if (doCleanUp) {
         preparePaint(paint);
         cleanupPaint(output, current);
      }

      return paint;
   }

   @NotNull
   public static Graphics2D createGraphics(@NotNull BufferedImage image) {
      Graphics2D g = image.createGraphics();
      g.clipRect(0, 0, image.getWidth(), image.getHeight());
      return g;
   }

   @NotNull
   public static Composite deriveComposite(@NotNull Graphics2D g, float opacity) {
      Composite composite = g.getComposite();
      if (composite instanceof AlphaComposite) {
         AlphaComposite ac = (AlphaComposite)composite;
         return AlphaComposite.getInstance(ac.getRule(), ac.getAlpha() * opacity);
      }

      if (composite != null) {
         LOGGER.log(Logger.Level.WARNING, () -> String.format("Composite %s will be overridden by opacity %s", composite, opacity));
      }

      return AlphaComposite.getInstance(3, opacity);
   }

   public static void safelyDrawImage(@NotNull Output output, @NotNull Graphics2D g, @NotNull Image image, @Nullable ImageObserver observer) {
      Paint p = g.getPaint();
      if (p instanceof GraphicsUtil.WrappingPaint) {
         GraphicsUtil.WrappingPaint wrappingPaint = (GraphicsUtil.WrappingPaint)p;
         Paint inner = wrappingPaint.innerPaint();
         Rectangle r = new Rectangle(0, 0, image.getWidth(observer), image.getHeight(observer));
         BufferedImage img = image instanceof BufferedImage ? (BufferedImage)image : ImageUtil.toBufferedImage(image);
         TexturePaint texturePaint = new TexturePaint(img, r);
         wrappingPaint.setPaint(exchangePaint(output, wrappingPaint.paint(), texturePaint, false));
         g.fill(r);
         wrappingPaint.setPaint(exchangePaint(output, texturePaint, inner, false));
      } else {
         g.drawImage(image, 0, 0, observer);
      }
   }

   public interface DisposablePaint {
      void cleanupIfNeeded(@NotNull Output var1);
   }

   public interface WrappingPaint {
      void setPaint(@NotNull Paint var1);

      @NotNull
      Paint paint();

      @NotNull
      default Paint innerPaint() {
         Paint p = this.paint();
         return p instanceof GraphicsUtil.WrappingPaint ? ((GraphicsUtil.WrappingPaint)p).innerPaint() : p;
      }

      default void safelySetPaint(@NotNull Output output, @NotNull Paint paint, boolean updateRefCounts) {
         this.setPaint(GraphicsUtil.exchangePaint(output, this.paint(), paint, updateRefCounts));
      }
   }
}

