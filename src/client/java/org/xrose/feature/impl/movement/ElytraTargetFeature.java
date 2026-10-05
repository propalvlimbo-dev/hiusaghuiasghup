package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.xrose.event.EventTarget;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.setting.BindSetting;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.math.BestPoint;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ElytraTargetFeature extends Feature {
   private static ElytraTargetFeature instance;
   public final BooleanSetting predictate = this.register(new BooleanSetting("Перегон", true));
   public final BindSetting predictateKey = this.register(new BindSetting("Бинд Перегона", -1));
   public final NumberSetting predictValue = this.register(new NumberSetting("Значение", 3.0, 1.0, 6.0, 0.1, "").visibleWhen(() -> this.predictate.getValue()));
   public final NumberSetting findRange = this.register(new NumberSetting("Дистанция наводки", 32.0, 6.0, 64.0, 1.0, ""));
   public final BooleanSetting elytraSlowdown = this.register(new BooleanSetting("Замедлять", true));
   public final BindSetting elytraSlowdownKey = this.register(new BindSetting("Бинд Замедления", -1));
   public final NumberSetting slowdownRadius = this.register(
      new NumberSetting("Радиус", 1.9, 1.0, 6.0, 0.1, "").visibleWhen(() -> this.elytraSlowdown.getValue())
   );
   public final NumberSetting minSpeed = this.register(new NumberSetting("Скорость", 0.2, 0.1, 1.0, 0.05, "").visibleWhen(() -> this.elytraSlowdown.getValue()));
   public final BooleanSetting hitAfterOvertake = this.register(new BooleanSetting("Sloth bypa$", false));
   public final BindSetting hitAfterOvertakeKey = this.register(new BindSetting("Бинд Sloth bypa$", -1));

   public ElytraTargetFeature() {
      super("Elytra Target", "Elytra targeting helpers for Aura", FeatureCategory.MOVEMENT, -1);
      instance = this;
   }

   public static ElytraTargetFeature getInstance() {
      if (instance == null) {
         instance = FeatureManager.INSTANCE.getFeature(ElytraTargetFeature.class);
      }

      return instance;
   }

   public static ElytraTargetFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(ElytraTargetFeature.class);
   }

   public boolean canAttack(LivingEntity target) {
      if (target != null && Minecraft.getInstance().player != null) {
         AuraFeature aura = AuraFeature.getInstance();
         float attackDist = aura != null ? aura.attackDistance() : 4.0F;
         double distNearest = Minecraft.getInstance().player.getEyePosition().distanceTo(BestPoint.getNearestPoint(target));
         return distNearest <= attackDist;
      } else {
         return false;
      }
   }

   public static boolean isPredictateActive() {
      ElytraTargetFeature feature = getEnabled();
      return feature != null && feature.predictate.getValue();
   }

   public static double getPredictValue() {
      ElytraTargetFeature feature = getEnabled();
      return feature != null ? feature.predictValue.getValue() : 3.0;
   }

   public static float getPredictValueFloat() {
      return (float)getPredictValue();
   }

   public static double getFindRange() {
      ElytraTargetFeature feature = getEnabled();
      return feature != null ? feature.findRange.getValue() : 32.0;
   }

   public static float getFindRangeFloat() {
      return (float)getFindRange();
   }

   public static boolean isElytraSlowdownActive() {
      ElytraTargetFeature feature = getEnabled();
      return feature != null && feature.elytraSlowdown.getValue();
   }

   public static double getSlowdownRadius() {
      ElytraTargetFeature feature = getEnabled();
      return feature != null ? feature.slowdownRadius.getValue() : 3.0;
   }

   public static float getMinSpeed() {
      ElytraTargetFeature feature = getEnabled();
      return feature != null ? feature.minSpeed.getFloat() : 0.3F;
   }

   public static boolean isHitAfterOvertake() {
      ElytraTargetFeature feature = getEnabled();
      return feature != null && feature.hitAfterOvertake.getValue();
   }

   public static boolean isShowPredictPoint() {
      return getEnabled() != null;
   }

   @EventTarget
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (Minecraft.getInstance().gui.screen() == null && event.getAction() == 1) {
         if (this.predictateKey.matches(event.getKey())) {
            this.predictate.toggle();
         }

         if (this.elytraSlowdownKey.matches(event.getKey())) {
            this.elytraSlowdown.toggle();
         }

         if (this.hitAfterOvertakeKey.matches(event.getKey())) {
            this.hitAfterOvertake.toggle();
         }
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      if (Minecraft.getInstance().gui.screen() == null && event.getAction() == 1) {
         if (this.predictateKey.matchesMouse(event.getButton())) {
            this.predictate.toggle();
         }

         if (this.elytraSlowdownKey.matchesMouse(event.getButton())) {
            this.elytraSlowdown.toggle();
         }

         if (this.hitAfterOvertakeKey.matchesMouse(event.getButton())) {
            this.hitAfterOvertake.toggle();
         }
      }
   }
}

