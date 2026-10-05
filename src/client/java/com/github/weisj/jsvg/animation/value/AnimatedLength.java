package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public final class AnimatedLength implements LengthValue {
   @NotNull
   private final Track track;
   @NotNull
   private final LengthValue initial;
   @NotNull
   private final Length[] values;

   public AnimatedLength(@NotNull Track track, @NotNull LengthValue initial, @NotNull Length[] values) {
      this.track = track;
      this.initial = initial;
      this.values = values;
   }

   @NotNull
   public AnimatedLength derive(@NotNull LengthValue initialValue) {
      return this.initial != Length.INHERITED ? this : new AnimatedLength(this.track, initialValue, this.values);
   }

   @NotNull
   public LengthValue initial() {
      return this.initial;
   }

   @Override
   public float resolve(@NotNull MeasureContext context) {
      long timestamp = context.timestamp();
      Track.InterpolationProgress progress = this.track.interpolationProgress(timestamp, this.values.length);
      if (progress.isInitial()) {
         return this.initial.resolve(context);
      }

      int i = progress.iterationIndex();
      assert i >= 0;
      assert this.values.length > 0;
      if (i >= this.values.length - 1) {
         return this.values[this.values.length - 1].resolve(context);
      }

      float start = this.values[i].resolve(context);
      float end = this.values[i + 1].resolve(context);
      return this.track.floatInterpolator().interpolate(this.initial.resolve(context), start, end, progress.indexProgress());
   }

   @Override
   public boolean isConstantlyZero() {
      for (Length value : this.values) {
         if (!value.isConstantlyZero()) {
            return false;
         }
      }

      return this.initial.isConstantlyZero();
   }

   @Override
   public boolean isConstantlyNonNegative() {
      for (Length value : this.values) {
         if (!value.isConstantlyNonNegative()) {
            return false;
         }
      }

      return this.initial.isConstantlyNonNegative();
   }
}

