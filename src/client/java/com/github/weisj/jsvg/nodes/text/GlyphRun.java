package com.github.weisj.jsvg.nodes.text;

import java.awt.Shape;
import java.util.List;
import org.jetbrains.annotations.NotNull;

class GlyphRun extends AbstractGlyphRun<Shape> {
   GlyphRun(@NotNull Shape shape, @NotNull AbstractGlyphRun.Metrics metrics, @NotNull List<AbstractGlyphRun.PaintableEmoji> emojis) {
      super(shape, metrics, emojis);
   }
}

