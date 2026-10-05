package com.github.weisj.jsvg.attributes;

import com.github.weisj.jsvg.parser.impl.AttributeParser;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.geom.AffineTransform;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PreserveAspectRatio {
   @NotNull
   public final PreserveAspectRatio.Align align;
   @NotNull
   public final PreserveAspectRatio.MeetOrSlice meetOrSlice;

   private PreserveAspectRatio(@NotNull PreserveAspectRatio.Align align, @NotNull PreserveAspectRatio.MeetOrSlice meetOrSlice) {
      this.align = align;
      this.meetOrSlice = meetOrSlice;
   }

   @NotNull
   public static PreserveAspectRatio none() {
      return new PreserveAspectRatio(PreserveAspectRatio.Align.None, PreserveAspectRatio.MeetOrSlice.Meet);
   }

   @NotNull
   public static PreserveAspectRatio forDisplay() {
      return new PreserveAspectRatio(PreserveAspectRatio.Align.xMidYMid, PreserveAspectRatio.MeetOrSlice.Meet);
   }

   @NotNull
   public static PreserveAspectRatio parse(@Nullable String preserveAspectRation, @NotNull AttributeParser parser) {
      return parse(preserveAspectRation, null, parser);
   }

   @NotNull
   public static PreserveAspectRatio parse(@Nullable String preserveAspectRation, @Nullable PreserveAspectRatio fallback, @NotNull AttributeParser parser) {
      PreserveAspectRatio.Align align = PreserveAspectRatio.Align.xMidYMid;
      PreserveAspectRatio.MeetOrSlice meetOrSlice = PreserveAspectRatio.MeetOrSlice.Meet;
      if (preserveAspectRation == null) {
         return fallback != null ? fallback : new PreserveAspectRatio(align, meetOrSlice);
      }

      String[] components = parser.parseStringList(preserveAspectRation, SeparatorMode.COMMA_AND_WHITESPACE);
      if (components.length >= 1 && components.length <= 2) {
         align = parser.parseEnum(components[0], align);
         if (components.length > 1) {
            meetOrSlice = parser.parseEnum(components[1], meetOrSlice);
         }

         return new PreserveAspectRatio(align, meetOrSlice);
      } else {
         throw new IllegalArgumentException("Too many arguments specified: " + preserveAspectRation);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof PreserveAspectRatio)) {
         return false;
      }

      PreserveAspectRatio that = (PreserveAspectRatio)o;
      return this.align == that.align && this.meetOrSlice == that.meetOrSlice;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.align, this.meetOrSlice);
   }

   @NotNull
   public AffineTransform computeViewportTransform(@NotNull FloatSize size, @NotNull ViewBox viewBox) {
      AffineTransform viewTransform = new AffineTransform();
      if (this.align == PreserveAspectRatio.Align.None) {
         viewTransform.scale(size.width / viewBox.width, size.height / viewBox.height);
      } else {
         double xScale = size.width / viewBox.width;
         double yScale = size.height / viewBox.height;
         switch (this.meetOrSlice) {
            case Meet:
               xScale = yScale = Math.min(xScale, yScale);
               break;
            case Slice:
               xScale = yScale = Math.max(xScale, yScale);
               break;
            default:
               throw new IllegalStateException();
         }

         viewTransform.translate(this.align.xAlign.align(size.width, viewBox.width * xScale), this.align.yAlign.align(size.height, viewBox.height * yScale));
         viewTransform.scale(xScale, yScale);
      }

      viewTransform.translate(-viewBox.x, -viewBox.y);
      return viewTransform;
   }

   @Override
   public String toString() {
      return "PreserveAspectRatio{align=" + this.align + ", meetOrSlice=" + this.meetOrSlice + '}';
   }

   public enum Align {
      None(PreserveAspectRatio.AlignType.Min, PreserveAspectRatio.AlignType.Min),
      xMinYMin(PreserveAspectRatio.AlignType.Min, PreserveAspectRatio.AlignType.Min),
      xMidYMin(PreserveAspectRatio.AlignType.Mid, PreserveAspectRatio.AlignType.Min),
      xMaxYMin(PreserveAspectRatio.AlignType.Max, PreserveAspectRatio.AlignType.Min),
      xMinYMid(PreserveAspectRatio.AlignType.Min, PreserveAspectRatio.AlignType.Mid),
      xMidYMid(PreserveAspectRatio.AlignType.Mid, PreserveAspectRatio.AlignType.Mid),
      xMaxYMid(PreserveAspectRatio.AlignType.Max, PreserveAspectRatio.AlignType.Mid),
      xMinYMax(PreserveAspectRatio.AlignType.Min, PreserveAspectRatio.AlignType.Max),
      xMidYMax(PreserveAspectRatio.AlignType.Mid, PreserveAspectRatio.AlignType.Max),
      xMaxYMax(PreserveAspectRatio.AlignType.Max, PreserveAspectRatio.AlignType.Max);

      @NotNull
      private final PreserveAspectRatio.AlignType xAlign;
      @NotNull
      private final PreserveAspectRatio.AlignType yAlign;

      Align(@NotNull PreserveAspectRatio.AlignType xAlign, @NotNull PreserveAspectRatio.AlignType yAlign) {
         this.xAlign = xAlign;
         this.yAlign = yAlign;
      }

      @Override
      public String toString() {
         return this.name() + "{" + this.xAlign + ", " + this.yAlign + "}";
      }

      // $VF: synthetic method
      private static PreserveAspectRatio.Align[] $values() {
         return new PreserveAspectRatio.Align[]{None, xMinYMin, xMidYMin, xMaxYMin, xMinYMid, xMidYMid, xMaxYMid, xMinYMax, xMidYMax, xMaxYMax};
      }
   }

   private enum AlignType {
      Min {
         @Override
         double align(double size1, double size2) {
            return 0.0;
         }
      },
      Mid {
         @Override
         double align(double size1, double size2) {
            return (size1 - size2) / 2.0;
         }
      },
      Max {
         @Override
         double align(double size1, double size2) {
            return size1 - size2;
         }
      };

      AlignType() {
      }

      abstract double align(double var1, double var3);

      // $VF: synthetic method
      private static PreserveAspectRatio.AlignType[] $values() {
         return new PreserveAspectRatio.AlignType[]{Min, Mid, Max};
      }
   }

   public enum MeetOrSlice {
      Meet,
      Slice;

      // $VF: synthetic method
      private static PreserveAspectRatio.MeetOrSlice[] $values() {
         return new PreserveAspectRatio.MeetOrSlice[]{Meet, Slice};
      }
   }
}

