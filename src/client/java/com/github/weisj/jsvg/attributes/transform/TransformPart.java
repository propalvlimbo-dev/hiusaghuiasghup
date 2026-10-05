package com.github.weisj.jsvg.attributes.transform;

import com.github.weisj.jsvg.attributes.HasMatchName;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class TransformPart {
   private static final TransformPart IDENTITY_MATRIX = new TransformPart(
      TransformPart.TransformType.MATRIX, new Length[]{Length.ONE, Length.ZERO, Length.ZERO, Length.ONE, Length.ZERO, Length.ZERO}
   );
   private static final TransformPart IDENTITY_TRANSLATE = new TransformPart(TransformPart.TransformType.TRANSLATE, new Length[]{Length.ZERO, Length.ZERO});
   private static final TransformPart IDENTITY_SCALE = new TransformPart(TransformPart.TransformType.SCALE, new Length[]{Length.ONE, Length.ONE});
   private static final TransformPart IDENTITY_ROTATE = new TransformPart(
      TransformPart.TransformType.ROTATE, new Length[]{Length.ZERO, Length.ZERO, Length.ZERO}
   );
   private static final TransformPart IDENTITY_SKEW = new TransformPart(TransformPart.TransformType.ROTATE, new Length[]{Length.ZERO, Length.ZERO});
   private final TransformPart.TransformType type;
   private final Length[] values;

   public TransformPart(TransformPart.TransformType type, @NotNull Length[] values) {
      this.type = type;
      this.values = values;
   }

   @NotNull
   public static TransformPart identityOfType(@NotNull TransformPart.TransformType type) {
      switch (type) {
         case MATRIX:
            return IDENTITY_MATRIX;
         case TRANSLATE:
         case TRANSLATE_X:
         case TRANSLATE_Y:
            return IDENTITY_TRANSLATE;
         case SCALE:
         case SCALE_X:
         case SCALE_Y:
            return IDENTITY_SCALE;
         case ROTATE:
            return IDENTITY_ROTATE;
         case SKEW:
         case SKEW_X:
         case SKEW_Y:
            return IDENTITY_SKEW;
         default:
            throw new IllegalArgumentException("Unknown transform type: " + type);
      }
   }

   private static float getEntry(@NotNull TransformPart part, int index, float fallback, @NotNull MeasureContext context) {
      return part.values.length > index ? part.values[index].resolve(context) : fallback;
   }

   @NotNull
   public static AffineTransform interpolate(@NotNull TransformPart a, @NotNull TransformPart b, @NotNull MeasureContext measureContext, float t) {
      TransformPart.TransformType aType = a.type.interpolationType();
      TransformPart.TransformType bType = b.type.interpolationType();
      if (aType != bType) {
         return GeometryUtil.interpolate(a.toTransform(measureContext), b.toTransform(measureContext), t);
      }

      switch (aType) {
         case MATRIX:
            return GeometryUtil.interpolate(a.toTransform(measureContext), b.toTransform(measureContext), t);
         case TRANSLATE:
         case TRANSLATE_X:
         case TRANSLATE_Y:
            return AffineTransform.getTranslateInstance(
               GeometryUtil.lerp(t, a.values[0].resolve(measureContext), b.values[0].resolve(measureContext)),
               GeometryUtil.lerp(t, getEntry(a, 1, 0.0F, measureContext), getEntry(b, 1, 0.0F, measureContext))
            );
         case SCALE:
         case SCALE_X:
         case SCALE_Y:
            float aScaleX = a.values[0].resolve(measureContext);
            float aScaleY = getEntry(a, 1, aScaleX, measureContext);
            float bScaleX = b.values[0].resolve(measureContext);
            float bScaleY = getEntry(a, 1, bScaleX, measureContext);
            return AffineTransform.getScaleInstance(GeometryUtil.lerp(t, aScaleX, bScaleX), GeometryUtil.lerp(t, aScaleY, bScaleY));
         case ROTATE:
            return AffineTransform.getRotateInstance(
               Math.toRadians(GeometryUtil.lerp(t, a.values[0].resolve(measureContext), b.values[0].resolve(measureContext))),
               GeometryUtil.lerp(t, getEntry(a, 1, 0.0F, measureContext), getEntry(b, 1, 0.0F, measureContext)),
               GeometryUtil.lerp(t, getEntry(a, 2, 0.0F, measureContext), getEntry(b, 2, 0.0F, measureContext))
            );
         case SKEW:
         case SKEW_X:
         case SKEW_Y:
            return AffineTransform.getShearInstance(
               Math.tan(Math.toRadians(GeometryUtil.lerp(t, a.values[0].resolve(measureContext), b.values[0].resolve(measureContext)))),
               Math.tan(Math.toRadians(GeometryUtil.lerp(t, getEntry(a, 1, 0.0F, measureContext), getEntry(b, 1, 0.0F, measureContext))))
            );
         default:
            throw new IllegalStateException();
      }
   }

   public boolean canBeFlattened() {
      for (Length value : this.values) {
         if (!value.isAbsolute()) {
            return false;
         }
      }

      return true;
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   public AffineTransform toTransform(@NotNull MeasureContext measureContext) {
      return this.applyToTransform(new AffineTransform(), measureContext);
   }

   @Contract(value = "_,_ -> param1", pure = true)
   @NotNull
   public AffineTransform applyToTransform(@NotNull AffineTransform transform, @NotNull MeasureContext measureContext) {
      return this.applyToTransform(transform, measureContext, 1.0F);
   }

   @Contract(value = "_,_,_ -> param1", pure = true)
   @NotNull
   public AffineTransform applyToTransform(@NotNull AffineTransform transform, @NotNull MeasureContext measureContext, float progress) {
      switch (this.type) {
         case MATRIX:
            transform.concatenate(
               new AffineTransform(
                  this.values[0].resolve(measureContext),
                  this.values[1].resolve(measureContext),
                  this.values[2].resolve(measureContext),
                  this.values[3].resolve(measureContext),
                  this.values[4].resolve(measureContext),
                  this.values[5].resolve(measureContext)
               )
            );
            break;
         case TRANSLATE:
            transform.translate(progress * this.values[0].resolve(measureContext), progress * this.values[1].resolve(measureContext));
            break;
         case TRANSLATE_X:
            transform.translate(this.values[0].resolve(measureContext), 0.0);
            break;
         case TRANSLATE_Y:
            transform.translate(0.0, this.values[0].resolve(measureContext));
            break;
         case SCALE:
            if (this.values.length == 1) {
               float scale = this.values[0].resolve(measureContext);
               transform.scale(scale, scale);
            } else {
               transform.scale(this.values[0].resolve(measureContext), this.values[1].resolve(measureContext));
            }
            break;
         case SCALE_X:
            transform.scale(this.values[0].resolve(measureContext), 1.0);
            break;
         case SCALE_Y:
            transform.scale(1.0, this.values[0].resolve(measureContext));
            break;
         case ROTATE:
            if (this.values.length == 1) {
               transform.rotate(Math.toRadians(this.values[0].resolve(measureContext)));
            } else {
               transform.rotate(
                  Math.toRadians(this.values[0].resolve(measureContext)), this.values[1].resolve(measureContext), this.values[2].resolve(measureContext)
               );
            }
            break;
         case SKEW:
            if (this.values.length == 1) {
               transform.shear(Math.tan(Math.toRadians(this.values[0].resolve(measureContext))), 0.0);
            } else {
               transform.shear(
                  Math.tan(Math.toRadians(this.values[0].resolve(measureContext))), Math.tan(Math.toRadians(this.values[1].resolve(measureContext)))
               );
            }
            break;
         case SKEW_X:
            transform.shear(Math.tan(Math.toRadians(this.values[0].resolve(measureContext))), 0.0);
            break;
         case SKEW_Y:
            transform.shear(0.0, Math.tan(Math.toRadians(this.values[0].resolve(measureContext))));
      }

      return transform;
   }

   @Override
   public String toString() {
      return "TransformPart{type=" + this.type + ", values=" + Arrays.toString(this.values) + '}';
   }

   public enum TransformType implements HasMatchName {
      MATRIX,
      TRANSLATE,
      TRANSLATE_X("translateX"),
      TRANSLATE_Y("translateY"),
      SCALE,
      SCALE_X("scaleX"),
      SCALE_Y("scaleY"),
      ROTATE,
      SKEW,
      SKEW_X("skewX"),
      SKEW_Y("skewY");

      @NotNull
      private final String matchName;

      TransformType(@NotNull String matchName) {
         this.matchName = matchName;
      }

      TransformType() {
         this.matchName = this.name();
      }

      @NotNull
      @Override
      public String matchName() {
         return this.matchName;
      }

      TransformPart.TransformType interpolationType() {
         switch (this) {
            case TRANSLATE_X:
            case TRANSLATE_Y:
               return TRANSLATE;
            case SCALE:
            case ROTATE:
            case SKEW:
            default:
               return this;
            case SCALE_X:
            case SCALE_Y:
               return SCALE;
            case SKEW_X:
            case SKEW_Y:
               return SKEW;
         }
      }

      // $VF: synthetic method
      private static TransformPart.TransformType[] $values() {
         return new TransformPart.TransformType[]{MATRIX, TRANSLATE, TRANSLATE_X, TRANSLATE_Y, SCALE, SCALE_X, SCALE_Y, ROTATE, SKEW, SKEW_X, SKEW_Y};
      }
   }
}

