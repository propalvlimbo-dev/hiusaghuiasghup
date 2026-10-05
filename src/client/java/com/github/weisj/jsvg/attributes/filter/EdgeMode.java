package com.github.weisj.jsvg.attributes.filter;

import com.github.weisj.jsvg.nodes.filter.FilterContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import com.github.weisj.jsvg.util.ImageUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum EdgeMode {
   Duplicate {
      @Override
      public ImageProducer convolve(
         @NotNull RenderContext context,
         @NotNull FilterContext filterContext,
         @NotNull ImageProducer producer,
         @NotNull EdgeMode.ConvolveOperation convolveOperation
      ) {
         return EdgeMode.convolveDuplicate(context, filterContext, producer, convolveOperation);
      }
   },
   Wrap {
      @Override
      public ImageProducer convolve(
         @NotNull RenderContext context,
         @NotNull FilterContext filterContext,
         @NotNull ImageProducer producer,
         @NotNull EdgeMode.ConvolveOperation convolveOperation
      ) {
         return EdgeMode.convolveWrap(context, filterContext, producer, convolveOperation);
      }
   },
   None {
      @Override
      public ImageProducer convolve(
         @NotNull RenderContext context,
         @NotNull FilterContext filterContext,
         @NotNull ImageProducer producer,
         @NotNull EdgeMode.ConvolveOperation convolveOperation
      ) {
         return EdgeMode.applyConvolutions(filterContext.renderingHints(), ImageUtil.copy(context, producer, ImageUtil.Premultiplied.Yes), convolveOperation, 0);
      }
   };

   EdgeMode() {
   }

   public abstract ImageProducer convolve(
      @NotNull RenderContext var1, @NotNull FilterContext var2, @NotNull ImageProducer var3, @NotNull EdgeMode.ConvolveOperation var4
   );

   private static EdgeMode.EdgeModeImage prepareEdgeModeImage(
      @NotNull RenderContext context, @NotNull ImageProducer producer, @NotNull EdgeMode.ConvolveOperation convolveOperation
   ) {
      Image img = context.platformSupport().createImage(producer);
      int width = img.getWidth(null);
      int height = img.getHeight(null);
      Dimension kernelSize = convolveOperation.maximumKernelSize();
      int xSize = kernelSize.width;
      int ySize = kernelSize.height;
      BufferedImage bufferedImage = ImageUtil.createCompatibleTransparentImage(width + xSize, height + ySize, ImageUtil.Premultiplied.Yes);
      Graphics2D g = GraphicsUtil.createGraphics(bufferedImage);
      int xOff = xSize / 2;
      int yOff = ySize / 2;
      g.translate(xOff, yOff);
      g.drawImage(img, null, null);
      g.dispose();
      return new EdgeMode.EdgeModeImage(bufferedImage, xOff, yOff, width, height);
   }

   @NotNull
   private static ImageProducer convolveDuplicate(
      @NotNull RenderContext context,
      @NotNull FilterContext filterContext,
      @NotNull ImageProducer producer,
      @NotNull EdgeMode.ConvolveOperation convolveOperation
   ) {
      EdgeMode.EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, convolveOperation);
      int xOff = edgeModeImage.xOff;
      int yOff = edgeModeImage.yOff;
      int width = edgeModeImage.width;
      int height = edgeModeImage.height;
      Graphics2D g = GraphicsUtil.createGraphics(edgeModeImage.img);
      BufferedImage top = edgeModeImage.img.getSubimage(xOff, yOff, width, 1);
      BufferedImage left = edgeModeImage.img.getSubimage(xOff, yOff, 1, height);
      BufferedImage right = edgeModeImage.img.getSubimage(xOff + width - 1, yOff, 1, height);
      BufferedImage bottom = edgeModeImage.img.getSubimage(xOff, yOff + height - 1, width, 1);
      g.drawImage(top, xOff, 0, width, yOff, null);
      g.drawImage(bottom, xOff, yOff + height, width, yOff, null);
      g.drawImage(left, 0, yOff, xOff, height, null);
      g.drawImage(right, xOff + height, yOff, xOff, height, null);
      Color topLeft = new Color(top.getRGB(0, 0), true);
      Color topRight = new Color(top.getRGB(top.getWidth() - 1, 0), true);
      Color bottomLeft = new Color(bottom.getRGB(0, 0), true);
      Color bottomRight = new Color(bottom.getRGB(bottom.getWidth() - 1, 0), true);
      g.setColor(topLeft);
      g.fillRect(0, 0, xOff, yOff);
      g.setColor(topRight);
      g.fillRect(xOff + width, 0, xOff, yOff);
      g.setColor(bottomLeft);
      g.fillRect(0, yOff + height, xOff, yOff);
      g.setColor(bottomRight);
      g.fillRect(xOff + width, yOff + height, xOff, yOff);
      g.dispose();
      ImageProducer output = applyConvolutions(filterContext.renderingHints(), edgeModeImage.img, convolveOperation, 1);
      CropImageFilter cropImageFilter = new CropImageFilter(xOff, yOff, width, height);
      return new FilteredImageSource(output, cropImageFilter);
   }

   private static ImageProducer convolveWrap(
      @NotNull RenderContext context,
      @NotNull FilterContext filterContext,
      @NotNull ImageProducer producer,
      @NotNull EdgeMode.ConvolveOperation convolveOperation
   ) {
      EdgeMode.EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, convolveOperation);
      int xOff = edgeModeImage.xOff;
      int yOff = edgeModeImage.yOff;
      int width = edgeModeImage.width;
      int height = edgeModeImage.height;
      Graphics2D g = GraphicsUtil.createGraphics(edgeModeImage.img);
      BufferedImage top = edgeModeImage.img.getSubimage(xOff, yOff, width, yOff);
      BufferedImage left = edgeModeImage.img.getSubimage(xOff, yOff, xOff, height);
      BufferedImage right = edgeModeImage.img.getSubimage(width - 1, yOff, xOff, height);
      BufferedImage bottom = edgeModeImage.img.getSubimage(xOff, height - 1, width, yOff);
      BufferedImage topLeft = edgeModeImage.img.getSubimage(xOff, yOff, xOff, yOff);
      BufferedImage topRight = edgeModeImage.img.getSubimage(width - 1, yOff, xOff, yOff);
      BufferedImage bottomLeft = edgeModeImage.img.getSubimage(xOff, height - 1, width, yOff);
      BufferedImage bottomRight = edgeModeImage.img.getSubimage(width - 1, height - 1, xOff, yOff);
      g.drawImage(bottom, xOff, 0, null);
      g.drawImage(top, xOff, yOff + height, null);
      g.drawImage(right, 0, yOff, null);
      g.drawImage(left, xOff + width, yOff, null);
      g.drawImage(bottomRight, 0, 0, null);
      g.drawImage(bottomLeft, xOff + width, 0, null);
      g.drawImage(topRight, 0, yOff + height, null);
      g.drawImage(topLeft, xOff + width, yOff + height, null);
      ImageProducer output = applyConvolutions(filterContext.renderingHints(), edgeModeImage.img, convolveOperation, 1);
      CropImageFilter cropImageFilter = new CropImageFilter(xOff, yOff, width, height);
      return new FilteredImageSource(output, cropImageFilter);
   }

   private static ImageProducer applyConvolutions(
      @Nullable RenderingHints hints, @NotNull BufferedImage image, @NotNull EdgeMode.ConvolveOperation convolveOperation, int awtEdgeMode
   ) {
      return convolveOperation.convolve(image, hints, awtEdgeMode);
   }

   // $VF: synthetic method
   private static EdgeMode[] $values() {
      return new EdgeMode[]{Duplicate, Wrap, None};
   }

   public interface ConvolveOperation {
      @NotNull
      Dimension maximumKernelSize();

      @NotNull
      ImageProducer convolve(@NotNull BufferedImage var1, @Nullable RenderingHints var2, int var3);
   }

   private static final class EdgeModeImage {
      @NotNull
      private final BufferedImage img;
      private final int xOff;
      private final int yOff;
      private final int width;
      private final int height;

      private EdgeModeImage(@NotNull BufferedImage img, int xOff, int yOff, int width, int height) {
         this.img = img;
         this.xOff = xOff;
         this.yOff = yOff;
         this.width = width;
         this.height = height;
      }
   }
}
