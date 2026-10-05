package com.github.weisj.jsvg.attributes;

import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.supplier.ImmutableSupplier;
import org.jetbrains.annotations.NotNull;

public enum MaskType {
   Luminance(() -> ImageUtil::createLuminosityBuffer),
   Alpha(() -> ImageUtil::createCompatibleTransparentImage);

   @NotNull
   private final ImmutableSupplier<BlittableImage.BufferSurfaceSupplier> bufferSurfaceSupplier;

   MaskType(@NotNull ImmutableSupplier<BlittableImage.BufferSurfaceSupplier> bufferSurfaceSupplier) {
      this.bufferSurfaceSupplier = bufferSurfaceSupplier;
   }

   @NotNull
   public BlittableImage.BufferSurfaceSupplier bufferSurface() {
      return this.bufferSurfaceSupplier.get();
   }

   // $VF: synthetic method
   private static MaskType[] $values() {
      return new MaskType[]{Luminance, Alpha};
   }
}
