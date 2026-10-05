package com.github.weisj.jsvg.renderer.impl.context;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.stroke.LineCap;
import com.github.weisj.jsvg.attributes.stroke.LineJoin;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import java.util.Arrays;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StrokeContext {
   @Nullable
   public final LengthValue strokeWidth;
   @Nullable
   public final LineCap lineCap;
   @Nullable
   public final LineJoin lineJoin;
   public final float miterLimit;
   public final Length @Nullable [] dashPattern;
   @Nullable
   public final LengthValue dashOffset;

   public StrokeContext(
      @Nullable LengthValue strokeWidth,
      @Nullable LineCap lineCap,
      @Nullable LineJoin lineJoin,
      float miterLimit,
      Length @Nullable [] dashPattern,
      @Nullable LengthValue dashOffset
   ) {
      this.strokeWidth = strokeWidth;
      this.lineCap = lineCap;
      this.lineJoin = lineJoin;
      this.miterLimit = miterLimit;
      this.dashPattern = validateDashPattern(dashPattern);
      this.dashOffset = dashOffset;
   }

   @Contract("null -> null")
   private static Length @Nullable [] validateDashPattern(Length @Nullable [] pattern) {
      if (pattern == null) {
         return null;
      }

      if (pattern.length == 0) {
         return pattern;
      }

      for (Length length : pattern) {
         if (length.raw() < 0.0F) {
            return new Length[0];
         }

         if (!length.isZero()) {
            return pattern;
         }
      }

      return new Length[0];
   }

   @NotNull
   public StrokeContext derive(@Nullable StrokeContext context) {
      if (context == null) {
         return this;
      } else {
         return context.isTrivial()
            ? this
            : new StrokeContext(
               LengthValue.derive(this.strokeWidth, context.strokeWidth),
               context.lineCap != null ? context.lineCap : this.lineCap,
               context.lineJoin != null ? context.lineJoin : this.lineJoin,
               Length.isSpecified(context.miterLimit) ? context.miterLimit : this.miterLimit,
               context.dashPattern != null ? context.dashPattern : this.dashPattern,
               context.dashOffset != null ? context.dashOffset : this.dashOffset
            );
      }
   }

   public boolean isTrivial() {
      return this.strokeWidth == null
         && this.lineCap == null
         && this.lineJoin == null
         && Length.isUnspecified(this.miterLimit)
         && this.dashPattern == null
         && this.dashOffset == null;
   }

   @NotNull
   public static StrokeContext createDefault() {
      return new StrokeContext(Unit.RAW.valueOf(1.0F), LineCap.Butt, LineJoin.Miter, 4.0F, new Length[0], Length.ZERO);
   }

   @NotNull
   public static StrokeContext parse(@NotNull AttributeNode attributeNode) {
      return new StrokeContext(
         attributeNode.getLength("stroke-width", PercentageDimension.LENGTH, Inherited.YES, Animatable.YES),
         attributeNode.getEnumNullable("stroke-linecap", LineCap.class),
         attributeNode.getEnumNullable("stroke-linejoin", LineJoin.class),
         attributeNode.getNonNegativeFloat("stroke-miterlimit", Float.NaN),
         attributeNode.getLengthList("stroke-dasharray", null, PercentageDimension.LENGTH),
         attributeNode.getLength("stroke-dashoffset", PercentageDimension.CUSTOM, Inherited.YES, Animatable.YES)
      );
   }

   @Override
   public String toString() {
      return "StrokeContext{strokeWidth="
         + this.strokeWidth
         + ", lineCap="
         + this.lineCap
         + ", lineJoin="
         + this.lineJoin
         + ", miterLimit="
         + this.miterLimit
         + ", dashPattern="
         + Arrays.toString(this.dashPattern)
         + ", dashOffset="
         + this.dashOffset
         + '}';
   }
}

