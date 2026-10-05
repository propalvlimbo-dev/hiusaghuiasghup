package org.xrose.feature.impl.movement;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.xrose.context.MinecraftContext;
import org.xrose.context.RotationContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.FireworkEvent;
import org.xrose.event.events.render.Render2DEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.ScaleUtil;
import org.xrose.utils.render.gui.TextAlign;
import org.xrose.utils.render.gui.UiFontStyle;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ElytraBoosterFeature extends Feature implements MinecraftContext {
   private static final int[] AI_YAW_VECTORS = new int[]{-45, 45, 135, -135};
   private static final int[] AI_PITCH_VECTORS = new int[]{-45, 45};
   public final BooleanSetting showOverlay = this.register(new BooleanSetting("Показать инфо", true).configKey("movement.elytrabooster.showinfo"));
   public final BooleanSetting boost = this.register(new BooleanSetting("Ускорение", true).configKey("movement.elytrabooster.boost"));
   public final ModeSetting mode = this.register(
      new ModeSetting("Режим ускорения", "Bravo", "Bravo", "Кастомный", "ReallyWorld")
         .configKey("movement.elytrabooster.mode")
         .visibleWhen(() -> this.boost.getValue())
   );
   public final BooleanSetting maxspeed = this.register(
      new BooleanSetting("Скорость по Углам", false)
         .configKey("movement.elytrabooster.maxspeed")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный"))
   );
   public final NumberSetting speedxz = this.register(
      new NumberSetting("Скорость XZ", 1.65, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.speedxz")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && !this.maxspeed.getValue())
   );
   public final NumberSetting speedy = this.register(
      new NumberSetting("Скорость Y", 1.59, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.speedy")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && !this.maxspeed.getValue())
   );
   public final NumberSetting yaw05 = this.register(
      new NumberSetting("Yaw 0-5°", 1.6, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw05")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw510 = this.register(
      new NumberSetting("Yaw 5-10°", 1.62, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw510")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw1015 = this.register(
      new NumberSetting("Yaw 10-15°", 1.65, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw1015")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw1520 = this.register(
      new NumberSetting("Yaw 15-20°", 1.68, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw1520")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw2025 = this.register(
      new NumberSetting("Yaw 20-25°", 1.74, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw2025")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw2530 = this.register(
      new NumberSetting("Yaw 25-30°", 1.8, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw2530")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw3035 = this.register(
      new NumberSetting("Yaw 30-35°", 1.8, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw3035")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw3540 = this.register(
      new NumberSetting("Yaw 35-40°", 1.8, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw3540")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting yaw4045 = this.register(
      new NumberSetting("Yaw 40-45°", 1.82, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.yaw4045")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch05 = this.register(
      new NumberSetting("Pitch 0-5°", 1.59, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch05")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch510 = this.register(
      new NumberSetting("Pitch 5-10°", 1.6, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch510")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch1015 = this.register(
      new NumberSetting("Pitch 10-15°", 1.61, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch1015")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch1520 = this.register(
      new NumberSetting("Pitch 15-20°", 1.62, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch1520")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch2025 = this.register(
      new NumberSetting("Pitch 20-25°", 1.68, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch2025")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch2530 = this.register(
      new NumberSetting("Pitch 25-30°", 1.74, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch2530")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch3035 = this.register(
      new NumberSetting("Pitch 30-35°", 1.95, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch3035")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch3540 = this.register(
      new NumberSetting("Pitch 35-40°", 2.0, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch3540")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   public final NumberSetting pitch4045 = this.register(
      new NumberSetting("Pitch 40-45°", 2.2, 1.5, 2.5, 0.01, "")
         .configKey("movement.elytrabooster.pitch4045")
         .visibleWhen(() -> this.boost.getValue() && this.mode.is("Кастомный") && this.maxspeed.getValue())
   );
   private final NumberSetting[] yawSettings = new NumberSetting[]{
      this.yaw05, this.yaw510, this.yaw1015, this.yaw1520, this.yaw2025, this.yaw2530, this.yaw3035, this.yaw3540, this.yaw4045
   };
   private final NumberSetting[] pitchSettings = new NumberSetting[]{
      this.pitch05, this.pitch510, this.pitch1015, this.pitch1520, this.pitch2025, this.pitch2530, this.pitch3035, this.pitch3540, this.pitch4045
   };
   private double timerAccumulator = 0.0;

   public ElytraBoosterFeature() {
      super("ElytraBooster", "Boosts the speed given by firework rockets while gliding", FeatureCategory.MOVEMENT, -1);
   }

   public static boolean shouldSkipBaseTick() {
      return false;
   }

   public static boolean consumeExtraTick() {
      ElytraBoosterFeature booster = FeatureManager.INSTANCE.getEnabled(ElytraBoosterFeature.class);
      LocalPlayer player = Minecraft.getInstance().player;
      if (booster != null && player != null && player.isFallFlying()) {
         booster.timerAccumulator = booster.timerAccumulator + (booster.boost.getValue() ? 0.02 : 0.0);
         if (booster.timerAccumulator < 1.0) {
            return false;
         }

         booster.timerAccumulator--;
         return true;
      } else {
         return false;
      }
   }

   @EventTarget
   private void onFirework(FireworkEvent event) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && this.boost.getValue() && event.getBoostedEntity() == player) {
         float yaw = player.getYRot();
         float pitch = player.getXRot();
         if (this.isAuraActive()) {
            yaw = Mth.wrapDegrees(this.auraYaw());
            pitch = this.auraPitch();
         }

         ElytraBoosterFeature.BoostValues values = this.computeBoost(yaw, pitch);
         event.setSpeed(Math.max(values.speedXZ(), values.speedY()));
      }
   }

   @EventTarget
   public void onRender2D(Render2DEvent event) {
      if (this.showOverlay.getValue()) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null && player.isFallFlying()) {
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            if (this.isAuraActive()) {
               yaw = Mth.wrapDegrees(this.auraYaw());
               pitch = this.auraPitch();
            }

            String pitchText = String.format(Locale.US, "Y: %.1f°", pitch);
            String yawText = String.format(Locale.US, "XZ: %.1f°", Mth.wrapDegrees(yaw));
            String text = pitchText + "  ·  " + yawText;
            float unit = ScaleUtil.toGuiPixels(1.0F, Minecraft.getInstance().getWindow().getGuiScale());
            float centerX = event.getGuiGraphicsExtractor().guiWidth() / 2.0F;
            float centerY = event.getGuiGraphicsExtractor().guiHeight() / 2.0F;
            Render2DUtil.text(centerX, centerY + 32.0F * unit, 8.0F * unit, text)
               .style(UiFontStyle.SEMIBOLD)
               .align(TextAlign.CENTER)
               .color(ColorUtil.multiplyAlpha(Theme.getAccent(), 0.9019608F))
               .outline(ColorUtil.multiplyAlpha(-1342177280, 1.0F), 0.8F)
               .draw();
         }
      }
   }

   private boolean isAuraActive() {
      return FeatureManager.INSTANCE.getEnabled(AuraFeature.class) != null;
   }

   private float auraYaw() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (RotationContext.isActive()) {
         return RotationContext.getServerYaw();
      } else {
         return player != null ? player.getYRot() : 0.0F;
      }
   }

   private float auraPitch() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (RotationContext.isActive()) {
         return RotationContext.getServerPitch();
      } else {
         return player != null ? player.getXRot() : 0.0F;
      }
   }

   private ElytraBoosterFeature.BoostValues computeBoost(float yaw, float pitch) {
      if (this.boost.getValue() && mc.player != null) {
         if (this.isAuraActive()) {
            yaw = Mth.wrapDegrees(this.auraYaw());
            pitch = this.auraPitch();
         }

         if (pitch < -75.0F) {
            return new ElytraBoosterFeature.BoostValues(1.5F, 1.5F);
         } else if (this.mode.is("Кастомный")) {
            return this.handleCustomMode(yaw, pitch);
         } else if (this.mode.is("Bravo")) {
            return new ElytraBoosterFeature.BoostValues(1.8F, 1.8F);
         } else {
            return this.mode.is("ReallyWorld") ? this.handleReallyWorldMode(yaw, pitch) : new ElytraBoosterFeature.BoostValues(1.5F, 1.5F);
         }
      } else {
         return new ElytraBoosterFeature.BoostValues(1.5F, 1.5F);
      }
   }

   private ElytraBoosterFeature.BoostValues handleCustomMode(float yaw, float pitch) {
      if (!this.maxspeed.getValue()) {
         return new ElytraBoosterFeature.BoostValues(this.speedxz.getValue().floatValue(), this.speedy.getValue().floatValue());
      }

      float convertedYaw = this.convertAngleToRange(Mth.wrapDegrees(yaw));
      float convertedPitch = this.convertAngleToRange(Math.abs(pitch));
      float xzSpeed = this.getSpeedForYaw(convertedYaw);
      float ySpeed = this.getSpeedForPitch(convertedPitch);
      if (ySpeed > xzSpeed) {
         xzSpeed = ySpeed;
      }

      return new ElytraBoosterFeature.BoostValues(xzSpeed, ySpeed);
   }

   private ElytraBoosterFeature.BoostValues handleReallyWorldMode(float yaw, float pitch) {
      float speed = this.getAiBoost(pitch, yaw, false, true);
      return new ElytraBoosterFeature.BoostValues(speed, speed);
   }

   private float getSpeedForYaw(float yaw) {
      int index = (int)(yaw / 5.0F);
      if (index >= this.yawSettings.length) {
         index = this.yawSettings.length - 1;
      }

      if (index < 0) {
         index = 0;
      }

      return this.yawSettings[index].getValue().floatValue();
   }

   private float getSpeedForPitch(float pitch) {
      int index = (int)(pitch / 5.0F);
      if (index >= this.pitchSettings.length) {
         index = this.pitchSettings.length - 1;
      }

      if (index < 0) {
         index = 0;
      }

      return this.pitchSettings[index].getValue().floatValue();
   }

   private float convertAngleToRange(float angle) {
      float absAngle = Math.abs(angle);
      if (absAngle > 90.0F) {
         absAngle = 180.0F - absAngle;
      }

      if (absAngle > 45.0F) {
         absAngle = 90.0F - absAngle;
      }

      return absAngle;
   }

   private float getAiBoost(float pitch, float yaw, boolean isBravo, boolean applyRwCap) {
      if (Math.abs(pitch) > 55.0F) {
         return 1.55F;
      }

      float boost = this.adjustBoostForYaw(yaw, applyRwCap);
      boost = this.adjustBoostForPitch(pitch, boost);
      boost = Math.max(isBravo ? 1.65F : 1.6F, boost);
      return Math.min(boost, isBravo ? 1.9F : 2.2F);
   }

   private float adjustBoostForYaw(float yaw, boolean applyRwCap) {
      int idx = findClosestVector(yaw, AI_YAW_VECTORS);
      if (idx == -1) {
         return 1.6F;
      }

      float dist = Math.abs(Mth.wrapDegrees(yaw) - AI_YAW_VECTORS[idx]);
      float maxBoost = 2.2F;
      float minBoostVal = 1.6F;
      float maxDistance = 12.0F;
      float smartBoost = 0.0F;
      if (dist <= maxDistance) {
         float ratio = dist / maxDistance;
         smartBoost = maxBoost - (maxBoost - minBoostVal) * ratio;
      }

      float variableSpeed = getVariableSpeed(dist);
      float finalSpeed = Math.max(smartBoost, variableSpeed);
      return applyRwCap ? Math.min(finalSpeed, 1.8F) : finalSpeed;
   }

   private float adjustBoostForPitch(float pitch, float boost) {
      int idx = findClosestVector(pitch, AI_PITCH_VECTORS);
      if (idx == -1) {
         return boost;
      }

      float dist = Math.abs(Math.abs(pitch) - Math.abs(AI_PITCH_VECTORS[idx]));
      if (dist < 30.0F) {
         boost += 0.4F * (1.0F - dist / 30.0F);
      }

      return boost;
   }

   private static float getVariableSpeed(float dist) {
      float[] thresholds = new float[]{4.0F, 8.0F, 11.0F, 15.0F, 21.0F, 28.0F};
      float[] speeds = new float[]{2.2F, 2.1F, 2.0F, 1.9F, 1.8F, 1.7F, 1.6F};
      int level = 0;

      while (level < thresholds.length && dist >= thresholds[level]) {
         level++;
      }

      return speeds[level];
   }

   private static int findClosestVector(float angle, int[] vectors) {
      int minIdx = -1;
      float minDist = Float.MAX_VALUE;

      for (int i = 0; i < vectors.length; i++) {
         float d = Math.abs(Mth.wrapDegrees(angle) - vectors[i]);
         if (d < minDist) {
            minDist = d;
            minIdx = i;
         }
      }

      return minIdx;
   }

   public record BoostValues(float speedXZ, float speedY) {
   }
}

