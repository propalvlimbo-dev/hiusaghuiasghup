package com.github.weisj.jsvg.animation.interpolation;

import com.github.weisj.jsvg.animation.Additive;
import com.github.weisj.jsvg.animation.AnimationValuesType;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.paint.impl.AwtSVGPaint;
import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultInterpolator implements FloatInterpolator, FloatListInterpolator, PaintInterpolator, TransformInterpolator {
   private final Additive additive;
   private final AnimationValuesType valuesType;

   public DefaultInterpolator(AnimationValuesType valuesType, Additive additive) {
      this.valuesType = valuesType;
      this.additive = additive;
   }

   @Override
   public float interpolate(float initial, float a, float b, float progress) {
      switch (this.valuesType) {
         case FROM_BY:
            return a + b * progress;
         case BY:
            return initial + b * progress;
         case TO:
            return initial + (b - initial) * progress;
         case FROM_TO:
         case VALUES:
         default:
            float result = a + (b - a) * progress;
            if (this.additive == Additive.SUM) {
               result += initial;
            }

            return result;
      }
   }

   private static float @NotNull [] ensureCacheCapacity(float @Nullable [] cache, int length) {
      return cache != null && cache.length == length ? cache : new float[length];
   }

   private static float @NotNull [] arrayLerp(float @Nullable [] cache, float @NotNull [] from, float @Nullable [] to, float progress) {
      float t = progress;
      boolean isEndTime = GeometryUtil.approximatelyEqual(t, 1.0);
      if (to == null || from.length != to.length && !isEndTime) {
         t = 0.0F;
      }

      float[] result = cache;
      if (to == null || GeometryUtil.approximatelyEqual(t, 0.0)) {
         result = ensureCacheCapacity(result, from.length);
         System.arraycopy(from, 0, result, 0, from.length);
      } else if (isEndTime) {
         result = ensureCacheCapacity(result, to.length);
         System.arraycopy(to, 0, result, 0, to.length);
      } else {
         result = ensureCacheCapacity(result, from.length);

         for (int i = 0; i < from.length; i++) {
            result[i] = GeometryUtil.lerp(t, from[i], to[i]);
         }
      }

      return result;
   }

   private static float @NotNull [] saxpy(float @Nullable [] cache, float @NotNull [] b, float @Nullable [] x, float a) {
      float[] result = ensureCacheCapacity(cache, b.length);
      System.arraycopy(b, 0, result, 0, b.length);
      if (x == null) {
         return b;
      }

      int n = Math.min(result.length, x.length);

      for (int i = 0; i < n; i++) {
         result[i] += a * x[i];
      }

      for (int i = n; i < result.length; i++) {
         result[i] += a * x[i % n];
      }

      return result;
   }

   @Override
   public float @NotNull [] interpolate(float @NotNull [] initial, float @NotNull [] a, float @Nullable [] b, float progress, float @Nullable [] cache) {
      switch (this.valuesType) {
         case FROM_BY:
            return saxpy(cache, a, b, progress);
         case BY:
            return saxpy(cache, initial, b, progress);
         case TO:
            return arrayLerp(cache, initial, b, progress);
         case FROM_TO:
         case VALUES:
         default:
            float[] result = arrayLerp(cache, a, b, progress);
            if (this.additive == Additive.SUM) {
               result = saxpy(result, result, initial, 1.0F);
            }

            return result;
      }
   }

   @Nullable
   private static RGBColor extractColor(@NotNull SVGPaint p) {
      if (!(p instanceof SimplePaintSVGPaint)) {
         return null;
      } else {
         Paint paint = ((SimplePaintSVGPaint)p).paint();
         if (paint instanceof Color) {
            return new RGBColor((Color)paint);
         } else {
            return paint instanceof RGBColor ? (RGBColor)paint : null;
         }
      }
   }

   @NotNull
   @Override
   public SVGPaint interpolate(@NotNull SVGPaint initial, @NotNull SVGPaint a, @NotNull SVGPaint b, float progress) {
      RGBColor colorA = extractColor(a);
      RGBColor colorB = extractColor(b);
      if (colorA != null && colorB != null) {
         switch (this.valuesType) {
            case FROM_BY:
               return new AwtSVGPaint(RGBColor.saxpy(progress, colorA, colorB));
            case BY: {
               RGBColor initialColor = extractColor(initial);
               if (initialColor == null) {
                  return initial;
               }

               return new AwtSVGPaint(RGBColor.saxpy(progress, initialColor, colorB));
            }
            case TO: {
               RGBColor initialColor = extractColor(initial);
               if (initialColor == null) {
                  return initial;
               }

               return new AwtSVGPaint(RGBColor.interpolate(progress, initialColor, colorB));
            }
            case FROM_TO:
            case VALUES:
            default:
               RGBColor result = RGBColor.interpolate(progress, colorA, colorB);
               if (this.additive == Additive.SUM) {
                  RGBColor initialColor = extractColor(initial);
                  if (initialColor == null) {
                     return initial;
                  }

                  result = RGBColor.add(initialColor, result);
               }

               return new AwtSVGPaint(result);
         }
      } else {
         return this.discreteAnimation(initial, a, b, progress);
      }
   }

   private SVGPaint discreteAnimation(@NotNull SVGPaint initial, @NotNull SVGPaint a, @NotNull SVGPaint b, float progress) {
      if (this.additive != Additive.REPLACE) {
         return initial;
      } else {
         return GeometryUtil.approximatelyEqual(progress, 1.0) ? b : a;
      }
   }

   @NotNull
   @Override
   public AffineTransform interpolate(
      @NotNull MeasureContext context, @NotNull TransformValue initial, @NotNull TransformPart a, @NotNull TransformPart b, float progress
   ) {
      switch (this.valuesType) {
         case FROM_BY:
            return b.applyToTransform(a.applyToTransform(new AffineTransform(), context), context, progress);
         case BY:
            return b.applyToTransform(initial.get(context), context, progress);
         case TO:
            return GeometryUtil.interpolate(initial.get(context), b.toTransform(context), progress);
         case FROM_TO:
         case VALUES:
         default:
            AffineTransform result = TransformPart.interpolate(a, b, context, progress);
            if (this.additive == Additive.SUM) {
               result.preConcatenate(initial.get(context));
            }

            return result;
      }
   }
}

