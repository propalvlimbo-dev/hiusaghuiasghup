package com.github.weisj.jsvg.nodes.text;

import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Float;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

class MutableGlyphRun extends AbstractGlyphRun<Path2D> {
   public MutableGlyphRun() {
      super(new Float(), AbstractGlyphRun.Metrics.createDefault(), new ArrayList<>());
   }

   public void append(@NotNull GlyphRun glyphRun) {
      this.shape().append(glyphRun.shape(), false);
      this.metrics().union(glyphRun.metrics());
      this.emojis().addAll(glyphRun.emojis());
   }
}

