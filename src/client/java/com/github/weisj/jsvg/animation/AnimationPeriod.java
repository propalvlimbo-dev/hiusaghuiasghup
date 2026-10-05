package com.github.weisj.jsvg.animation;

import com.github.weisj.jsvg.animation.time.Interval;
import com.github.weisj.jsvg.renderer.animation.Animation;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AnimationPeriod implements Animation {
   private final long start;
   private final long end;
   private final boolean freeze;

   public AnimationPeriod(long start, long end, boolean freeze) {
      this.start = start;
      this.end = end;
      this.freeze = freeze;
   }

   @NotNull
   public AnimationPeriod derive(@Nullable Track track) {
      if (track == null) {
         return this;
      }

      List<Interval> intervals = track.intervals();
      float repeatCount = track.repeatCount();
      long intervalsBegin = intervals.get(0).begin().milliseconds();
      Interval lastInterval = intervals.get(intervals.size() - 1);
      long intervalsEnd = (long)((float)lastInterval.begin().milliseconds() + repeatCount * (float)lastInterval.duration().milliseconds());
      long animationStartTime = Math.min(this.start, intervalsBegin);
      long animationEndTime;
      if (Float.isFinite(repeatCount)) {
         animationEndTime = Math.max(this.end, intervalsEnd);
      } else {
         animationEndTime = Long.MAX_VALUE;
      }

      boolean animationFreezes = this.freeze || track.fill() == Fill.FREEZE;
      return new AnimationPeriod(animationStartTime, animationEndTime, animationFreezes);
   }

   @Override
   public long duration() {
      return this.end - this.start;
   }

   @Override
   public long startTime() {
      return this.start;
   }

   @Override
   public long endTime() {
      return this.end;
   }
}

