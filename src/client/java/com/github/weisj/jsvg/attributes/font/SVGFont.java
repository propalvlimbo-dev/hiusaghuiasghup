package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.nodes.text.Glyph;
import org.jetbrains.annotations.NotNull;

public interface SVGFont {
   static float defaultFontSize() {
      return 10.0F;
   }

   @NotNull
   static String defaultFontFamily() {
      return "Serif";
   }

   static float exFromEm(float em) {
      return em / 2.0F;
   }

   static float emFromEx(float ex) {
      return 2.0F * ex;
   }

   @NotNull
   Glyph codepointGlyph(@NotNull String var1);

   @NotNull
   String family();

   int size();

   float effectiveExHeight();

   float effectiveEmHeight();

   float mathematicalBaseline();

   float hangingBaseline();

   float romanBaseline();

   float centerBaseline();

   float middleBaseline();

   float textUnderBaseline();

   float textOverBaseline();
}
