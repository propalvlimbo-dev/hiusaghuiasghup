package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.geometry.AWTSVGShape;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.util.PathUtil;
import java.awt.Rectangle;
import org.jetbrains.annotations.NotNull;

@ElementCategories({Category.Graphic, Category.Shape})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Path extends ShapeNode {
   public static final String TAG = "path";

   @NotNull
   @Override
   public String tagName() {
      return "path";
   }

   @NotNull
   @Override
   protected SVGShape buildShape(@NotNull AttributeNode attributeNode) {
      String pathValue = attributeNode.getValue("d");
      return pathValue == null ? new AWTSVGShape<>(new Rectangle()) : PathUtil.parseFromPathData(pathValue, FillRule.Nonzero);
   }

   @Override
   protected boolean shouldPaintStartEndMarkersInMiddle() {
      return false;
   }
}

