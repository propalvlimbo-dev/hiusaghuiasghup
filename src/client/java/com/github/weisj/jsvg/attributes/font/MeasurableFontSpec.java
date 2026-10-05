package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MeasurableFontSpec extends FontSpec {
   private final int currentWeight;
   @NotNull
   private final Length currentSize;

   MeasurableFontSpec(
      @NotNull String[] families,
      @Nullable FontStyle style,
      @Nullable Length sizeAdjust,
      @NotNull Percentage stretch,
      int currentWeight,
      @NotNull Length currentSize
   ) {
      super(families, style, sizeAdjust, stretch);
      this.currentWeight = currentWeight;
      this.currentSize = currentSize;
   }

   @NotNull
   public static MeasurableFontSpec createDefault() {
      return new MeasurableFontSpec(
         new String[]{SVGFont.defaultFontFamily()}, FontStyle.normal(), null, FontStretch.Normal.percentage(), 400, Unit.RAW.valueOf(SVGFont.defaultFontSize())
      );
   }

   @NotNull
   public String[] families() {
      return this.families;
   }

   @NotNull
   public FontStyle style() {
      assert this.style != null;
      return this.style;
   }

   @NotNull
   public Percentage stretch() {
      return this.stretch;
   }

   public int currentWeight() {
      return this.currentWeight;
   }

   public float emSize(@NotNull MeasureContext context) {
      return this.currentSize.resolveFontSize(context);
   }

   public float effectiveSize(@NotNull MeasureContext context) {
      float emSize = this.emSize(context);
      return this.sizeAdjust != null ? SVGFont.emFromEx(emSize * this.sizeAdjust.resolveFontSize(context)) : emSize;
   }

   @NotNull
   public MeasurableFontSpec withFontSize(@Nullable FontSize size, @Nullable Length sizeAdjust) {
      return size == null && sizeAdjust == null
         ? this
         : new MeasurableFontSpec(
            this.families,
            this.style,
            sizeAdjust != null ? sizeAdjust : this.sizeAdjust,
            this.stretch,
            this.currentWeight,
            size != null ? size.size(this.currentSize) : this.currentSize
         );
   }

   @NotNull
   public MeasurableFontSpec derive(@Nullable AttributeFontSpec other) {
      if (other == null) {
         return this;
      }

      String[] newFamilies = other.families != null && other.families.length > 0 ? other.families : this.families;
      FontStyle newStyle = other.style != null ? other.style : this.style;
      int newWeight = other.weight() != null ? other.weight().weight(this.currentWeight) : this.currentWeight;
      Length newSize = other.size() != null ? other.size().size(this.currentSize) : this.currentSize;
      Length newSizeAdjust = other.sizeAdjust != null ? other.sizeAdjust : this.sizeAdjust;
      Percentage newStretch = other.stretch.isSpecified() ? other.stretch : this.stretch;
      return new MeasurableFontSpec(newFamilies, newStyle, newSizeAdjust, newStretch, newWeight, newSize);
   }

   @Override
   public String toString() {
      return "MeasurableFontSpec{families="
         + Arrays.toString(this.families)
         + ", style="
         + this.style
         + ", sizeAdjust="
         + this.sizeAdjust
         + ", stretch="
         + this.stretch
         + ", currentWeight="
         + this.currentWeight
         + ", currentSize="
         + this.currentSize
         + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof MeasurableFontSpec)) {
         return false;
      }

      if (!super.equals(o)) {
         return false;
      }

      MeasurableFontSpec fontSpec = (MeasurableFontSpec)o;
      return this.currentWeight == fontSpec.currentWeight && this.currentSize.equals(fontSpec.currentSize);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), this.currentWeight, this.currentSize);
   }
}

