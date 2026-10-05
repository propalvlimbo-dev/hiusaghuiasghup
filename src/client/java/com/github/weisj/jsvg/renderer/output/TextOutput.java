package com.github.weisj.jsvg.renderer.output;

import com.github.weisj.jsvg.nodes.text.NullTextOutput;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;

public interface TextOutput {
   void codepoint(@NotNull String var1, @NotNull AffineTransform var2, @NotNull RenderContext var3);

   @NotNull
   static TextOutput createDefault() {
      return NullTextOutput.INSTANCE;
   }

   void beginText();

   void glyphRunBreak();

   void endText();
}
