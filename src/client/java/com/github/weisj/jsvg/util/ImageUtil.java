package com.github.weisj.jsvg.util;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageProducer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ImageUtil {
   private ImageUtil() {
   }

   @NotNull
   public static BufferedImage createCompatibleTransparentImage(@NotNull Output output, double width, double height) {
      return createCompatibleTransparentImage(output.transform(), width, height);
   }

   @NotNull
   public static BufferedImage createCompatibleTransparentImage(int width, int height) {
      return createCompatibleTransparentImage(width, height, ImageUtil.Premultiplied.No);
   }

   @NotNull
   public static BufferedImage createCompatibleTransparentImage(int width, int height, ImageUtil.Premultiplied preMultiplied) {
      int type = preMultiplied == ImageUtil.Premultiplied.Yes ? 3 : 2;
      return new BufferedImage(width, height, type);
   }

   @NotNull
   public static BufferedImage createCompatibleTransparentImage(@Nullable AffineTransform at, double width, double height) {
      return createCompatibleTransparentImage(
         (int)Math.ceil(GeometryUtil.scaleXOfTransform(at) * width), (int)Math.ceil(GeometryUtil.scaleYOfTransform(at) * height)
      );
   }

   @NotNull
   public static BufferedImage createLuminosityBuffer(@Nullable AffineTransform at, double width, double height) {
      return new BufferedImage((int)Math.ceil(GeometryUtil.scaleXOfTransform(at) * width), (int)Math.ceil(GeometryUtil.scaleYOfTransform(at) * height), 10);
   }

   public static boolean is_INT_PACK_Data(@NotNull SampleModel sm, boolean requireAlpha) {
      if (!(sm instanceof SinglePixelPackedSampleModel)) {
         return false;
      }

      if (sm.getDataType() != 3) {
         return false;
      }

      int[] masks = ((SinglePixelPackedSampleModel)sm).getBitMasks();
      if (masks.length == 3) {
         if (requireAlpha) {
            return false;
         }
      } else if (masks.length != 4) {
         return false;
      }

      if (masks[0] != 16711680) {
         return false;
      } else if (masks[1] != 65280) {
         return false;
      } else {
         return masks[2] != 255 ? false : masks.length != 4 || masks[3] == -16777216;
      }
   }

   public static int[] getINT_RGBA_DataBank(@NotNull Raster raster) {
      DataBufferInt dstDB = (DataBufferInt)raster.getDataBuffer();
      return dstDB.getBankData()[0];
   }

   public static int getINT_RGBA_DataOffset(@NotNull Raster raster) {
      DataBufferInt dstDB = (DataBufferInt)raster.getDataBuffer();
      SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel)raster.getSampleModel();
      return dstDB.getOffset() + sppsm.getOffset(raster.getMinX() - raster.getSampleModelTranslateX(), raster.getMinY() - raster.getSampleModelTranslateY());
   }

   public static int getINT_RGBA_DataAdjust(@NotNull Raster raster) {
      return getINT_RGBA_ScanlineStride(raster) - raster.getWidth();
   }

   public static int getINT_RGBA_ScanlineStride(@NotNull Raster raster) {
      return ((SinglePixelPackedSampleModel)raster.getSampleModel()).getScanlineStride();
   }

   @NotNull
   public static BufferedImage copy(@NotNull RenderContext context, @NotNull ImageProducer producer, ImageUtil.Premultiplied premultiplied) {
      return toBufferedImage(context.platformSupport().createImage(producer), premultiplied);
   }

   @NotNull
   public static BufferedImage toBufferedImage(@NotNull Image img) {
      return toBufferedImage(img, ImageUtil.Premultiplied.No);
   }

   @NotNull
   public static BufferedImage toBufferedImage(@NotNull Image img, ImageUtil.Premultiplied premultiplied) {
      BufferedImage bufferedImage = createCompatibleTransparentImage(img.getWidth(null), img.getHeight(null), premultiplied);
      Graphics2D g = GraphicsUtil.createGraphics(bufferedImage);
      g.drawImage(img, null, null);
      g.dispose();
      return bufferedImage;
   }

   @NotNull
   public static ColorModel coerceData(@NotNull WritableRaster wr, @NotNull ColorModel cm, boolean newAlphaPreMultiplied) {
      if (!cm.hasAlpha()) {
         return cm;
      } else {
         return cm.isAlphaPremultiplied() == newAlphaPreMultiplied ? cm : cm.coerceData(wr, newAlphaPreMultiplied);
      }
   }

   public enum Premultiplied {
      Yes,
      No;

      // $VF: synthetic method
      private static ImageUtil.Premultiplied[] $values() {
         return new ImageUtil.Premultiplied[]{Yes, No};
      }
   }
}

