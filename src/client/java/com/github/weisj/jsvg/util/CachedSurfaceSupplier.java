package com.github.weisj.jsvg.util;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CachedSurfaceSupplier {
   @NotNull
   private final BlittableImage.BufferSurfaceSupplier surfaceSupplier;
   @NotNull
   private final ThreadLocal<CachedSurfaceSupplier.Cache> cache = ThreadLocal.withInitial(() -> new CachedSurfaceSupplier.Cache());

   public CachedSurfaceSupplier(BlittableImage.@NotNull BufferSurfaceSupplier surfaceSupplier) {
      this.surfaceSupplier = surfaceSupplier;
   }

   public boolean useCache(@NotNull Output output, @NotNull RenderContext renderContext) {
      return renderContext.platformSupport().isLongLived()
         && output.renderingHint(SVGRenderingHints.KEY_CACHE_OFFSCREEN_IMAGE) != SVGRenderingHints.VALUE_NO_CACHE;
   }

   @NotNull
   public BlittableImage.BufferSurfaceSupplier surfaceSupplier(boolean useCache) {
      return !useCache ? this.surfaceSupplier : this::createBufferSurface;
   }

   @NotNull
   public BufferedImage createBufferSurface(@Nullable AffineTransform at, double width, double height) {
      if (at != null) {
         throw new UnsupportedOperationException("CachedSurfaceSupplier does not support transformations");
      }

      CachedSurfaceSupplier.Cache c = this.cache.get();
      Iterator<CachedSurfaceSupplier.CachedImage> it = c.images.iterator();

      while (it.hasNext()) {
         CachedSurfaceSupplier.CachedImage cachedImage = it.next();
         BufferedImage img = cachedImage.image.get();
         if (img == null) {
            it.remove();
         } else if (!cachedImage.inUse && img.getWidth() >= width && img.getHeight() >= height) {
            cachedImage.inUse = true;
            c.lastIssuedCleaner = new CachedSurfaceSupplier.ResourceCleaner(null, () -> cachedImage.free());
            return img.getSubimage(0, 0, (int)width, (int)height);
         }
      }

      BufferedImage image = this.surfaceSupplier.createBufferSurface(null, width, height);
      CachedSurfaceSupplier.CachedImage cachedImage = new CachedSurfaceSupplier.CachedImage(image);
      c.images.add(cachedImage);
      c.lastIssuedCleaner = new CachedSurfaceSupplier.ResourceCleaner(null, () -> cachedImage.free());
      return image;
   }

   @Nullable
   public CachedSurfaceSupplier.ResourceCleaner resourceCleaner(Object owner, boolean useCache) {
      if (!useCache) {
         return null;
      }

      CachedSurfaceSupplier.ResourceCleaner cleaner = this.cache.get().lastIssuedCleaner;
      return cleaner != null ? cleaner.withOwner(owner) : null;
   }

   private static class Cache {
      private final List<CachedSurfaceSupplier.CachedImage> images = new ArrayList<>();
      @Nullable
      private CachedSurfaceSupplier.ResourceCleaner lastIssuedCleaner;

      private Cache() {
      }
   }

   private static class CachedImage {
      @NotNull
      private final SoftReference<BufferedImage> image;
      private boolean inUse = true;

      private CachedImage(@NotNull BufferedImage image) {
         this.image = new SoftReference<>(image);
      }

      private void free() {
         this.inUse = false;
      }
   }

   public static class ResourceCleaner {
      @Nullable
      private final Object owner;
      @Nullable
      private Runnable cleaner;

      public ResourceCleaner(@Nullable Object owner, @Nullable Runnable cleaner) {
         this.owner = owner;
         this.cleaner = cleaner;
      }

      public void clean(Object owner) {
         if (this.owner == owner) {
            if (this.cleaner == null) {
               throw new IllegalStateException("Resource already cleaned");
            }

            this.cleaner.run();
            this.cleaner = null;
         }
      }

      @NotNull
      private CachedSurfaceSupplier.ResourceCleaner withOwner(Object owner) {
         return new CachedSurfaceSupplier.ResourceCleaner(owner, this.cleaner);
      }
   }
}

