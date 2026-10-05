package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ElytraMotionFeature extends Feature {
   private final NumberSetting distance = this.register(new NumberSetting("Дистанция", 3.0, 1.0, 6.0, 0.1, ""));
   private final BooleanSetting bypass = this.register(new BooleanSetting("Обход", false));
   private boolean waitTarget = true;

   public ElytraMotionFeature() {
      super("ElytraMotion", "Управление движением элитр", FeatureCategory.MOVEMENT, -1);
   }

   @Override
   protected void onDisable() {
      LocalPlayer player = ElytraMotionFeature.MinecraftHolder.player();
      if (player != null) {
         this.waitTarget = true;
         player.setNoGravity(false);
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null) {
         AuraFeature aura = AuraFeature.getInstance();
         LivingEntity target = aura != null && aura.isEnabled() ? aura.getTarget() : null;
         if (target == null) {
            if (!this.waitTarget) {
               player.setNoGravity(false);
               this.waitTarget = true;
            }
         } else {
            this.waitTarget = false;
            float dist = (float)player.getEyePosition().distanceTo(target.getBoundingBox().getCenter());
            if (player.isFallFlying() && dist < this.distance.getValue()) {
               if (this.bypass.getValue()) {
                  double rad = Math.toRadians(player.getYRot());
                  double forward = 0.01;
                  double down = -1.0E-4;
                  double moveX = -Math.sin(rad) * forward;
                  double moveZ = Math.cos(rad) * forward;
                  player.setDeltaMovement(moveX, down, moveZ);
               } else {
                  player.setDeltaMovement(Vec3.ZERO);
               }

               player.setNoGravity(true);
            } else {
               player.setNoGravity(false);
            }
         }
      }
   }

   private static final class MinecraftHolder {
      static LocalPlayer player() {
         return Minecraft.getInstance().player;
      }
   }
}

