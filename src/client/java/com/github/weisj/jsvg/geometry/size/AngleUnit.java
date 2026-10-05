package com.github.weisj.jsvg.geometry.size;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;

public enum AngleUnit {
   Deg,
   Grad,
   Rad,
   Turn,
   Raw("");

   private static final AngleUnit[] units = values();
   private static final double GRADIANS_TO_RADIANS = 0.015707962916848627;
   @NotNull
   private final String suffix;

   public static AngleUnit[] units() {
      return units;
   }

   AngleUnit(@NotNull String suffix) {
      this.suffix = suffix;
   }

   AngleUnit() {
      this.suffix = this.name().toLowerCase(Locale.ENGLISH);
   }

   @NotNull
   public String suffix() {
      return this.suffix;
   }

   public float toRadians(float value) {
      switch (this) {
         case Deg:
         case Raw:
            return (float)Math.toRadians(value);
         case Grad:
            return (float)(value * 0.015707962916848627);
         case Rad:
            return value;
         case Turn:
            return (float)(value * Math.PI * 2.0);
         default:
            throw new IllegalArgumentException("Unknown angle unit " + this);
      }
   }

   // $VF: synthetic method
   private static AngleUnit[] $values() {
      return new AngleUnit[]{Deg, Grad, Rad, Turn, Raw};
   }
}
