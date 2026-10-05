package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import org.jetbrains.annotations.NotNull;

public interface HasFontContext {
   @NotNull
   Mutator<MeasurableFontSpec> fontSpec();
}
