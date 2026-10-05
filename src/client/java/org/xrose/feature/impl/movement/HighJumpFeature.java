package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SlimeBlock;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.ModeSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class HighJumpFeature extends Feature {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Boat", "Boat", "Shulker Screen", "Slime Boost", "FunTime Soul Sand"));
   private boolean wasOnSlimeBlock;

   public HighJumpFeature() {
      super("HighJump", "Увеличивает высоту прыжка", FeatureCategory.MOVEMENT, -1);
   }

   @Override
   protected void onDisable() {
      this.wasOnSlimeBlock = false;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null && client.level != null) {
         if (this.mode.is("Boat")) {
            this.handleBoat(client, player);
         } else if (this.mode.is("Shulker Screen")) {
            this.handleShulkerScreen(player);
         } else if (this.mode.is("Slime Boost")) {
            this.handleSlimeBoost(client, player);
         } else if (this.mode.is("FunTime Soul Sand")) {
            this.handleFunTimeSoulSand(player);
         }
      }
   }

   private void handleBoat(Minecraft client, LocalPlayer player) {
      if (client.gui.screen() instanceof ShulkerBoxScreen) {
         player.setDeltaMovement(player.getDeltaMovement().add(0.0, 1.0, 0.0));
         player.setPos(player.getX(), player.getY() + 0.24, player.getZ());
      }
   }

   private void handleShulkerScreen(LocalPlayer player) {
      Minecraft client = Minecraft.getInstance();
      if (client.gui.screen() instanceof ShulkerBoxScreen) {
         player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.9, 0.0));
      }
   }

   private void handleSlimeBoost(Minecraft client, LocalPlayer player) {
      boolean onSlime = isOnSlimeBlock(client, player);
      if (player.onGround() && onSlime) {
         this.wasOnSlimeBlock = true;
      } else if (this.wasOnSlimeBlock && !player.onGround() && player.getDeltaMovement().y > 0.0) {
         player.setDeltaMovement(player.getDeltaMovement().add(0.0, 1.35, 0.0));
         this.wasOnSlimeBlock = false;
      } else if (!onSlime) {
         this.wasOnSlimeBlock = false;
      }
   }

   private void handleFunTimeSoulSand(LocalPlayer player) {
      if (player.isInWater() && !player.isUnderWater()) {
         player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.56, 0.0));
      }
   }

   private static boolean isOnSlimeBlock(Minecraft client, LocalPlayer player) {
      BlockPos below = player.blockPosition().below();
      return client.level.getBlockState(below).getBlock() instanceof SlimeBlock;
   }
}

