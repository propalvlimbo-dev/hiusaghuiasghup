package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.EdgeMode;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageFilter;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeGaussianBlur extends AbstractFilterPrimitive {
   public static final String TAG = "fegaussianblur";
   private static final double SQRT_2_PI = Math.sqrt(Math.PI * 2);
   private static final double THREE_QUARTER_SQRT_2_PI = SQRT_2_PI * 3.0 / 4.0;
   private static final float KERNEL_PRECISION = 0.001F;
   private static final double BOX_BLUR_APPROXIMATION_THRESHOLD = 2.0;
   private float[] stdDeviation;
   private EdgeMode edgeMode;
   private double xCurrent;
   private double yCurrent;
   private Kernel xBlur;
   private Kernel yBlur;
   private boolean onlyAlpha;

   @NotNull
   @Override
   public String tagName() {
      return "fegaussianblur";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.stdDeviation = attributeNode.getFloatList("stdDeviation");
      this.edgeMode = attributeNode.getEnum("edgeMode", EdgeMode.Duplicate);
   }

   @Internal
   public void setOnlyAlpha(boolean onlyAlpha) {
      this.onlyAlpha = onlyAlpha;
   }

   private double[] computeAbsoluteStdDeviation(@Nullable AffineTransform at) {
      if (this.stdDeviation.length == 0) {
         return new double[]{0.0, 0.0};
      }

      double xSigma = this.stdDeviation[0];
      double ySigma = this.stdDeviation[Math.min(this.stdDeviation.length - 1, 1)];
      if (at != null) {
         xSigma *= GeometryUtil.scaleXOfTransform(at);
         ySigma *= GeometryUtil.scaleYOfTransform(at);
      }

      return new double[]{xSigma, ySigma};
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      LayoutBounds input = this.impl().layoutInput(filterLayoutContext);
      double[] sigma = this.computeAbsoluteStdDeviation(null);
      int hExtend = kernelDiameterForStandardDeviation(sigma[0]);
      int vExtend = kernelDiameterForStandardDeviation(sigma[1]);
      this.impl().saveLayoutResult(input.grow(hExtend, vExtend, filterLayoutContext), filterLayoutContext);
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      if (this.stdDeviation.length == 0) {
         this.impl().noop(filterContext);
      } else {
         double[] sigma = this.computeAbsoluteStdDeviation(filterContext.info().output().transform());
         double xSigma = sigma[0];
         double ySigma = sigma[1];
         if (xSigma <= 0.0 && ySigma <= 0.0) {
            this.impl().noop(filterContext);
         } else {
            Channel inputChannel = this.impl().inputChannel(filterContext);
            if (this.onlyAlpha) {
               inputChannel = inputChannel.alphaChannel();
            }

            ImageProducer input = inputChannel.producer();
            Kernel xBlurKernel = null;
            Kernel yBlurKernel = null;
            int dX = kernelDiameterForStandardDeviation(xSigma);
            int dY = kernelDiameterForStandardDeviation(ySigma);
            if (xSigma > 0.0 && xSigma < 2.0) {
               xBlurKernel = this.createConvolveKernel(dX, xSigma, true);
            }

            if (ySigma > 0.0 && ySigma < 2.0) {
               yBlurKernel = this.createConvolveKernel(dX, ySigma, false);
            }

            ImageProducer output = this.edgeMode
               .convolve(context, filterContext, input, new FeGaussianBlur.MixedQualityConvolveOperation(xBlurKernel, yBlurKernel, dX, dY));
            this.impl().saveResult(new ImageProducerChannel(output), filterContext);
         }
      }
   }

   @NotNull
   private Kernel createConvolveKernel(int diameter, double sigma, boolean horizontal) {
      if (horizontal && this.xBlur != null && this.xCurrent == sigma) {
         return this.xBlur;
      }

      if (!horizontal && this.yBlur != null && this.yCurrent == sigma) {
         return this.yBlur;
      }

      if (horizontal) {
         this.xCurrent = sigma;
      } else {
         this.yCurrent = sigma;
      }

      float[] data = computeGaussianKernelData(diameter, sigma);
      if (horizontal) {
         this.xBlur = new Kernel(diameter, 1, data);
      } else {
         this.yBlur = new Kernel(1, diameter, data);
      }

      return horizontal ? this.xBlur : this.yBlur;
   }

   private static float normalConvolve(float x, double standardDeviation) {
      return (float)(Math.pow(Math.E, -x * x / (2.0 * standardDeviation * standardDeviation)) / (standardDeviation * SQRT_2_PI));
   }

   private static float[] computeGaussianKernelData(int diameter, double standardDeviation) {
      float[] data = new float[diameter];
      int mid = diameter / 2;
      float total = 0.0F;

      for (int i = 0; i < diameter; i++) {
         data[i] = normalConvolve((float)i - mid, standardDeviation);
         total += data[i];
      }

      if (total > 0.0F) {
         for (int i = 0; i < diameter; i++) {
            data[i] /= total;
         }
      }

      return data;
   }

   public static int kernelDiameterForStandardDeviation(double standardDeviation) {
      if (!(standardDeviation < 2.0)) {
         return (int)Math.floor(THREE_QUARTER_SQRT_2_PI * standardDeviation + 0.5);
      }

      float areaSum = (float)(0.5 / (standardDeviation * SQRT_2_PI));

      int i;
      for (i = 0; areaSum < 0.49899999995250255; i++) {
         areaSum += normalConvolve(i, standardDeviation);
      }

      return i * 2 + 1;
   }

   private static final class MixedQualityConvolveOperation implements EdgeMode.ConvolveOperation {
      @Nullable
      private final Kernel xKernel;
      @Nullable
      private final Kernel yKernel;
      private final int dX;
      private final int dY;

      private MixedQualityConvolveOperation(@Nullable Kernel xKernel, @Nullable Kernel yKernel, int dX, int dY) {
         this.xKernel = xKernel;
         this.yKernel = yKernel;
         this.dX = dX;
         this.dY = dY;
      }

      @NotNull
      @Override
      public Dimension maximumKernelSize() {
         return new Dimension(this.xKernel != null ? this.xKernel.getXOrigin() : this.dX, this.yKernel != null ? this.yKernel.getXOrigin() : this.dY);
      }

      @NotNull
      @Override
      public ImageProducer convolve(@NotNull BufferedImage image, @Nullable RenderingHints hints, int awtEdgeMode) {
         WritableRaster raster = image.getRaster();
         if (!image.getColorModel().isAlphaPremultiplied()) {
            throw new IllegalStateException("Image should be premultiplied");
         } else if (this.xKernel != null && this.yKernel != null) {
            BufferedImageOp op = new MultiConvolveOp(
               new ConvolveOp[]{new ConvolveOp(this.xKernel, awtEdgeMode, hints), new ConvolveOp(this.yKernel, awtEdgeMode, hints)}
            );
            return new FilteredImageSource(image.getSource(), new BufferedImageFilter(op));
         } else if (this.xKernel != null) {
            this.verticalBoxBlur(raster);
            return new FilteredImageSource(image.getSource(), new BufferedImageFilter(new ConvolveOp(this.xKernel, awtEdgeMode, hints)));
         } else if (this.yKernel != null) {
            this.horizontalBoxBlur(raster);
            return new FilteredImageSource(image.getSource(), new BufferedImageFilter(new ConvolveOp(this.yKernel, awtEdgeMode, hints)));
         } else {
            this.horizontalBoxBlur(raster);
            this.verticalBoxBlur(raster);
            return image.getSource();
         }
      }

      private void horizontalBoxBlur(@NotNull WritableRaster raster) {
         if ((this.dX & 1) == 0) {
            InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, this.dX, this.dX / 2);
            InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, this.dX, this.dX / 2 - 1);
            InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, this.dX + 1, this.dX / 2);
         } else {
            InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, this.dX, this.dX / 2);
            InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, this.dX, this.dX / 2);
            InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, this.dX, this.dX / 2);
         }
      }

      private void verticalBoxBlur(@NotNull WritableRaster raster) {
         if ((this.dY & 1) == 0) {
            InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, this.dY, this.dY / 2);
            InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, this.dY, this.dY / 2 - 1);
            InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, this.dY + 1, this.dY / 2);
         } else {
            InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, this.dY, this.dY / 2);
            InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, this.dY, this.dY / 2);
            InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, this.dY, this.dY / 2);
         }
      }
   }
}

