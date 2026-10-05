package com.github.weisj.jsvg.attributes.stroke;

public enum LineJoin {
   Miter(0),
   Round(1),
   Bevel(2);

   private final int awtCode;

   LineJoin(int awtCode) {
      this.awtCode = awtCode;
   }

   public int awtCode() {
      return this.awtCode;
   }

   // $VF: synthetic method
   private static LineJoin[] $values() {
      return new LineJoin[]{Miter, Round, Bevel};
   }
}
