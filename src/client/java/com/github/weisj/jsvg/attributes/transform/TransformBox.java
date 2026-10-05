package com.github.weisj.jsvg.attributes.transform;

import com.github.weisj.jsvg.attributes.HasMatchName;
import org.jetbrains.annotations.NotNull;

public enum TransformBox implements HasMatchName {
   FillBox("fill-box"),
   StrokeBox("stroke-box"),
   ViewBox("view-box");

   @NotNull
   private final String matchName;

   TransformBox(@NotNull String matchName) {
      this.matchName = matchName;
   }

   @NotNull
   @Override
   public String matchName() {
      return this.matchName;
   }

   // $VF: synthetic method
   private static TransformBox[] $values() {
      return new TransformBox[]{FillBox, StrokeBox, ViewBox};
   }
}
