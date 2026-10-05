package com.github.weisj.jsvg.renderer.output.impl;

import com.github.weisj.jsvg.paint.SVGPaint;
import org.jetbrains.annotations.Nullable;

public interface CurrentColorProvider {
   @Nullable
   SVGPaint currentColor();
}
