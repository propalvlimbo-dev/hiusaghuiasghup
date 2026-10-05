package com.github.weisj.jsvg.nodes.animation;

import com.github.weisj.jsvg.animation.value.AnimatedColor;
import com.github.weisj.jsvg.animation.value.AnimatedFloatList;
import com.github.weisj.jsvg.animation.value.AnimatedLength;
import com.github.weisj.jsvg.animation.value.AnimatedPaint;
import com.github.weisj.jsvg.animation.value.AnimatedPercentage;
import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.parser.PaintParser;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import java.awt.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories(Category.Animation)
@PermittedContent(categories = Category.Descriptive)
public final class Animate extends BaseAnimationNode {
   public static final String TAG = "animate";

   @NotNull
   @Override
   public String tagName() {
      return "animate";
   }

   @Nullable
   public AnimatedLength animatedLength(@NotNull LengthValue initial, PercentageDimension dimension, @NotNull AttributeNode attributeNode) {
      if (this.track == null) {
         return null;
      }

      Length[] lengths = new Length[this.values.length];

      for (int i = 0; i < this.values.length; i++) {
         if (isPlaceholder(this.values[i])) {
            lengths[i] = Length.ZERO;
         } else {
            lengths[i] = attributeNode.parser().parseLength(this.values[i], null, dimension);
         }

         if (lengths[i] == null) {
            return null;
         }
      }

      return new AnimatedLength(this.track, initial, lengths);
   }

   @Nullable
   public AnimatedFloatList animatedFloatList(@NotNull FloatListValue initial, @NotNull AttributeNode attributeNode) {
      if (this.track == null) {
         return null;
      }

      float[][] lists = new float[this.values.length][];

      for (int i = 0; i < this.values.length; i++) {
         if (isPlaceholder(this.values[i])) {
            lists[i] = new float[0];
         } else {
            lists[i] = attributeNode.parser().parseFloatList(this.values[i]);
         }
      }

      return new AnimatedFloatList(this.track, initial, lists);
   }

   @Nullable
   public AnimatedPercentage animatedPercentage(@NotNull PercentageValue initial, @NotNull AttributeNode attributeNode) {
      if (this.track == null) {
         return null;
      }

      float[] percentages = new float[this.values.length];

      for (int i = 0; i < this.values.length; i++) {
         if (isPlaceholder(this.values[i])) {
            percentages[i] = Percentage.ZERO.value();
         } else {
            Percentage p = attributeNode.parser().parsePercentage(this.values[i], null);
            if (p == null) {
               return null;
            }

            percentages[i] = p.value();
         }
      }

      return new AnimatedPercentage(this.track, initial, percentages, Percentage.ONE);
   }

   @Nullable
   public AnimatedPaint animatedPaint(@NotNull SVGPaint initial, @NotNull AttributeNode attributeNode) {
      if (this.track == null) {
         return null;
      }

      SVGPaint[] paints = new SVGPaint[this.values.length];

      for (int i = 0; i < this.values.length; i++) {
         if (isPlaceholder(this.values[i])) {
            paints[i] = PredefinedPaints.NONE;
         } else {
            SVGPaint p = attributeNode.parsePaint(this.values[i]);
            if (p == null) {
               return null;
            }

            paints[i] = p;
         }
      }

      return new AnimatedPaint(this.track, initial, paints);
   }

   @Nullable
   public AnimatedColor animatedColor(@NotNull ColorValue initial, @NotNull AttributeNode attributeNode) {
      if (this.track == null) {
         return null;
      }

      RGBColor[] paints = new RGBColor[this.values.length];

      for (int i = 0; i < this.values.length; i++) {
         if (isPlaceholder(this.values[i])) {
            paints[i] = new RGBColor(PaintParser.DEFAULT_COLOR);
         } else {
            Color c = attributeNode.parser().paintParser().parseColor(this.values[i]);
            if (c == null) {
               return null;
            }

            paints[i] = new RGBColor(c);
         }
      }

      return new AnimatedColor(this.track, initial, paints);
   }
}

