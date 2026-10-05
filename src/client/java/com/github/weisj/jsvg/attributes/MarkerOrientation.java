package com.github.weisj.jsvg.attributes;

import com.github.weisj.jsvg.geometry.size.Angle;
import com.github.weisj.jsvg.parser.impl.AttributeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MarkerOrientation {
   private MarkerOrientation() {
   }

   @NotNull
   public static MarkerOrientation parse(@Nullable String value, @NotNull AttributeParser parser) {
      if (value == null) {
         return MarkerOrientation.AngleOrientation.DEFAULT;
      }

      if ("auto".equals(value)) {
         return MarkerOrientation.AutoOrientation.INSTANCE;
      }

      if ("auto-start-reverse".equals(value)) {
         return MarkerOrientation.AutoStartReverseOrientation.INSTANCE;
      }

      Angle angle = parser.parseAngle(value, Angle.UNSPECIFIED);
      return angle.isSpecified() ? new MarkerOrientation.AngleOrientation(angle) : MarkerOrientation.AngleOrientation.DEFAULT;
   }

   public abstract float orientationFor(@NotNull MarkerOrientation.MarkerType var1, float var2, float var3, float var4, float var5);

   private static final class AngleOrientation extends MarkerOrientation {
      @NotNull
      private static final MarkerOrientation.AngleOrientation DEFAULT = new MarkerOrientation.AngleOrientation(Angle.ZERO);
      @NotNull
      private final Angle angle;

      private AngleOrientation(@NotNull Angle angle) {
         this.angle = angle;
      }

      @Override
      public float orientationFor(@NotNull MarkerOrientation.MarkerType type, float dxIn, float dyIn, float dxOut, float dyOut) {
         return this.angle.radians();
      }
   }

   private static final class AutoOrientation extends MarkerOrientation {
      @NotNull
      private static final MarkerOrientation.AutoOrientation INSTANCE = new MarkerOrientation.AutoOrientation();

      @Override
      public float orientationFor(@NotNull MarkerOrientation.MarkerType type, float dxIn, float dyIn, float dxOut, float dyOut) {
         switch (type) {
            case START:
               return (float)Math.atan2(dyOut, dxOut);
            case MID:
               return (float)Math.atan2((dyIn + dyOut) / 2.0F, (dxIn + dxOut) / 2.0F);
            case END:
               return (float)Math.atan2(dyIn, dxIn);
            default:
               throw new IllegalStateException();
         }
      }
   }

   private static final class AutoStartReverseOrientation extends MarkerOrientation {
      @NotNull
      private static final MarkerOrientation.AutoStartReverseOrientation INSTANCE = new MarkerOrientation.AutoStartReverseOrientation();

      @Override
      public float orientationFor(@NotNull MarkerOrientation.MarkerType type, float dxIn, float dyIn, float dxOut, float dyOut) {
         switch (type) {
            case START:
               return (float)Math.atan2(-dyOut, -dxOut);
            case MID:
               return (float)Math.atan2((dyIn + dyOut) / 2.0F, (dxIn + dxOut) / 2.0F);
            case END:
               return (float)Math.atan2(dyIn, dxIn);
            default:
               throw new IllegalStateException();
         }
      }
   }

   public enum MarkerType {
      START,
      MID,
      END;

      // $VF: synthetic method
      private static MarkerOrientation.MarkerType[] $values() {
         return new MarkerOrientation.MarkerType[]{START, MID, END};
      }
   }
}

