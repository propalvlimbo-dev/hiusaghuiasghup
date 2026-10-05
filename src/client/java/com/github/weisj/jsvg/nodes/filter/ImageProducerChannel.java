package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;

public class ImageProducerChannel implements Channel, PixelProvider {
   @NotNull
   private final ImageProducer producer;
   private BufferedImage rasterBuffer;
   private WritableRaster raster;

   public ImageProducerChannel(@NotNull ImageProducer producer) {
      this.producer = producer;
   }

   @NotNull
   @Override
   public ImageProducer producer() {
      return this.producer;
   }

   @NotNull
   @Override
   public Channel applyFilter(@NotNull ImageFilter filter) {
      return new ImageProducerChannel(new FilteredImageSource(this.producer, filter));
   }

   @NotNull
   @Override
   public Image toImage(@NotNull RenderContext context) {
      return this.rasterBuffer(context);
   }

   @NotNull
   private BufferedImage rasterBuffer(@NotNull RenderContext context) {
      if (this.rasterBuffer == null) {
         this.rasterBuffer = Channel.makeNonAliased(context.platformSupport().createImage(this.producer()));
      }

      return this.rasterBuffer;
   }

   @NotNull
   @Override
   public PixelProvider pixels(@NotNull RenderContext context) {
      if (this.raster == null) {
         this.raster = this.rasterBuffer(context).getRaster();
      }

      return this;
   }

   @Override
   public int pixelAt(double x, double y) {
      int[] rgb = this.raster.getPixel((int)x, (int)y, (int[])null);
      return rgb[3] << 24 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
   }
}

