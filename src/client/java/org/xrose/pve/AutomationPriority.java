package org.xrose.pve;

public enum AutomationPriority {
   BACKGROUND(0),
   FEATURE(100),
   BOT(200),
   EMERGENCY(300);

   private final int weight;

   AutomationPriority(int weight) {
      this.weight = weight;
   }

   public int weight() {
      return this.weight;
   }

   // $VF: synthetic method
   private static AutomationPriority[] $values() {
      return new AutomationPriority[]{BACKGROUND, FEATURE, BOT, EMERGENCY};
   }
}
