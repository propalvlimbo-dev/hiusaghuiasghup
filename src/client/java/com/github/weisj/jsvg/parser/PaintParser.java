package com.github.weisj.jsvg.parser;

import com.github.weisj.jsvg.paint.SVGPaint;
import java.awt.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PaintParser {
   @NotNull
   Color DEFAULT_COLOR = Color.BLACK;

   @Nullable
   Color parseColor(@NotNull String var1);

   @Nullable
   SVGPaint parsePaint(@Nullable String var1);
}
