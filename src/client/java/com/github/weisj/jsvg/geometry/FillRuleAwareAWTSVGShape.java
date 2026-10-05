package com.github.weisj.jsvg.geometry;

import com.github.weisj.jsvg.attributes.value.Value;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import java.awt.geom.Path2D;
import org.jetbrains.annotations.NotNull;

public final class FillRuleAwareAWTSVGShape extends AWTSVGShape<Path2D> {
   public FillRuleAwareAWTSVGShape(@NotNull Value<Path2D> shape) {
      super(shape);
   }

   @NotNull
   public Path2D shape(@NotNull RenderContext context, boolean validate) {
      Path2D shape = (Path2D)super.shape(context, validate);
      shape.setWindingRule(RenderContextAccessor.instance().fillRule(context).awtWindingRule);
      return shape;
   }
}

