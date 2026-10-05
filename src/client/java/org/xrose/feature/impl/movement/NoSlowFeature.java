package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class NoSlowFeature extends Feature {
   public final ModeSetting itemMode = this.register(
      new ModeSetting("Item Mode", "ReallyWorld", "ReallyWorld", "Old Grim", "Grim", "GrimLast", "StormGrim", "SpookyAnarchy", "FunTime", "FunTimeCrossBow")
   );
   public final BooleanSetting sprint = this.register(new BooleanSetting("Sprint", true));
   private int ticks;
   private int cycleCounter;
   private boolean forcedSprintDuringEat;
   private boolean sprintWasPressedBeforeEat;
   private boolean wasUsingItem;
   private int goldenAppleDelay;

   public NoSlowFeature() {
      super("No Slow", "Prevents item-use movement slowdown", FeatureCategory.MOVEMENT, -1);
   }

   public static boolean shouldCancelSlowdown(LocalPlayer player) {
      NoSlowFeature feature = FeatureManager.INSTANCE.getEnabled(NoSlowFeature.class);
      return feature != null && player != null ? feature.handleUsingItem(player) : false;
   }

   @EventTarget
   private void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null) {
         boolean isUsingItemNow = player.isUsingItem();
         String mode = this.itemMode.getValue();
         this.wasUsingItem = isUsingItemNow;
         if (player.isUsingItem()) {
            this.ticks++;
            if (mode.equals("SpookyAnarchy") && this.isEatingGoldenApple(player)) {
               this.goldenAppleDelay = 5;
            }

            if (mode.equals("GrimLast")) {
               this.handleGrimLast(player);
            }

            if (mode.equals("ReallyWorld")) {
               if (!player.isAutoSpinAttack()) {
                  this.updateEatSprintBoost();
               }
            } else {
               this.resetEatSprintBoostIfNeeded();
            }
         } else {
            this.ticks = 0;
            this.cycleCounter = 0;
            this.resetEatSprintBoostIfNeeded();
         }

         if (this.goldenAppleDelay > 0) {
            this.goldenAppleDelay--;
         }
      }
   }

   private boolean handleUsingItem(LocalPlayer player) {
      String mode = this.itemMode.getValue();
      InteractionHand first = player.getUsedItemHand();
      InteractionHand second = first == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      switch (mode) {
         case "Old Grim":
            if (player.getOffhandItem().getUseAnimation() == ItemUseAnimation.NONE || player.getMainHandItem().getUseAnimation() == ItemUseAnimation.NONE) {
               this.sendUsePacket(first);
               this.sendUsePacket(second);
               return true;
            }
            break;
         case "ReallyWorld":
            int[] thresholds = player.isJumping() ? new int[]{5, 5, 4} : new int[]{5, 5, 4};
            int threshold = thresholds[this.cycleCounter % thresholds.length];
            if (this.ticks >= threshold) {
               this.ticks = 0;
               this.cycleCounter++;
               return true;
            }
            break;
         case "Grim":
            if (this.ticks > 1 && player.getUseItemRemainingTicks() > 0) {
               this.ticks = 0;
               return true;
            }
            break;
         case "GrimLast":
            if (this.isSpookyTimeServer() && this.isEatingGoldenApple(player)) {
               return false;
            }

            return this.handleGrimLastNoslow(player);
         case "StormGrim":
            if (this.ticks >= 3) {
               this.ticks = 0;
               return true;
            }
            break;
         case "SpookyAnarchy":
            if (this.isEatingGoldenApple(player) && this.goldenAppleDelay > 0) {
               return false;
            }

            if (this.ticks > 1 && player.getUseItemRemainingTicks() > 1) {
               this.ticks = 0;
               return true;
            }
            break;
         case "FunTimeCrossBow":
            boolean mainHandCrossbow = player.getMainHandItem().getItem() instanceof CrossbowItem;
            boolean offHandCrossbow = player.getOffhandItem().getItem() instanceof CrossbowItem;
            if ((mainHandCrossbow || offHandCrossbow) && this.ticks >= 1) {
               this.ticks = 0;
               this.cycleCounter++;
               return true;
            }
            break;
         case "FunTime":
            if (this.ticks > 0 && player.getTicksUsingItem() > 1) {
               boolean mainHandCrossbowx = player.getMainHandItem().getItem() instanceof CrossbowItem;
               boolean offHandCrossbowx = player.getOffhandItem().getItem() instanceof CrossbowItem;
               if (mainHandCrossbowx || offHandCrossbowx) {
                  this.ticks = 0;
                  return true;
               }

               if (player.onGround() && this.isOnSnowOrCarpet()) {
                  player.connection.send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, player.blockPosition().above(), Direction.DOWN));
                  this.ticks = 0;
                  return true;
               }
            }
      }

      return false;
   }

   private boolean isSpookyTimeServer() {
      Minecraft client = Minecraft.getInstance();
      if (client.getConnection() != null && client.getConnection().getServerData() != null) {
         String addr = client.getConnection().getServerData().ip.toLowerCase();
         return addr.contains("spookytime.net") || addr.contains("spookytime");
      } else {
         return false;
      }
   }

   private boolean isEatingGoldenApple(LocalPlayer player) {
      if (player != null && player.isUsingItem()) {
         Item active = player.getUseItem().getItem();
         return active == Items.GOLDEN_APPLE || active == Items.ENCHANTED_GOLDEN_APPLE;
      } else {
         return false;
      }
   }

   private void handleGrimLast(LocalPlayer player) {
      if (player != null && player.connection != null) {
         if (!this.isSpookyTimeServer() || !this.isEatingGoldenApple(player)) {
            int useTime = player.getTicksUsingItem();
            if (useTime == 2) {
               player.connection.send(new ServerboundPlayerActionPacket(Action.DROP_ALL_ITEMS, BlockPos.ZERO, player.getDirection()));
            }
         }
      }
   }

   private boolean handleGrimLastNoslow(LocalPlayer player) {
      if (player == null) {
         return false;
      }

      if (player.getUsedItemHand() == InteractionHand.OFF_HAND) {
         return false;
      }

      int useTime = player.getTicksUsingItem();
      return useTime > 4;
   }

   private boolean isOnSnowOrCarpet() {
      Minecraft client = Minecraft.getInstance();
      return client.player != null && client.level != null ? client.level.getBlockState(client.player.blockPosition()).getBlock() == Blocks.SNOW : false;
   }

   private void sendUsePacket(InteractionHand hand) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null && client.getConnection() != null) {
         client.getConnection().send(new ServerboundUseItemPacket(hand, 0, player.getYRot(), player.getXRot()));
      }
   }

   private void updateEatSprintBoost() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null && client.options != null) {
         boolean eating = player.isUsingItem() && player.getUseItem().getUseAnimation() == ItemUseAnimation.EAT;
         if (eating) {
            if (!this.forcedSprintDuringEat) {
               this.sprintWasPressedBeforeEat = client.options.keySprint.isDown();
               this.forcedSprintDuringEat = true;
            }

            if (this.sprint.getValue()) {
               client.options.keySprint.setDown(true);
               player.setSprinting(true);
            }
         } else {
            this.resetEatSprintBoostIfNeeded();
         }
      }
   }

   private void resetEatSprintBoostIfNeeded() {
      Minecraft client = Minecraft.getInstance();
      if (this.forcedSprintDuringEat && client.options != null) {
         if (!this.sprintWasPressedBeforeEat) {
            client.options.keySprint.setDown(false);
         }

         this.forcedSprintDuringEat = false;
         this.sprintWasPressedBeforeEat = false;
      }
   }

   @Override
   protected void onDisable() {
      this.resetEatSprintBoostIfNeeded();
      this.ticks = 0;
      this.cycleCounter = 0;
      this.wasUsingItem = false;
      this.goldenAppleDelay = 0;
      if (Minecraft.getInstance().options != null) {
         Minecraft.getInstance().options.keySprint.setDown(false);
      }
   }
}

