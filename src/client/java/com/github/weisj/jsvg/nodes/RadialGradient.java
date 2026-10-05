package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.paint.impl.jdk.SVGRadialGradientPaint;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.Color;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Float;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories(Category.Gradient)
@PermittedContent(categories = Category.Descriptive, anyOf = {Stop.class, Animate.class, AnimateTransform.class, Set.class})
public final class RadialGradient extends AbstractGradient<RadialGradient> {
   public static final String TAG = "radialgradient";
   private Length cx;
   private Length cy;
   private Length r;
   private Length fr;
   private Length fx;
   private Length fy;

   @NotNull
   @Override
   public String tagName() {
      return "radialgradient";
   }

   protected void buildGradient(@NotNull AttributeNode attributeNode, @Nullable RadialGradient template) {
      this.cx = attributeNode.getLength("cx", PercentageDimension.WIDTH, template != null ? template.cx : Unit.PERCENTAGE_WIDTH.valueOf(50.0F));
      this.cy = attributeNode.getLength("cy", PercentageDimension.HEIGHT, template != null ? template.cy : Unit.PERCENTAGE_HEIGHT.valueOf(50.0F));
      this.r = attributeNode.getLength("r", PercentageDimension.LENGTH, template != null ? template.r : Unit.PERCENTAGE_LENGTH.valueOf(50.0F));
      this.fr = attributeNode.getLength("fr", PercentageDimension.LENGTH, template != null ? template.fr : Unit.PERCENTAGE_LENGTH.valueOf(0.0F));
      this.fx = attributeNode.getLength("fx", PercentageDimension.WIDTH, template != null ? template.fx : this.cx);
      this.fy = attributeNode.getLength("fy", PercentageDimension.HEIGHT, template != null ? template.fy : this.cy);
   }

   @NotNull
   @Override
   protected Paint gradientForBounds(@NotNull MeasureContext measure, @NotNull Rectangle2D bounds, Percentage[] gradOffsets, @NotNull Color[] gradColors) {
      assert gradColors.length > 0;
      Float center = new Float(this.cx.resolve(measure), this.cy.resolve(measure));
      Float focusCenter = new Float(this.fx.resolve(measure), this.fy.resolve(measure));
      float radius = this.r.resolve(measure);
      float focusRadius = this.fr.resolve(measure);
      if (radius <= 0.0F) {
         return gradColors[gradColors.length - 1];
      } else {
         return focusRadius == 0.0F
            ? new RadialGradientPaint(
               center,
               radius,
               focusCenter,
               this.offsetsToFractions(gradOffsets),
               gradColors,
               this.spreadMethod.cycleMethod(),
               ColorSpaceType.SRGB,
               this.computeViewTransform(measure, bounds)
            )
            : new SVGRadialGradientPaint(
               center,
               radius,
               focusCenter,
               focusRadius,
               this.offsetsToFractions(gradOffsets),
               gradColors,
               this.spreadMethod.cycleMethod(),
               ColorSpaceType.SRGB,
               this.computeViewTransform(measure, bounds)
            );
      }
   }

   @Override
   public String toString() {
      return "RadialGradient{spreadMethod="
         + this.spreadMethod
         + ", gradientTransform="
         + this.gradientTransform
         + ", cx="
         + this.cx
         + ", cy="
         + this.cy
         + ", r="
         + this.r
         + ", fr="
         + this.fr
         + ", fx="
         + this.fx
         + ", fy="
         + this.fy
         + ", colors="
         + Arrays.toString(this.colors())
         + ", offsets="
         + Arrays.toString(this.offsets())
         + '}';
   }
}

