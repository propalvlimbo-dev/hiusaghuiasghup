package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AnimatedColor implements SVGPaint, ColorValue {
   @NotNull
   private final Track track;
   @NotNull
   private final ColorValue initial;
   @NotNull
   private final RGBColor[] values;
   private Color current;
   private long currentTimestamp = -1L;

   public AnimatedColor(@NotNull Track track, @NotNull ColorValue initial, @NotNull RGBColor[] values) {
      this.track = track;
      this.initial = initial;
      this.values = values;
   }

   @NotNull
   public AnimatedColor derive(@NotNull SVGPaint value) {
      if (this.initial != RGBColor.INHERITED) {
         return this;
      }

      ColorValue c = null;
      if (value instanceof ColorValue) {
         c = (ColorValue)value;
      } else if (value instanceof SimplePaintSVGPaint) {
         Paint p = ((SimplePaintSVGPaint)value).paint();
         if (p instanceof Color) {
            c = new RGBColor((Color)p);
         } else if (p instanceof RGBColor) {
            c = (RGBColor)p;
         }
      } else {
         c = RGBColor.DEFAULT;
      }

      return c == null ? this : new AnimatedColor(this.track, c, this.values);
   }

   @NotNull
   @Override
   public Color get(@NotNull MeasureContext context) {
      return this.current(context);
   }

   @NotNull
   private Color current(@NotNull MeasureContext context) {
      long timestamp = context.timestamp();
      if (this.currentTimestamp == timestamp) {
         return this.current;
      }

      this.currentTimestamp = timestamp;
      this.current = this.computeCurrent(context, timestamp);
      return this.current;
   }

   @NotNull
   private Color computeCurrent(@NotNull MeasureContext context, long timestamp) {
      Track.InterpolationProgress progress = this.track.interpolationProgress(timestamp, this.values.length);
      if (progress.isInitial()) {
         return Objects.requireNonNull(this.initial).get(context);
      }

      int i = progress.iterationIndex();
      assert i >= 0;
      assert this.values.length > 0;
      if (i >= this.values.length - 1) {
         return this.values[this.values.length - 1].toColor();
      }

      RGBColor start = this.values[i];
      RGBColor end = this.values[i + 1];
      float t = progress.indexProgress();
      return RGBColor.interpolate(t, start, end).toColor();
   }

   @Override
   public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape, @Nullable Rectangle2D bounds) {
      output.setPaint(this.current(context.measureContext()));
      output.fillShape(shape);
   }

   @Override
   public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape, @Nullable Rectangle2D bounds) {
      output.setPaint(this.current(context.measureContext()));
      output.drawShape(shape);
   }

   @Override
   public boolean isVisible(@NotNull RenderContext context) {
      return RGBColor.isVisible(this.current(context.measureContext()));
   }
}

