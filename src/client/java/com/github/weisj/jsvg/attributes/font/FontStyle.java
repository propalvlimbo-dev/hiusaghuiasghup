package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.geometry.size.Angle;
import com.github.weisj.jsvg.geometry.size.AngleUnit;
import java.awt.geom.AffineTransform;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class FontStyle {
   private FontStyle() {
   }

   @Nullable
   public AffineTransform transform() {
      return null;
   }

   @NotNull
   public static FontStyle normal() {
      return FontStyle.Normal.INSTANCE;
   }

   @NotNull
   public static FontStyle italic() {
      return FontStyle.Italic.INSTANCE;
   }

   @NotNull
   public static FontStyle oblique() {
      return FontStyle.Oblique.DEFAULT;
   }

   @NotNull
   public static FontStyle oblique(@NotNull Angle angle) {
      return new FontStyle.Oblique(angle);
   }

   static final class Italic extends FontStyle {
      @NotNull
      private static final FontStyle.Italic INSTANCE = new FontStyle.Italic();

      @Override
      public String toString() {
         return "Italic";
      }

      @Override
      public boolean equals(Object obj) {
         return obj instanceof FontStyle.Italic;
      }

      @Override
      public int hashCode() {
         return FontStyle.Italic.class.hashCode();
      }
   }

   static final class Normal extends FontStyle {
      @NotNull
      private static final FontStyle.Normal INSTANCE = new FontStyle.Normal();

      @Override
      public String toString() {
         return "Normal";
      }

      @Override
      public boolean equals(Object obj) {
         return obj instanceof FontStyle.Normal;
      }

      @Override
      public int hashCode() {
         return FontStyle.Normal.class.hashCode();
      }
   }

   static final class Oblique extends FontStyle {
      @NotNull
      public static final Angle DEFAULT_ANGLE = new Angle(AngleUnit.Deg, 14.0F);
      @NotNull
      public static final FontStyle.Oblique DEFAULT = new FontStyle.Oblique(DEFAULT_ANGLE);
      @NotNull
      private final Angle angle;

      public Oblique(@NotNull Angle angle) {
         this.angle = angle;
      }

      @NotNull
      @Override
      public AffineTransform transform() {
         return AffineTransform.getShearInstance(-this.angle.radians(), 0.0);
      }

      @Override
      public String toString() {
         return "Oblique{" + this.angle + '}';
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }

         if (!(o instanceof FontStyle.Oblique)) {
            return false;
         }

         FontStyle.Oblique that = (FontStyle.Oblique)o;
         return this.angle.equals(that.angle);
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(this.angle);
      }
   }
}

