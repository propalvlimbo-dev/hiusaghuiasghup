package com.github.weisj.jsvg.parser.resources.impl;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.resources.RenderableResource;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;

public class SVGResource implements RenderableResource {
   @NotNull
   private final SVGDocument document;

   public SVGResource(@NotNull SVGDocument document) {
      this.document = document;
   }

   @NotNull
   @Override
   public FloatSize intrinsicSize(@NotNull RenderContext context) {
      return this.document.size();
   }

   @Override
   public void render(@NotNull Output output, @NotNull RenderContext context, @NotNull AffineTransform imgTransform) {
      output.applyTransform(imgTransform);
      this.document.renderWithPlatform(context.platformSupport(), output, null);
   }
}

