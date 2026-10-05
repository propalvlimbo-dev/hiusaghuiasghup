package com.github.weisj.jsvg.paint.impl;

import com.github.weisj.jsvg.attributes.MaskType;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import com.github.weisj.jsvg.util.CachedSurfaceSupplier;
import com.github.weisj.jsvg.util.ColorUtil;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MaskedPaint implements Paint, GraphicsUtil.WrappingPaint, GraphicsUtil.DisposablePaint {
   @NotNull
   private Paint paint;
   @NotNull
   private final Raster maskRaster;
   @NotNull
   private final Point maskOffset;
   @Nullable
   private final CachedSurfaceSupplier.ResourceCleaner cleaner;
   @NotNull
   private final MaskType maskType;

   public MaskedPaint(
      @NotNull Paint paint,
      @NotNull Raster maskRaster,
      @NotNull Point2D maskOffset,
      @Nullable CachedSurfaceSupplier.ResourceCleaner cleaner,
      @NotNull MaskType maskType
   ) {
      this.paint = paint;
      this.maskRaster = maskRaster;
      this.maskOffset = new Point((int)Math.floor(maskOffset.getX()), (int)Math.floor(maskOffset.getY()));
      this.cleaner = cleaner;
      this.maskType = maskType;
   }

   @Override
   public void cleanupIfNeeded(@NotNull Output output) {
      if (this.cleaner != null) {
         this.cleaner.clean(output);
      }
   }

   @Override
   public void setPaint(@NotNull Paint paint) {
      this.paint = paint;
   }

   @NotNull
   @Override
   public Paint paint() {
      return this.paint;
   }

   @Override
   public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
      PaintContext parentContext = this.paint.createContext(null, deviceBounds, userBounds, xform, hints);
      return new MaskedPaint.MaskPaintContext(parentContext, this.maskRaster, this.maskOffset, this.maskType);
   }

   @Override
   public int getTransparency() {
      return 3;
   }

   private static final class MaskPaintContext implements PaintContext {
      @NotNull
      private final PaintContext parentContext;
      @NotNull
      private final ColorModel colorModel;
      private final int numColorComponents;
      @NotNull
      private final ColorModel parentColorModel;
      @NotNull
      private final Raster maskRaster;
      @NotNull
      private final Point offset;
      private final int maskBand;

      MaskPaintContext(@NotNull PaintContext parentContext, @NotNull Raster maskRaster, @NotNull Point offset, @NotNull MaskType maskType) {
         this.parentContext = parentContext;
         this.parentColorModel = parentContext.getColorModel();
         this.maskRaster = maskRaster;
         this.offset = offset;
         if (parentContext.getColorModel().hasAlpha()) {
            this.colorModel = this.parentColorModel;
         } else {
            this.colorModel = new ComponentColorModel(parentContext.getColorModel().getColorSpace(), true, false, 1, 0);
         }

         this.numColorComponents = this.colorModel.getNumColorComponents();
         this.maskBand = maskType == MaskType.Alpha ? maskRaster.getNumBands() - 1 : 0;
      }

      @NotNull
      @Override
      public ColorModel getColorModel() {
         return this.colorModel;
      }

      @Override
      public void dispose() {
         this.parentContext.dispose();
      }

      @Override
      public Raster getRaster(int x, int y, int w, int h) {
         Raster parentRaster = this.parentContext.getRaster(x, y, w, h);
         int parentMinX = parentRaster.getMinX();
         int parentMinY = parentRaster.getMinY();
         WritableRaster result;
         if (parentRaster instanceof WritableRaster) {
            if (this.parentColorModel.equals(this.colorModel)) {
               result = parentRaster.createCompatibleWritableRaster();
               result.setDataElements(-parentMinX, -parentMinY, parentRaster);
            } else {
               BufferedImage parentImage = new BufferedImage(
                  this.parentColorModel, (WritableRaster)parentRaster, this.parentColorModel.isAlphaPremultiplied(), null
               );
               result = Raster.createWritableRaster(this.colorModel.createCompatibleSampleModel(w, h), new Point(0, 0));
               BufferedImage resultImage = new BufferedImage(this.colorModel, result, false, null);
               Graphics graphics = resultImage.getGraphics();
               graphics.drawImage(parentImage, 0, 0, null);
               graphics.dispose();
            }
         } else {
            result = Raster.createInterleavedRaster(0, w, h, this.getColorModel().getNumComponents(), new Point(0, 0));
            ColorConvertOp colorConvertOp = new ColorConvertOp(this.parentColorModel.getColorSpace(), this.colorModel.getColorSpace(), null);
            colorConvertOp.filter(parentRaster, result);
         }

         int softMaskMinX = this.maskRaster.getMinX();
         int softMaskMinY = this.maskRaster.getMinY();
         int softMaskMaxX = softMaskMinX + this.maskRaster.getWidth();
         int softMaskMaxY = softMaskMinY + this.maskRaster.getHeight();

         for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
               int rx = x + i - this.offset.x;
               int ry = y + j - this.offset.y;
               int luminance;
               if (rx >= softMaskMinX && rx < softMaskMaxX && ry >= softMaskMinY && ry < softMaskMaxY) {
                  luminance = this.maskRaster.getSample(rx, ry, this.maskBand);
               } else {
                  luminance = 0;
               }

               int newAlpha = ColorUtil.div255(luminance * result.getSample(i, j, this.numColorComponents));
               result.setSample(i, j, this.numColorComponents, newAlpha);
            }
         }

         return result;
      }
   }
}

