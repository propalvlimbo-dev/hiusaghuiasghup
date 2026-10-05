package com.github.weisj.jsvg.attributes.stroke;

public enum LineCap {
   Butt(0),
   Square(2),
   Round(1);

   private final int awtCode;

   LineCap(int awtCode) {
      this.awtCode = awtCode;
   }

   public int awtCode() {
      return this.awtCode;
   }

   // $VF: synthetic method
   private static LineCap[] $values() {
      return new LineCap[]{Butt, Square, Round};
   }
}
