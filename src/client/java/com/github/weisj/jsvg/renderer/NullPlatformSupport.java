package com.github.weisj.jsvg.renderer;

import java.awt.image.ImageObserver;
import org.jetbrains.annotations.Nullable;

public final class NullPlatformSupport implements PlatformSupport {
   public static final NullPlatformSupport INSTANCE = new NullPlatformSupport();

   private NullPlatformSupport() {
   }

   @Nullable
   @Override
   public ImageObserver imageObserver() {
      return null;
   }

   @Nullable
   @Override
   public PlatformSupport.TargetSurface targetSurface() {
      return null;
   }
}

