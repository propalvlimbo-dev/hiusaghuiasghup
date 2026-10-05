package com.github.weisj.jsvg.nodes.filter;

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
public final class FeComposite extends AbstractCompositeFilterPrimitive {
   public static final String TAG = "fecomposite";
   private CompositeModeComposite composite;

   @NotNull
   @Override
   public String tagName() {
      return "fecomposite";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.composite = new CompositeModeComposite(attributeNode);
   }

   @NotNull
   @Override
   protected Composite composite() {
      return this.composite.composite();
   }
}

