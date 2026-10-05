package com.github.weisj.jsvg.attributes;

import org.jetbrains.annotations.NotNull;

public enum ColorInterpolation implements HasMatchName {
   S_RGB("sRGB"),
   LinearRGB("linearRGB"),
   Auto("auto"),
   Inherit("inherit");

   @NotNull
   private final String matchName;

   ColorInterpolation(@NotNull String matchName) {
      this.matchName = matchName;
   }

   @NotNull
   @Override
   public String matchName() {
      return this.matchName;
   }

   // $VF: synthetic method
   private static ColorInterpolation[] $values() {
      return new ColorInterpolation[]{S_RGB, LinearRGB, Auto, Inherit};
   }
}
