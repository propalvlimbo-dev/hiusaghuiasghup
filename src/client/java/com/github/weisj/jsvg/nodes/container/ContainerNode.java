package com.github.weisj.jsvg.nodes.container;

import com.github.weisj.jsvg.nodes.SVGNode;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class ContainerNode extends BaseContainerNode<SVGNode> {
   private final List<@NotNull SVGNode> children = new ArrayList<>();

   @Override
   protected void doAdd(@NotNull SVGNode node) {
      this.children.add(node);
   }

   @Override
   public List<? extends @NotNull SVGNode> children() {
      return this.children;
   }
}

