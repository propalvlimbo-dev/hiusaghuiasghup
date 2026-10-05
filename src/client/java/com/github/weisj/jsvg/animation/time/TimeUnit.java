package com.github.weisj.jsvg.animation.time;

import com.github.weisj.jsvg.attributes.SuffixUnit;
import org.jetbrains.annotations.NotNull;

public enum TimeUnit implements SuffixUnit<TimeUnit, Duration> {
   Hour(3600000L, "h"),
   Minute(60000L, "min"),
   Second(1000L, "s"),
   Millisecond(1L, "ms"),
   Raw(1000L, "");

   private static final TimeUnit[] units = values();
   @NotNull
   private final String suffix;
   private final long milliseconds;

   @NotNull
   public TimeUnit[] units() {
      return units;
   }

   TimeUnit(long milliseconds, @NotNull String suffix) {
      this.suffix = suffix;
      this.milliseconds = milliseconds;
   }

   @NotNull
   public Duration valueOf(float value) {
      return value == 0.0F ? new Duration(0L) : new Duration((long)((float)this.milliseconds * value));
   }

   @NotNull
   @Override
   public String suffix() {
      return this.suffix;
   }

   // $VF: synthetic method
   private static TimeUnit[] $values() {
      return new TimeUnit[]{Hour, Minute, Second, Millisecond, Raw};
   }
}
