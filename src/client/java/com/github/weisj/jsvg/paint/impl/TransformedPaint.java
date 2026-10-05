package com.github.weisj.jsvg.paint.impl;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import org.jetbrains.annotations.NotNull;

public final class TransformedPaint implements Paint {
   @NotNull
   private final Paint paint;
   @NotNull
   private final AffineTransform transform;

   public TransformedPaint(@NotNull Paint paint, @NotNull AffineTransform transform) {
      this.paint = paint;
      this.transform = transform;
   }

   @Override
   public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
      xform.concatenate(this.transform);
      return this.paint.createContext(cm, deviceBounds, userBounds, xform, hints);
   }

   @Override
   public int getTransparency() {
      return this.paint.getTransparency();
   }
}

