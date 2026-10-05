package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.BlendMode;
import com.github.weisj.jsvg.util.ColorUtil;
import java.awt.AlphaComposite;
import java.awt.Composite;
import org.jetbrains.annotations.NotNull;

public final class BlendModeComposite extends AbstractBlendComposite {
   @NotNull
   private final AbstractBlendComposite.Blender blender;

   private BlendModeComposite(BlendMode blendMode) {
      this.blender = createBlender(blendMode);
   }

   @NotNull
   public static Composite create(BlendMode mode) {
      return mode == BlendMode.Normal ? AlphaComposite.SrcOver : new BlendModeComposite(mode);
   }

   @NotNull
   @Override
   protected AbstractBlendComposite.Blender blender() {
      return this.blender;
   }

   @NotNull
   private static AbstractBlendComposite.Blender createBlender(BlendMode blendMode) {
      switch (blendMode) {
         case Normal:
            throw new IllegalStateException("Use AlphaComposite.SrcOver instead");
         case Multiply:
            return BlendModeComposite::blendMultiply;
         case Screen:
            return BlendModeComposite::blendScreen;
         case Overlay:
            return BlendModeComposite::blendOverlay;
         case Darken:
            return BlendModeComposite::blendDarken;
         case Lighten:
            return BlendModeComposite::blendLighten;
         case ColorDodge:
            return BlendModeComposite::blendColorDodge;
         case ColorBurn:
            return BlendModeComposite::blendColorBurn;
         case HardLight:
            return BlendModeComposite::blendHardLight;
         case SoftLight:
            return BlendModeComposite::blendSoftLight;
         case Difference:
            return BlendModeComposite::blendDifference;
         case Exclusion:
            return BlendModeComposite::blendExclusion;
         case Hue:
            return BlendModeComposite::blendHue;
         case Saturation:
            return BlendModeComposite::blendSaturation;
         case Color:
            return BlendModeComposite::blendColor;
         case Luminosity:
            return BlendModeComposite::blendLuminosity;
         default:
            throw new IllegalStateException("Mode not recognized " + blendMode);
      }
   }

   private static void blendMultiply(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = ColorUtil.div255(src[0] * srcM + dst[0] * dstM + src[0] * dst[0]);
      result[1] = ColorUtil.div255(src[1] * srcM + dst[1] * dstM + src[1] * dst[1]);
      result[2] = ColorUtil.div255(src[2] * srcM + dst[2] * dstM + src[2] * dst[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendScreen(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      result[0] = src[0] + dst[0] - ColorUtil.div255(src[0] * dst[0]);
      result[1] = src[1] + dst[1] - ColorUtil.div255(src[1] * dst[1]);
      result[2] = src[2] + dst[2] - ColorUtil.div255(src[2] * dst[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendOverlay(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      int dstComp = dstA * 255 / 2;
      result[0] = src[0] <= dstComp
         ? ColorUtil.div255(src[0] * srcM + 2 * dst[0] * dstM + 2 * src[0] * dst[0])
         : src[0] + dst[0] + ColorUtil.div255(dstA * src[0] + srcA * dst[0] - srcA * dstA - 2 * src[0] * dst[0]);
      result[1] = src[1] <= dstComp
         ? ColorUtil.div255(src[1] * srcM + 2 * dst[1] * dstM + 2 * src[1] * dst[1])
         : src[1] + dst[1] + ColorUtil.div255(dstA * src[1] + srcA * dst[1] - srcA * dstA - 2 * src[1] * dst[1]);
      result[2] = src[2] <= dstComp
         ? ColorUtil.div255(src[2] * srcM + 2 * dst[2] * dstM + 2 * src[2] * dst[2])
         : src[2] + dst[2] + ColorUtil.div255(dstA * src[2] + srcA * dst[2] - srcA * dstA - 2 * src[2] * dst[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendDarken(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = Math.min(ColorUtil.div255(srcM * src[0]) + dst[0], ColorUtil.div255(dstM * dst[0]) + src[0]);
      result[1] = Math.min(ColorUtil.div255(srcM * src[1]) + dst[1], ColorUtil.div255(dstM * dst[1]) + src[1]);
      result[2] = Math.min(ColorUtil.div255(srcM * src[2]) + dst[2], ColorUtil.div255(dstM * dst[2]) + src[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendLighten(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = Math.max(ColorUtil.div255(srcM * src[0]) + dst[0], ColorUtil.div255(dstM * dst[0]) + src[0]);
      result[1] = Math.max(ColorUtil.div255(srcM * src[1]) + dst[1], ColorUtil.div255(dstM * dst[1]) + src[1]);
      result[2] = Math.max(ColorUtil.div255(srcM * src[2]) + dst[2], ColorUtil.div255(dstM * dst[2]) + src[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendColorDodge(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = colorDodge(src[0], dst[0], srcM, srcA, dstM, dstA);
      result[1] = colorDodge(src[1], dst[1], srcM, srcA, dstM, dstA);
      result[2] = colorDodge(src[2], dst[2], srcM, srcA, dstM, dstA);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static int colorDodge(int src, int dst, int srcM, int srcA, int dstM, int dstA) {
      if (dst == 0) {
         return ColorUtil.div255(srcM * src);
      } else {
         return src == srcA
            ? ColorUtil.div255(srcM * srcA + dstM * dst + srcA * dstA)
            : Math.min(
               ColorUtil.div255(srcM * src + dstM * dst + srcA * dstA),
               ColorUtil.div255(srcM * src + dstM * dst) + Math.round(ColorUtil.div255(dstA * src) / (255.0F - (float)dst / dstA))
            );
      }
   }

   private static void blendColorBurn(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = colorBurn(src[0], dst[0], srcM, srcA, dstM, dstA);
      result[1] = colorBurn(src[1], dst[1], srcM, srcA, dstM, dstA);
      result[2] = colorBurn(src[2], dst[2], srcM, srcA, dstM, dstA);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static int colorBurn(int src, int dst, int srcM, int srcA, int dstM, int dstA) {
      if (dst == dstA) {
         return ColorUtil.div255(srcM * src + dstM * dstA + srcA * dstA);
      }

      if (src == 0) {
         return ColorUtil.div255(dstM * dst);
      }

      float srcC = (float)src / srcA;
      float dstC = (float)dst / dstA;
      int b = Math.round(Math.min(255.0F, (255.0F - dstC) / srcC));
      return ColorUtil.div255(srcM * src + dstM * dst) + ColorUtil.div255(ColorUtil.div255(srcA * dstA) * b);
   }

   private static void blendHardLight(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      int srcComp = srcA * 255 / 2;
      result[0] = src[0] <= srcComp
         ? ColorUtil.div255(2 * src[0] * srcM + dst[0] * dstM + 2 * src[0] * dst[0])
         : src[0] + dst[0] + ColorUtil.div255(dstA * src[0] + srcA * dst[0] - srcA * dstA - 2 * src[0] * dst[0]);
      result[1] = src[1] <= srcComp
         ? ColorUtil.div255(2 * src[1] * srcM + dst[1] * dstM + 2 * src[1] * dst[1])
         : src[1] + dst[1] + ColorUtil.div255(dstA * src[1] + srcA * dst[1] - srcA * dstA - 2 * src[1] * dst[1]);
      result[2] = src[2] <= srcComp
         ? ColorUtil.div255(2 * src[2] * srcM + dst[2] * dstM + 2 * src[2] * dst[2])
         : src[2] + dst[2] + ColorUtil.div255(dstA * src[2] + srcA * dst[2] - srcA * dstA - 2 * src[2] * dst[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendSoftLight(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = softLight(src[0], dst[0], srcM, srcA, dstM, dstA);
      result[1] = softLight(src[1], dst[1], srcM, srcA, dstM, dstA);
      result[2] = softLight(src[2], dst[2], srcM, srcA, dstM, dstA);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static int softLight(int src, int dst, int srcM, int srcA, int dstM, int dstA) {
      float srcC = (float)src / srcA;
      float dstC = (float)dst / dstA;
      float b;
      if (srcC <= 0.5) {
         b = dstC - (1.0F - 2.0F * srcC) * dstC * (1.0F - dstC);
      } else {
         float d;
         if (dstC <= 0.25) {
            d = ((16.0F * dstC - 12.0F) * dstC + 4.0F) * dstC;
         } else {
            d = (float)Math.sqrt(dstC);
         }

         b = dstC + (2.0F * srcC - 1.0F) * (d - dstC);
      }

      int bb = Math.round(b * 255.0F);
      return ColorUtil.div255(srcM * src + dstM * dst) + ColorUtil.div255(ColorUtil.div255(srcA * dstA) * bb);
   }

   private static void blendDifference(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = ColorUtil.div255(srcM * src[0] + dstM * dst[0] + Math.abs(dstA * src[0] - srcA * dst[0]));
      result[1] = ColorUtil.div255(srcM * src[1] + dstM * dst[1] + Math.abs(dstA * src[1] - srcA * dst[1]));
      result[2] = ColorUtil.div255(srcM * src[2] + dstM * dst[2] + Math.abs(dstA * src[2] - srcA * dst[2]));
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendExclusion(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      result[0] = ColorUtil.div255(srcM * src[0] + dstM * dst[0] + dstA * src[0] + srcA * dst[0] - 2 * ColorUtil.div255(src[0] * dst[0]));
      result[1] = ColorUtil.div255(srcM * src[1] + dstM * dst[1] + dstA * src[1] + srcA * dst[1] - 2 * ColorUtil.div255(src[1] * dst[1]));
      result[2] = ColorUtil.div255(srcM * src[2] + dstM * dst[2] + dstA * src[2] + srcA * dst[2] - 2 * ColorUtil.div255(src[2] * dst[2]));
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendHue(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      float[] srcHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
      float[] dstHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);
      ColorUtil.convertHSLtoRGB(srcHSL[0], dstHSL[1], dstHSL[2], result);
      result[0] = ColorUtil.div255(srcM * src[0] + dstM * dst[0] + ColorUtil.div255(srcA * dstA) * result[0]);
      result[1] = ColorUtil.div255(srcM * src[1] + dstM * dst[1] + ColorUtil.div255(srcA * dstA) * result[1]);
      result[2] = ColorUtil.div255(srcM * src[2] + dstM * dst[2] + ColorUtil.div255(srcA * dstA) * result[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendSaturation(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      float[] srcHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
      float[] dstHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);
      ColorUtil.convertHSLtoRGB(dstHSL[0], srcHSL[1], dstHSL[2], result);
      result[0] = ColorUtil.div255(srcM * src[0] + dstM * dst[0] + ColorUtil.div255(srcA * dstA) * result[0]);
      result[1] = ColorUtil.div255(srcM * src[1] + dstM * dst[1] + ColorUtil.div255(srcA * dstA) * result[1]);
      result[2] = ColorUtil.div255(srcM * src[2] + dstM * dst[2] + ColorUtil.div255(srcA * dstA) * result[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendColor(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      float[] srcHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
      float[] dstHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);
      ColorUtil.convertHSLtoRGB(srcHSL[0], srcHSL[1], dstHSL[2], result);
      result[0] = ColorUtil.div255(srcM * src[0] + dstM * dst[0] + ColorUtil.div255(srcA * dstA) * result[0]);
      result[1] = ColorUtil.div255(srcM * src[1] + dstM * dst[1] + ColorUtil.div255(srcA * dstA) * result[1]);
      result[2] = ColorUtil.div255(srcM * src[2] + dstM * dst[2] + ColorUtil.div255(srcA * dstA) * result[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }

   private static void blendLuminosity(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
      int srcA = src[3];
      int dstA = dst[3];
      int srcM = 255 - dstA;
      int dstM = 255 - srcA;
      float[] srcHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
      float[] dstHSL = new float[3];
      ColorUtil.convertRGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);
      ColorUtil.convertHSLtoRGB(dstHSL[0], dstHSL[1], srcHSL[2], result);
      result[0] = ColorUtil.div255(srcM * src[0] + dstM * dst[0] + ColorUtil.div255(srcA * dstA) * result[0]);
      result[1] = ColorUtil.div255(srcM * src[1] + dstM * dst[1] + ColorUtil.div255(srcA * dstA) * result[1]);
      result[2] = ColorUtil.div255(srcM * src[2] + dstM * dst[2] + ColorUtil.div255(srcA * dstA) * result[2]);
      result[3] = srcA + dstA - ColorUtil.div255(srcA * dstA);
   }
}

