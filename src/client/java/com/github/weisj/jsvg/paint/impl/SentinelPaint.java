package com.github.weisj.jsvg.paint.impl;

import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class SentinelPaint implements SVGPaint {
   private final String name;

   public SentinelPaint(@NotNull String name) {
      this.name = name;
   }

   @Override
   public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape, @Nullable Rectangle2D bounds) {
      throw new IllegalStateException("Sentinel color " + this.name + " shouldn't be used for painting directly");
   }

   @Override
   public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape, @Nullable Rectangle2D bounds) {
      throw new IllegalStateException("Sentinel color " + this.name + " shouldn't be used for painting directly");
   }

   @Override
   public String toString() {
      return "SVGPaint." + this.name;
   }
}

