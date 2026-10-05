package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.attributes.Coordinate;
import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.container.CommonInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.Point;
import java.awt.geom.Point2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories({Category.Container, Category.Structural})
@PermittedContent(
   categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
   anyOf = {Anchor.class, ClipPath.class, Filter.class, Image.class, Mask.class, Marker.class, Pattern.class, Style.class, Text.class, View.class}
)
public final class SVG extends CommonInnerViewContainer {
   public static final String TAG = "svg";
   @NotNull
   private static final Coordinate<LengthValue> TOP_LEVEL_TRANSFORM_ORIGIN = new Coordinate<>(
      Unit.PERCENTAGE_WIDTH.valueOf(50.0F), Unit.PERCENTAGE_WIDTH.valueOf(50.0F)
   );
   private static final float FALLBACK_WIDTH = 300.0F;
   private static final float FALLBACK_HEIGHT = 150.0F;
   private boolean isTopLevel;
   private AnimationPeriod animationPeriod;

   @NotNull
   @Override
   public String tagName() {
      return "svg";
   }

   public boolean isTopLevel() {
      return this.isTopLevel;
   }

   @NotNull
   public AnimationPeriod animationPeriod() {
      return this.animationPeriod;
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      this.isTopLevel = attributeNode.element().parent() == null;
      super.build(attributeNode);
      this.animationPeriod = attributeNode.document().animationPeriod();
   }

   @NotNull
   @Override
   protected Point2D outerLocation(@NotNull MeasureContext context) {
      return this.isTopLevel ? new Point(0, 0) : super.outerLocation(context);
   }

   @NotNull
   @Override
   public Coordinate<LengthValue> transformOrigin() {
      return this.isTopLevel ? TOP_LEVEL_TRANSFORM_ORIGIN : super.transformOrigin();
   }

   @NotNull
   @Override
   protected Overflow defaultOverflow() {
      return Overflow.Hidden;
   }

   @NotNull
   public FloatSize sizeForTopLevel(@Nullable ViewBox outerViewBox, float em, float ex) {
      FloatSize size = outerViewBox != null ? outerViewBox.size() : new FloatSize(100.0F, 100.0F);
      MeasureContext topLevelContext = MeasureContext.createInitial(size, em, ex, AnimationState.NO_ANIMATION);
      return new FloatSize(
         this.width.orElseIfUnspecified(this.viewBox != null ? this.viewBox.width : 300.0F).resolve(topLevelContext),
         this.height.orElseIfUnspecified(this.viewBox != null ? this.viewBox.height : 150.0F).resolve(topLevelContext)
      );
   }
}

