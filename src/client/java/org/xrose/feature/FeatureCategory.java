package org.xrose.feature;

import lombok.Generated;

public enum FeatureCategory {
   COMBAT("Combat"),
   MOVEMENT("Movement"),
   VISUAL("Visual"),
   PLAYER("Player"),
   MISC("Misc"),
   PVE("PVE");

   private final String displayName;

   FeatureCategory(String displayName) {
      this.displayName = displayName;
   }

   @Generated
   public String getDisplayName() {
      return this.displayName;
   }

   // $VF: synthetic method
   private static FeatureCategory[] $values() {
      return new FeatureCategory[]{COMBAT, MOVEMENT, VISUAL, PLAYER, MISC, PVE};
   }
}
