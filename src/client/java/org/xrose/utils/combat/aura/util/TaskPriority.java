package org.xrose.utils.combat.aura.util;

public enum TaskPriority {
   HIGH_IMPORTANCE_1(100),
   HIGH_IMPORTANCE_2(90),
   NORMAL(0);

   private final int priority;

   TaskPriority(int priority) {
      this.priority = priority;
   }

   public int getPriority() {
      return this.priority;
   }

   // $VF: synthetic method
   private static TaskPriority[] $values() {
      return new TaskPriority[]{HIGH_IMPORTANCE_1, HIGH_IMPORTANCE_2, NORMAL};
   }
}
