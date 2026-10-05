package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.Descriptive)
@PermittedContent(any = true, charData = true)
public final class Title extends MetaSVGNode {
   public static final String TAG = "title";

   @NotNull
   @Override
   public String tagName() {
      return "title";
   }
}

