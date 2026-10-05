package com.github.weisj.jsvg.parser.css;

import com.google.errorprone.annotations.Immutable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

@Immutable
public final class StyleProperty {
   @NotNull
   private final String name;
   @NotNull
   private final String value;

   public StyleProperty(@NotNull String name, @NotNull String value) {
      this.name = name;
      this.value = value;
   }

   @NotNull
   public String name() {
      return this.name;
   }

   @NotNull
   public String value() {
      return this.value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         StyleProperty that = (StyleProperty)o;
         return this.name.equals(that.name) && this.value.equals(that.value);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.name, this.value);
   }

   @Override
   public String toString() {
      return "StyleProperty{name='" + this.name + '\'' + ", value='" + this.value + '\'' + '}';
   }
}

