package org.xrose.feature.impl.visual;

import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.optimize.optimize;

@optimize
public final class SeeInvisibleFeature extends Feature {
   private static final float DEFAULT_ALPHA = 0.4F;
   private final NumberSetting alpha = this.register(new NumberSetting("Alpha", 0.4, 0.05, 1.0, 0.05, ""));

   public SeeInvisibleFeature() {
      super("See Invisible", "Делает невидимые сущности видимыми с настраиваемой прозрачностью", FeatureCategory.VISUAL, -1);
   }

   public static boolean isActive() {
      return FeatureManager.INSTANCE.getEnabled(SeeInvisibleFeature.class) != null;
   }

   public static float getAlpha() {
      SeeInvisibleFeature feature = FeatureManager.INSTANCE.getEnabled(SeeInvisibleFeature.class);
      return feature != null ? feature.alpha.getFloat() : 0.4F;
   }
}

