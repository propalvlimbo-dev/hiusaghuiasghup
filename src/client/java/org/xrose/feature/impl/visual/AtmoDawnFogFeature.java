package org.xrose.feature.impl.visual;

import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ColorSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.optimize.optimize;

@optimize
public final class AtmoDawnFogFeature extends Feature {
   public static final String MODE_DUSK = "Dusk";
   public static final String MODE_NIGHT = "Night";
   public static final String MODE_THEME = "Theme";
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "Dusk", "Dusk", "Night", "Theme").configKey("render.atmodawnfog.mode"));
   public final NumberSetting density = this.register(new NumberSetting("Density", 0.35, 0.05, 0.8, 0.01, "").configKey("render.atmodawnfog.density"));
   public final NumberSetting scatterHeight = this.register(
      new NumberSetting("Scatter Height", 76.0, 60.0, 120.0, 1.0, " blocks").configKey("render.atmodawnfog.scatterheight")
   );
   public final NumberSetting godRays = this.register(new NumberSetting("God Rays", 0.75, 0.0, 1.0, 0.01, "").configKey("render.atmodawnfog.godrays"));
   public final NumberSetting softness = this.register(new NumberSetting("Softness", 0.6, 0.0, 1.0, 0.01, "").configKey("render.atmodawnfog.softness"));
   public final NumberSetting sunGlow = this.register(new NumberSetting("Sun Glow", 0.85, 0.0, 1.5, 0.01, "").configKey("render.atmodawnfog.sunglow"));
   public final BooleanSetting rainbow = this.register(
      new BooleanSetting("Rainbow", true).configKey("render.atmodawnfog.rainbow").visibleWhen(() -> !this.mode.is("Night"))
   );
   public final NumberSetting rainbowBrightness = this.register(
      new NumberSetting("Rainbow Brightness", 0.55, 0.1, 1.0, 0.01, "")
         .configKey("render.atmodawnfog.rainbowbrightness")
         .visibleWhen(() -> this.rainbow.getValue() && !this.mode.is("Night"))
   );
   public final NumberSetting rainbowSize = this.register(
      new NumberSetting("Rainbow Size", 54.0, 46.0, 60.0, 0.5, "")
         .configKey("render.atmodawnfog.rainbowsize")
         .visibleWhen(() -> this.rainbow.getValue() && !this.mode.is("Night"))
   );
   public final ColorSetting dawnColor = this.register(
      new ColorSetting("Dawn Color", -21126).configKey("render.atmodawnfog.dawncolor").visibleWhen(() -> !this.mode.is("Theme"))
   );
   public final NumberSetting stars = this.register(
      new NumberSetting("Stars", 0.85, 0.0, 1.0, 0.01, "").configKey("render.atmodawnfog.stars").visibleWhen(() -> this.mode.is("Night"))
   );
   public final NumberSetting aurora = this.register(
      new NumberSetting("Aurora", 0.6, 0.0, 1.0, 0.01, "").configKey("render.atmodawnfog.aurora").visibleWhen(() -> this.mode.is("Night"))
   );

   public AtmoDawnFogFeature() {
      super("AtmoDawnFog", "Cinematic atmosphere: fog, god rays, glowing sun and starry nights", FeatureCategory.VISUAL, -1);
   }

   public static AtmoDawnFogFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(AtmoDawnFogFeature.class);
   }

   public int modeIndex() {
      String value = this.mode.getValue();
      if ("Dusk".equals(value)) {
         return 1;
      } else if ("Night".equals(value)) {
         return 3;
      } else {
         return "Theme".equals(value) ? 2 : 0;
      }
   }
}

