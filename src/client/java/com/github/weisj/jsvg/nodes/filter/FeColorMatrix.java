package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ColorSpaceAwareRGBImageFilter;
import com.github.weisj.jsvg.util.ColorUtil;
import java.util.Arrays;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeColorMatrix extends AbstractFilterPrimitive {
   public static final String TAG = "fecolormatrix";
   private static final String KEY_VALUES = "values";
   @Nullable
   private FeColorMatrix.AffineRGBImageFilter filter;

   @NotNull
   @Override
   public String tagName() {
      return "fecolormatrix";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      String type = attributeNode.getValue("type");
      if (type == null) {
         type = "matrix";
      }

      this.filter = null;
      switch (type.toLowerCase(Locale.ENGLISH)) {
         case "matrix":
            double[] colorTransform = attributeNode.getDoubleList("values");
            if (colorTransform.length == 20) {
               boolean isIdentity = Arrays.equals(
                  colorTransform, new double[]{1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0}
               );
               if (!isIdentity) {
                  this.filter = new FeColorMatrix.MatrixRGBFilter(colorTransform);
               }
            }
            break;
         case "saturate":
            float s = attributeNode.getFloat("values", 1.0F);
            if (s != 1.0F) {
               this.filter = new FeColorMatrix.NoAlphaMatrixRGBFilter(
                  0.213 + 0.787 * s,
                  0.715 - 0.715 * s,
                  0.072 - 0.072 * s,
                  0.213 - 0.213 * s,
                  0.715 + 0.285 * s,
                  0.072 - 0.072 * s,
                  0.213 - 0.213 * s,
                  0.715 - 0.715 * s,
                  0.072 + 0.928 * s
               );
            }
            break;
         case "huerotate":
            float hueRotate = attributeNode.getFloat("values", 0.0F);
            if (hueRotate != 1.0F) {
               double radians = Math.toRadians(hueRotate);
               double sin = Math.sin(radians);
               double cos = Math.cos(radians);
               this.filter = new FeColorMatrix.NoAlphaMatrixRGBFilter(
                  0.213 + cos * 0.787 - sin * 0.2127,
                  0.715 - 0.715 * cos - 0.715 * sin,
                  0.072 - 0.072 * cos + 0.982 * sin,
                  0.213 - cos * 0.213 + sin * 0.143,
                  0.715 + 0.285 * cos + 0.14 * sin,
                  0.072 - 0.072 * cos - 0.283 * sin,
                  0.213 - cos * 0.213 - sin * 0.787,
                  0.715 - 0.715 * cos + 0.715 * sin,
                  0.072 + 0.982 * cos + 0.072 * sin
               );
            }
            break;
         case "luminancetoalpha":
            this.filter = new FeColorMatrix.LuminanceToAlphaFilter();
      }
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      LayoutBounds bounds = this.impl()
         .layoutInput(filterLayoutContext)
         .withFlags(new LayoutBounds.ComputeFlags(this.filter != null && !this.filter.isLinear()));
      this.impl().saveLayoutResult(bounds, filterLayoutContext);
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      FeColorMatrix.AffineRGBImageFilter f = this.filter;
      if (f == null) {
         this.impl().noop(filterContext);
      } else {
         f.setConvertToLinear(this.colorInterpolation(filterContext) == ColorInterpolation.LinearRGB);
         this.impl().saveResult(this.impl().inputChannel(filterContext).applyFilter(f), filterContext);
      }
   }

   private abstract static class AffineRGBImageFilter extends ColorSpaceAwareRGBImageFilter {
      private AffineRGBImageFilter() {
      }

      abstract boolean isLinear();
   }

   public static final class LuminanceToAlphaFilter extends FeColorMatrix.AffineRGBImageFilter {
      @Override
      boolean isLinear() {
         return true;
      }

      @Override
      public int filterRGB(int x, int y, int rgb) {
         int[] argb = this.getRGB(rgb);
         int na = ColorUtil.computeLuminance(argb[2], argb[1], argb[0]);
         return (na & 0xFF) << 24;
      }
   }

   private static final class MatrixRGBFilter extends FeColorMatrix.AffineRGBImageFilter {
      private final double r1;
      private final double r2;
      private final double r3;
      private final double r4;
      private final double r5;
      private final double g1;
      private final double g2;
      private final double g3;
      private final double g4;
      private final double g5;
      private final double b1;
      private final double b2;
      private final double b3;
      private final double b4;
      private final double b5;
      private final double a1;
      private final double a2;
      private final double a3;
      private final double a4;
      private final double a5;

      private MatrixRGBFilter(double[] values) {
         this.r1 = values[0];
         this.r2 = values[1];
         this.r3 = values[2];
         this.r4 = values[3];
         this.r5 = values[4];
         this.g1 = values[5];
         this.g2 = values[6];
         this.g3 = values[7];
         this.g4 = values[8];
         this.g5 = values[9];
         this.b1 = values[10];
         this.b2 = values[11];
         this.b3 = values[12];
         this.b4 = values[13];
         this.b5 = values[14];
         this.a1 = values[15];
         this.a2 = values[16];
         this.a3 = values[17];
         this.a4 = values[18];
         this.a5 = values[19];
      }

      @Override
      boolean isLinear() {
         return this.r5 == 0.0 && this.g5 == 0.0 && this.b5 == 0.0 && this.a5 == 0.0;
      }

      @Override
      public int filterRGB(int x, int y, int rgb) {
         int[] argb = this.getRGB(rgb);
         int a = argb[3];
         int r = argb[2];
         int g = argb[1];
         int b = argb[0];
         argb[3] = ColorUtil.toRgbRange(this.a1 * r + this.a2 * g + this.a3 * b + this.a4 * a + this.a5 * 255.0);
         argb[2] = ColorUtil.toRgbRange(this.r1 * r + this.r2 * g + this.r3 * b + this.r4 * a + this.r5 * 255.0);
         argb[1] = ColorUtil.toRgbRange(this.g1 * r + this.g2 * g + this.g3 * b + this.g4 * a + this.g5 * 255.0);
         argb[0] = ColorUtil.toRgbRange(this.b1 * r + this.b2 * g + this.b3 * b + this.b4 * a + this.b5 * 255.0);
         return this.pack(argb);
      }
   }

   private static final class NoAlphaMatrixRGBFilter extends FeColorMatrix.AffineRGBImageFilter {
      private final double r1;
      private final double r2;
      private final double r3;
      private final double g1;
      private final double g2;
      private final double g3;
      private final double b1;
      private final double b2;
      private final double b3;

      private NoAlphaMatrixRGBFilter(double r1, double r2, double r3, double g1, double g2, double g3, double b1, double b2, double b3) {
         this.r1 = r1;
         this.r2 = r2;
         this.r3 = r3;
         this.g1 = g1;
         this.g2 = g2;
         this.g3 = g3;
         this.b1 = b1;
         this.b2 = b2;
         this.b3 = b3;
      }

      @Override
      boolean isLinear() {
         return true;
      }

      @Override
      public int filterRGB(int x, int y, int rgb) {
         int[] argb = this.getRGB(rgb);
         int r = argb[2];
         int g = argb[1];
         int b = argb[0];
         argb[2] = ColorUtil.toRgbRange(this.r1 * r + this.r2 * g + this.r3 * b);
         argb[1] = ColorUtil.toRgbRange(this.g1 * r + this.g2 * g + this.g3 * b);
         argb[0] = ColorUtil.toRgbRange(this.b1 * r + this.b2 * g + this.b3 * b);
         return this.pack(argb);
      }
   }
}

