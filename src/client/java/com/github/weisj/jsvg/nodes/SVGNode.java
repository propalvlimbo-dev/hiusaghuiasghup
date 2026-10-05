package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SVGNode {
   @NotNull
   String tagName();

   @Nullable
   String id();

   void build(@NotNull AttributeNode var1);

   default void addContent(@NotNull TextContent.Segment content) {
   }
}
