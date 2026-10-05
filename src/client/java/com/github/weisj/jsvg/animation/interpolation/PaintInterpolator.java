package com.github.weisj.jsvg.animation.interpolation;

import com.github.weisj.jsvg.paint.SVGPaint;
import org.jetbrains.annotations.NotNull;

public interface PaintInterpolator {
   @NotNull
   SVGPaint interpolate(@NotNull SVGPaint var1, @NotNull SVGPaint var2, @NotNull SVGPaint var3, float var4);
}
