package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.geometry.size.Length;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class LengthFontSize implements FontSize {
   @NotNull
   private final Length size;

   public LengthFontSize(@NotNull Length size) {
      this.size = size;
   }

   @NotNull
   @Override
   public Length size(@NotNull Length parentSize) {
      return this.size;
   }

   @Override
   public String toString() {
      return "LengthFontSize{size=" + this.size + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof LengthFontSize)) {
         return false;
      }

      LengthFontSize that = (LengthFontSize)o;
      return this.size.equals(that.size);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.size);
   }
}

