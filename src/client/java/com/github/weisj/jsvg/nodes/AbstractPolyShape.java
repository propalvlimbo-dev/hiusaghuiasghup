package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.animation.value.AnimatedFloatList;
import com.github.weisj.jsvg.animation.value.AnimatedPath;
import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.ConstantFloatList;
import com.github.weisj.jsvg.attributes.value.ConstantValue;
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.geometry.AWTSVGShape;
import com.github.weisj.jsvg.geometry.FillRuleAwareAWTSVGShape;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.util.PathUtil;
import java.awt.Rectangle;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPolyShape extends ShapeNode {
   @NotNull
   @Override
   protected final SVGShape buildShape(@NotNull AttributeNode attributeNode) {
      FloatListValue points = attributeNode.getFloatList("points", Inherited.NO, Animatable.YES);
      if (points instanceof AnimatedFloatList) {
         return new FillRuleAwareAWTSVGShape(new AnimatedPath((AnimatedFloatList)points, this.doClose()));
      }

      float[] pointsArray = ((ConstantFloatList)points).value();
      return pointsArray.length > 0
         ? new FillRuleAwareAWTSVGShape(new ConstantValue<>(PathUtil.setPolyLine(null, pointsArray, this.doClose())))
         : new AWTSVGShape<>(new Rectangle());
   }

   protected abstract boolean doClose();
}

