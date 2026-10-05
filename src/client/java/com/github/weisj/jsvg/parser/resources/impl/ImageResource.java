package com.github.weisj.jsvg.parser.resources.impl;

import com.github.weisj.jsvg.parser.resources.RenderableResource;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.jetbrains.annotations.NotNull;

public class ImageResource implements RenderableResource {
   @NotNull
   private final BufferedImage image;

   public ImageResource(@NotNull BufferedImage image) {
      this.image = image;
   }

   @NotNull
   @Override
   public FloatSize intrinsicSize(@NotNull RenderContext context) {
      return new FloatSize(this.image.getWidth(context.platformSupport().imageObserver()), this.image.getHeight(context.platformSupport().imageObserver()));
   }

   @Override
   public void render(@NotNull Output output, @NotNull RenderContext context, @NotNull AffineTransform imgTransform) {
      int imgWidth = this.image.getWidth();
      int imgHeight = this.image.getHeight();
      Object imageAntialiasing = output.renderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING);
      if (imageAntialiasing == SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF) {
         output.drawImage(this.image, imgTransform, context.platformSupport().imageObserver());
      } else {
         output.applyTransform(imgTransform);
         Rectangle imgRect = new Rectangle(0, 0, imgWidth, imgHeight);
         output.setPaint(new TexturePaint(this.image, imgRect));
         output.fillShape(imgRect);
      }
   }
}

