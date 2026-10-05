package com.github.weisj.jsvg.animation;

public enum AnimationValuesType {
   VALUES,
   FROM_TO,
   FROM_BY,
   BY,
   TO;

   public boolean endIsBy() {
      return this == FROM_BY || this == BY;
   }

   // $VF: synthetic method
   private static AnimationValuesType[] $values() {
      return new AnimationValuesType[]{VALUES, FROM_TO, FROM_BY, BY, TO};
   }
}
