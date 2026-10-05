package com.github.weisj.jsvg.geometry.size;

import com.github.weisj.jsvg.attributes.SuffixUnit;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

public enum Unit implements SuffixUnit<Unit, Length> {
   PX,
   CM,
   Q,
   MM,
   IN,
   EM,
   REM,
   EX,
   CH,
   PT,
   PC,
   VW,
   VH,
   VI,
   VB,
   V_MIN("vmin"),
   V_MAX("vmax"),
   PERCENTAGE("%"),
   PERCENTAGE_LENGTH("%"),
   PERCENTAGE_WIDTH("%"),
   PERCENTAGE_HEIGHT("%"),
   RAW("");

   private static final Unit[] units = values();
   @NotNull
   private final String suffix;

   @NotNull
   public Unit[] units() {
      return units;
   }

   Unit(@NotNull String suffix) {
      this.suffix = suffix;
   }

   Unit() {
      this.suffix = this.name().toLowerCase(Locale.ENGLISH);
   }

   @NotNull
   public Length valueOf(float value) {
      return value == 0.0F ? Length.ZERO : new Length(this, value);
   }

   @NotNull
   @Override
   public String suffix() {
      return this.suffix;
   }

   public boolean isPercentage() {
      switch (this) {
         case PERCENTAGE:
         case PERCENTAGE_LENGTH:
         case PERCENTAGE_WIDTH:
         case PERCENTAGE_HEIGHT:
            return true;
         default:
            return false;
      }
   }

   // $VF: synthetic method
   private static Unit[] $values() {
      return new Unit[]{
         PX, CM, Q, MM, IN, EM, REM, EX, CH, PT, PC, VW, VH, VI, VB, V_MIN, V_MAX, PERCENTAGE, PERCENTAGE_LENGTH, PERCENTAGE_WIDTH, PERCENTAGE_HEIGHT, RAW
      };
   }
}
