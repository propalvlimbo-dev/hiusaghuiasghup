package com.github.weisj.jsvg.attributes.value;

import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public interface Value<T> {
   T get(@NotNull MeasureContext var1);
}
