package org.xrose.feature.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.utils.inventory.InventoryUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoToolFeature extends Feature {
   private static final int HOTBAR_SIZE = 9;
   private static final int INVENTORY_SIZE = 36;
   public final BooleanSetting useInventory = this.register(new BooleanSetting("Use Inventory", true));
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "Silent", "Silent", "Normal"));
   private int originalHotbarSlot = -1;
   private int swappedInventorySlot = -1;
   private int silentServerSlot = -1;

   public AutoToolFeature() {
      super("AutoTool", "Picks the best tool for the targeted block", FeatureCategory.PLAYER, -1);
   }

   @Override
   protected void onDisable() {
      this.restore();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player == null || client.level == null || player.isCreative() || client.gui.screen() != null) {
         this.restore();
      } else if (!this.isBreakingBlock(client)) {
         this.restore();
      } else {
         BlockHitResult hit = (BlockHitResult)client.hitResult;
         BlockState state = client.level.getBlockState(hit.getBlockPos());
         int bestSlot = this.findBestToolSlot(player, state);
         if (bestSlot != -1) {
            if (this.originalHotbarSlot == -1) {
               this.originalHotbarSlot = player.getInventory().getSelectedSlot();
            }

            if (bestSlot < 9) {
               this.selectHotbarTool(player, bestSlot);
            } else {
               if (this.swappedInventorySlot != bestSlot) {
                  this.restoreSilentServerSlot(player);
                  this.restoreSwappedItem();
                  InventoryUtil.swapWithHotbar(bestSlot, this.originalHotbarSlot);
                  this.swappedInventorySlot = bestSlot;
               }

               player.getInventory().setSelectedSlot(this.originalHotbarSlot);
            }
         }
      }
   }

   public boolean swapForBlock(BlockState state) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (this.isEnabled() && player != null && state != null) {
         int bestSlot = this.findBestToolSlot(player, state);
         if (bestSlot < 0) {
            return false;
         }

         if (this.originalHotbarSlot == -1) {
            this.originalHotbarSlot = player.getInventory().getSelectedSlot();
         }

         if (bestSlot < 9) {
            this.selectHotbarTool(player, bestSlot);
         } else if (this.useInventory.getValue()) {
            this.restoreSilentServerSlot(player);
            this.restoreSwappedItem();
            InventoryUtil.swapWithHotbar(bestSlot, this.originalHotbarSlot);
            this.swappedInventorySlot = bestSlot;
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isBreakingBlock(Minecraft client) {
      return client.options.keyAttack.isDown() && client.hitResult != null && client.hitResult.getType() == Type.BLOCK;
   }

   private int findBestToolSlot(LocalPlayer player, BlockState state) {
      int limit = this.useInventory.getValue() ? 36 : 9;
      int bestSlot = -1;
      float bestSpeed = 1.0F;

      for (int slot = 0; slot < limit; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.isEmpty()) {
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
               bestSpeed = speed;
               bestSlot = slot;
            }
         }
      }

      return bestSlot;
   }

   private void selectHotbarTool(LocalPlayer player, int slot) {
      if (this.mode.is("Normal")) {
         this.restoreSilentServerSlot(player);
         player.getInventory().setSelectedSlot(slot);
      } else if (slot == player.getInventory().getSelectedSlot()) {
         this.restoreSilentServerSlot(player);
      } else {
         if (this.silentServerSlot != slot) {
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            this.silentServerSlot = slot;
         }
      }
   }

   private void restore() {
      if (this.originalHotbarSlot != -1) {
         LocalPlayer player = Minecraft.getInstance().player;
         this.restoreSwappedItem();
         if (player != null) {
            this.restoreSilentServerSlot(player);
            player.getInventory().setSelectedSlot(this.originalHotbarSlot);
         }

         this.originalHotbarSlot = -1;
      }
   }

   private void restoreSwappedItem() {
      if (this.swappedInventorySlot != -1) {
         InventoryUtil.swapWithHotbar(this.swappedInventorySlot, this.originalHotbarSlot);
         this.swappedInventorySlot = -1;
      }
   }

   private void restoreSilentServerSlot(LocalPlayer player) {
      if (this.silentServerSlot != -1) {
         player.connection.send(new ServerboundSetCarriedItemPacket(player.getInventory().getSelectedSlot()));
         this.silentServerSlot = -1;
      }
   }
}

