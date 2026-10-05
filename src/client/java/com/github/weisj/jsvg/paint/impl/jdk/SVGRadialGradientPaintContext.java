package com.github.weisj.jsvg.paint.impl.jdk;

import java.awt.Color;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;

final class SVGRadialGradientPaintContext extends SVGMultipleGradientPaintContext {
   private final boolean isSimpleFocus;
   private final boolean isNonCyclic;
   private final float centerX;
   private final float centerY;
   private float focusX;
   private float focusY;
   private final float radiusSq;
   private final float focusRadius;
   private final float focusRadiusSq;
   private final float constA;
   private final float constB;
   private final float gDeltaDelta;
   private final float trivial;
   private static final float FOCUS_CLAMP_DOWNSCALE = 0.99F;
   private static final int SQRT_LUT_SIZE = 2048;
   private static final float[] sqrtLookup = new float[2049];

   SVGRadialGradientPaintContext(
      @NotNull SVGRadialGradientPaint paint,
      @NotNull AffineTransform t,
      float cx,
      float cy,
      float r,
      float fx,
      float fy,
      float fr,
      float @NotNull [] fractions,
      @NotNull Color[] colors,
      CycleMethod cycleMethod,
      ColorSpaceType colorSpace
   ) {
      super(paint, t, fractions, colors, cycleMethod, colorSpace);
      this.centerX = cx;
      this.centerY = cy;
      this.focusX = fx;
      this.focusY = fy;
      this.isSimpleFocus = this.focusX == this.centerX && this.focusY == this.centerY && fr == 0.0F;
      this.isNonCyclic = cycleMethod == CycleMethod.NO_CYCLE;
      this.radiusSq = r * r;
      this.focusRadius = fr;
      this.focusRadiusSq = fr * fr;
      float dX = this.focusX - this.centerX;
      float dY = this.focusY - this.centerY;
      double distSq = dX * dX + dY * dY;
      if (distSq > this.radiusSq * 0.99F) {
         float scale = (float)Math.sqrt(this.radiusSq * 0.99F / distSq);
         dX *= scale;
         dY *= scale;
         this.focusX = this.centerX + dX;
         this.focusY = this.centerY + dY;
      }

      this.trivial = (float)Math.sqrt(this.radiusSq - dX * dX);
      this.constA = this.a02 - this.centerX;
      this.constB = this.a12 - this.centerY;
      this.gDeltaDelta = 2.0F * (this.a00 * this.a00 + this.a10 * this.a10) / this.radiusSq;
   }

   @Override
   protected void fillRaster(int[] pixels, int off, int adjust, int x, int y, int w, int h) {
      if (this.isSimpleFocus && this.isNonCyclic && this.isSimpleLookup) {
         this.simpleNonCyclicFillRaster(pixels, off, adjust, x, y, w, h);
      } else {
         this.cyclicCircularGradientFillRaster(pixels, off, adjust, x, y, w, h);
      }
   }

   private void simpleNonCyclicFillRaster(int[] pixels, int off, int adjust, int x, int y, int w, int h) {
      float rowX = this.a00 * x + this.a01 * y + this.constA;
      float rowY = this.a10 * x + this.a11 * y + this.constB;
      float deltaDelta = this.gDeltaDelta;
      adjust += w;
      int rgbclip = this.gradient[this.fastGradientArraySize];

      for (int j = 0; j < h; j++) {
         float gRel = (rowX * rowX + rowY * rowY) / this.radiusSq;
         float gDelta = 2.0F * (this.a00 * rowX + this.a10 * rowY) / this.radiusSq + deltaDelta / 2.0F;

         int i;
         for (i = 0; i < w && gRel >= 1.0F; i++) {
            pixels[off + i] = rgbclip;
            gRel += gDelta;
            gDelta += deltaDelta;
         }

         while (i < w && gRel < 1.0F) {
            int gIndex;
            if (gRel <= 0.0F) {
               gIndex = 0;
            } else {
               float fIndex = gRel * 2048.0F;
               int iIndex = (int)fIndex;
               float s0 = sqrtLookup[iIndex];
               float s1 = sqrtLookup[iIndex + 1] - s0;
               fIndex = s0 + (fIndex - iIndex) * s1;
               gIndex = (int)(fIndex * this.fastGradientArraySize);
            }

            pixels[off + i] = this.gradient[gIndex];
            gRel += gDelta;
            gDelta += deltaDelta;
            i++;
         }

         while (i < w) {
            pixels[off + i] = rgbclip;
            i++;
         }

         off += adjust;
         rowX += this.a01;
         rowY += this.a11;
      }
   }

   private void cyclicCircularGradientFillRaster(int[] pixels, int off, int adjust, int x, int y, int w, int h) {
      double constC = -this.radiusSq + this.centerX * this.centerX + this.centerY * this.centerY;
      float constX = this.a00 * x + this.a01 * y + this.a02;
      float constY = this.a10 * x + this.a11 * y + this.a12;
      float precalc2 = 2.0F * this.centerY;
      float precalc3 = -2.0F * this.centerX;
      int indexer = off;
      int pixInc = w + adjust;

      for (int j = 0; j < h; j++) {
         float userX = this.a01 * j + constX;
         float userY = this.a11 * j + constY;

         for (int i = 0; i < w; i++) {
            double solutionX;
            double solutionY;
            if (userX == this.focusX) {
               solutionX = this.focusX;
               solutionY = this.centerY;
               solutionY += userY > this.focusY ? this.trivial : -this.trivial;
            } else {
               float slope = (userY - this.focusY) / (userX - this.focusX);
               float yIntercept = userY - slope * userX;
               double a = slope * slope + 1.0F;
               double b = precalc3 + -2.0F * slope * (this.centerY - yIntercept);
               double c = constC + yIntercept * (yIntercept - precalc2);
               float det = (float)Math.sqrt(b * b - 4.0 * a * c);
               double solutionXx = -b;
               double var34 = solutionXx + (userX < this.focusX ? -det : det);
               solutionX = var34 / (2.0 * a);
               solutionY = slope * solutionX + yIntercept;
            }

            int colorAtPoint = this.getColorAtPoint(userX, userY, (float)solutionX, (float)solutionY);
            pixels[indexer + i] = colorAtPoint;
            userX += this.a00;
            userY += this.a10;
         }

         indexer += pixInc;
      }
   }

   private int getColorAtPoint(float userX, float userY, float solutionX, float solutionY) {
      float currentToFocusSq = this.getCurrentToFocusSq(userX, userY);
      return currentToFocusSq <= this.focusRadiusSq
         ? this.indexIntoGradientsArrays(0.0F)
         : this.getColorAtPointOutsideFocusCircle(solutionX, solutionY, currentToFocusSq);
   }

   private float getCurrentToFocusSq(float x, float y) {
      float deltaXSq = x - this.focusX;
      deltaXSq *= deltaXSq;
      float deltaYSq = y - this.focusY;
      deltaYSq *= deltaYSq;
      return deltaXSq + deltaYSq;
   }

   private int getColorAtPointOutsideFocusCircle(float solutionX, float solutionY, float currentToFocusSq) {
      float intersectToFocusSq = this.getCurrentToFocusSq(solutionX, solutionY);
      float gradientPosition = this.computeGradientPosition(currentToFocusSq, intersectToFocusSq);
      return this.indexIntoGradientsArrays(gradientPosition);
   }

   private float computeGradientPosition(float currentToFocusSq, float intersectToFocusSq) {
      float gradientPosition;
      if (this.focusRadius > 0.0F) {
         float currentToFocus = (float)Math.sqrt(currentToFocusSq);
         float intersectToFocus = (float)Math.sqrt(intersectToFocusSq);
         gradientPosition = (currentToFocus - this.focusRadius) / (intersectToFocus - this.focusRadius);
      } else {
         gradientPosition = (float)Math.sqrt(currentToFocusSq / intersectToFocusSq);
      }

      return gradientPosition;
   }

   static {
      for (int i = 0; i < sqrtLookup.length; i++) {
         sqrtLookup[i] = (float)Math.sqrt(i / 2048.0F);
      }
   }
}

