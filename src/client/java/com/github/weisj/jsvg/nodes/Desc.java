package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.Descriptive)
@PermittedContent(any = true, charData = true)
public final class Desc extends MetaSVGNode {
   public static final String TAG = "desc";

   @NotNull
   @Override
   public String tagName() {
      return "desc";
   }
}

