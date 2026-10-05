package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSVGNode implements SVGNode {
   @Nullable
   private String id;

   @Nullable
   @Override
   public String id() {
      return this.id;
   }

   @MustBeInvokedByOverriders
   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      this.id = attributeNode.getValue("id");
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "{id='" + this.id + '\'' + '}';
   }
}

