package com.github.weisj.jsvg.util;

import java.awt.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ColorUtil {
   private static final int[][] SRGBtoLinearRGBPre = new int[256][256];
   private static final int[] SRGBtoLinearRGB = new int[256];
   private static final int[][] LinearRGBtoSRGBPre = new int[256][256];
   private static final int[] LinearRGBtoSRGB = new int[256];

   private ColorUtil() {
   }

   public static int div255(int x) {
      x += 128;
      return x + (x >> 8) >> 8;
   }

   public static int computeLuminance(int r, int g, int b) {
      return toRgbRange(0.2125 * r + 0.7164 * g + 0.0712 * b);
   }

   public static int toRgbRange(double value) {
      return (int)Math.max(Math.min(Math.round(value), 255L), 0L);
   }

   public static Color withAlpha(@NotNull Color c, float alpha) {
      int a = Math.max(Math.min(255, (int)(alpha * 255.0F)), 0);
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
   }

   public static String toString(@Nullable Color c) {
      return c == null ? "null" : String.format("Color[%d,%d,%d,%d]", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
   }

   public static void convertRGBPretoHSL(int r, int g, int b, int a, float @NotNull [] hsl) {
      if (r < 0) {
         r = 0;
      } else if (r > 255) {
         r = 255;
      }

      if (g < 0) {
         g = 0;
      } else if (g > 255) {
         g = 255;
      }

      if (b < 0) {
         b = 0;
      } else if (b > 255) {
         b = 255;
      }

      float componentR = (float)r / a;
      float componentG = (float)g / a;
      float componentB = (float)b / a;
      float minComponent;
      float maxComponent;
      if (componentR > componentG) {
         minComponent = componentG;
         maxComponent = componentR;
      } else {
         minComponent = componentR;
         maxComponent = componentG;
      }

      if (componentB > maxComponent) {
         maxComponent = componentB;
      }

      if (componentB < minComponent) {
         minComponent = componentB;
      }

      float deltaMax = maxComponent - minComponent;
      float l = (maxComponent + minComponent) / 2.0F;
      float h;
      float s;
      if (deltaMax - 0.01F <= 0.0F) {
         h = 0.0F;
         s = 0.0F;
      } else {
         if (l < 0.5F) {
            assert maxComponent + minComponent != 0.0F;
            s = deltaMax / (maxComponent + minComponent);
         } else {
            s = deltaMax / (2.0F - maxComponent - minComponent);
         }

         assert deltaMax > 0.0F;
         float deltaR = ((maxComponent - componentR) / 6.0F + deltaMax / 2.0F) / deltaMax;
         float deltaG = ((maxComponent - componentG) / 6.0F + deltaMax / 2.0F) / deltaMax;
         float deltaB = ((maxComponent - componentB) / 6.0F + deltaMax / 2.0F) / deltaMax;
         if (componentR == maxComponent) {
            h = deltaB - deltaG;
         } else if (componentG == maxComponent) {
            h = 0.33333334F + deltaR - deltaB;
         } else {
            h = 0.6666667F + deltaG - deltaR;
         }

         if (h < 0.0F) {
            h++;
         }

         if (h > 1.0F) {
            h--;
         }
      }

      hsl[0] = h;
      hsl[1] = s;
      hsl[2] = l;
   }

   public static void convertHSLtoRGB(float h, float s, float l, int @NotNull [] rgb) {
      if (h < 0.0F) {
         h = 0.0F;
      } else if (h > 1.0F) {
         h = 1.0F;
      }

      if (s < 0.0F) {
         s = 0.0F;
      } else if (s > 1.0F) {
         s = 1.0F;
      }

      if (l < 0.0F) {
         l = 0.0F;
      } else if (l > 1.0F) {
         l = 1.0F;
      }

      int r;
      int g;
      int b;
      if (s - 0.01F <= 0.0F) {
         r = (int)(l * 255.0F);
         g = (int)(l * 255.0F);
         b = (int)(l * 255.0F);
      } else {
         float y;
         if (l < 0.5F) {
            y = l * (1.0F + s);
         } else {
            y = l + s - s * l;
         }

         float x = 2.0F * l - y;
         r = (int)(255.0F * hue2RGB(x, y, h + 0.33333334F));
         g = (int)(255.0F * hue2RGB(x, y, h));
         b = (int)(255.0F * hue2RGB(x, y, h - 0.33333334F));
      }

      rgb[0] = r;
      rgb[1] = g;
      rgb[2] = b;
   }

   private static float hue2RGB(float v1, float v2, float vH) {
      if (vH < 0.0F) {
         vH++;
      }

      if (vH > 1.0F) {
         vH--;
      }

      if (6.0F * vH < 1.0F) {
         return v1 + (v2 - v1) * 6.0F * vH;
      } else if (2.0F * vH < 1.0F) {
         return v2;
      } else {
         return 3.0F * vH < 2.0F ? v1 + (v2 - v1) * (0.6666667F - vH) * 6.0F : v1;
      }
   }

   public static void sRGBtoLinearRGBinPlace(int @NotNull [] argb) {
      argb[0] = SRGBtoLinearRGB[argb[0]];
      argb[1] = SRGBtoLinearRGB[argb[1]];
      argb[2] = SRGBtoLinearRGB[argb[2]];
   }

   public static void linearRGBtoSRGBinPlace(int @NotNull [] argb) {
      argb[0] = LinearRGBtoSRGB[argb[0]];
      argb[1] = LinearRGBtoSRGB[argb[1]];
      argb[2] = LinearRGBtoSRGB[argb[2]];
   }

   public static void sRGBtoLinearRGBPreInPlace(int @NotNull [] argb) {
      int alpha = argb[3];
      int[] table = SRGBtoLinearRGBPre[alpha];
      argb[0] = table[argb[0]];
      argb[1] = table[argb[1]];
      argb[2] = table[argb[2]];
   }

   public static void linearRGBtoSRGBPreInPlace(int @NotNull [] argb) {
      int alpha = argb[3];
      int[] table = LinearRGBtoSRGBPre[alpha];
      argb[0] = table[argb[0]];
      argb[1] = table[argb[1]];
      argb[2] = table[argb[2]];
   }

   public static int sRGBtoLinearRGBBand(int value) {
      return SRGBtoLinearRGB[value];
   }

   public static int sRGBtoLinearRGB(int argb) {
      int a = argb >>> 24;
      int r = SRGBtoLinearRGB[argb >> 16 & 0xFF];
      int g = SRGBtoLinearRGB[argb >> 8 & 0xFF];
      int b = SRGBtoLinearRGB[argb & 0xFF];
      return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   public static int linearRGBtoSRGBBand(int value) {
      return LinearRGBtoSRGB[value];
   }

   public static int linearRGBtoSRGB(int argb) {
      int a = argb >>> 24;
      int r = LinearRGBtoSRGB[argb >> 16 & 0xFF];
      int g = LinearRGBtoSRGB[argb >> 8 & 0xFF];
      int b = LinearRGBtoSRGB[argb & 0xFF];
      return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   private static int convertSRGBtoLinearRGB(int color, float alpha) {
      float factor = 255.0F * alpha;
      float input = color / factor;
      float output;
      if (input <= 0.04045F) {
         output = input / 12.92F;
      } else {
         output = (float)Math.pow((input + 0.055) / 1.055, 2.4);
      }

      return Math.round(output * factor);
   }

   private static int convertLinearRGBtoSRGB(int color, float alpha) {
      float factor = 255.0F * alpha;
      float input = color / factor;
      float output;
      if (input <= 0.0031308) {
         output = input * 12.92F;
      } else {
         output = 1.055F * (float)Math.pow(input, 0.4166666666666667) - 0.055F;
      }

      return Math.round(output * factor);
   }

   public static int clampColor(int v) {
      return Math.max(Math.min(255, v), 0);
   }

   static {
      for (int k = 0; k < 256; k++) {
         SRGBtoLinearRGB[k] = convertSRGBtoLinearRGB(k, 1.0F);
         LinearRGBtoSRGB[k] = convertLinearRGBtoSRGB(k, 1.0F);
      }

      SRGBtoLinearRGBPre[255] = SRGBtoLinearRGB;
      LinearRGBtoSRGBPre[255] = LinearRGBtoSRGB;

      for (int i = 0; i < 255; i++) {
         int[] sRGBtoLinear = new int[256];
         int[] linearTosRGB = new int[256];
         float alpha = i / 255.0F;

         for (int k = 0; k < 256; k++) {
            sRGBtoLinear[k] = convertSRGBtoLinearRGB(k, alpha);
            linearTosRGB[k] = convertLinearRGBtoSRGB(k, alpha);
         }

         SRGBtoLinearRGBPre[i] = sRGBtoLinear;
         LinearRGBtoSRGBPre[i] = linearTosRGB;
      }
   }
}

