package com.github.weisj.jsvg.geometry.size;

import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

@Immutable
public final class Percentage implements Comparable<Percentage>, PercentageValue {
   public static final float UNSPECIFIED_RAW = Float.NaN;
   @NotNull
   public static final Percentage UNSPECIFIED = new Percentage(Float.NaN);
   @NotNull
   public static final Percentage ZERO = new Percentage(0.0F);
   @NotNull
   public static final Percentage ONE = new Percentage(1.0F);
   @NotNull
   public static final Percentage INHERITED = new Percentage(1.0F);
   private final float value;

   public Percentage(float value) {
      this.value = value;
   }

   public static boolean isUnspecified(float value) {
      return Float.isNaN(value);
   }

   public static boolean isSpecified(float value) {
      return !isUnspecified(value);
   }

   public float value() {
      return this.value;
   }

   @Override
   public float get(@NotNull MeasureContext context) {
      return this.value;
   }

   @NotNull
   @Override
   public PercentageValue multiply(@NotNull PercentageValue other) {
      if (this.value == 1.0F) {
         return other;
      }

      if (other instanceof Percentage) {
         float otherValue = ((Percentage)other).value;
         if (otherValue == 0.0F || this.value == 0.0F) {
            return ZERO;
         } else {
            return otherValue == 1.0F ? this : new Percentage(this.value * otherValue);
         }
      } else {
         return other.multiply(this);
      }
   }

   public boolean isUnspecified() {
      return isUnspecified(this.value);
   }

   public boolean isSpecified() {
      return !this.isUnspecified();
   }

   @NotNull
   public Percentage orElseIfUnspecified(float value) {
      return this.isUnspecified() ? new Percentage(value) : this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Percentage percentage = (Percentage)o;
         return Float.compare(this.value, percentage.value) == 0;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.value);
   }

   @Override
   public String toString() {
      return this.value * 100.0F + "%";
   }

   public int compareTo(@NotNull Percentage o) {
      return Float.compare(this.value, o.value);
   }
}

