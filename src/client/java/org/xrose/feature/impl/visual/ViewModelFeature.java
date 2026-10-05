package org.xrose.feature.impl.visual;

import net.minecraft.world.entity.HumanoidArm;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.ButtonSetting;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.optimize.optimize;

@optimize
public final class ViewModelFeature extends Feature {
   public final NumberSetting rightX = this.register(new NumberSetting("Right X", 0.0, -1.0, 1.0, 0.05, ""));
   public final NumberSetting rightY = this.register(new NumberSetting("Right Y", 0.0, -1.0, 1.0, 0.05, ""));
   public final NumberSetting rightZ = this.register(new NumberSetting("Right Z", 0.0, -1.0, 1.0, 0.05, ""));
   public final NumberSetting leftX = this.register(new NumberSetting("Left X", 0.0, -1.0, 1.0, 0.05, ""));
   public final NumberSetting leftY = this.register(new NumberSetting("Left Y", 0.0, -1.0, 1.0, 0.05, ""));
   public final NumberSetting leftZ = this.register(new NumberSetting("Left Z", 0.0, -1.0, 1.0, 0.05, ""));
   public final ButtonSetting resetPosition = this.register(new ButtonSetting("Reset Position", "Reset", this::resetPosition));

   public ViewModelFeature() {
      super("ViewModel", "Adjusts first person hand positions", FeatureCategory.VISUAL, -1);
   }

   public float offsetX(HumanoidArm arm) {
      return value(arm == HumanoidArm.RIGHT ? this.rightX : this.leftX);
   }

   public float offsetY(HumanoidArm arm) {
      return value(arm == HumanoidArm.RIGHT ? this.rightY : this.leftY);
   }

   public float offsetZ(HumanoidArm arm) {
      return value(arm == HumanoidArm.RIGHT ? this.rightZ : this.leftZ);
   }

   private static float value(NumberSetting setting) {
      return setting.getValue().floatValue();
   }

   private void resetPosition() {
      this.rightX.reset();
      this.rightY.reset();
      this.rightZ.reset();
      this.leftX.reset();
      this.leftY.reset();
      this.leftZ.reset();
   }
}

