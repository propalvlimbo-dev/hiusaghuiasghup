package com.github.weisj.jsvg.attributes;

import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;

public enum FillRule {
   Nonzero(1),
   EvenOdd(0),
   Inherit(1);

   public final int awtWindingRule;

   FillRule(int awtWindingRule) {
      this.awtWindingRule = awtWindingRule;
   }

   @NotNull
   public static FillRule parse(@NotNull AttributeNode attributeNode) {
      return attributeNode.getEnum("fill-rule", Inherit);
   }

   // $VF: synthetic method
   private static FillRule[] $values() {
      return new FillRule[]{Nonzero, EvenOdd, Inherit};
   }
}
