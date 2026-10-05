package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnimatedPaint implements SVGPaint {
   @NotNull
   private final Track track;
   @NotNull
   private final SVGPaint initial;
   @NotNull
   private final SVGPaint[] values;
   private SVGPaint current;
   private long currentTimestamp = -1L;

   public AnimatedPaint(@NotNull Track track, @NotNull SVGPaint initial, @NotNull SVGPaint[] values) {
      this.track = track;
      this.initial = initial;
      this.values = values;
   }

   @NotNull
   public AnimatedPaint derive(@NotNull SVGPaint initialValue) {
      return this.initial != PredefinedPaints.INHERITED ? this : new AnimatedPaint(this.track, initialValue, this.values);
   }

   @NotNull
   private SVGPaint current(@NotNull MeasureContext context) {
      long timestamp = context.timestamp();
      if (this.currentTimestamp == timestamp) {
         return this.current;
      }

      this.currentTimestamp = timestamp;
      this.current = this.computeCurrent(timestamp);
      return this.current;
   }

   @NotNull
   private SVGPaint computeCurrent(long timestamp) {
      Track.InterpolationProgress progress = this.track.interpolationProgress(timestamp, this.values.length);
      if (progress.isInitial()) {
         return this.initial;
      }

      int i = progress.iterationIndex();
      assert i >= 0;
      assert this.values.length > 0;
      if (i >= this.values.length - 1) {
         return this.values[this.values.length - 1];
      }

      SVGPaint start = this.values[i];
      SVGPaint end = this.values[i + 1];
      return this.track.paintInterpolator().interpolate(this.initial, start, end, progress.indexProgress());
   }

   @Override
   public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape, @Nullable Rectangle2D bounds) {
      this.current(context.measureContext()).fillShape(output, context, shape, bounds);
   }

   @Override
   public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape, @Nullable Rectangle2D bounds) {
      this.current(context.measureContext()).drawShape(output, context, shape, bounds);
   }

   @Override
   public boolean isVisible(@NotNull RenderContext context) {
      return this.current(context.measureContext()).isVisible(context);
   }
}

