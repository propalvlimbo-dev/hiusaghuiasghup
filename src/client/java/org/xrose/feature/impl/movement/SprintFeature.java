package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.utils.combat.ServerSprintTracker;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class SprintFeature extends Feature {
   public static volatile int tickStop = 0;
   public final BooleanSetting noReset = this.register(new BooleanSetting("Don't Reset Sprint", false));

   public SprintFeature() {
      super("Auto Sprint", "Auto sprint", FeatureCategory.MOVEMENT, 86);
   }

   public static SprintFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(SprintFeature.class);
   }

   public static boolean isServerSprinting() {
      return ServerSprintTracker.isServerSprinting();
   }

   public static void resetServerState() {
      ServerSprintTracker.resetServerState();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft mc = event.getClient();
      if (mc.player != null) {
         boolean horizontal = mc.player.horizontalCollision;
         boolean sneaking = mc.player.isCrouching() && !mc.player.isSwimming();
         boolean canSprint = !horizontal && mc.player.zza > 0.0F;
         if (!sneaking) {
            if (tickStop > 0) {
               if (mc.player.isSprinting()) {
                  mc.player.setSprinting(false);
               }

               tickStop--;
            } else {
               if (canSprint && !mc.player.isSprinting()) {
                  mc.player.setSprinting(true);
               }
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      resetServerState();
   }
}

