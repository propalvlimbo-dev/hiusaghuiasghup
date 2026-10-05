package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.value.FloatValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public final class AnimatedFloat implements FloatValue {
   @NotNull
   private final Track track;
   @NotNull
   private final FloatValue initial;
   private final float @NotNull [] values;

   AnimatedFloat(@NotNull Track track, @NotNull FloatValue initial, float @NotNull [] values) {
      this.track = track;
      this.initial = initial;
      this.values = values;
   }

   @Override
   public float get(@NotNull MeasureContext context) {
      long timestamp = context.timestamp();
      Track.InterpolationProgress progress = this.track.interpolationProgress(timestamp, this.values.length);
      if (progress.isInitial()) {
         return this.initial.get(context);
      }

      int i = progress.iterationIndex();
      assert i >= 0;
      assert this.values.length > 0;
      if (i >= this.values.length - 1) {
         return this.values[i];
      }

      float start = this.values[i];
      float end = this.values[i + 1];
      return this.track.floatInterpolator().interpolate(this.initial.get(context), start, end, progress.indexProgress());
   }
}

