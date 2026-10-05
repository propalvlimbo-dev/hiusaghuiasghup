package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public final class AnimatedPercentage implements PercentageValue {
   @NotNull
   private final Track track;
   @NotNull
   private final PercentageValue initial;
   private final float @NotNull [] values;
   private final PercentageValue multiplier;

   public AnimatedPercentage(@NotNull Track track, @NotNull PercentageValue initial, float @NotNull [] values, PercentageValue multiplier) {
      this.track = track;
      this.initial = initial;
      this.values = values;
      this.multiplier = multiplier;
   }

   @NotNull
   public AnimatedPercentage derive(@NotNull PercentageValue initial) {
      return this.initial != Percentage.INHERITED ? this : new AnimatedPercentage(this.track, initial, this.values, this.multiplier);
   }

   @NotNull
   @Override
   public PercentageValue multiply(@NotNull PercentageValue other) {
      return new AnimatedPercentage(this.track, this.initial, this.values, this.multiplier.multiply(other));
   }

   @NotNull
   public PercentageValue initial() {
      return this.initial;
   }

   private float getBase(@NotNull MeasureContext context) {
      long timestamp = context.timestamp();
      Track.InterpolationProgress progress = this.track.interpolationProgress(timestamp, this.values.length);
      if (progress.isInitial()) {
         return this.initial.get(context);
      }

      int i = progress.iterationIndex();
      assert i >= 0;
      assert this.values.length > 0;
      if (i >= this.values.length - 1) {
         return this.values[this.values.length - 1];
      }

      float start = this.values[i];
      float end = this.values[i + 1];
      return this.track.floatInterpolator().interpolate(this.initial.get(context), start, end, progress.indexProgress());
   }

   @Override
   public float get(@NotNull MeasureContext context) {
      return this.getBase(context) * this.multiplier.get(context);
   }
}

