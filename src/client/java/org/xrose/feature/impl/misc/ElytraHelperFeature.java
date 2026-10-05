package org.xrose.feature.impl.misc;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import org.lwjgl.glfw.GLFW;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BindSetting;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.InputBindSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.utils.combat.StopWatch;
import org.xrose.utils.inventory.InventoryFlowManager;
import org.xrose.utils.inventory.InventoryTask;
import org.xrose.utils.inventory.swap.MovementController;
import org.xrose.utils.inventory.swap.SwapSettings;
import org.xrose.utils.network.PacketUtil;
import org.xrose.utils.text.ChatUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ElytraHelperFeature extends Feature {
   private static final int HOTBAR_SELECT_DELAY_TICKS = 0;
   private static final int HOTBAR_RESTORE_DELAY_TICKS = 0;
   private static final int INVENTORY_SELECT_DELAY_TICKS = 0;
   private static final int INVENTORY_RESTORE_DELAY_TICKS = 0;
   private static final int CHEST_MENU_SLOT = 6;
   private static final List<Item> CHESTPLATES = List.of(
      Items.NETHERITE_CHESTPLATE,
      Items.DIAMOND_CHESTPLATE,
      Items.CHAINMAIL_CHESTPLATE,
      Items.IRON_CHESTPLATE,
      Items.GOLDEN_CHESTPLATE,
      Items.LEATHER_CHESTPLATE
   );
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Aggressive", "Aggressive", "Legit", "HvH"));
   private final InputBindSetting swapBind = this.register(new InputBindSetting("Swap Bind", -1));
   private final InputBindSetting fireworkBind = this.register(new InputBindSetting("Firework Bind", -1));
   private final BooleanSetting autoTakeoff = this.register(new BooleanSetting("Auto Takeoff", false));
   private final BooleanSetting autoSprint = this.register(new BooleanSetting("Auto Sprint", false));
   private final BooleanSetting autoFirework = this.register(new BooleanSetting("Auto Firework", false));
   private final StopWatch autoFireworkTimer = new StopWatch();
   private final StopWatch fireworkUseTimer = new StopWatch();
   private final StopWatch fireworkCooldown = new StopWatch();
   private boolean autoFireworkArmed;
   private boolean lastSwapPressed;
   private boolean lastFireworkPressed;
   private ElytraHelperFeature.FireworkPhase fireworkPhase = ElytraHelperFeature.FireworkPhase.IDLE;
   private int fireworkPhaseTicks;
   private int previousHotbarSlot = -1;
   private int restoreInventorySlot = -1;
   private final MovementController swapMovement = new MovementController();
   private final MovementController fireworkMovement = new MovementController();
   private ElytraHelperFeature.SwapPhase swapPhase = ElytraHelperFeature.SwapPhase.IDLE;
   private ElytraHelperFeature.FireworkUsePhase fireworkUsePhase = ElytraHelperFeature.FireworkUsePhase.IDLE;
   private int armorSlot = -1;
   private int fireworkSlot = -1;
   private int savedFireworkSlot = -1;
   private boolean fireworkFromInventory = false;
   private long swapPhaseStartTime;
   private long fireworkPhaseStartTime;
   private int swapCurrentDelay;
   private int fireworkCurrentDelay;
   private boolean shouldJumpForTakeoff;
   private boolean wasFallFlying;

   public ElytraHelperFeature() {
      super("Elytra Helper", "Elytra swap and firework with Aggressive/Legit mode.", FeatureCategory.MISC, -1);
      this.autoSprint.visibleWhen(this.autoTakeoff::getValue);
      this.autoFirework.visibleWhen(this.autoTakeoff::getValue);
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && event.getClient().level != null) {
         if (this.mode.is("Aggressive")) {
            this.tickAggressive();
         } else if (this.mode.is("HvH")) {
            this.tickHvH();
         } else {
            this.tickLegit();
         }
      } else {
         this.resetState();
      }
   }

   private void tickAggressive() {
      this.updateFireworkUse();
      boolean swapPressed = this.isBindHeld(this.swapBind);
      boolean fireworkPressed = this.isBindHeld(this.fireworkBind);
      if (Minecraft.getInstance().gui.screen() != null) {
         this.lastSwapPressed = swapPressed;
         this.lastFireworkPressed = fireworkPressed;
      } else {
         if (!this.lastSwapPressed && swapPressed && !this.isBusy()) {
            this.startArmorSwap();
         }

         if (!this.lastFireworkPressed && fireworkPressed && !this.isBusy()) {
            this.useFirework();
         }

         this.armAutoFireworkOnTakeoff();
         this.processAutoFirework();
         this.lastSwapPressed = swapPressed;
         this.lastFireworkPressed = fireworkPressed;
      }
   }

   private void tickLegit() {
      this.processArmorSwapLoop();
      this.processFireworkUseLoop();
      this.processLegitAutoTakeoff();
      this.processLegitAutoFirework();
      boolean swapPressed = this.isBindHeld(this.swapBind);
      boolean fireworkPressed = this.isBindHeld(this.fireworkBind);
      if (Minecraft.getInstance().gui.screen() != null) {
         this.lastSwapPressed = swapPressed;
         this.lastFireworkPressed = fireworkPressed;
      } else {
         if (!this.lastSwapPressed && swapPressed && !this.isBusy()) {
            this.startArmorSwapLegit();
         }

         if (!this.lastFireworkPressed && fireworkPressed && !this.isBusy()) {
            this.useFireworkLegit();
         }

         this.lastSwapPressed = swapPressed;
         this.lastFireworkPressed = fireworkPressed;
      }
   }

   private void tickHvH() {
      this.processArmorSwapLoop();
      this.updateFireworkUse();
      this.processLegitAutoTakeoff();
      this.processAutoFirework();
      boolean swapPressed = this.isBindHeld(this.swapBind);
      boolean fireworkPressed = this.isBindHeld(this.fireworkBind);
      if (Minecraft.getInstance().gui.screen() != null) {
         this.lastSwapPressed = swapPressed;
         this.lastFireworkPressed = fireworkPressed;
      } else {
         if (!this.lastSwapPressed && swapPressed && !this.isBusy()) {
            this.startArmorSwapLegit();
         }

         if (!this.lastFireworkPressed && fireworkPressed && !this.isBusy()) {
            this.useFirework();
         }

         this.armAutoFireworkOnTakeoff();
         this.lastSwapPressed = swapPressed;
         this.lastFireworkPressed = fireworkPressed;
      }
   }

   @EventTarget
   public void onInput(PlayerInputEvent event) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         if (this.mode.is("Aggressive")) {
            if (!this.autoTakeoff.getValue() || !this.isElytraEquipped() || !this.isElytraUsable()) {
               return;
            }

            if (player.onGround()) {
               event.setJump(true);
            } else if (!player.isFallFlying()) {
               if (this.autoSprint.getValue() && this.canSprintForTakeoff()) {
                  player.setSprinting(true);
               }

               event.setJump(true);
            }
         } else {
            if (this.swapMovement.isBlocked()) {
               event.setDirectionalLow(false, false, false, false);
               event.setJump(false);
            }

            if (this.fireworkMovement.isBlocked()) {
               event.setDirectionalLow(false, false, false, false);
               event.setJump(false);
            }

            if (this.shouldJumpForTakeoff) {
               if (this.autoSprint.getValue() && this.canSprintForTakeoff()) {
                  player.setSprinting(true);
               }

               event.setJump(true);
            }
         }
      }
   }

   private void startArmorSwap() {
      int slot = this.findChestSwapSlot();
      boolean swappingToElytra = !this.isElytraEquipped();
      if (slot == -1) {
         ChatUtil.info(swappingToElytra ? "No elytra found." : "No chestplate found.");
      } else {
         this.moveItem(slot, 6);
         ChatUtil.info(swappingToElytra ? "Swapped to elytra." : "Swapped to chestplate.");
         if (swappingToElytra) {
            this.armAutoFirework();
         } else {
            this.autoFireworkArmed = false;
         }
      }
   }

   private int findChestSwapSlot() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return -1;
      } else {
         return this.isElytraEquipped()
            ? this.findFirstMenuSlot(stack -> CHESTPLATES.contains(stack.getItem()))
            : this.findFirstMenuSlot(stack -> stack.is(Items.ELYTRA));
      }
   }

   private int findFirstMenuSlot(Predicate<ItemStack> predicate) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return -1;
      }

      for (int slot = 36; slot < 45; slot++) {
         if (predicate.test(player.inventoryMenu.getSlot(slot).getItem())) {
            return slot;
         }
      }

      for (int slot = 9; slot < 36; slot++) {
         if (predicate.test(player.inventoryMenu.getSlot(slot).getItem())) {
            return slot;
         }
      }

      return -1;
   }

   private void useFirework() {
      if (this.mode.is("HvH")) {
         this.useFireworkHvH();
      } else {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null && this.isElytraEquipped()) {
            if (this.fireworkUseTimer.finished(1.0)) {
               if (!player.getCooldowns().isOnCooldown(new ItemStack(Items.FIREWORK_ROCKET))) {
                  this.previousHotbarSlot = player.getInventory().getSelectedSlot();
                  this.restoreInventorySlot = -1;
                  if (player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
                     this.startFireworkPhase(ElytraHelperFeature.FireworkPhase.USE, 0);
                  } else {
                     int hotbarSlot = this.findHotbarSlot(Items.FIREWORK_ROCKET);
                     if (hotbarSlot != -1) {
                        this.syncSelectedHotbarSlot(hotbarSlot);
                        this.startFireworkPhase(ElytraHelperFeature.FireworkPhase.WAIT_SELECTED, 0);
                     } else {
                        int inventorySlot = this.findInventorySlot(Items.FIREWORK_ROCKET);
                        if (inventorySlot != -1) {
                           this.restoreInventorySlot = inventorySlot;
                           this.queueFireworkSwap(inventorySlot, this.previousHotbarSlot);
                           this.startFireworkPhase(ElytraHelperFeature.FireworkPhase.WAIT_SWAP, 0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void useFireworkHvH() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && this.isElytraEquipped()) {
         if (this.fireworkUseTimer.finished(1.0)) {
            if (!player.getCooldowns().isOnCooldown(new ItemStack(Items.FIREWORK_ROCKET))) {
               this.silentUseItem(Items.FIREWORK_ROCKET);
               this.fireworkUseTimer.reset();
            }
         }
      }
   }

   private void updateFireworkUse() {
      if (this.fireworkPhase != ElytraHelperFeature.FireworkPhase.IDLE) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null) {
            this.resetFireworkUse();
         } else if (this.fireworkPhaseTicks > 0) {
            this.fireworkPhaseTicks--;
         } else {
            switch (this.fireworkPhase) {
               case WAIT_SWAP:
                  if (!InventoryFlowManager.isIdle()) {
                     return;
                  }

                  this.startFireworkPhase(ElytraHelperFeature.FireworkPhase.WAIT_SELECTED, 0);
                  break;
               case WAIT_SELECTED:
                  this.startFireworkPhase(ElytraHelperFeature.FireworkPhase.USE, 0);
                  break;
               case USE:
                  if (mc.player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
                     PacketUtil.sendUseItem(InteractionHand.MAIN_HAND);
                     mc.player.swing(InteractionHand.MAIN_HAND);
                     this.fireworkUseTimer.reset();
                  }

                  this.startFireworkPhase(ElytraHelperFeature.FireworkPhase.RESTORE, this.restoreInventorySlot != -1 ? 0 : 0);
                  break;
               case RESTORE:
                  if (this.restoreInventorySlot != -1) {
                     this.queueFireworkSwap(this.restoreInventorySlot, this.previousHotbarSlot);
                     this.startFireworkPhase(ElytraHelperFeature.FireworkPhase.WAIT_RESTORE, 0);
                  } else if (this.previousHotbarSlot >= 0 && this.previousHotbarSlot <= 8) {
                     this.syncSelectedHotbarSlot(this.previousHotbarSlot);
                     this.resetFireworkUse();
                  } else {
                     this.resetFireworkUse();
                  }
                  break;
               case WAIT_RESTORE:
                  if (!InventoryFlowManager.isIdle()) {
                     return;
                  }

                  this.resetFireworkUse();
                  break;
               default:
                  this.resetFireworkUse();
            }
         }
      }
   }

   private void startFireworkPhase(ElytraHelperFeature.FireworkPhase phase, int ticks) {
      this.fireworkPhase = phase;
      this.fireworkPhaseTicks = Math.max(0, ticks);
   }

   private void queueFireworkSwap(int inventorySlot, int hotbarSlot) {
      InventoryFlowManager.addTask(() -> this.clickSwap(inventorySlot, hotbarSlot));
   }

   private void resetFireworkUse() {
      this.fireworkPhase = ElytraHelperFeature.FireworkPhase.IDLE;
      this.fireworkPhaseTicks = 0;
      this.previousHotbarSlot = -1;
      this.restoreInventorySlot = -1;
   }

   private void processAutoFirework() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         if (!this.autoTakeoff.getValue() || !this.autoFirework.getValue()) {
            this.autoFireworkArmed = false;
         } else if (this.autoFireworkArmed && Minecraft.getInstance().gui.screen() == null && !this.isBusy()) {
            if (this.isElytraEquipped() && player.isFallFlying() && this.autoFireworkTimer.finished(75.0)) {
               this.useFirework();
               this.autoFireworkArmed = false;
               this.autoFireworkTimer.reset();
            }
         }
      }
   }

   private void armAutoFirework() {
      this.autoFireworkArmed = this.autoTakeoff.getValue() && this.autoFirework.getValue();
      this.autoFireworkTimer.reset();
   }

   private void armAutoFireworkOnTakeoff() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.wasFallFlying = false;
      } else {
         boolean flying = player.isFallFlying();
         if (flying && !this.wasFallFlying && this.autoTakeoff.getValue() && this.autoFirework.getValue()) {
            this.armAutoFirework();
         }

         this.wasFallFlying = flying;
      }
   }

   private boolean canSprintForTakeoff() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player == null
         ? false
         : !player.horizontalCollision && !player.hasEffect(MobEffects.BLINDNESS) && !player.isUsingItem() && !player.isInWater() && !player.isUnderWater();
   }

   private boolean isBusy() {
      if (this.mode.is("Aggressive")) {
         return !InventoryFlowManager.script.isFinished()
            || !InventoryFlowManager.postScript.isFinished()
            || !InventoryTask.isSwapAndUseIdle()
            || this.fireworkPhase != ElytraHelperFeature.FireworkPhase.IDLE;
      } else {
         return this.mode.is("HvH")
            ? this.swapPhase != ElytraHelperFeature.SwapPhase.IDLE
               || this.fireworkPhase != ElytraHelperFeature.FireworkPhase.IDLE
               || !InventoryFlowManager.script.isFinished()
               || !InventoryFlowManager.postScript.isFinished()
               || !InventoryTask.isSwapAndUseIdle()
            : this.swapPhase != ElytraHelperFeature.SwapPhase.IDLE || this.fireworkUsePhase != ElytraHelperFeature.FireworkUsePhase.IDLE;
      }
   }

   private boolean isElytraEquipped() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
   }

   private boolean isElytraUsable() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return false;
      } else {
         ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
         if (stack.getItem() != Items.ELYTRA) {
            return false;
         } else {
            return stack.getMaxDamage() <= 0 ? true : stack.getDamageValue() < stack.getMaxDamage() - 1;
         }
      }
   }

   private SwapSettings buildSettings() {
      return SwapSettings.legit();
   }

   private void startArmorSwapLegit() {
      boolean elytraEquipped = this.isElytraEquipped();
      int slot = this.findChestArmorSlot(elytraEquipped ? null : Items.ELYTRA);
      if (slot == -1) {
         ChatUtil.info(elytraEquipped ? "No chestplate found." : "No elytra found.");
      } else {
         this.armorSlot = slot;
         ChatUtil.info(elytraEquipped ? "Swapped to chestplate." : "Swapped to elytra.");
         SwapSettings settings = this.buildSettings();
         if (settings.shouldStopMovement()) {
            this.startSwapPhase(ElytraHelperFeature.SwapPhase.PRE_STOP, settings.randomPreStopDelay());
         } else {
            this.startSwapPhase(ElytraHelperFeature.SwapPhase.SWAP_ARMOR, 0);
         }
      }
   }

   private int findChestArmorSlot(Item targetItem) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return -1;
      }

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            Equippable component = (Equippable)stack.get(DataComponents.EQUIPPABLE);
            if (component != null && component.slot() == EquipmentSlot.CHEST) {
               if (targetItem == null) {
                  if (stack.getItem() != Items.ELYTRA) {
                     return i;
                  }
               } else if (stack.getItem() == targetItem) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   private void processArmorSwapLoop() {
      if (this.swapPhase != ElytraHelperFeature.SwapPhase.IDLE) {
         boolean continueProcessing = true;

         for (int iterations = 0; continueProcessing && iterations < 10; continueProcessing = this.processArmorSwapTick()) {
            iterations++;
         }
      }
   }

   private boolean processArmorSwapTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.resetSwapState();
         return false;
      }

      long elapsed = System.currentTimeMillis() - this.swapPhaseStartTime;
      SwapSettings settings = this.buildSettings();
      switch (this.swapPhase) {
         case PRE_STOP:
            if (elapsed >= this.swapCurrentDelay) {
               this.swapMovement.saveState();
               this.swapMovement.block();
               if (settings.shouldStopSprint()) {
                  this.swapMovement.stopSprint();
               }

               this.startSwapPhase(ElytraHelperFeature.SwapPhase.STOPPING, 0);
               return true;
            }
            break;
         case STOPPING:
            this.swapMovement.block();
            if (settings.shouldStopSprint()) {
               this.swapMovement.stopSprint();
            }

            this.startSwapPhase(ElytraHelperFeature.SwapPhase.WAIT_STOP, settings.randomWaitStopDelay());
            return this.swapCurrentDelay == 0;
         case WAIT_STOP:
            this.swapMovement.block();
            boolean stopped = this.swapMovement.isPlayerStopped(settings.getVelocityThreshold());
            boolean timeout = elapsed >= this.swapCurrentDelay;
            if (!stopped && !timeout) {
               break;
            }

            this.startSwapPhase(ElytraHelperFeature.SwapPhase.PRE_SWAP, settings.randomPreSwapDelay());
            return this.swapCurrentDelay == 0;
         case PRE_SWAP:
            this.swapMovement.block();
            if (elapsed >= this.swapCurrentDelay) {
               this.startSwapPhase(ElytraHelperFeature.SwapPhase.SWAP_ARMOR, 0);
               return true;
            }
            break;
         case SWAP_ARMOR:
            int fromSlot = this.wrapSlot(this.armorSlot);
            this.swapPickup(fromSlot, 6);
            this.startSwapPhase(ElytraHelperFeature.SwapPhase.POST_SWAP, settings.randomPostSwapDelay());
            return this.swapCurrentDelay == 0;
         case POST_SWAP:
            if (elapsed >= this.swapCurrentDelay) {
               if (settings.shouldCloseInventory()) {
                  this.closeScreen();
               }

               this.startSwapPhase(ElytraHelperFeature.SwapPhase.RESUMING, settings.randomResumeDelay());
               return this.swapCurrentDelay == 0;
            }
            break;
         case RESUMING:
            if (elapsed >= this.swapCurrentDelay) {
               if (this.buildSettings().shouldStopMovement()) {
                  this.swapMovement.restoreFromCurrent();
               }

               this.resetSwapState();
               return false;
            }
      }

      return false;
   }

   private void useFireworkLegit() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && this.isElytraEquipped()) {
         this.savedFireworkSlot = player.getInventory().getSelectedSlot();
         int hotbarSlot = this.findHotbarSlot(Items.FIREWORK_ROCKET);
         if (hotbarSlot != -1) {
            this.fireworkSlot = hotbarSlot;
            this.fireworkFromInventory = false;
            this.selectSlot(this.fireworkSlot);
            this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.AWAIT_ITEM, 0);
         } else {
            int invSlot = this.findInventorySlot(Items.FIREWORK_ROCKET);
            if (invSlot != -1) {
               this.fireworkSlot = invSlot;
               this.fireworkFromInventory = true;
               SwapSettings settings = this.buildSettings();
               if (settings.shouldStopMovement()) {
                  this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.PRE_STOP, settings.randomPreStopDelay());
               } else {
                  this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.SWAP_TO_HAND, 0);
               }
            } else {
               ChatUtil.info("No fireworks found.");
            }
         }
      }
   }

   private void processFireworkUseLoop() {
      if (this.fireworkUsePhase != ElytraHelperFeature.FireworkUsePhase.IDLE) {
         boolean continueProcessing = true;

         for (int iterations = 0; continueProcessing && iterations < 10; continueProcessing = this.processFireworkUseTick()) {
            iterations++;
         }
      }
   }

   private boolean processFireworkUseTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.resetFireworkState();
         return false;
      }

      long elapsed = System.currentTimeMillis() - this.fireworkPhaseStartTime;
      SwapSettings settings = this.buildSettings();
      switch (this.fireworkUsePhase) {
         case PRE_STOP:
            if (elapsed >= this.fireworkCurrentDelay) {
               this.fireworkMovement.saveState();
               this.fireworkMovement.block();
               if (settings.shouldStopSprint()) {
                  this.fireworkMovement.stopSprint();
               }

               this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.STOPPING, 0);
               return true;
            }
            break;
         case STOPPING:
            this.fireworkMovement.block();
            if (settings.shouldStopSprint()) {
               this.fireworkMovement.stopSprint();
            }

            this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.WAIT_STOP, settings.randomWaitStopDelay());
            return this.fireworkCurrentDelay == 0;
         case WAIT_STOP:
            this.fireworkMovement.block();
            boolean stopped = this.fireworkMovement.isPlayerStopped(settings.getVelocityThreshold());
            boolean timeout = elapsed >= this.fireworkCurrentDelay;
            if (!stopped && !timeout) {
               break;
            }

            this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.PRE_SWAP, settings.randomPreSwapDelay());
            return this.fireworkCurrentDelay == 0;
         case PRE_SWAP:
            this.fireworkMovement.block();
            if (elapsed >= this.fireworkCurrentDelay) {
               this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.SWAP_TO_HAND, 0);
               return true;
            }
            break;
         case SWAP_TO_HAND:
            int hotbarSlot = player.getInventory().getSelectedSlot();
            this.clickSwap(this.fireworkSlot, hotbarSlot);
            this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.AWAIT_ITEM, 0);
            return true;
         case AWAIT_ITEM:
            if (player.getMainHandItem().getItem() == Items.FIREWORK_ROCKET) {
               this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.USE, 0);
               return true;
            }
            break;
         case USE:
            Minecraft mc = Minecraft.getInstance();
            PacketUtil.sendUseItem(InteractionHand.MAIN_HAND);
            player.swing(InteractionHand.MAIN_HAND);
            this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.POST_USE, settings.randomPostSwapDelay());
            return this.fireworkCurrentDelay == 0;
         case POST_USE:
            if (elapsed >= this.fireworkCurrentDelay) {
               if (this.fireworkFromInventory) {
                  this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.SWAP_BACK, 0);
                  return true;
               }

               this.selectSlot(this.savedFireworkSlot);
               this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.RESUMING, settings.randomResumeDelay());
               return this.fireworkCurrentDelay == 0;
            }
            break;
         case SWAP_BACK:
            hotbarSlot = player.getInventory().getSelectedSlot();
            this.clickSwap(this.fireworkSlot, hotbarSlot);
            this.selectSlot(this.savedFireworkSlot);
            if (settings.shouldCloseInventory()) {
               this.closeScreen();
            }

            this.startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase.RESUMING, settings.randomResumeDelay());
            return this.fireworkCurrentDelay == 0;
         case RESUMING:
            if (elapsed >= this.fireworkCurrentDelay) {
               if (this.buildSettings().shouldStopMovement()) {
                  this.fireworkMovement.restoreFromCurrent();
               }

               this.resetFireworkState();
               return false;
            }
      }

      return false;
   }

   private int findHotbarSlot(Item item) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }

   private int findInventorySlot(Item item) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return -1;
      }

      for (int i = 9; i < 36; i++) {
         if (player.getInventory().getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }

   private void processLegitAutoTakeoff() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.shouldJumpForTakeoff = false;
      } else if (!this.autoTakeoff.getValue()) {
         this.shouldJumpForTakeoff = false;
      } else if (this.swapPhase != ElytraHelperFeature.SwapPhase.IDLE) {
         this.shouldJumpForTakeoff = false;
      } else if (this.fireworkUsePhase != ElytraHelperFeature.FireworkUsePhase.IDLE) {
         this.shouldJumpForTakeoff = false;
      } else if (!this.isElytraEquipped()) {
         this.shouldJumpForTakeoff = false;
      } else {
         if (player.onGround()) {
            this.shouldJumpForTakeoff = true;
         } else {
            this.shouldJumpForTakeoff = false;
            if (this.isElytraUsable() && !player.isFallFlying() && !player.getAbilities().flying) {
               player.connection.send(new ServerboundPlayerCommandPacket(player, Action.START_FALL_FLYING));
            }
         }
      }
   }

   private void processLegitAutoFirework() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (this.autoTakeoff.getValue() && this.autoFirework.getValue() && player != null) {
         if (this.isElytraEquipped() && player.isFallFlying()) {
            if (this.fireworkUsePhase == ElytraHelperFeature.FireworkUsePhase.IDLE && !player.isUsingItem()) {
               if (this.autoFireworkTimer.finished(1000.0)) {
                  this.useFireworkLegit();
                  this.autoFireworkTimer.reset();
               }
            }
         }
      }
   }

   private void startSwapPhase(ElytraHelperFeature.SwapPhase phase, int delay) {
      this.swapPhase = phase;
      this.swapPhaseStartTime = System.currentTimeMillis();
      this.swapCurrentDelay = delay;
   }

   private void startFireworkUsePhase(ElytraHelperFeature.FireworkUsePhase phase, int delay) {
      this.fireworkUsePhase = phase;
      this.fireworkPhaseStartTime = System.currentTimeMillis();
      this.fireworkCurrentDelay = delay;
   }

   private void resetSwapState() {
      this.swapMovement.reset();
      this.swapPhase = ElytraHelperFeature.SwapPhase.IDLE;
      this.armorSlot = -1;
      this.swapPhaseStartTime = 0L;
      this.swapCurrentDelay = 0;
   }

   private void resetFireworkState() {
      this.fireworkMovement.reset();
      this.fireworkUsePhase = ElytraHelperFeature.FireworkUsePhase.IDLE;
      this.fireworkSlot = -1;
      this.savedFireworkSlot = -1;
      this.fireworkFromInventory = false;
      this.fireworkPhaseStartTime = 0L;
      this.fireworkCurrentDelay = 0;
   }

   private void resetLegitState() {
      this.resetSwapState();
      this.resetFireworkState();
      this.shouldJumpForTakeoff = false;
   }

   private void resetState() {
      this.autoFireworkArmed = false;
      this.wasFallFlying = false;
      this.lastSwapPressed = false;
      this.lastFireworkPressed = false;
      this.autoFireworkTimer.reset();
      this.fireworkUseTimer.reset();
      this.resetFireworkUse();
      this.resetLegitState();
   }

   private boolean isBindHeld(InputBindSetting bind) {
      if (bind != null && bind.isBound() && !MenuOverlay.isOpen()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getWindow() == null) {
            return false;
         }

         long window = mc.getWindow().handle();
         int code = bind.getValue();
         return BindSetting.isMouse(code) ? GLFW.glfwGetMouseButton(window, BindSetting.rawButton(code)) == 1 : InputConstants.isKeyDown(mc.getWindow(), code);
      } else {
         return false;
      }
   }

   private void syncSelectedHotbarSlot(int slot) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && slot >= 0 && slot <= 8) {
         if (player.getInventory().getSelectedSlot() != slot) {
            player.getInventory().setSelectedSlot(slot);
         }
      }
   }

   private void selectSlot(int slot) {
      this.syncSelectedHotbarSlot(slot);
   }

   private void silentUseItem(Item item) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && Minecraft.getInstance().getConnection() != null) {
         int hotbarSlot = this.findHotbarSlot(item);
         if (hotbarSlot != -1) {
            int currentSlot = player.getInventory().getSelectedSlot();
            if (hotbarSlot != currentSlot) {
               PacketUtil.sendHeldItemChange(hotbarSlot);
            }

            PacketUtil.sendUseItem(InteractionHand.MAIN_HAND);
            if (hotbarSlot != currentSlot) {
               PacketUtil.sendHeldItemChange(currentSlot);
            }
         } else {
            int invSlot = this.findInventorySlot(item);
            if (invSlot != -1) {
               int currentHotbarSlot = player.getInventory().getSelectedSlot();
               this.clickSwap(invSlot, currentHotbarSlot);
               PacketUtil.sendUseItem(InteractionHand.MAIN_HAND);
               this.clickSwap(invSlot, currentHotbarSlot);
               this.closeScreen();
            }
         }
      }
   }

   private void moveItem(int from, int to) {
      if (from != to && from != -1) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null && Minecraft.getInstance().gameMode != null) {
            int count = player.inventoryMenu.slots.size() - 10;
            if (from >= count && count == 36) {
               this.clickSwap(to, from - count);
            } else {
               this.clickSwap(from, 0);
               this.clickSwap(to, 0);
               this.clickSwap(from, 0);
            }
         }
      }
   }

   private void swapPickup(int from, int to) {
      this.clickPickup(from);
      this.clickPickup(to);
      this.clickPickup(from);
   }

   private void clickSwap(int slot, int button) {
      this.click(slot, button, ContainerInput.SWAP);
   }

   private void clickPickup(int slot) {
      this.click(slot, 0, ContainerInput.PICKUP);
   }

   private void click(int slot, int button, ContainerInput input) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.gameMode != null) {
         mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, button, input, player);
      }
   }

   private int wrapSlot(int slot) {
      return slot < 9 ? slot + 36 : slot;
   }

   private void closeScreen() {
      InventoryTask.closeScreen(true);
   }

   private enum FireworkPhase {
      IDLE,
      WAIT_SWAP,
      WAIT_SELECTED,
      USE,
      RESTORE,
      WAIT_RESTORE;

      // $VF: synthetic method
      private static ElytraHelperFeature.FireworkPhase[] $values() {
         return new ElytraHelperFeature.FireworkPhase[]{IDLE, WAIT_SWAP, WAIT_SELECTED, USE, RESTORE, WAIT_RESTORE};
      }
   }

   private enum FireworkUsePhase {
      IDLE,
      PRE_STOP,
      STOPPING,
      WAIT_STOP,
      PRE_SWAP,
      SWAP_TO_HAND,
      AWAIT_ITEM,
      USE,
      POST_USE,
      SWAP_BACK,
      RESUMING;

      // $VF: synthetic method
      private static ElytraHelperFeature.FireworkUsePhase[] $values() {
         return new ElytraHelperFeature.FireworkUsePhase[]{
            IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAP_TO_HAND, AWAIT_ITEM, USE, POST_USE, SWAP_BACK, RESUMING
         };
      }
   }

   private enum SwapPhase {
      IDLE,
      PRE_STOP,
      STOPPING,
      WAIT_STOP,
      PRE_SWAP,
      SWAP_ARMOR,
      POST_SWAP,
      RESUMING;

      // $VF: synthetic method
      private static ElytraHelperFeature.SwapPhase[] $values() {
         return new ElytraHelperFeature.SwapPhase[]{IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAP_ARMOR, POST_SWAP, RESUMING};
      }
   }
}

