package com.github.weisj.jsvg.renderer.output.impl;

import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import org.jetbrains.annotations.NotNull;

public final class RenderingHintsUtil {
   private RenderingHintsUtil() {
   }

   public static void setupSVGRenderingHints(@NotNull Graphics2D g) {
      Object aaHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
      if (aaHint != RenderingHints.VALUE_ANTIALIAS_DEFAULT) {
         setSVGRenderingHint(
            g,
            SVGRenderingHints.KEY_IMAGE_ANTIALIASING,
            aaHint == RenderingHints.VALUE_ANTIALIAS_ON ? SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON : SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF
         );
      } else {
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }

      if (g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL) == RenderingHints.VALUE_STROKE_DEFAULT) {
         g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      }

      setSVGRenderingHint(g, SVGRenderingHints.KEY_MASK_CLIP_RENDERING, SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_DEFAULT);
   }

   private static void setSVGRenderingHint(@NotNull Graphics2D g, @NotNull Key key, @NotNull Object o) {
      if (g.getRenderingHint(key) == null) {
         g.setRenderingHint(key, o);
      }
   }
}

