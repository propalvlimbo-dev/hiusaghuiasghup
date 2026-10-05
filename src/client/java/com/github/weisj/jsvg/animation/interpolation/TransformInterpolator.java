package com.github.weisj.jsvg.animation.interpolation;

import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;

public interface TransformInterpolator {
   @NotNull
   AffineTransform interpolate(@NotNull MeasureContext var1, @NotNull TransformValue var2, @NotNull TransformPart var3, @NotNull TransformPart var4, float var5);
}
