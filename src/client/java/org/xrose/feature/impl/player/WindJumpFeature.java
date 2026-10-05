package org.xrose.feature.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BindSetting;
import org.xrose.feature.setting.InputBindSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.impl.LinearConstructor;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.network.PacketUtil;
import org.xrose.utils.text.ChatUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class WindJumpFeature extends Feature {
   private static final float THROW_PITCH = 90.0F;
   private static final long PRE_SWAP_DELAY_MS = 35L;
   private static final long PRE_USE_DELAY_MS = 35L;
   private static final long PRE_RESTORE_DELAY_MS = 35L;
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "Bypass", "Bypass"));
   public final InputBindSetting keySetting = this.register(new InputBindSetting("Key", -1));
   private boolean throwCharge;
   private boolean lastBindDown;
   private long lastUseTime;
   private long actionTimer;
   private int previousSlot = -1;
   private int pendingHotbarSlot = -1;
   private int pendingInventorySlotId = -1;
   private boolean keysOverridden;
   private boolean wasForwardPressed;
   private boolean wasBackPressed;
   private boolean wasLeftPressed;
   private boolean wasRightPressed;
   private boolean wasJumpPressed;
   private boolean wasSprintPressed;
   private WindJumpFeature.ThrowState throwState = WindJumpFeature.ThrowState.IDLE;
   private WindJumpFeature.ThrowMode throwMode = WindJumpFeature.ThrowMode.NONE;

   public WindJumpFeature() {
      super("WindJump", "Wind charge jump helper.", FeatureCategory.PLAYER, -1);
   }

   @Override
   protected void onDisable() {
      this.throwCharge = false;
      this.lastUseTime = 0L;
      this.lastBindDown = false;
      this.restoreMovement();
      AngleConnection.INSTANCE.startReturning();
      this.resetThrowState();
   }

   @EventTarget
   public void onPlayerInput(PlayerInputEvent event) {
      if (this.throwState != WindJumpFeature.ThrowState.IDLE) {
         event.setDirectionalLow(false, false, false, false);
         event.setJump(false);
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null && client.level != null) {
         this.updateBindState();
         if (this.throwState != WindJumpFeature.ThrowState.IDLE) {
            this.maintainMovementStop();
            this.processThrow();
         } else if (this.throwCharge) {
            this.throwCharge = false;
            if (System.currentTimeMillis() - this.lastUseTime >= 200L) {
               if (!player.getCooldowns().isOnCooldown(Items.WIND_CHARGE.getDefaultInstance())) {
                  int hotbarSlot = this.findChargeInHotbar(player);
                  if (hotbarSlot != -1) {
                     this.startThrow(WindJumpFeature.ThrowMode.HOTBAR, hotbarSlot, -1);
                  } else {
                     int inventorySlot = this.findChargeInInventory(player);
                     if (inventorySlot != -1) {
                        this.startThrow(WindJumpFeature.ThrowMode.INVENTORY, -1, this.wrapSlot(inventorySlot));
                     } else {
                        ChatUtil.error("Wind charge not found");
                     }
                  }
               }
            }
         }
      } else {
         this.resetThrowState();
      }
   }

   private void updateBindState() {
      Minecraft client = Minecraft.getInstance();
      if (client.getWindow() != null && client.gui.screen() == null && !MenuOverlay.isOpen()) {
         boolean bindDown = this.keySetting.isBound() && (this.bindHeldByKeyboard(client) || this.bindHeldByMouse(client));
         if (bindDown && !this.lastBindDown) {
            this.throwCharge = true;
         }

         this.lastBindDown = bindDown;
      } else {
         this.lastBindDown = false;
      }
   }

   private boolean bindHeldByKeyboard(Minecraft client) {
      return !BindSetting.isKeyboard(this.keySetting.getValue()) ? false : GLFW.glfwGetKey(client.getWindow().handle(), this.keySetting.getValue()) == 1;
   }

   private boolean bindHeldByMouse(Minecraft client) {
      return !BindSetting.isMouse(this.keySetting.getValue())
         ? false
         : GLFW.glfwGetMouseButton(client.getWindow().handle(), BindSetting.rawButton(this.keySetting.getValue())) == 1;
   }

   private void startThrow(WindJumpFeature.ThrowMode chargeMode, int hotbarSlot, int inventorySlotId) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null) {
         this.throwMode = chargeMode;
         this.previousSlot = player.getInventory().getSelectedSlot();
         this.pendingHotbarSlot = hotbarSlot;
         this.pendingInventorySlotId = inventorySlotId;
         this.wasForwardPressed = client.options.keyUp.isDown();
         this.wasBackPressed = client.options.keyDown.isDown();
         this.wasLeftPressed = client.options.keyLeft.isDown();
         this.wasRightPressed = client.options.keyRight.isDown();
         this.wasJumpPressed = client.options.keyJump.isDown();
         this.wasSprintPressed = client.options.keySprint.isDown();
         this.keysOverridden = true;
         this.maintainMovementStop();
         if (player.isSprinting()) {
            player.setSprinting(false);
         }

         this.throwState = WindJumpFeature.ThrowState.WAIT_BEFORE_SWAP;
         this.actionTimer = System.currentTimeMillis();
      }
   }

   private int findChargeInHotbar(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getItem(i).is(Items.WIND_CHARGE)) {
            return i;
         }
      }

      return -1;
   }

   private int findChargeInInventory(LocalPlayer player) {
      for (int i = 9; i < 36; i++) {
         if (player.getInventory().getItem(i).is(Items.WIND_CHARGE)) {
            return i;
         }
      }

      return -1;
   }

   private int wrapSlot(int slot) {
      return slot;
   }

   private void processThrow() {
      switch (this.throwState) {
         case WAIT_BEFORE_SWAP:
            if (System.currentTimeMillis() - this.actionTimer < 35L) {
               return;
            }

            switch (this.throwMode) {
               case INVENTORY:
                  this.startInventoryChargeSwap();
               case HOTBAR:
                  this.throwState = WindJumpFeature.ThrowState.WAIT_BEFORE_USE;
                  this.actionTimer = System.currentTimeMillis();
                  return;
               default:
                  this.resetThrowState();
                  return;
            }
         case WAIT_BEFORE_USE:
            this.rotateDown();
            if (System.currentTimeMillis() - this.actionTimer < 35L) {
               return;
            }

            switch (this.throwMode) {
               case HOTBAR:
                  this.startHotbarChargeUse();
                  break;
               case INVENTORY:
                  this.useInventoryCharge();
                  break;
               default:
                  this.resetThrowState();
                  return;
            }

            this.lastUseTime = System.currentTimeMillis();
            this.throwState = WindJumpFeature.ThrowState.WAIT_BEFORE_RESTORE;
            this.actionTimer = System.currentTimeMillis();
            break;
         case WAIT_BEFORE_RESTORE:
            if (System.currentTimeMillis() - this.actionTimer < 35L) {
               return;
            }

            switch (this.throwMode) {
               case HOTBAR:
                  this.finishHotbarChargeUse();
                  break;
               case INVENTORY:
                  this.finishInventoryChargeUse();
            }

            AngleConnection.INSTANCE.startReturning();
            this.restoreMovement();
            this.resetThrowState();
      }
   }

   private void rotateDown() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         Angle throwAngle = new Angle(client.player.getYRot(), 90.0F);
         AngleConnection.INSTANCE.rotateTo(throwAngle, 3, new AngleConfig(new LinearConstructor(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);
      }
   }

   private void startHotbarChargeUse() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null && client.gameMode != null) {
         this.maintainMovementStop();
         if (this.pendingHotbarSlot != this.previousSlot) {
            this.syncSelectedHotbarSlot(player, this.pendingHotbarSlot);
         }

         PacketUtil.sendUseItem(InteractionHand.MAIN_HAND, player.getYRot(), 90.0F);
         player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private void finishHotbarChargeUse() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null) {
         this.maintainMovementStop();
         if (this.pendingHotbarSlot != this.previousSlot && this.previousSlot != -1) {
            this.syncSelectedHotbarSlot(player, this.previousSlot);
         }
      }
   }

   private void startInventoryChargeSwap() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null && client.gameMode != null && this.pendingInventorySlotId != -1 && this.previousSlot != -1) {
         this.maintainMovementStop();
         client.gameMode.handleContainerInput(player.inventoryMenu.containerId, this.pendingInventorySlotId, this.previousSlot, ContainerInput.SWAP, player);
      }
   }

   private void useInventoryCharge() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null && client.gameMode != null) {
         this.maintainMovementStop();
         PacketUtil.sendUseItem(InteractionHand.MAIN_HAND, player.getYRot(), 90.0F);
         player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private void finishInventoryChargeUse() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null && client.gameMode != null && this.pendingInventorySlotId != -1 && this.previousSlot != -1) {
         this.maintainMovementStop();
         client.gameMode.handleContainerInput(player.inventoryMenu.containerId, this.pendingInventorySlotId, this.previousSlot, ContainerInput.SWAP, player);
         this.syncSelectedHotbarSlot(player, this.previousSlot);
      }
   }

   private void syncSelectedHotbarSlot(LocalPlayer player, int slot) {
      if (player.getInventory().getSelectedSlot() != slot) {
         player.getInventory().setSelectedSlot(slot);
      }

      player.connection.send(new ServerboundSetCarriedItemPacket(slot));
   }

   private void maintainMovementStop() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         client.options.keyUp.setDown(false);
         client.options.keyDown.setDown(false);
         client.options.keyLeft.setDown(false);
         client.options.keyRight.setDown(false);
         client.options.keyJump.setDown(false);
         client.options.keySprint.setDown(false);
         if (client.player.isSprinting()) {
            client.player.setSprinting(false);
         }
      }
   }

   private void restoreMovement() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         if (this.keysOverridden) {
            client.options.keyUp.setDown(this.wasForwardPressed);
            client.options.keyDown.setDown(this.wasBackPressed);
            client.options.keyLeft.setDown(this.wasLeftPressed);
            client.options.keyRight.setDown(this.wasRightPressed);
            client.options.keyJump.setDown(this.wasJumpPressed);
            client.options.keySprint.setDown(this.wasSprintPressed);
            this.keysOverridden = false;
         }
      }
   }

   private void resetThrowState() {
      this.throwState = WindJumpFeature.ThrowState.IDLE;
      this.throwMode = WindJumpFeature.ThrowMode.NONE;
      this.actionTimer = 0L;
      this.previousSlot = -1;
      this.pendingHotbarSlot = -1;
      this.pendingInventorySlotId = -1;
      this.keysOverridden = false;
   }

   private enum ThrowMode {
      NONE,
      HOTBAR,
      INVENTORY;

      // $VF: synthetic method
      private static WindJumpFeature.ThrowMode[] $values() {
         return new WindJumpFeature.ThrowMode[]{NONE, HOTBAR, INVENTORY};
      }
   }

   private enum ThrowState {
      IDLE,
      WAIT_BEFORE_SWAP,
      WAIT_BEFORE_USE,
      WAIT_BEFORE_RESTORE;

      // $VF: synthetic method
      private static WindJumpFeature.ThrowState[] $values() {
         return new WindJumpFeature.ThrowState[]{IDLE, WAIT_BEFORE_SWAP, WAIT_BEFORE_USE, WAIT_BEFORE_RESTORE};
      }
   }
}

