package com.github.weisj.jsvg.util;

import java.awt.image.RGBImageFilter;

public abstract class ColorSpaceAwareRGBImageFilter extends RGBImageFilter {
   private final int[] tmp = new int[4];
   private boolean convertToLinear;

   protected int[] getRGB(int rgb) {
      this.tmp[3] = rgb >> 24 & 0xFF;
      this.tmp[2] = rgb >> 16 & 0xFF;
      this.tmp[1] = rgb >> 8 & 0xFF;
      this.tmp[0] = rgb & 0xFF;
      if (this.convertToLinear) {
         ColorUtil.sRGBtoLinearRGBinPlace(this.tmp);
      }

      return this.tmp;
   }

   protected int pack(int[] argb) {
      if (this.convertToLinear) {
         ColorUtil.linearRGBtoSRGBinPlace(argb);
      }

      return (argb[3] & 0xFF) << 24 | (argb[2] & 0xFF) << 16 | (argb[1] & 0xFF) << 8 | argb[0] & 0xFF;
   }

   public void setConvertToLinear(boolean convertToLinear) {
      this.convertToLinear = convertToLinear;
   }
}

