package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.attributes.HasMatchName;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import org.jetbrains.annotations.NotNull;

public enum PredefinedFontSize implements HasMatchName, FontSize {
   xxSmall("xx-small", 0.6F),
   xSmall("x-small", 0.75F),
   small(0.8888889F),
   medium(1.0F),
   large(1.2F),
   xLarge("x-large", 1.5F),
   xxLarge("xx-large", 2.0F),
   xxxLarge("xxx-large", 3.0F),
   larger(1.3F),
   smaller(0.7F),
   Number(0.0F);

   @NotNull
   private final String matchName;
   private final float scalingFactor;

   PredefinedFontSize(@NotNull String matchName, float scalingFactor) {
      this.matchName = matchName;
      this.scalingFactor = scalingFactor;
   }

   PredefinedFontSize(float scalingFactor) {
      this.scalingFactor = scalingFactor;
      this.matchName = this.name();
   }

   @NotNull
   @Override
   public String matchName() {
      return this.matchName;
   }

   @NotNull
   @Override
   public Length size(@NotNull Length parentSize) {
      if (this == Number) {
         throw new UnsupportedOperationException("Number font-size needs to parsed explicitly");
      } else {
         return this != smaller && this != larger ? Unit.RAW.valueOf(SVGFont.defaultFontSize() * this.scalingFactor) : parentSize.multiply(this.scalingFactor);
      }
   }

   // $VF: synthetic method
   private static PredefinedFontSize[] $values() {
      return new PredefinedFontSize[]{xxSmall, xSmall, small, medium, large, xLarge, xxLarge, xxxLarge, larger, smaller, Number};
   }
}
