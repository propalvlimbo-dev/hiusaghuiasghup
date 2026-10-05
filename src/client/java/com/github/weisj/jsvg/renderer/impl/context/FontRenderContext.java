package com.github.weisj.jsvg.renderer.impl.context;

import com.github.weisj.jsvg.attributes.text.DominantBaseline;
import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FontRenderContext {
   @Nullable
   private final Length letterSpacing;
   @Nullable
   private final DominantBaseline dominantBaseline;
   @Nullable
   private final TextAnchor textAnchor;

   public FontRenderContext(@Nullable Length letterSpacing, @Nullable DominantBaseline dominantBaseline, @Nullable TextAnchor textAnchor) {
      this.letterSpacing = letterSpacing;
      this.dominantBaseline = dominantBaseline;
      this.textAnchor = textAnchor;
   }

   @NotNull
   public Length letterSpacing() {
      return this.letterSpacing != null ? this.letterSpacing : Length.ZERO;
   }

   @NotNull
   public TextAnchor textAnchor() {
      return this.textAnchor != null ? this.textAnchor : TextAnchor.Start;
   }

   @NotNull
   public DominantBaseline dominantBaseline() {
      return this.dominantBaseline != null ? this.dominantBaseline : DominantBaseline.Auto;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof FontRenderContext)) {
         return false;
      }

      FontRenderContext that = (FontRenderContext)o;
      return Objects.equals(this.letterSpacing, that.letterSpacing) && this.dominantBaseline == that.dominantBaseline && this.textAnchor == that.textAnchor;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.letterSpacing, this.dominantBaseline, this.textAnchor);
   }

   @NotNull
   public static FontRenderContext createDefault() {
      return new FontRenderContext(null, null, null);
   }

   @NotNull
   public static FontRenderContext parse(@NotNull AttributeNode attributeNode) {
      DominantBaseline dominantBaseline = attributeNode.getEnum("dominant-baseline", DominantBaseline.Auto);
      if (dominantBaseline == DominantBaseline.Auto && attributeNode.tagIsOneOf("textpath", "tspan")) {
         dominantBaseline = null;
      }

      return new FontRenderContext(
         attributeNode.getLength("letter-spacing", PercentageDimension.NONE), dominantBaseline, attributeNode.getEnumNullable("text-anchor", TextAnchor.class)
      );
   }

   @NotNull
   public FontRenderContext derive(@Nullable FontRenderContext frc) {
      return frc != null && !frc.equals(this)
         ? new FontRenderContext(
            frc.letterSpacing != null ? frc.letterSpacing : this.letterSpacing,
            frc.dominantBaseline != null ? frc.dominantBaseline : this.dominantBaseline,
            frc.textAnchor != null ? frc.textAnchor : this.textAnchor
         )
         : this;
   }

   @Override
   public String toString() {
      return "FontRenderContext{letterSpacing=" + this.letterSpacing + ", dominantBaseline=" + this.dominantBaseline + ", textAnchor=" + this.textAnchor + '}';
   }
}

