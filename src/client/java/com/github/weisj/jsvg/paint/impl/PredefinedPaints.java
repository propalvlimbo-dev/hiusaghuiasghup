package com.github.weisj.jsvg.paint.impl;

import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.parser.PaintParser;
import org.jetbrains.annotations.NotNull;

public final class PredefinedPaints {
   @NotNull
   public static final AwtSVGPaint DEFAULT_PAINT = new AwtSVGPaint(PaintParser.DEFAULT_COLOR);
   @NotNull
   public static final SVGPaint NONE = new NonePaint();
   @NotNull
   public static final SVGPaint CURRENT_COLOR = new SentinelPaint("currentColor");
   @NotNull
   public static final SVGPaint CONTEXT_FILL = new SentinelPaint("contextFill");
   @NotNull
   public static final SVGPaint CONTEXT_STROKE = new SentinelPaint("contextStroke");
   @NotNull
   public static final SVGPaint INHERITED = new SentinelPaint("inherited");

   private PredefinedPaints() {
   }
}

