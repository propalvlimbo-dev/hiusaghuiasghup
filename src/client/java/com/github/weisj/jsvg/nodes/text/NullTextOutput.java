package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;

public final class NullTextOutput implements TextOutput {
   public static final NullTextOutput INSTANCE = new NullTextOutput();

   private NullTextOutput() {
   }

   @Override
   public void codepoint(@NotNull String codepoint, @NotNull AffineTransform glyphTransform, @NotNull RenderContext context) {
   }

   @Override
   public void beginText() {
   }

   @Override
   public void glyphRunBreak() {
   }

   @Override
   public void endText() {
   }
}

