package org.xrose.utils.combat.aura.util;

public final class StopWatch {
   private long last = System.currentTimeMillis();

   public void reset() {
      this.last = System.currentTimeMillis();
   }

   public long elapsedTime() {
      return System.currentTimeMillis() - this.last;
   }

   public boolean finished(double delayMs) {
      return this.elapsedTime() >= delayMs;
   }

   public boolean every(double delayMs) {
      if (!this.finished(delayMs)) {
         return false;
      }

      this.reset();
      return true;
   }

   public StopWatch setMs(long ms) {
      this.last = System.currentTimeMillis() - ms;
      return this;
   }
}

