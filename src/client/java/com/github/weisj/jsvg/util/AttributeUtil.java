package com.github.weisj.jsvg.util;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AttributeUtil {
   private AttributeUtil() {
   }

   public static boolean isBlank(@NotNull String s) {
      for (int i = 0; i < s.length(); i++) {
         if (!Character.isWhitespace(s.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   @SafeVarargs
   @NotNull
   public static <T> T @Nullable [] toNonnullArray(@Nullable T... values) {
      if (values == null) {
         return null;
      }

      for (T value : values) {
         if (value == null) {
            return null;
         }
      }

      return values;
   }

   @NotNull
   public static <T> T notNullOrElse(@Nullable T value, @NotNull T defaultValue) {
      return value != null ? value : defaultValue;
   }

   @NotNull
   public static AttributeUtil.AxisPair parseAxisPair(
      @NotNull AttributeNode node,
      @NotNull String xAttr,
      @NotNull String yAttr,
      @NotNull Length fallback,
      Inherited inherited,
      @NotNull Function<LengthValue, @Nullable LengthValue> validator
   ) {
      LengthValue initialRx = node.getLength(xAttr, PercentageDimension.WIDTH, Length.UNSPECIFIED, inherited, Animatable.NO);
      initialRx = validator.apply(initialRx);
      if (initialRx == null) {
         initialRx = Length.UNSPECIFIED;
      }

      LengthValue initialRy = node.getLength(yAttr, PercentageDimension.HEIGHT, initialRx, inherited, Animatable.NO);
      initialRy = validator.apply(initialRy);
      if (initialRy == null) {
         initialRy = Length.UNSPECIFIED;
      }

      LengthValue rx;
      LengthValue ry;
      if (initialRx == Length.UNSPECIFIED && initialRy == Length.UNSPECIFIED) {
         rx = notNullOrElse(node.getAnimatedLength(xAttr, fallback, PercentageDimension.WIDTH), initialRx);
         ry = notNullOrElse(node.getAnimatedLength(yAttr, fallback, PercentageDimension.HEIGHT), initialRy);
         if (rx == Length.UNSPECIFIED) {
            rx = ry;
         } else if (ry == Length.UNSPECIFIED) {
            ry = rx;
         }
      } else if (initialRx == Length.UNSPECIFIED) {
         ry = notNullOrElse(node.getAnimatedLength(yAttr, initialRy, PercentageDimension.HEIGHT), initialRy);
         rx = notNullOrElse(node.getAnimatedLength(xAttr, ry, PercentageDimension.WIDTH), ry);
      } else if (initialRy == Length.UNSPECIFIED) {
         rx = notNullOrElse(node.getAnimatedLength(xAttr, initialRx, PercentageDimension.WIDTH), initialRx);
         ry = notNullOrElse(node.getAnimatedLength(yAttr, rx, PercentageDimension.HEIGHT), rx);
      } else {
         rx = notNullOrElse(node.getAnimatedLength(xAttr, initialRx, PercentageDimension.WIDTH), initialRx);
         ry = notNullOrElse(node.getAnimatedLength(yAttr, initialRy, PercentageDimension.HEIGHT), initialRy);
      }

      if (rx == Length.UNSPECIFIED) {
         rx = fallback;
      }

      if (ry == Length.UNSPECIFIED) {
         ry = fallback;
      }

      return new AttributeUtil.AxisPair(rx, ry);
   }

   @Contract(pure = true)
   public static <T> boolean arrayContains(T @NotNull [] arr, T element) {
      for (T t : arr) {
         if (Objects.equals(t, element)) {
            return true;
         }
      }

      return false;
   }

   public static final class AxisPair {
      @NotNull
      private final LengthValue xAxis;
      @NotNull
      private final LengthValue yAxis;

      public AxisPair(@NotNull LengthValue xAxis, @NotNull LengthValue yAxis) {
         this.xAxis = xAxis;
         this.yAxis = yAxis;
      }

      @NotNull
      public LengthValue xAxis() {
         return this.xAxis;
      }

      @NotNull
      public LengthValue yAxis() {
         return this.yAxis;
      }
   }
}

