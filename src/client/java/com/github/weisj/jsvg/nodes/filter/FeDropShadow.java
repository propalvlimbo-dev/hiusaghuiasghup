package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeDropShadow extends ChainedFilterPrimitive {
   public static final String TAG = "fedropshadow";
   private FilterPrimitive[] primitives;

   @NotNull
   @Override
   public String tagName() {
      return "fedropshadow";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      AttributeNode child = attributeNode.copy();
      Map<String, String> attributes = child.attributes();
      String resultKey = "result";
      FilterChannelKey inputId = attributeNode.getFilterChannelKey("in", this.outerLastResult);
      String resultId = attributes.get(resultKey);
      if (resultId == null) {
         resultId = "dropshadow-" + attributeNode.hashCode();
      }

      attributes.put(resultKey, resultId);
      FeGaussianBlur blur = new FeGaussianBlur();
      blur.build(child);
      blur.setOnlyAlpha(true);
      attributes.put("in", resultId);
      String offsetResultId = resultId + "-offset-" + resultId.hashCode();
      attributes.put(resultKey, offsetResultId);
      FeOffset offset = new FeOffset();
      offset.build(child);
      attributes.put(resultKey, resultId);
      FeFlood flood = new FeFlood();
      flood.build(child);
      attributes.put("in2", offsetResultId);
      attributes.put("operator", "in");
      FeComposite composite = new FeComposite();
      composite.build(child);
      FeMergeNode node1 = new FeMergeNode();
      node1.build(child);
      attributes.put("in", inputId.key().toString());
      FeMergeNode node2 = new FeMergeNode();
      node2.build(child);
      FeMerge merge = new FeMerge();
      merge.addChild(null, node1);
      merge.addChild(null, node2);
      merge.build(child);
      this.primitives = new FilterPrimitive[]{blur, offset, flood, composite, merge};
   }

   @NotNull
   @Override
   protected FilterPrimitive[] primitives() {
      return this.primitives;
   }
}

