package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.container.CommonInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

@ElementCategories({Category.Container, Category.Structural})
@PermittedContent(
   categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
   anyOf = {Anchor.class, ClipPath.class, Filter.class, Image.class, Mask.class, Marker.class, Pattern.class, Style.class, Text.class, View.class}
)
public final class Symbol extends CommonInnerViewContainer {
   public static final String TAG = "symbol";
   private Length refX;
   private Length refY;

   @NotNull
   @Override
   public String tagName() {
      return "symbol";
   }

   @NotNull
   @Override
   protected Point2D anchorLocation(@NotNull MeasureContext context) {
      return new Float(-this.refX.resolve(context), -this.refY.resolve(context));
   }

   @NotNull
   @Override
   protected Overflow defaultOverflow() {
      return Overflow.Hidden;
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.refX = attributeNode.getHorizontalReferenceLengthFromKey("refX");
      this.refY = attributeNode.getVerticalReferenceLengthFromKey("refY");
   }

   @Override
   public boolean requiresInstantiation() {
      return true;
   }
}

