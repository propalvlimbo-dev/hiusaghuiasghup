package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.util.ColorUtil;
import com.github.weisj.jsvg.util.ImageUtil;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBlendComposite implements Composite {
   private boolean convertToLinearRGB;

   protected AbstractBlendComposite() {
   }

   @NotNull
   protected abstract AbstractBlendComposite.Blender blender();

   private static boolean isColorModelInvalid(ColorModel cm) {
      if (cm instanceof DirectColorModel && cm.getTransferType() == 3) {
         DirectColorModel directCM = (DirectColorModel)cm;
         return directCM.getRedMask() != 16711680
            || directCM.getGreenMask() != 65280
            || directCM.getBlueMask() != 255
            || directCM.getNumComponents() == 4 && directCM.getAlphaMask() != -16777216;
      } else {
         return true;
      }
   }

   public void setConvertToLinearRGB(boolean convertToLinearRGB) {
      this.convertToLinearRGB = convertToLinearRGB;
   }

   @Override
   public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
      if (!isColorModelInvalid(srcColorModel) && !isColorModelInvalid(dstColorModel)) {
         return new AbstractBlendComposite.BlendingContext(this.blender(), srcColorModel, dstColorModel, this.convertToLinearRGB);
      } else {
         throw new RasterFormatException("Incompatible color models");
      }
   }

   @FunctionalInterface
   public interface Blender {
      void blend(int @NotNull [] var1, int @NotNull [] var2, int @NotNull [] var3);
   }

   private static final class BlendingContext implements CompositeContext {
      @NotNull
      private final AbstractBlendComposite.Blender blender;
      @NotNull
      private final ColorModel sourceColorModel;
      @NotNull
      private final ColorModel destinationColorModel;
      private final boolean convertToLinearRGB;

      private BlendingContext(
         @NotNull AbstractBlendComposite.Blender blender,
         @NotNull ColorModel sourceColorModel,
         @NotNull ColorModel destinationColorModel,
         boolean convertToLinearRGB
      ) {
         this.blender = blender;
         this.convertToLinearRGB = convertToLinearRGB;
         this.sourceColorModel = sourceColorModel;
         this.destinationColorModel = destinationColorModel;
      }

      @Override
      public void dispose() {
      }

      @Override
      public void compose(@NotNull Raster src, @NotNull Raster dstIn, @NotNull WritableRaster dstOut) {
         ColorModel srcPre = this.sourceColorModel;
         if (!this.sourceColorModel.isAlphaPremultiplied()) {
            srcPre = ImageUtil.coerceData((WritableRaster)src, this.sourceColorModel, true);
         }

         ColorModel dstInPre = this.destinationColorModel;
         if (!this.destinationColorModel.isAlphaPremultiplied()) {
            dstInPre = ImageUtil.coerceData((WritableRaster)dstIn, this.destinationColorModel, true);
         }

         int width = Math.min(src.getWidth(), dstIn.getWidth());
         int height = Math.min(src.getHeight(), dstIn.getHeight());
         int[] result = new int[4];
         int[] srcPixel = new int[4];
         int[] dstPixel = new int[4];
         int[] srcPixels = new int[width];
         int[] dstPixels = new int[width];
         int minY = dstOut.getMinY();
         int maxY = minY + height;

         for (int y = minY; y < maxY; y++) {
            src.getDataElements(dstOut.getMinX(), y, width, 1, srcPixels);
            dstIn.getDataElements(dstOut.getMinX(), y, width, 1, dstPixels);

            for (int x = 0; x < width; x++) {
               int pixel = srcPixels[x];
               srcPixel[0] = pixel >> 16 & 0xFF;
               srcPixel[1] = pixel >> 8 & 0xFF;
               srcPixel[2] = pixel & 0xFF;
               srcPixel[3] = pixel >> 24 & 0xFF;
               pixel = dstPixels[x];
               dstPixel[0] = pixel >> 16 & 0xFF;
               dstPixel[1] = pixel >> 8 & 0xFF;
               dstPixel[2] = pixel & 0xFF;
               dstPixel[3] = pixel >> 24 & 0xFF;
               if (this.convertToLinearRGB) {
                  ColorUtil.sRGBtoLinearRGBPreInPlace(srcPixel);
                  ColorUtil.sRGBtoLinearRGBPreInPlace(dstPixel);
                  this.blender.blend(srcPixel, dstPixel, result);
                  ColorUtil.linearRGBtoSRGBPreInPlace(result);
               } else {
                  this.blender.blend(srcPixel, dstPixel, result);
               }

               dstPixels[x] = (result[3] & 0xFF) << 24 | (result[0] & 0xFF) << 16 | (result[1] & 0xFF) << 8 | result[2] & 0xFF;
            }

            dstOut.setDataElements(0, y, width, 1, dstPixels);
         }

         if (!this.sourceColorModel.isAlphaPremultiplied()) {
            ImageUtil.coerceData((WritableRaster)src, srcPre, false);
         }

         if (!this.destinationColorModel.isAlphaPremultiplied()) {
            ImageUtil.coerceData(dstOut, dstInPre, false);
            if (dstIn != dstOut) {
               ImageUtil.coerceData((WritableRaster)dstIn, dstInPre, false);
            }
         }
      }
   }
}

