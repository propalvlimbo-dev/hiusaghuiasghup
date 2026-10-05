package com.github.weisj.jsvg.attributes.filter;

import org.jetbrains.annotations.NotNull;

public enum DefaultFilterChannel implements FilterChannelKey {
   SourceGraphic,
   SourceAlpha,
   BackgroundImage,
   BackgroundAlpha,
   FillPaint,
   StrokePaint,
   LastResult;

   @NotNull
   @Override
   public Object key() {
      return this == LastResult ? this : this.toString();
   }

   // $VF: synthetic method
   private static DefaultFilterChannel[] $values() {
      return new DefaultFilterChannel[]{SourceGraphic, SourceAlpha, BackgroundImage, BackgroundAlpha, FillPaint, StrokePaint, LastResult};
   }
}
