package com.github.weisj.jsvg.attributes;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class Coordinate<T> {
   @NotNull
   private final T x;
   @NotNull
   private final T y;

   public Coordinate(@NotNull T x, @NotNull T y) {
      this.x = x;
      this.y = y;
   }

   @NotNull
   public T x() {
      return this.x;
   }

   @NotNull
   public T y() {
      return this.y;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Coordinate<?> that = (Coordinate<?>)o;
         return Objects.equals(this.x, that.x) && Objects.equals(this.y, that.y);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.x, this.y);
   }

   @Override
   public String toString() {
      return "Coordinate{x=" + this.x + ", y=" + this.y + '}';
   }
}

