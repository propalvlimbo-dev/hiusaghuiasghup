package com.github.weisj.jsvg.attributes.stroke;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.impl.context.StrokeContext;
import java.awt.BasicStroke;
import java.awt.Stroke;
import org.jetbrains.annotations.NotNull;

public final class StrokeResolver {

   private StrokeResolver() {
   }

   @NotNull
   public static Stroke resolve(float pathLengthFactor, @NotNull MeasureContext measureContext, @NotNull StrokeContext context) {
      LengthValue strokeWidth = context.strokeWidth;
      LineCap lineCap = context.lineCap;
      LineJoin lineJoin = context.lineJoin;
      float miterLimit = context.miterLimit;
      Length[] dashPattern = context.dashPattern;
      LengthValue dashOffset = context.dashOffset;
      assert strokeWidth != null;
      assert lineCap != null;
      assert lineJoin != null;
      assert Length.isSpecified(miterLimit);
      assert dashOffset != null;
      assert dashPattern != null;
      miterLimit = Math.max(1.0F, miterLimit);
      float[] dashes = new float[dashPattern.length];
      float offsetLength = 0.0F;

      for (int i = 0; i < dashes.length; i++) {
         float dash = dashPattern[i].resolve(measureContext) * pathLengthFactor;
         offsetLength += dash;
         dashes[i] = dash;
      }

      float phase = dashOffset.resolve(measureContext) * pathLengthFactor;
      if (phase < 0.0F) {
         phase += offsetLength;
      }

      return dashes.length == 0
         ? new BasicStroke(strokeWidth.resolve(measureContext), lineCap.awtCode(), lineJoin.awtCode(), miterLimit)
         : new BasicStroke(strokeWidth.resolve(measureContext), lineCap.awtCode(), lineJoin.awtCode(), miterLimit, dashes, phase);
   }
}

