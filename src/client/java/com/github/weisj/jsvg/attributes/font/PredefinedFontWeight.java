package com.github.weisj.jsvg.attributes.font;

import com.google.errorprone.annotations.Immutable;

@Immutable
public enum PredefinedFontWeight implements FontWeight {
   Normal {
      @Override
      public int weight(int parentWeight) {
         return 400;
      }
   },
   Bold {
      @Override
      public int weight(int parentWeight) {
         return 700;
      }
   },
   Bolder {
      @Override
      public int weight(int parentWeight) {
         if (parentWeight < 400) {
            return 400;
         } else {
            return parentWeight < 600 ? 600 : Math.max(parentWeight, 900);
         }
      }
   },
   Lighter {
      @Override
      public int weight(int parentWeight) {
         if (parentWeight > 700) {
            return 700;
         } else {
            return parentWeight > 500 ? 400 : Math.min(parentWeight, 100);
         }
      }
   },
   Number {
      @Override
      public int weight(int parentWeight) {
         throw new UnsupportedOperationException("Number needs to be parsed explicitly");
      }
   };

   public static final int NORMAL_WEIGHT = 400;
   public static final int BOLD_WEIGHT = 700;

   PredefinedFontWeight() {
   }

   // $VF: synthetic method
   private static PredefinedFontWeight[] $values() {
      return new PredefinedFontWeight[]{Normal, Bold, Bolder, Lighter, Number};
   }
}
