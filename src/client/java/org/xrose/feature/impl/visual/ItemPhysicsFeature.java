package org.xrose.feature.impl.visual;

import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import sdk.api.optimize.optimize;

@optimize
public final class ItemPhysicsFeature extends Feature {
   public ItemPhysicsFeature() {
      super("ItemPhysics", "Dropped items lie on the ground", FeatureCategory.VISUAL, -1);
   }
}

