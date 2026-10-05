package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGRectangle;
import com.github.weisj.jsvg.geometry.SVGRoundRectangle;
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
public final class Rect extends ShapeNode {
   public static final String TAG = "rect";

   @NotNull
   @Override
   public String tagName() {
      return "rect";
   }

   @NotNull
   @Override
   protected SVGShape buildShape(@NotNull AttributeNode node) {
      LengthValue x = node.getLength("x", PercentageDimension.WIDTH, Length.ZERO, Inherited.NO, Animatable.YES);
      LengthValue y = node.getLength("y", PercentageDimension.HEIGHT, Length.ZERO, Inherited.NO, Animatable.YES);
      LengthValue width = node.getLength("width", PercentageDimension.WIDTH, Length.ZERO, Inherited.NO, Animatable.YES);
      LengthValue height = node.getLength("height", PercentageDimension.HEIGHT, Length.ZERO, Inherited.NO, Animatable.YES);
      AttributeUtil.AxisPair radius = AttributeUtil.parseAxisPair(node, "rx", "ry", Length.ZERO, Inherited.NO, v -> !v.isConstantlyNonNegative() ? null : v);
      LengthValue rx = radius.xAxis();
      LengthValue ry = radius.yAxis();
      boolean isNonRounded = rx.isConstantlyZero() && ry.isConstantlyZero();
      return isNonRounded ? new SVGRectangle(x, y, width, height) : new SVGRoundRectangle(x, y, width, height, rx, ry);
   }
}

