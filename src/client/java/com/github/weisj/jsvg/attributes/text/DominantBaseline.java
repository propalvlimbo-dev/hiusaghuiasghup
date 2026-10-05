package com.github.weisj.jsvg.attributes.text;

import com.github.weisj.jsvg.attributes.HasMatchName;
import org.jetbrains.annotations.NotNull;

public enum DominantBaseline implements HasMatchName {
   Auto,
   Ideographic,
   Alphabetic,
   Hanging,
   Mathematical,
   Central,
   Middle,
   TextAfterEdge("text-after-edge"),
   TextBottom("text-bottom"),
   TextBeforeEdge("text-before-edge"),
   TextTop("text-top");

   @NotNull
   private final String matchName;

   DominantBaseline(@NotNull String matchName) {
      this.matchName = matchName;
   }

   DominantBaseline() {
      this.matchName = this.name();
   }

   @NotNull
   @Override
   public String matchName() {
      return this.matchName;
   }

   // $VF: synthetic method
   private static DominantBaseline[] $values() {
      return new DominantBaseline[]{Auto, Ideographic, Alphabetic, Hanging, Mathematical, Central, Middle, TextAfterEdge, TextBottom, TextBeforeEdge, TextTop};
   }
}
