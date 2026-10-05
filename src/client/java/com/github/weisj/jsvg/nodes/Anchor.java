package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.nodes.container.CommonRenderableContainerNode;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.Container)
@PermittedContent(
   categories = {Category.Container, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient, Category.Animation},
   anyOf = {Anchor.class, ClipPath.class, Filter.class, Image.class, Mask.class, Marker.class, Pattern.class, Style.class, Text.class, View.class}
)
public final class Anchor extends CommonRenderableContainerNode implements ShapedContainer<SVGNode> {
   public static final String TAG = "a";

   @NotNull
   @Override
   public String tagName() {
      return "a";
   }
}

