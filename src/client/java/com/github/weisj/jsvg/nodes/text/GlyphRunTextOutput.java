package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import java.awt.geom.AffineTransform;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GlyphRunTextOutput implements TextOutput {
   private final StringBuilder codepoints = new StringBuilder();
   @Nullable
   private AffineTransform glyphTransform;
   @Nullable
   private RenderContext context;

   protected abstract void glyphRun(@NotNull String var1, @NotNull AffineTransform var2, @NotNull RenderContext var3);

   protected abstract void onTextStart();

   protected abstract void onTextEnd();

   @Override
   public void codepoint(@NotNull String codepoint, @NotNull AffineTransform glyphTransform, @NotNull RenderContext context) {
      if (this.context == null) {
         this.context = context;
      }

      if (this.glyphTransform == null) {
         this.glyphTransform = glyphTransform;
      }

      this.codepoints.append(codepoint);
   }

   private void reset() {
      this.context = null;
      this.codepoints.setLength(0);
      this.glyphTransform = null;
   }

   @Override
   public final void beginText() {
      this.reset();
      this.onTextStart();
   }

   @Override
   public void glyphRunBreak() {
      if (this.codepoints.length() != 0) {
         this.glyphRun(this.codepoints.toString(), Objects.requireNonNull(this.glyphTransform), Objects.requireNonNull(this.context));
         this.reset();
      }
   }

   @Override
   public final void endText() {
      this.glyphRunBreak();
      this.onTextEnd();
   }
}

