package com.github.weisj.jsvg.attributes.font;

import com.google.errorprone.annotations.Immutable;

@Immutable
public final class NumberFontWeight implements FontWeight {
   private final float weight;

   public NumberFontWeight(float weight) {
      this.weight = weight;
   }

   @Override
   public int weight(int parentWeight) {
      return (int)this.weight;
   }

   @Override
   public String toString() {
      return "NumberFontWeight{weight=" + this.weight + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof NumberFontWeight)) {
         return false;
      }

      NumberFontWeight that = (NumberFontWeight)o;
      return this.weight == that.weight;
   }

   @Override
   public int hashCode() {
      return Float.hashCode(this.weight);
   }
}

