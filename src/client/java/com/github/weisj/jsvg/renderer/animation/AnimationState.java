package com.github.weisj.jsvg.renderer.animation;

import com.google.errorprone.annotations.Immutable;
import java.util.Objects;

@Immutable
public final class AnimationState {
   public static final AnimationState NO_ANIMATION = new AnimationState(0L, 0L);
   private final long startTime;
   private final long currentTime;

   public AnimationState(long startTime, long currentTime) {
      this.startTime = startTime;
      this.currentTime = currentTime;
   }

   public long timestamp() {
      return this.currentTime - this.startTime;
   }

   @Override
   public String toString() {
      return "AnimationState{startTime=" + this.startTime + ", currentTime=" + this.currentTime + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         AnimationState that = (AnimationState)o;
         return this.startTime == that.startTime && this.currentTime == that.currentTime;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.startTime, this.currentTime);
   }
}

