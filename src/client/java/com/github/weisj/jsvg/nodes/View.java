package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import org.jetbrains.annotations.NotNull;

@ElementCategories({})
@PermittedContent(categories = Category.Descriptive)
public final class View extends MetaSVGNode {
   public static final String TAG = "view";

   @NotNull
   @Override
   public String tagName() {
      return "view";
   }
}

