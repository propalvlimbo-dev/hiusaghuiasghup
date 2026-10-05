package com.github.weisj.jsvg.renderer.awt;

import com.github.weisj.jsvg.renderer.PlatformSupport;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import org.jetbrains.annotations.NotNull;

public final class AwtComponentPlatformSupport implements PlatformSupport {
   @NotNull
   private final Component component;

   public AwtComponentPlatformSupport(@NotNull Component component) {
      this.component = component;
   }

   @Override
   public float fontSize() {
      Font font = this.component.getFont();
      return font != null ? font.getSize2D() : PlatformSupport.super.fontSize();
   }

   @NotNull
   @Override
   public String fontFamily() {
      Font font = this.component.getFont();
      return font != null ? font.getFamily() : PlatformSupport.super.fontFamily();
   }

   @NotNull
   @Override
   public PlatformSupport.TargetSurface targetSurface() {
      return this.component::repaint;
   }

   @Override
   public boolean isLongLived() {
      return true;
   }

   @NotNull
   @Override
   public ImageObserver imageObserver() {
      return this.component;
   }

   @NotNull
   @Override
   public Image createImage(@NotNull ImageProducer imageProducer) {
      return this.component.createImage(imageProducer);
   }

   @Override
   public String toString() {
      return "AwtComponentSupport{component=" + this.component + '}';
   }
}

