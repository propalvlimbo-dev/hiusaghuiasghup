package com.github.weisj.jsvg.geometry.size;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class Angle {
   public static final float UNSPECIFIED_RAW = Float.NaN;
   @NotNull
   public static final Angle UNSPECIFIED = new Angle(AngleUnit.Raw, Float.NaN);
   @NotNull
   public static final Angle ZERO = new Angle(AngleUnit.Raw, 0.0F);
   private final float radian;

   public Angle(AngleUnit unit, float value) {
      this.radian = unit.toRadians(value);
   }

   public static boolean isUnspecified(float value) {
      return Float.isNaN(value);
   }

   public static boolean isSpecified(float value) {
      return !isUnspecified(value);
   }

   public float radians() {
      return this.radian;
   }

   public boolean isUnspecified() {
      return isUnspecified(this.radian);
   }

   public boolean isSpecified() {
      return !this.isUnspecified();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Angle angle = (Angle)o;
         return Float.compare(this.radian, angle.radian) == 0;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.radian);
   }

   @Override
   public String toString() {
      return "Angle{radian=" + this.radian + '}';
   }
}

