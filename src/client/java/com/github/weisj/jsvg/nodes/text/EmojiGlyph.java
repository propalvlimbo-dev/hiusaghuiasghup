package com.github.weisj.jsvg.nodes.text;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.jetbrains.annotations.NotNull;

public class EmojiGlyph extends Glyph {
   @NotNull
   private final String codepoint;
   BufferedImage largeBitmap;

   public EmojiGlyph(@NotNull String codepoint, float advance) {
      super(new Rectangle(), advance, false);
      this.codepoint = codepoint;
   }

   @NotNull
   public String codepoint() {
      return this.codepoint;
   }
}

