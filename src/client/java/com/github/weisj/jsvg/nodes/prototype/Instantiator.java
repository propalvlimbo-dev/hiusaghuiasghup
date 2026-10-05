package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.ContextElementAttributes;
import org.jetbrains.annotations.NotNull;

public interface Instantiator {
   boolean canInstantiate(@NotNull SVGNode var1);

   @NotNull
   default ContextElementAttributes createContextAttributes(@NotNull RenderContext context) {
      return new ContextElementAttributes(context.fillPaint(), context.strokePaint());
   }
}
