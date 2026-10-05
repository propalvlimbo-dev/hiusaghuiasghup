package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGEllipse;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.util.AttributeUtil;
import org.jetbrains.annotations.NotNull;

@ElementCategories({Category.BasicShape, Category.Graphic, Category.Shape})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Ellipse extends ShapeNode {
   public static final String TAG = "ellipse";

   @NotNull
   @Override
   public String tagName() {
      return "ellipse";
   }

   @NotNull
   @Override
   protected SVGShape buildShape(@NotNull AttributeNode node) {
      AttributeUtil.AxisPair radius = AttributeUtil.parseAxisPair(node, "rx", "ry", Length.ZERO, Inherited.NO, v -> !v.isConstantlyNonNegative() ? null : v);
      LengthValue rx = radius.xAxis();
      LengthValue ry = radius.yAxis();
      return new SVGEllipse(
         node.getLength("cx", PercentageDimension.WIDTH, Length.ZERO, Inherited.NO, Animatable.YES),
         node.getLength("cy", PercentageDimension.HEIGHT, Length.ZERO, Inherited.NO, Animatable.YES),
         rx,
         ry
      );
   }
}

