package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AttributeFontSpec extends FontSpec implements Mutator<MeasurableFontSpec> {
   @Nullable
   private final FontSize size;
   @Nullable
   private final FontWeight weight;

   AttributeFontSpec(
      @NotNull String[] families,
      @Nullable FontStyle style,
      @Nullable Length sizeAdjust,
      @NotNull Percentage stretch,
      @Nullable FontSize size,
      @Nullable FontWeight weight
   ) {
      super(families, style, sizeAdjust, stretch);
      this.size = size;
      this.weight = weight;
   }

   @Nullable
   public FontWeight weight() {
      return this.weight;
   }

   @Nullable
   public FontSize size() {
      return this.size;
   }

   @NotNull
   public MeasurableFontSpec mutate(@NotNull MeasurableFontSpec element) {
      return element.derive(this);
   }

   @Override
   public String toString() {
      return "AttributeFontSpec{families="
         + Arrays.toString(this.families)
         + ", style="
         + this.style
         + ", weight="
         + this.weight
         + ", size="
         + this.size
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

      if (!(o instanceof AttributeFontSpec)) {
         return false;
      }

      if (!super.equals(o)) {
         return false;
      }

      AttributeFontSpec fontSpec = (AttributeFontSpec)o;
      return Objects.equals(this.size, fontSpec.size) && Objects.equals(this.weight, fontSpec.weight);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), this.size, this.weight);
   }
}

