package com.github.weisj.jsvg.animation.time;

import org.jetbrains.annotations.NotNull;

public final class Interval {
   @NotNull
   private final Duration begin;
   @NotNull
   private final Duration end;

   public Interval(@NotNull Duration begin, @NotNull Duration end) {
      this.begin = begin;
      this.end = end;
   }

   @NotNull
   public Duration begin() {
      return this.begin;
   }

   @NotNull
   public Duration end() {
      return this.begin;
   }

   @NotNull
   public Duration duration() {
      return this.end.minus(this.begin);
   }

   @Override
   public String toString() {
      return "Interval{begin=" + this.begin + ", end=" + this.end + '}';
   }

   public boolean isValid() {
      return !this.begin.isIndefinite() && !this.end.isIndefinite() && this.begin.milliseconds() < this.end.milliseconds();
   }
}

