package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public final class AnimatedFloatList implements FloatListValue {
   @NotNull
   private final Track track;
   @NotNull
   private final FloatListValue initial;
   private final float @NotNull [] @NotNull [] values;
   private float[] cache;
   private long timestamp = 0L;
   private Track.InterpolationProgress progressCacheKey = null;

   public AnimatedFloatList(@NotNull Track track, @NotNull FloatListValue initial, float @NotNull [] @NotNull [] values) {
      this.track = track;
      this.initial = initial;
      this.values = values;
   }

   @NotNull
   public FloatListValue initial() {
      return this.initial;
   }

   public boolean isDirty(long timestamp) {
      return this.timestamp != timestamp;
   }

   @Override
   public float @NotNull [] get(@NotNull MeasureContext context) {
      long ts = context.timestamp();
      if (ts != this.timestamp) {
         this.timestamp = ts;
         Track.InterpolationProgress progress = this.track.interpolationProgress(ts, this.values.length);
         if (progress.equals(this.progressCacheKey)) {
            return this.cache;
         }

         this.progressCacheKey = progress;
         if (progress.isInitial()) {
            this.cache = this.initial.get(context);
            return this.cache;
         }

         int index = progress.iterationIndex();
         float[] start = this.values[index];
         float[] end = index == this.values.length - 1 ? null : this.values[index + 1];
         float fraction = progress.indexProgress();
         this.cache = this.track.floatListInterpolator().interpolate(this.initial.get(context), start, end, fraction, this.cache);
      }

      return this.cache;
   }
}

