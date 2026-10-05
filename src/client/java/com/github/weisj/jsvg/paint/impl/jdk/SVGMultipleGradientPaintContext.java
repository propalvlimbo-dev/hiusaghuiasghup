package com.github.weisj.jsvg.paint.impl.jdk;

import com.github.weisj.jsvg.util.ColorUtil;
import java.awt.Color;
import java.awt.PaintContext;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

abstract class SVGMultipleGradientPaintContext implements PaintContext {
   private static final float MIN_INTERVAL_LENGTH = 0.001F;
   protected ColorModel model;
   private static final ColorModel XRGB_MODEL = new DirectColorModel(24, 16711680, 65280, 255);
   protected static ColorModel cachedModel;
   protected static WeakReference<Raster> cached;
   protected Raster saved;
   protected CycleMethod cycleMethod;
   protected ColorSpaceType colorSpace;
   protected float a00;
   protected float a01;
   protected float a10;
   protected float a11;
   protected float a02;
   protected float a12;
   protected boolean isSimpleLookup;
   protected int fastGradientArraySize;
   protected int[] gradient;
   private int[][] gradients;
   private float[] normalizedIntervals;
   private final float[] fractions;
   private int transparencyTest;
   protected static final int GRADIENT_SIZE = 256;
   protected static final int GRADIENT_SIZE_INDEX = 255;
   private static final int MAX_GRADIENT_ARRAY_SIZE = 5000;

   protected SVGMultipleGradientPaintContext(
      @NotNull SVGMultipleGradientPaint mgp,
      @NotNull AffineTransform t,
      float @NotNull [] fractions,
      @NotNull Color[] colors,
      CycleMethod cycleMethod,
      ColorSpaceType colorSpace
   ) {
      AffineTransform tInv;
      try {
         t.invert();
         tInv = t;
      } catch (NoninvertibleTransformException e) {
         tInv = new AffineTransform();
      }

      double[] m = new double[6];
      tInv.getMatrix(m);
      this.a00 = (float)m[0];
      this.a10 = (float)m[1];
      this.a01 = (float)m[2];
      this.a11 = (float)m[3];
      this.a02 = (float)m[4];
      this.a12 = (float)m[5];
      this.cycleMethod = cycleMethod;
      this.colorSpace = colorSpace;
      this.fractions = fractions;
      int[] grad = mgp.gradient != null ? mgp.gradient.get() : null;
      int[][] grads = mgp.gradients != null ? mgp.gradients.get() : null;
      if (grad == null && grads == null) {
         this.calculateLookupData(colors);
         mgp.model = this.model;
         mgp.normalizedIntervals = this.normalizedIntervals;
         mgp.isSimpleLookup = this.isSimpleLookup;
         if (this.isSimpleLookup) {
            mgp.fastGradientArraySize = this.fastGradientArraySize;
            mgp.gradient = new SoftReference<>(this.gradient);
         } else {
            mgp.gradients = new SoftReference<>(this.gradients);
         }
      } else {
         this.model = mgp.model;
         this.normalizedIntervals = mgp.normalizedIntervals;
         this.isSimpleLookup = mgp.isSimpleLookup;
         this.gradient = grad;
         this.fastGradientArraySize = mgp.fastGradientArraySize;
         this.gradients = grads;
      }
   }

   private void calculateLookupData(Color[] colors) {
      Color[] normalizedColors;
      if (this.colorSpace == ColorSpaceType.LINEAR_RGB) {
         normalizedColors = new Color[colors.length];

         for (int i = 0; i < colors.length; i++) {
            int argb = colors[i].getRGB();
            normalizedColors[i] = new Color(ColorUtil.sRGBtoLinearRGB(argb), true);
         }
      } else {
         normalizedColors = colors;
      }

      this.normalizedIntervals = new float[this.fractions.length - 1];

      for (int i = 0; i < this.normalizedIntervals.length; i++) {
         this.normalizedIntervals[i] = this.fractions[i + 1] - this.fractions[i];
      }

      this.transparencyTest = -16777216;
      this.gradients = new int[this.normalizedIntervals.length][];
      float minInterval = 1.0F;

      for (float interval : this.normalizedIntervals) {
         if (interval > 0.001F) {
            minInterval = Math.min(minInterval, interval);
         }
      }

      int estimatedSize = 0;

      for (float normalizedInterval : this.normalizedIntervals) {
         estimatedSize += (int)(normalizedInterval / minInterval * 256.0F);
      }

      if (estimatedSize > 5000) {
         this.calculateMultipleArrayGradient(normalizedColors);
      } else {
         this.calculateSingleArrayGradient(normalizedColors, minInterval);
      }

      if (this.transparencyTest >>> 24 == 255) {
         this.model = XRGB_MODEL;
      } else {
         this.model = ColorModel.getRGBdefault();
      }
   }

   private void calculateSingleArrayGradient(Color[] colors, float minInterval) {
      this.isSimpleLookup = true;
      int gradientsTot = 1;

      for (int i = 0; i < this.gradients.length; i++) {
         int nGradients = (int)(this.normalizedIntervals[i] / minInterval * 255.0F);
         gradientsTot += nGradients;
         this.gradients[i] = new int[nGradients];
         int rgb1 = colors[i].getRGB();
         int rgb2 = colors[i + 1].getRGB();
         this.interpolate(rgb1, rgb2, this.gradients[i]);
         this.transparencyTest &= rgb1;
         this.transparencyTest &= rgb2;
      }

      this.gradient = new int[gradientsTot];
      int curOffset = 0;

      for (int[] ints : this.gradients) {
         System.arraycopy(ints, 0, this.gradient, curOffset, ints.length);
         curOffset += ints.length;
      }

      this.gradient[this.gradient.length - 1] = colors[colors.length - 1].getRGB();
      if (this.colorSpace == ColorSpaceType.LINEAR_RGB) {
         for (int i = 0; i < this.gradient.length; i++) {
            this.gradient[i] = ColorUtil.linearRGBtoSRGB(this.gradient[i]);
         }
      }

      this.fastGradientArraySize = this.gradient.length - 1;
   }

   private void calculateMultipleArrayGradient(Color[] colors) {
      this.isSimpleLookup = false;

      for (int i = 0; i < this.gradients.length; i++) {
         this.gradients[i] = new int[256];
         int rgb1 = colors[i].getRGB();
         int rgb2 = colors[i + 1].getRGB();
         this.interpolate(rgb1, rgb2, this.gradients[i]);
         this.transparencyTest &= rgb1;
         this.transparencyTest &= rgb2;
      }

      if (this.colorSpace == ColorSpaceType.LINEAR_RGB) {
         for (int j = 0; j < this.gradients.length; j++) {
            for (int i = 0; i < this.gradients[j].length; i++) {
               this.gradients[j][i] = ColorUtil.linearRGBtoSRGB(this.gradients[j][i]);
            }
         }
      }
   }

   private void interpolate(int rgb1, int rgb2, int[] output) {
      float stepSize = 1.0F / output.length;
      int a1 = rgb1 >> 24 & 0xFF;
      int r1 = rgb1 >> 16 & 0xFF;
      int g1 = rgb1 >> 8 & 0xFF;
      int b1 = rgb1 & 0xFF;
      int da = (rgb2 >> 24 & 0xFF) - a1;
      int dr = (rgb2 >> 16 & 0xFF) - r1;
      int dg = (rgb2 >> 8 & 0xFF) - g1;
      int db = (rgb2 & 0xFF) - b1;

      for (int i = 0; i < output.length; i++) {
         output[i] = (int)(a1 + i * da * stepSize + 0.5) << 24
            | (int)(r1 + i * dr * stepSize + 0.5) << 16
            | (int)(g1 + i * dg * stepSize + 0.5) << 8
            | (int)(b1 + i * db * stepSize + 0.5);
      }
   }

   private static float mod1(float x) {
      float result = x - (int)x;
      if (result < 0.0F) {
         result++;
      }

      return result;
   }

   private static float mod1Reflect(float x) {
      float result = Math.abs(x);
      int part = (int)x;
      result -= part;
      if (part % 2 == 1) {
         result = 1.0F - result;
      }

      return result;
   }

   protected final int indexIntoGradientsArrays(float position) {
      if (this.cycleMethod == CycleMethod.NO_CYCLE) {
         position = Math.min(1.0F, Math.max(0.0F, position));
      } else if (this.cycleMethod == CycleMethod.REPEAT) {
         position = mod1(position);
      } else {
         position = mod1Reflect(position);
      }

      if (this.isSimpleLookup) {
         return this.gradient[(int)(position * this.fastGradientArraySize)];
      }

      for (int i = 0; i < this.gradients.length; i++) {
         if (position < this.fractions[i + 1]) {
            float delta = position - this.fractions[i];
            int index = (int)(delta / this.normalizedIntervals[i] * 255.0F);
            return this.gradients[i][index];
         }
      }

      return this.gradients[this.gradients.length - 1][255];
   }

   @Override
   public final Raster getRaster(int x, int y, int w, int h) {
      Raster raster = this.saved;
      if (raster == null || raster.getWidth() < w || raster.getHeight() < h) {
         raster = getCachedRaster(this.model, w, h);
         this.saved = raster;
      }

      DataBufferInt rasterDB = (DataBufferInt)raster.getDataBuffer();
      int[] pixels = rasterDB.getData(0);
      int off = rasterDB.getOffset();
      int scanlineStride = ((SinglePixelPackedSampleModel)raster.getSampleModel()).getScanlineStride();
      int adjust = scanlineStride - w;
      this.fillRaster(pixels, off, adjust, x, y, w, h);
      return raster;
   }

   protected abstract void fillRaster(int[] var1, int var2, int var3, int var4, int var5, int var6, int var7);

   private static synchronized Raster getCachedRaster(ColorModel cm, int w, int h) {
      if (Objects.equals(cm, cachedModel) && cached != null) {
         Raster ras = cached.get();
         if (ras != null && ras.getWidth() >= w && ras.getHeight() >= h) {
            cached = null;
            return ras;
         }
      }

      return cm.createCompatibleWritableRaster(w, h);
   }

   private static synchronized void putCachedRaster(ColorModel cm, Raster ras) {
      if (cached != null) {
         Raster cras = cached.get();
         if (cras != null) {
            int cw = cras.getWidth();
            int ch = cras.getHeight();
            int iw = ras.getWidth();
            int ih = ras.getHeight();
            if (cw >= iw && ch >= ih) {
               return;
            }

            if (cw * ch >= iw * ih) {
               return;
            }
         }
      }

      cachedModel = cm;
      cached = new WeakReference<>(ras);
   }

   @Override
   public final void dispose() {
      if (this.saved != null) {
         putCachedRaster(this.model, this.saved);
         this.saved = null;
      }
   }

   @Override
   public final ColorModel getColorModel() {
      return this.model;
   }
}

