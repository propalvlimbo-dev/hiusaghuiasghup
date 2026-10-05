package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import org.jetbrains.annotations.NotNull;

@ElementCategories({Category.BasicShape, Category.Graphic, Category.Shape})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Polyline extends AbstractPolyShape {
   public static final String TAG = "polyline";

   @NotNull
   @Override
   public String tagName() {
      return "polyline";
   }

   @Override
   protected boolean doClose() {
      return false;
   }

   @Override
   protected boolean shouldPaintStartEndMarkersInMiddle() {
      return false;
   }
}

