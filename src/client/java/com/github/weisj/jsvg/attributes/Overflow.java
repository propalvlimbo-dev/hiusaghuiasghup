package com.github.weisj.jsvg.attributes;

public enum Overflow {
   Auto(false),
   Visible(false),
   Hidden(true),
   Scroll(true);

   private final boolean establishesClip;

   Overflow(boolean establishesClip) {
      this.establishesClip = establishesClip;
   }

   public boolean establishesClip() {
      return this.establishesClip;
   }

   // $VF: synthetic method
   private static Overflow[] $values() {
      return new Overflow[]{Auto, Visible, Hidden, Scroll};
   }
}
