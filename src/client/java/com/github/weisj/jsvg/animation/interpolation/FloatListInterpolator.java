package com.github.weisj.jsvg.animation.interpolation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FloatListInterpolator {
   float @NotNull [] interpolate(float @NotNull [] var1, float @NotNull [] var2, float @Nullable [] var3, float var4, float @Nullable [] var5);
}
