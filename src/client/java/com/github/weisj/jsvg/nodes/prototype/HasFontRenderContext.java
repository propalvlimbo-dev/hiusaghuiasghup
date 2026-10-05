package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.renderer.impl.context.FontRenderContext;
import org.jetbrains.annotations.NotNull;

public interface HasFontRenderContext {
   @NotNull
   FontRenderContext fontRenderContext();
}
