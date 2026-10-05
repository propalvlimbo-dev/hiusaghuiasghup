package com.github.weisj.jsvg.attributes;

import java.awt.MultipleGradientPaint.CycleMethod;
import org.jetbrains.annotations.NotNull;

public enum SpreadMethod {
   Pad(CycleMethod.NO_CYCLE),
   Reflect(CycleMethod.REFLECT),
   Repeat(CycleMethod.REPEAT);

   private final CycleMethod cycleMethod;

   SpreadMethod(CycleMethod cycleMethod) {
      this.cycleMethod = cycleMethod;
   }

   @NotNull
   public CycleMethod cycleMethod() {
      return this.cycleMethod;
   }

   // $VF: synthetic method
   private static SpreadMethod[] $values() {
      return new SpreadMethod[]{Pad, Reflect, Repeat};
   }
}
