package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.BlendMode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import java.awt.Composite;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeBlend extends AbstractCompositeFilterPrimitive {
   public static final String TAG = "feblend";
   private Composite composite;

   @NotNull
   @Override
   public String tagName() {
      return "feblend";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      BlendMode blendMode = attributeNode.getEnum("mode", BlendMode.Normal);
      this.composite = BlendModeComposite.create(blendMode);
   }

   @NotNull
   @Override
   protected Composite composite() {
      return this.composite;
   }
}

