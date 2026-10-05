package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FontSpec {
   @NotNull
   protected final String[] families;
   @Nullable
   protected final FontStyle style;
   @Nullable
   protected final Length sizeAdjust;
   @NotNull
   protected final Percentage stretch;

   FontSpec(@NotNull String[] families, @Nullable FontStyle style, @Nullable Length sizeAdjust, @NotNull Percentage stretch) {
      this.families = families;
      this.style = style;
      this.sizeAdjust = sizeAdjust;
      this.stretch = stretch;
   }

   @Override
   public String toString() {
      return "FontSpec{families="
         + Arrays.toString(this.families)
         + ", style="
         + this.style
         + ", sizeAdjust="
         + this.sizeAdjust
         + ", stretch="
         + this.stretch
         + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof FontSpec)) {
         return false;
      }

      FontSpec fontSpec = (FontSpec)o;
      return Objects.equals(this.stretch, fontSpec.stretch)
         && Arrays.equals(this.families, fontSpec.families)
         && Objects.equals(this.style, fontSpec.style)
         && Objects.equals(this.sizeAdjust, fontSpec.sizeAdjust);
   }

   @Override
   public int hashCode() {
      int result = Objects.hash(this.style, this.sizeAdjust, this.stretch);
      return 31 * result + Arrays.hashCode(this.families);
   }
}

