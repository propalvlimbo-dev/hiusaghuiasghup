package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.util.ImageUtil;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;

final class InplaceBoxBlurFilter {
   private InplaceBoxBlurFilter() {
   }

   public static void horizontalPass(@NotNull Raster src, @NotNull WritableRaster dst, int skipX, int skipY, int boxSize, int loc) {
      int width = src.getWidth();
      int height = src.getHeight();
      if (width >= 2 * skipX + boxSize) {
         if (height >= 2 * skipY) {
            int srcScanStride = ImageUtil.getINT_RGBA_ScanlineStride(src);
            int dstScanStride = ImageUtil.getINT_RGBA_ScanlineStride(dst);
            int srcOff = ImageUtil.getINT_RGBA_DataOffset(src);
            int dstOff = ImageUtil.getINT_RGBA_DataOffset(dst);
            int[] srcPixels = ImageUtil.getINT_RGBA_DataBank(src);
            int[] dstPixels = ImageUtil.getINT_RGBA_DataBank(dst);
            int[] buffer = new int[boxSize];
            int scale = 16777216 / boxSize;

            for (int y = skipY; y < height - skipY; y++) {
               int sp = srcOff + y * srcScanStride;
               int dp = dstOff + y * dstScanStride;
               int rowEnd = sp + (width - skipX);
               int k = 0;
               int sumA = 0;
               int sumR = 0;
               int sumG = 0;
               int sumB = 0;
               sp += skipX;

               for (int end = sp + boxSize; sp < end; sp++) {
                  int curr = buffer[k] = srcPixels[sp];
                  sumA += curr >>> 24;
                  sumR += curr >> 16 & 0xFF;
                  sumG += curr >> 8 & 0xFF;
                  sumB += curr & 0xFF;
                  k++;
               }

               dp += skipX + loc;
               int prev = dstPixels[dp] = sumA * scale & 0xFF000000
                  | (sumR * scale & 0xFF000000) >>> 8
                  | (sumG * scale & 0xFF000000) >>> 16
                  | (sumB * scale & 0xFF000000) >>> 24;
               dp++;
               k = 0;

               while (sp < rowEnd) {
                  int curr = buffer[k];
                  if (curr == srcPixels[sp]) {
                     dstPixels[dp] = prev;
                  } else {
                     int var34 = sumA - (curr >>> 24);
                     int var35 = sumR - (curr >> 16 & 0xFF);
                     int var36 = sumG - (curr >> 8 & 0xFF);
                     int var37 = sumB - (curr & 0xFF);
                     curr = buffer[k] = srcPixels[sp];
                     sumA = var34 + (curr >>> 24);
                     sumR = var35 + (curr >> 16 & 0xFF);
                     sumG = var36 + (curr >> 8 & 0xFF);
                     sumB = var37 + (curr & 0xFF);
                     prev = dstPixels[dp] = sumA * scale & 0xFF000000
                        | (sumR * scale & 0xFF000000) >>> 8
                        | (sumG * scale & 0xFF000000) >>> 16
                        | (sumB * scale & 0xFF000000) >>> 24;
                  }

                  k = (k + 1) % boxSize;
                  sp++;
                  dp++;
               }
            }
         }
      }
   }

   public static void verticalPass(@NotNull Raster src, @NotNull WritableRaster dst, int skipX, int skipY, int boxSize, int loc) {
      int w = src.getWidth();
      int h = src.getHeight();
      if (w >= 2 * skipX) {
         if (h >= 2 * skipY + boxSize) {
            int srcScanStride = ImageUtil.getINT_RGBA_ScanlineStride(src);
            int dstScanStride = ImageUtil.getINT_RGBA_ScanlineStride(dst);
            int srcOff = ImageUtil.getINT_RGBA_DataOffset(src);
            int dstOff = ImageUtil.getINT_RGBA_DataOffset(dst);
            int[] srcPixels = ImageUtil.getINT_RGBA_DataBank(src);
            int[] dstPixels = ImageUtil.getINT_RGBA_DataBank(dst);
            int[] buffer = new int[boxSize];
            int scale = 16777216 / boxSize;

            for (int x = skipX; x < w - skipX; x++) {
               int sp = srcOff + x;
               int dp = dstOff + x;
               int colEnd = sp + (h - skipY) * srcScanStride;
               int k = 0;
               int sumA = 0;
               int sumR = 0;
               int sumG = 0;
               int sumB = 0;
               sp += skipY * srcScanStride;

               for (int end = sp + boxSize * srcScanStride; sp < end; sp += srcScanStride) {
                  int curr = buffer[k] = srcPixels[sp];
                  sumA += curr >>> 24;
                  sumR += curr >> 16 & 0xFF;
                  sumG += curr >> 8 & 0xFF;
                  sumB += curr & 0xFF;
                  k++;
               }

               dp += (skipY + loc) * dstScanStride;
               int prev = dstPixels[dp] = sumA * scale & 0xFF000000
                  | (sumR * scale & 0xFF000000) >>> 8
                  | (sumG * scale & 0xFF000000) >>> 16
                  | (sumB * scale & 0xFF000000) >>> 24;
               dp += dstScanStride;
               k = 0;

               while (sp < colEnd) {
                  int curr = buffer[k];
                  if (curr == srcPixels[sp]) {
                     dstPixels[dp] = prev;
                  } else {
                     int var34 = sumA - (curr >>> 24);
                     int var35 = sumR - (curr >> 16 & 0xFF);
                     int var36 = sumG - (curr >> 8 & 0xFF);
                     int var37 = sumB - (curr & 0xFF);
                     curr = buffer[k] = srcPixels[sp];
                     sumA = var34 + (curr >>> 24);
                     sumR = var35 + (curr >> 16 & 0xFF);
                     sumG = var36 + (curr >> 8 & 0xFF);
                     sumB = var37 + (curr & 0xFF);
                     prev = dstPixels[dp] = sumA * scale & 0xFF000000
                        | (sumR * scale & 0xFF000000) >>> 8
                        | (sumG * scale & 0xFF000000) >>> 16
                        | (sumB * scale & 0xFF000000) >>> 24;
                  }

                  k = (k + 1) % boxSize;
                  sp += srcScanStride;
                  dp += dstScanStride;
               }
            }
         }
      }
   }
}

