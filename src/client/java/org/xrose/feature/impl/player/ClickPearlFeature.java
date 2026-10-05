package org.xrose.feature.impl.player;

import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.InputBindSetting;
import org.xrose.pve.AutomationOwner;
import org.xrose.pve.AutomationPriority;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.utils.inventory.InventorySwap;
import org.xrose.utils.inventory.InventoryUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ClickPearlFeature extends Feature implements AutomationOwner {
   private static final int HOTBAR_SIZE = 9;
   public final InputBindSetting key = this.register(new InputBindSetting("Key", -1));
   private boolean throwQueued;
   private boolean releasePending;

   public ClickPearlFeature() {
      super("ClickPearl", "Quickly throws an ender pearl", FeatureCategory.PLAYER, -1);
   }

   @Override
   protected void onDisable() {
      this.throwQueued = false;
      this.releasePending = false;
      PveAutomationCoordinator.INSTANCE.release(this);
   }

   @EventTarget
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (event.getAction() == 1 && this.key.matches(event.getKey())) {
         this.queueThrow();
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      if (event.getAction() == 1 && this.key.matchesMouse(event.getButton()) && this.queueThrow()) {
         event.cancel();
      }
   }

   private boolean queueThrow() {
      Minecraft client = Minecraft.getInstance();
      if (client.gui.screen() == null
         && client.player != null
         && PveAutomationCoordinator.INSTANCE
            .acquire(this, AutomationPriority.EMERGENCY, EnumSet.of(AutomationResource.INVENTORY, AutomationResource.ROTATION))) {
         this.throwQueued = true;
         return true;
      } else {
         return false;
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.releasePending && !InventorySwap.isBusy()) {
         this.releasePending = false;
         PveAutomationCoordinator.INSTANCE.release(this);
      }

      if (this.throwQueued) {
         this.throwQueued = false;
         boolean asyncSwap = false;

         try {
            Minecraft client = event.getClient();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null) {
               return;
            }

            if (player.getMainHandItem().is(Items.ENDER_PEARL)) {
               this.throwFromMainHand(client, player);
               return;
            }

            int hotbarSlot = this.findPearlHotbarSlot(player);
            if (hotbarSlot == -1) {
               int containerSlot = this.findPearlContainerSlot(player);
               if (containerSlot != -1) {
                  InventorySwap.useFromSlot(containerSlot);
                  this.releasePending = true;
                  asyncSwap = true;
               }

               return;
            }

            int previousSlot = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(hotbarSlot);
            this.throwFromMainHand(client, player);
            player.getInventory().setSelectedSlot(previousSlot);
         } finally {
            if (!asyncSwap) {
               PveAutomationCoordinator.INSTANCE.release(this);
            }
         }
      }
   }

   private void throwFromMainHand(Minecraft client, LocalPlayer player) {
      client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
      player.swing(InteractionHand.MAIN_HAND);
   }

   private int findPearlHotbarSlot(LocalPlayer player) {
      for (int slot = 0; slot < 9; slot++) {
         if (player.getInventory().getItem(slot).is(Items.ENDER_PEARL)) {
            return slot;
         }
      }

      return -1;
   }

   private int findPearlContainerSlot(LocalPlayer player) {
      return InventoryUtil.findInventorySlot(player, stack -> stack.is(Items.ENDER_PEARL));
   }
}

