package com.github.weisj.jsvg.renderer;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlatformSupport {
   @Nullable
   ImageObserver imageObserver();

   @Nullable
   PlatformSupport.TargetSurface targetSurface();

   default float fontSize() {
      return SVGFont.defaultFontSize();
   }

   @NotNull
   default String fontFamily() {
      return SVGFont.defaultFontFamily();
   }

   @NotNull
   default Image createImage(@NotNull ImageProducer imageProducer) {
      return Toolkit.getDefaultToolkit().createImage(imageProducer);
   }

   default boolean isLongLived() {
      return false;
   }

   interface TargetSurface {
      void repaint();
   }
}
