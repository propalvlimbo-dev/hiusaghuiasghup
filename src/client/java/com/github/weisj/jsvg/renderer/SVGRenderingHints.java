package com.github.weisj.jsvg.renderer;

import java.awt.RenderingHints;
import org.jetbrains.annotations.Nullable;

public final class SVGRenderingHints {
   private static final int P_KEY_IMAGE_ANTIALIASING = 1;
   private static final int P_KEY_SOFT_CLIPPING = 2;
   private static final int P_KEY_CACHE_OFFSCREEN_IMAGE = 3;
   private static final int P_KEY_MASK_CLIP_RENDERING = 4;
   public static final RenderingHints.Key KEY_IMAGE_ANTIALIASING = new SVGRenderingHints.Key(1);
   public static final Object VALUE_IMAGE_ANTIALIASING_ON = SVGRenderingHints.Value.ON;
   public static final Object VALUE_IMAGE_ANTIALIASING_OFF = SVGRenderingHints.Value.OFF;
   public static final RenderingHints.Key KEY_SOFT_CLIPPING = new SVGRenderingHints.Key(2);
   public static final Object VALUE_SOFT_CLIPPING_ON = SVGRenderingHints.Value.ON;
   public static final Object VALUE_SOFT_CLIPPING_OFF = SVGRenderingHints.Value.OFF;
   public static final RenderingHints.Key KEY_MASK_CLIP_RENDERING = new SVGRenderingHints.Key(4);
   public static final Object VALUE_MASK_CLIP_RENDERING_FAST = SVGRenderingHints.Value.ON;
   public static final Object VALUE_MASK_CLIP_RENDERING_ACCURACY = SVGRenderingHints.Value.OFF;
   public static final Object VALUE_MASK_CLIP_RENDERING_DEFAULT = VALUE_MASK_CLIP_RENDERING_FAST;
   public static final RenderingHints.Key KEY_CACHE_OFFSCREEN_IMAGE = new SVGRenderingHints.Key(3);
   public static final Object VALUE_USE_CACHE = SVGRenderingHints.Value.ON;
   public static final Object VALUE_NO_CACHE = SVGRenderingHints.Value.OFF;

   private SVGRenderingHints() {
   }

   private static final class Key extends RenderingHints.Key {
      private Key(int privateKey) {
         super(privateKey);
      }

      @Override
      public boolean isCompatibleValue(@Nullable Object val) {
         return val instanceof SVGRenderingHints.Value;
      }
   }

   private enum Value {
      ON,
      OFF;

      // $VF: synthetic method
      private static SVGRenderingHints.Value[] $values() {
         return new SVGRenderingHints.Value[]{ON, OFF};
      }
   }
}

