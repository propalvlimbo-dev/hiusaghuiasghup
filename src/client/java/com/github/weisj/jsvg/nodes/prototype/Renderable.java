package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import org.jetbrains.annotations.NotNull;

public interface Renderable {
   default boolean requiresInstantiation() {
      return false;
   }

   boolean isVisible(@NotNull RenderContext var1);

   void render(@NotNull RenderContext var1, @NotNull Output var2);

   default boolean parseIsVisible(@NotNull AttributeNode node) {
      return parseVisibility(node);
   }

   static boolean parseVisibility(@NotNull AttributeNode node) {
      return !"none".equals(node.getValue("display")) && !"hidden".equals(node.getValue("visibility")) && !"collapse".equals(node.getValue("visibility"));
   }
}
