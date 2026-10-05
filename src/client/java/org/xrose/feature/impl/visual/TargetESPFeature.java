package org.xrose.feature.impl.visual;

import net.minecraft.world.entity.LivingEntity;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.impl.combat.TriggerBotFeature;
import org.xrose.feature.setting.ColorMode;
import org.xrose.feature.setting.ColorSetting;
import org.xrose.feature.setting.ModeSetting;
import sdk.api.optimize.optimize;

@optimize
public final class TargetESPFeature extends Feature {
   private static final String MODE_MARKER = "Marker";
   private static final String MODE_GHOSTS = "Ghosts";
   private static final String MODE_CIRCLE = "Circle";
   private static final String MODE_DEADHEADS = "Deadheads";
   private static final String MODE_CRYSTALS = "Crystals";
   private static final String MODE_CRYSTALS_V2 = "Crystals V2";
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "Marker", "Marker", "Ghosts", "Circle", "Deadheads", "Crystals", "Crystals V2"));
   public final ModeSetting colorMode = this.register(ColorMode.setting());
   public final ColorSetting color = this.register(new ColorSetting("Color", -16711800).visibleWhen(() -> ColorMode.isCustom(this.colorMode)));
   private static TargetESPFeature instance;

   public TargetESPFeature() {
      super("TargetESP", "Highlights the current combat target", FeatureCategory.VISUAL, -1);
      instance = this;
   }

   public static TargetESPFeature getInstance() {
      if (instance == null) {
         instance = FeatureManager.INSTANCE.getFeature(TargetESPFeature.class);
      }

      return instance;
   }

   public static TargetESPFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(TargetESPFeature.class);
   }

   public static LivingEntity getCurrentTargetSafe() {
      TargetESPFeature feature = getEnabled();
      return feature != null ? feature.getCurrentTarget() : null;
   }

   public LivingEntity getCurrentTarget() {
      AuraFeature aura = FeatureManager.INSTANCE.getEnabled(AuraFeature.class);
      LivingEntity target = aura != null ? aura.getCurrentTarget() : null;
      if (target == null) {
         TriggerBotFeature triggerBot = TriggerBotFeature.getEnabled();
         if (triggerBot != null) {
            target = triggerBot.getCurrentTarget();
         }
      }

      return target != null && target.isAlive() ? target : null;
   }

   public boolean usesGhostTargetEsp() {
      return this.mode.is("Ghosts");
   }

   public boolean usesCircleTargetEsp() {
      return this.mode.is("Circle");
   }

   public boolean usesDeadheadTargetEsp() {
      return this.mode.is("Deadheads");
   }

   public boolean usesCrystalTargetEsp() {
      return this.mode.is("Crystals");
   }

   public boolean usesCrystals2TargetEsp() {
      return this.mode.is("Crystals V2");
   }

   public int getMarkerColor() {
      return ColorMode.resolve(this.colorMode, this.color);
   }
}

