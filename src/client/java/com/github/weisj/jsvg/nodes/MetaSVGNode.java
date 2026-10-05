package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MetaSVGNode implements SVGNode {
   @Nullable
   @Override
   public String id() {
      return null;
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
   }
}

