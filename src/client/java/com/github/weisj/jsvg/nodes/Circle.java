package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGCircle;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;

@ElementCategories({Category.BasicShape, Category.Graphic, Category.Shape})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Circle extends ShapeNode {
   public static final String TAG = "circle";

   @NotNull
   @Override
   public String tagName() {
      return "circle";
   }

   @NotNull
   @Override
   protected SVGShape buildShape(@NotNull AttributeNode attributeNode) {
      return new SVGCircle(
         attributeNode.getLength("cx", PercentageDimension.WIDTH, Length.ZERO, Inherited.NO, Animatable.YES),
         attributeNode.getLength("cy", PercentageDimension.HEIGHT, Length.ZERO, Inherited.NO, Animatable.YES),
         attributeNode.getLength("r", PercentageDimension.LENGTH, Length.ZERO, Inherited.NO, Animatable.YES)
      );
   }
}

