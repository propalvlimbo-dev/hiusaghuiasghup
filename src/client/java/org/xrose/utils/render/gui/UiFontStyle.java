package org.xrose.utils.render.gui;

import sdk.api.optimize.optimize;

@optimize
public enum UiFontStyle {
   REGULAR(400, 0.0F),
   REGULAR_TRACKED(400, 0.015F),
   MEDIUM(500, 0.015F),
   SEMIBOLD(600, 0.015F);

   private final int weight;
   private final float letterSpacingEm;

   UiFontStyle(int weight, float letterSpacingEm) {
      this.weight = weight;
      this.letterSpacingEm = letterSpacingEm;
   }

   public int weight() {
      return this.weight;
   }

   public float letterSpacingEm() {
      return this.letterSpacingEm;
   }

   // $VF: synthetic method
   private static UiFontStyle[] $values() {
      return new UiFontStyle[]{REGULAR, REGULAR_TRACKED, MEDIUM, SEMIBOLD};
   }
}
