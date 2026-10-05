package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.renderer.impl.context.FontRenderContext;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import org.jetbrains.annotations.NotNull;

public interface HasContext extends HasPaintContext, HasFontContext, HasFontRenderContext {
   interface ByDelegate extends HasContext {
      @NotNull
      HasContext contextDelegate();

      @NotNull
      @Override
      default Mutator<MeasurableFontSpec> fontSpec() {
         return this.contextDelegate().fontSpec();
      }

      @NotNull
      @Override
      default FontRenderContext fontRenderContext() {
         return this.contextDelegate().fontRenderContext();
      }

      @NotNull
      @Override
      default Mutator<PaintContext> paintContext() {
         return this.contextDelegate().paintContext();
      }
   }
}
