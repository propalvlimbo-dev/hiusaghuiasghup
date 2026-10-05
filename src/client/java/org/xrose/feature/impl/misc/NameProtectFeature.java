package org.xrose.feature.impl.misc;

import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.TextSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class NameProtectFeature extends Feature {
   public final TextSetting name = this.register(new TextSetting("Name", "ElytrixUser", 32));
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "Replace", "Replace", "Blur"));

   public NameProtectFeature() {
      super("NameProtect", "Protects your nickname in client UI", FeatureCategory.MISC, -1);
   }
}

