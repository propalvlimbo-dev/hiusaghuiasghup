package com.github.weisj.jsvg.attributes.value;

import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.Color;
import org.jetbrains.annotations.NotNull;

public interface ColorValue {
   @NotNull
   Color get(@NotNull MeasureContext var1);
}
