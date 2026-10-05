package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGLine;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;

@ElementCategories({Category.BasicShape, Category.Graphic, Category.Shape})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Line extends ShapeNode {
   public static final String TAG = "line";

   @NotNull
   @Override
   public String tagName() {
      return "line";
   }

   @NotNull
   @Override
   protected SVGShape buildShape(@NotNull AttributeNode attributeNode) {
      return new SVGLine(
         attributeNode.getLength("x1", PercentageDimension.WIDTH, 0.0F),
         attributeNode.getLength("y1", PercentageDimension.HEIGHT, 0.0F),
         attributeNode.getLength("x2", PercentageDimension.WIDTH, 0.0F),
         attributeNode.getLength("y2", PercentageDimension.HEIGHT, 0.0F)
      );
   }
}

