package com.github.weisj.jsvg.nodes.filter;

import java.awt.image.ColorModel;
import java.awt.image.RGBImageFilter;

final class AlphaImageFilter extends RGBImageFilter {
   private final ColorModel model = ColorModel.getRGBdefault();

   @Override
   public int filterRGB(int x, int y, int rgb) {
      return this.model.getAlpha(rgb) << 24;
   }
}

