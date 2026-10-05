package org.xrose.utils.inventory;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.utils.inventory.swap.SwapExecutor;
import org.xrose.utils.inventory.swap.SwapSettings;

public final class InventorySwap implements MinecraftContext {
   public static final InventorySwap INSTANCE = new InventorySwap();
   private static final int HOTBAR_START = 36;
   private static final int HOTBAR_END = 45;
   private static final int OFFHAND_BUTTON = 40;
   private InventorySwap.State state = InventorySwap.State.IDLE;
   private int sourceSlot = -1;
   private int hotbarButton = 40;
   private boolean useAndReturn;
   private boolean waitForUseCompletion;
   private boolean immediateReturn;
   private boolean directUse;
   private boolean fastTiming;
   private ContainerInput clickInput = ContainerInput.SWAP;
   private boolean syntheticUseHeld;
   private int useWaitTicks;
   private static final int MAX_USE_WAIT_TICKS = 200;
   private boolean swapClickValid = true;
   private final SwapExecutor swapExecutor = new SwapExecutor();
   private final SwapExecutor returnExecutor = new SwapExecutor();

   private InventorySwap() {
   }

   public static void equip(int containerSlot) {
      LocalPlayer player = INSTANCE.player();
      if (player != null
         && INSTANCE.state == InventorySwap.State.IDLE
         && !DropAllInventoryController.blocksInventoryOperations()
         && player.containerMenu == player.inventoryMenu) {
         if (containerSlot >= 36 && containerSlot < 45) {
            mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, containerSlot, 40, ContainerInput.SWAP, player);
         } else {
            INSTANCE.begin(containerSlot, 40, false, false, false, false, ContainerInput.SWAP);
         }
      }
   }

   public static void moveToHotbar(int containerSlot, int hotbarSlot) {
      if (INSTANCE.player() != null
         && INSTANCE.state == InventorySwap.State.IDLE
         && !DropAllInventoryController.blocksInventoryOperations()
         && hotbarSlot >= 0
         && hotbarSlot <= 8) {
         INSTANCE.begin(containerSlot, hotbarSlot, false, false, false, false, ContainerInput.SWAP);
      }
   }

   public static void useFromSlot(int containerSlot) {
      useFromSlot(containerSlot, false);
   }

   public static void useFromSlot(int containerSlot, boolean waitForUseCompletion) {
      useFromSlot(containerSlot, waitForUseCompletion, false);
   }

   public static void useFromSlot(int containerSlot, boolean waitForUseCompletion, boolean immediateReturn) {
      useFromSlot(containerSlot, waitForUseCompletion, immediateReturn, false);
   }

   public static void useFromSlot(int containerSlot, boolean waitForUseCompletion, boolean immediateReturn, boolean fastTiming) {
      LocalPlayer player = INSTANCE.player();
      if (player != null
         && INSTANCE.state == InventorySwap.State.IDLE
         && !DropAllInventoryController.blocksInventoryOperations()
         && player.containerMenu == player.inventoryMenu) {
         INSTANCE.fastTiming = fastTiming;
         INSTANCE.begin(containerSlot, player.getInventory().getSelectedSlot(), true, waitForUseCompletion, immediateReturn, false, ContainerInput.SWAP);
      }
   }

   public static void useSelected(boolean waitForUseCompletion) {
      useSelected(waitForUseCompletion, false);
   }

   public static void useSelected(boolean waitForUseCompletion, boolean fastTiming) {
      LocalPlayer player = INSTANCE.player();
      if (player != null
         && INSTANCE.state == InventorySwap.State.IDLE
         && !DropAllInventoryController.blocksInventoryOperations()
         && player.containerMenu == player.inventoryMenu) {
         INSTANCE.fastTiming = fastTiming;
         INSTANCE.begin(-1, player.getInventory().getSelectedSlot(), true, waitForUseCompletion, false, true, ContainerInput.SWAP);
      }
   }

   public static boolean dropStack(int containerSlot) {
      LocalPlayer player = INSTANCE.player();
      if (player != null
         && INSTANCE.state == InventorySwap.State.IDLE
         && !DropAllInventoryController.blocksInventoryOperations()
         && player.containerMenu == player.inventoryMenu
         && player.inventoryMenu.isValidSlotIndex(containerSlot)
         && !player.inventoryMenu.getSlot(containerSlot).getItem().isEmpty()) {
         INSTANCE.begin(containerSlot, 1, false, false, false, false, ContainerInput.THROW);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isBusy() {
      return INSTANCE.state != InventorySwap.State.IDLE;
   }

   public static boolean shouldStopMovement() {
      return INSTANCE.swapExecutor.isBlocking() || INSTANCE.returnExecutor.isBlocking();
   }

   public static void abort() {
      INSTANCE.finish();
   }

   private void begin(
      int containerSlot,
      int hotbarButton,
      boolean useAndReturn,
      boolean waitForUseCompletion,
      boolean immediateReturn,
      boolean directUse,
      ContainerInput clickInput
   ) {
      this.sourceSlot = containerSlot;
      this.hotbarButton = hotbarButton;
      this.useAndReturn = useAndReturn;
      this.waitForUseCompletion = waitForUseCompletion;
      this.immediateReturn = immediateReturn;
      this.directUse = directUse;
      this.clickInput = clickInput == null ? ContainerInput.SWAP : clickInput;
      this.syntheticUseHeld = false;
      this.useWaitTicks = 0;
      this.swapClickValid = true;
      SwapSettings timing = this.fastTiming ? SwapSettings.fast() : SwapSettings.legit();
      if (this.directUse) {
         this.state = InventorySwap.State.USING;
      } else {
         this.state = InventorySwap.State.EXECUTING_SWAP;
         this.swapExecutor.execute(this::clickSourceToButton, timing.closeInventory(false), this::afterSwapClick);
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.state != InventorySwap.State.IDLE) {
         LocalPlayer player = this.player();
         if (player == null || player.containerMenu != player.inventoryMenu) {
            this.finish();
         } else if (this.state == InventorySwap.State.EXECUTING_SWAP) {
            this.swapExecutor.tick();
         } else if (this.state == InventorySwap.State.EXECUTING_RETURN) {
            this.returnExecutor.tick();
         } else {
            switch (this.state) {
               case USING:
                  int selected = player.getInventory().getSelectedSlot();
                  if (selected != this.hotbarButton) {
                     player.getInventory().setSelectedSlot(this.hotbarButton);
                  }

                  mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                  player.swing(InteractionHand.MAIN_HAND);
                  if (selected != this.hotbarButton) {
                     player.getInventory().setSelectedSlot(selected);
                  }

                  if (this.waitForUseCompletion && player.isUsingItem()) {
                     mc.options.keyUse.setDown(true);
                     this.syntheticUseHeld = true;
                     this.state = InventorySwap.State.WAITING_FOR_USE;
                  } else {
                     this.afterUse();
                  }
                  break;
               case WAITING_FOR_USE:
                  this.useWaitTicks++;
                  if (!player.isUsingItem() || this.useWaitTicks >= 200) {
                     this.releaseSyntheticUse();
                     this.afterUse();
                  }
                  break;
               default:
                  this.finish();
            }
         }
      }
   }

   private void clickSourceToButton() {
      LocalPlayer player = this.player();
      if (player != null
         && player.containerMenu == player.inventoryMenu
         && player.inventoryMenu.isValidSlotIndex(this.sourceSlot)
         && !player.inventoryMenu.getSlot(this.sourceSlot).getItem().isEmpty()) {
         this.click(player);
      } else {
         this.swapClickValid = false;
      }
   }

   private void afterSwapClick() {
      if (this.state == InventorySwap.State.EXECUTING_SWAP) {
         if (!this.swapClickValid) {
            this.finish();
         } else {
            if (this.useAndReturn) {
               this.state = InventorySwap.State.USING;
            } else {
               this.state = InventorySwap.State.IDLE;
               this.resetFields();
            }
         }
      }
   }

   private void afterUse() {
      if (this.directUse) {
         this.state = InventorySwap.State.IDLE;
         this.resetFields();
      } else {
         this.state = InventorySwap.State.EXECUTING_RETURN;
         this.returnExecutor
            .execute(this::clickSourceToButton, (this.fastTiming ? SwapSettings.fast() : SwapSettings.legit()).closeInventory(false), this::afterReturnClick);
      }
   }

   private void afterReturnClick() {
      if (this.state == InventorySwap.State.EXECUTING_RETURN) {
         if (!this.swapClickValid) {
            this.finish();
         } else {
            this.state = InventorySwap.State.IDLE;
            this.resetFields();
         }
      }
   }

   private void click(LocalPlayer player) {
      mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, this.sourceSlot, this.hotbarButton, this.clickInput, player);
   }

   private void finish() {
      this.releaseSyntheticUse();
      this.swapExecutor.cancel();
      this.returnExecutor.cancel();
      this.state = InventorySwap.State.IDLE;
      this.resetFields();
   }

   private void resetFields() {
      this.sourceSlot = -1;
      this.hotbarButton = 40;
      this.useAndReturn = false;
      this.waitForUseCompletion = false;
      this.immediateReturn = false;
      this.directUse = false;
      this.fastTiming = false;
      this.clickInput = ContainerInput.SWAP;
      this.useWaitTicks = 0;
      this.swapClickValid = true;
   }

   private void releaseSyntheticUse() {
      if (this.syntheticUseHeld) {
         mc.options.keyUse.setDown(false);
         this.syntheticUseHeld = false;
      }
   }

   private enum State {
      IDLE,
      EXECUTING_SWAP,
      USING,
      WAITING_FOR_USE,
      EXECUTING_RETURN;

      // $VF: synthetic method
      private static InventorySwap.State[] $values() {
         return new InventorySwap.State[]{IDLE, EXECUTING_SWAP, USING, WAITING_FOR_USE, EXECUTING_RETURN};
      }
   }
}

