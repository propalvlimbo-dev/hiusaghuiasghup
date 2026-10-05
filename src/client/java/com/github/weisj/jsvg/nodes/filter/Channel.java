package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import org.jetbrains.annotations.NotNull;

public interface Channel {
   @NotNull
   ImageProducer producer();

   @NotNull
   default Image toImage(@NotNull RenderContext context) {
      return context.platformSupport().createImage(this.producer());
   }

   @NotNull
   default BufferedImage toBufferedImageNonAliased(@NotNull RenderContext context) {
      return makeNonAliased(this.toImage(context));
   }

   @NotNull
   static BufferedImage makeNonAliased(@NotNull Image img) {
      BufferedImage bufferedImage = ImageUtil.createCompatibleTransparentImage(img.getWidth(null), img.getHeight(null));
      Graphics imageGraphics = bufferedImage.getGraphics();
      imageGraphics.drawImage(img, 0, 0, null);
      imageGraphics.dispose();
      return bufferedImage;
   }

   @NotNull
   Channel applyFilter(@NotNull ImageFilter var1);

   @NotNull
   PixelProvider pixels(@NotNull RenderContext var1);

   @NotNull
   default Channel alphaChannel() {
      return this.applyFilter(new AlphaImageFilter());
   }
}
