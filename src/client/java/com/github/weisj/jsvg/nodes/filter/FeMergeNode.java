package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.None)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeMergeNode extends AbstractSVGNode {
   public static final String TAG = "feMergeNode";
   private FilterChannelKey inputChannel;

   @NotNull
   @Override
   public String tagName() {
      return "feMergeNode";
   }

   @NotNull
   public FilterChannelKey inputChannel() {
      return this.inputChannel;
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.inputChannel = attributeNode.getFilterChannelKey("in", DefaultFilterChannel.LastResult);
   }
}

