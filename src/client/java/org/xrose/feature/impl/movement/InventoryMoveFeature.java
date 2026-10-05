package org.xrose.feature.impl.movement;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.event.events.screen.ScreenCloseEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.utils.combat.PlayerInteractionHelper;
import org.xrose.utils.inventory.InventoryFlowManager;
import org.xrose.utils.inventory.InventoryTask;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class InventoryMoveFeature extends Feature implements MinecraftContext {
   private static final long DEFAULT_PHASE_DELAY_MS = 0L;
   private static final long FUN_TIME_PHASE_DELAY_MS = 0L;
   private static final long FUN_TIME_CLOSE_DELAY_MS = 0L;
   private static final long FUN_TIME_CLOSE_MAX_DELAY_MS = 0L;
   private static final long FUN_TIME_RESTORE_DELAY_MS = 0L;
   private static final long FUN_TIME_SOFT_CLICK_DELAY_MS = 1L;
   private static final double FUN_TIME_CLOSE_MAX_SPEED_SQ = 1.0E-4;
   private static final int FUN_TIME_PACKETS_PER_TICK = 2;
   private static final int FUN_TIME_CLOSE_PACKETS_PER_TICK = 2;
   private static final long GRIM_STOP_MAX_WAIT_MS = 100L;
   private static final long GRIM_RESTORE_DELAY_MS = 20L;
   private static final double GRIM_STOP_SPEED = 0.03;
   public static boolean ignoreNextPacket = false;
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Normal", "Normal", "FunTime", "Legit"));
   private final List<Packet<?>> packets = new ArrayList<>();
   private InventoryMoveFeature.MovePhase movePhase = InventoryMoveFeature.MovePhase.READY;
   private long actionStartTime = 0L;
   private boolean playerFullyStopped = false;
   private boolean wasForwardPressed;
   private boolean wasBackPressed;
   private boolean wasLeftPressed;
   private boolean wasRightPressed;
   private boolean wasJumpPressed;
   private boolean keysOverridden = false;
   private boolean inventoryOpened = false;
   private boolean packetsHeld = false;
   private boolean closingScreen = false;
   private boolean delayedClosePending = false;
   private boolean bypassCloseEvent = false;
   private boolean softClickSlowdown = false;

   public InventoryMoveFeature() {
      super("Inventory Move", "Allows movement while screens are open.", FeatureCategory.MOVEMENT, -1);
   }

   public static InventoryMoveFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(InventoryMoveFeature.class);
   }

   public static boolean isClickPipelineBusy() {
      InventoryMoveFeature feature = getEnabled();
      return feature != null && (!InventoryFlowManager.isIdle() || feature.movePhase != InventoryMoveFeature.MovePhase.READY || feature.packetsHeld);
   }

   public String getSelectedMode() {
      return this.mode.getValue();
   }

   public boolean shouldPreserveInventoryMovementInput() {
      return getEnabled() != null
         && !this.isClickGuiOpen()
         && this.isInventoryScreenOpen()
         && InventoryFlowManager.isMovementAllowed()
         && !this.shouldFreezeCloseInput()
         && !this.shouldDisableMovementForContainer();
   }

   public boolean shouldSuppressSprintInput() {
      return this.softClickSlowdown && !this.delayedClosePending
         ? false
         : !this.isClickGuiOpen() && (this.closingScreen || this.movePhase != InventoryMoveFeature.MovePhase.READY && !this.shouldForceInventorySprint());
   }

   @Override
   protected void onDisable() {
      InventoryFlowManager.reset();
      this.resetState();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (mc.player != null && mc.level != null) {
         this.processLegitMovement();
      } else {
         this.resetState();
      }
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (event.getPhase() == PacketSendEvent.Phase.PRE) {
         if (event.getPacket() instanceof ServerboundContainerClickPacket) {
            if (ignoreNextPacket) {
               ignoreNextPacket = false;
               return;
            }

            if (this.isFunTimeMode()
               && (this.packetsHeld || this.hasDirectionalMovementInput())
               && InventoryFlowManager.shouldSkipExecution()
               && !this.shouldDisableMovementForContainer()) {
               this.packets.add(event.getPacket());
               event.cancel();
               this.packetsHeld = true;
               this.startSlowdownForInventoryClick((ServerboundContainerClickPacket)event.getPacket());
               return;
            }
         }

         if (event.getPacket() instanceof ServerboundContainerClosePacket
            && this.isFunTimeMode()
            && this.shouldDelayCloseScreen(mc.gui.screen())
            && !this.bypassCloseEvent) {
            event.cancel();
            this.startDelayedClose();
         }
      }
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE) {
         if (event.getPacket() instanceof ClientboundContainerClosePacket && this.shouldIgnoreServerClosePacket()) {
            event.cancel();
         }
      }
   }

   @EventTarget
   public void onScreenClose(ScreenCloseEvent event) {
      if (!this.bypassCloseEvent) {
         this.closingScreen = true;
         this.stopPlayerSprintBeforeClose();
         if (this.isFunTimeMode() && this.delayedClosePending) {
            event.cancel();
         } else if (this.shouldDelayCloseScreen(event.getScreen())) {
            event.cancel();
            this.startDelayedClose();
         } else {
            if (this.packetsHeld && this.movePhase == InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT) {
               if (this.isFunTimeMode()) {
                  event.cancel();
                  this.delayedClosePending = true;
               }

               this.movePhase = InventoryMoveFeature.MovePhase.SLOWING_DOWN;
               this.actionStartTime = System.currentTimeMillis();
            }
         }
      }
   }

   @EventTarget
   public void onInput(PlayerInputEvent event) {
      if (event != null && mc.player != null) {
         if (this.shouldFreezeCloseInput()) {
            event.inputNone();
         } else if (this.shouldForceSoftClickSprint()) {
            event.setSprint(true);
         } else {
            if (this.shouldForceInventorySprint()) {
               event.setSprint(true);
            } else if (this.shouldSuppressSprintInput()) {
               event.setSprint(false);
            }
         }
      }
   }

   private void processLegitMovement() {
      if (!this.isClickGuiOpen()) {
         if (this.shouldDisableMovementForContainer()) {
            if (this.inventoryOpened || this.movePhase != InventoryMoveFeature.MovePhase.READY || this.keysOverridden) {
               this.stopPlayerSprintBeforeClose();
               this.resetState();
            }
         } else {
            boolean hasOpenScreen = mc.gui.screen() != null;
            if (hasOpenScreen && !this.inventoryOpened && this.movePhase == InventoryMoveFeature.MovePhase.READY) {
               this.closingScreen = false;
               this.startLegitMovement();
               this.inventoryOpened = true;
            }

            if (!hasOpenScreen && this.inventoryOpened) {
               this.stopInventorySprint();
               if (this.isLegitMode()) {
                  this.inventoryOpened = false;
                  if (this.movePhase == InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT) {
                     this.movePhase = InventoryMoveFeature.MovePhase.SLOWING_DOWN;
                     this.actionStartTime = System.currentTimeMillis();
                  }

                  this.closingScreen = false;
               } else {
                  if (this.delayedClosePending || this.closingScreen) {
                     this.inventoryOpened = false;
                  } else if (this.packetsHeld && this.movePhase == InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT) {
                     this.movePhase = InventoryMoveFeature.MovePhase.SLOWING_DOWN;
                     this.actionStartTime = System.currentTimeMillis();
                     this.inventoryOpened = false;
                  } else if (!this.packetsHeld) {
                     this.resetState();
                     this.inventoryOpened = false;
                  }

                  this.closingScreen = this.packetsHeld || this.delayedClosePending;
               }
            } else {
               if (!hasOpenScreen && !this.inventoryOpened && this.movePhase == InventoryMoveFeature.MovePhase.READY) {
                  this.closingScreen = false;
               }

               if (this.movePhase != InventoryMoveFeature.MovePhase.READY) {
                  this.handleMovementStates();
               }
            }
         }
      }
   }

   private void startLegitMovement() {
      this.captureMovementKeyStates();
      this.movePhase = InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT;
      this.keysOverridden = true;
      this.packetsHeld = false;
   }

   private void handleMovementStates() {
      for (int phaseBudget = 0; phaseBudget < 6; phaseBudget++) {
         long elapsed = System.currentTimeMillis() - this.actionStartTime;
         boolean continueSameTick = false;
         switch (this.movePhase) {
            case SLOWING_DOWN:
               if (this.softClickSlowdown) {
                  this.keepSoftClickMovement();
               } else if (!this.keysOverridden) {
                  this.wasForwardPressed = this.isKeyPressed(mc.options.keyUp);
                  this.wasBackPressed = this.isKeyPressed(mc.options.keyDown);
                  this.wasLeftPressed = this.isKeyPressed(mc.options.keyLeft);
                  this.wasRightPressed = this.isKeyPressed(mc.options.keyRight);
                  this.wasJumpPressed = this.isKeyPressed(mc.options.keyJump);
                  mc.options.keyUp.setDown(false);
                  mc.options.keyDown.setDown(false);
                  mc.options.keyLeft.setDown(false);
                  mc.options.keyRight.setDown(false);
                  mc.options.keyJump.setDown(false);
                  this.keysOverridden = true;
               }

               if (!this.softClickSlowdown) {
                  this.stopInventorySprint();
               }

               if (this.isLegitMode() && !this.softClickSlowdown) {
                  if (!this.isPlayerStopped() && elapsed < 100L) {
                     return;
                  }

                  this.movePhase = InventoryMoveFeature.MovePhase.SEND_PACKETS;
                  this.actionStartTime = System.currentTimeMillis();
                  continueSameTick = false;
               } else {
                  long slowdownDelay = this.getSlowdownDelayMs();
                  if (!this.hasDelayElapsed(elapsed, slowdownDelay)) {
                     return;
                  }

                  this.movePhase = InventoryMoveFeature.MovePhase.SEND_PACKETS;
                  this.actionStartTime = System.currentTimeMillis();
                  continueSameTick = this.isInstantDelay(slowdownDelay);
               }
               break;
            case ALLOW_MOVEMENT:
               if (this.isInventoryScreenOpen()) {
                  InventoryFlowManager.updateMoveKeys();
                  this.forceInventorySprint();
                  this.wasForwardPressed = this.isKeyPressed(mc.options.keyUp);
                  this.wasBackPressed = this.isKeyPressed(mc.options.keyDown);
                  this.wasLeftPressed = this.isKeyPressed(mc.options.keyLeft);
                  this.wasRightPressed = this.isKeyPressed(mc.options.keyRight);
                  this.wasJumpPressed = this.isKeyPressed(mc.options.keyJump);
               }
               break;
            case CLOSE_SCREEN:
               this.stopPlayerSprintBeforeClose();
               if (this.shouldWaitBeforeClose(elapsed)) {
                  return;
               }

               this.closeDelayedScreen();
               this.movePhase = InventoryMoveFeature.MovePhase.SPEEDING_UP;
               this.actionStartTime = System.currentTimeMillis();
               continueSameTick = this.isInstantDelay(this.getRestoreDelayMs());
               break;
            case SPEEDING_UP:
               long restoreDelay = this.getRestoreDelayMs();
               if (!this.hasDelayElapsed(elapsed, restoreDelay)) {
                  if (this.softClickSlowdown) {
                     this.keepSoftClickMovement();
                  } else if (this.isFunTimeMode()) {
                     this.stopPlayerSprintBeforeClose();
                  }

                  return;
               }

               if (this.keysOverridden) {
                  this.restoreKeyStates();
               }

               this.movePhase = InventoryMoveFeature.MovePhase.FINISHED;
               continueSameTick = true;
               break;
            case SEND_PACKETS:
               if (this.softClickSlowdown) {
                  this.keepSoftClickMovement();
               } else {
                  this.stopInventorySprint();
               }

               if (this.isLegitMode() && !this.softClickSlowdown && !this.isPlayerStopped() && elapsed < 100L) {
                  return;
               }

               if (!this.packets.isEmpty()) {
                  if (this.isFunTimeMode()) {
                     if (!this.hasDelayElapsed(elapsed, 0L)) {
                        return;
                     }

                     int packetLimit = this.delayedClosePending ? 2 : 2;
                     int packetsToSend = Math.min(packetLimit, this.packets.size());

                     for (int i = 0; i < packetsToSend; i++) {
                        this.sendHeldPacket(this.packets.remove(0));
                     }

                     this.actionStartTime = System.currentTimeMillis();
                     if (!this.packets.isEmpty()) {
                        return;
                     }
                  }

                  if (!this.packets.isEmpty()) {
                     this.packets.forEach(this::sendHeldPacket);
                     this.packets.clear();
                  }
               }

               this.packetsHeld = false;
               if (this.delayedClosePending) {
                  this.movePhase = InventoryMoveFeature.MovePhase.CLOSE_SCREEN;
                  this.actionStartTime = System.currentTimeMillis();
                  continueSameTick = !this.isLegitMode() && this.isInstantDelay(0L);
               } else {
                  this.movePhase = InventoryMoveFeature.MovePhase.SPEEDING_UP;
                  this.actionStartTime = System.currentTimeMillis();
                  continueSameTick = this.isInstantDelay(this.getRestoreDelayMs());
               }
               break;
            case FINISHED:
               this.resetState();
               return;
            default:
               return;
         }

         if (!continueSameTick) {
            return;
         }
      }
   }

   private void restoreKeyStates() {
      mc.options.keyUp.setDown(this.wasForwardPressed && this.isKeyPressed(mc.options.keyUp));
      mc.options.keyDown.setDown(this.wasBackPressed && this.isKeyPressed(mc.options.keyDown));
      mc.options.keyLeft.setDown(this.wasLeftPressed && this.isKeyPressed(mc.options.keyLeft));
      mc.options.keyRight.setDown(this.wasRightPressed && this.isKeyPressed(mc.options.keyRight));
      mc.options.keyJump.setDown(this.wasJumpPressed && this.isKeyPressed(mc.options.keyJump));
      this.keysOverridden = false;
   }

   private void resetState() {
      if (this.keysOverridden) {
         this.restoreKeyStates();
      }

      this.movePhase = InventoryMoveFeature.MovePhase.READY;
      this.playerFullyStopped = false;
      this.inventoryOpened = false;
      this.packetsHeld = false;
      this.closingScreen = false;
      this.delayedClosePending = false;
      this.bypassCloseEvent = false;
      this.softClickSlowdown = false;
      this.packets.clear();
      this.restoreSprintKeyState();
   }

   private void stopInventorySprint() {
      if (mc.options != null) {
         mc.options.keySprint.setDown(false);
      }
   }

   private void forceInventorySprint() {
      if (!this.hasDirectionalMovementInput()) {
         this.stopPlayerSprintBeforeClose();
      } else {
         if (mc.options != null) {
            mc.options.keySprint.setDown(true);
         }

         if (mc.player != null && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
         }
      }
   }

   private void stopPlayerSprintBeforeClose() {
      this.stopInventorySprint();
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }

   private void keepSoftClickMovement() {
      if (this.isInventoryScreenOpen()) {
         InventoryFlowManager.updateMoveKeys();
         this.forceInventorySprint();
      }
   }

   private void restoreSprintKeyState() {
      if (mc.options != null) {
         mc.options.keySprint.setDown(this.isKeyPressed(mc.options.keySprint));
      }
   }

   private boolean isInventoryScreenOpen() {
      return mc.gui.screen() != null && !(mc.gui.screen() instanceof ChatScreen);
   }

   private boolean isClickGuiOpen() {
      return MenuOverlay.isOpen();
   }

   private boolean shouldForceInventorySprint() {
      return !this.closingScreen
         && InventoryFlowManager.isMovementAllowed()
         && this.isInventoryScreenOpen()
         && this.hasDirectionalMovementInput()
         && !this.shouldDisableMovementForContainer()
         && (this.movePhase == InventoryMoveFeature.MovePhase.READY || this.movePhase == InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT);
   }

   private boolean shouldForceSoftClickSprint() {
      return this.softClickSlowdown
         && !this.delayedClosePending
         && InventoryFlowManager.isMovementAllowed()
         && this.isInventoryScreenOpen()
         && this.hasDirectionalMovementInput();
   }

   private boolean shouldFreezeCloseInput() {
      return this.softClickSlowdown && !this.delayedClosePending
         ? false
         : this.isFunTimeMode()
            && (
               this.delayedClosePending
                  || this.movePhase == InventoryMoveFeature.MovePhase.SLOWING_DOWN
                  || this.movePhase == InventoryMoveFeature.MovePhase.SEND_PACKETS
                  || this.movePhase == InventoryMoveFeature.MovePhase.CLOSE_SCREEN
                  || this.movePhase == InventoryMoveFeature.MovePhase.SPEEDING_UP
            );
   }

   private long getSlowdownDelayMs() {
      if (this.softClickSlowdown) {
         return 1L;
      } else {
         return this.isFunTimeMode() ? 0L : 0L;
      }
   }

   private long getRestoreDelayMs() {
      if (this.isLegitMode()) {
         return 20L;
      } else {
         return this.isFunTimeMode() ? 0L : 0L;
      }
   }

   private boolean hasDelayElapsed(long elapsed, long delayMs) {
      return this.isInstantDelay(delayMs) || elapsed >= delayMs;
   }

   private boolean isInstantDelay(long delayMs) {
      return delayMs <= 1L;
   }

   private boolean shouldWaitBeforeClose(long elapsed) {
      return !this.isLegitMode() ? !this.hasDelayElapsed(elapsed, 0L) : !this.isPlayerStopped() && elapsed < 100L;
   }

   private boolean isFunTimeMode() {
      String value = this.mode.getValue();
      return "FunTime".equals(value) || "Legit".equals(value);
   }

   private boolean isLegitMode() {
      return "Legit".equals(this.mode.getValue());
   }

   private boolean shouldDisableMovementForContainer() {
      return this.isLegitMode() && mc.gui.screen() instanceof ContainerScreen;
   }

   private boolean isPlayerStopped() {
      if (mc.player == null) {
         return true;
      }

      double vx = Math.abs(mc.player.getDeltaMovement().x);
      double vz = Math.abs(mc.player.getDeltaMovement().z);
      return vx < 0.03 && vz < 0.03;
   }

   private boolean shouldDelayCloseScreen(Screen screen) {
      return this.isFunTimeMode() && screen != null && !(screen instanceof ChatScreen) && !this.isClickGuiOpen() && !this.shouldDisableMovementForContainer();
   }

   private boolean shouldIgnoreServerClosePacket() {
      return this.isFunTimeMode()
         && (
            this.delayedClosePending
               || this.closingScreen
               || this.packetsHeld
               || this.movePhase == InventoryMoveFeature.MovePhase.SLOWING_DOWN
               || this.movePhase == InventoryMoveFeature.MovePhase.SEND_PACKETS
               || this.movePhase == InventoryMoveFeature.MovePhase.CLOSE_SCREEN
               || this.movePhase == InventoryMoveFeature.MovePhase.SPEEDING_UP
         );
   }

   private void startSlowdownForInventoryClick(ServerboundContainerClickPacket packet) {
      if (this.isFunTimeMode() && this.movePhase == InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT && this.hasDirectionalMovementInput()) {
         ContainerInput containerInput = packet.containerInput();
         if (containerInput == ContainerInput.PICKUP
            || containerInput == ContainerInput.QUICK_CRAFT
            || containerInput == ContainerInput.PICKUP_ALL
            || containerInput == ContainerInput.QUICK_MOVE
            || containerInput == ContainerInput.THROW) {
            this.softClickSlowdown = !this.isLegitMode();
            if (this.softClickSlowdown) {
               this.keepSoftClickMovement();
            }

            this.movePhase = InventoryMoveFeature.MovePhase.SLOWING_DOWN;
            this.actionStartTime = System.currentTimeMillis();
         }
      }
   }

   private void sendHeldPacket(Packet<?> packet) {
      if (!(
         packet instanceof ServerboundContainerClickPacket clickPacket && mc.player != null && clickPacket.containerId() != mc.player.containerMenu.containerId
      )) {
         PlayerInteractionHelper.sendPacketWithOutEvent(packet);
      }
   }

   private boolean hasCloseMovement() {
      if (mc.player == null) {
         return false;
      }

      double motionX = mc.player.getDeltaMovement().x;
      double motionZ = mc.player.getDeltaMovement().z;
      return motionX * motionX + motionZ * motionZ > 1.0E-4;
   }

   private void startDelayedClose() {
      this.delayedClosePending = true;
      this.closingScreen = true;
      this.softClickSlowdown = false;
      this.stopPlayerSprintBeforeClose();
      if (this.movePhase == InventoryMoveFeature.MovePhase.READY) {
         this.captureMovementKeyStates();
         this.keysOverridden = false;
         this.inventoryOpened = true;
         this.movePhase = InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT;
      }

      if (this.movePhase == InventoryMoveFeature.MovePhase.ALLOW_MOVEMENT) {
         this.movePhase = InventoryMoveFeature.MovePhase.SLOWING_DOWN;
         this.actionStartTime = System.currentTimeMillis();
      }
   }

   private void captureMovementKeyStates() {
      this.wasForwardPressed = this.isKeyPressed(mc.options.keyUp);
      this.wasBackPressed = this.isKeyPressed(mc.options.keyDown);
      this.wasLeftPressed = this.isKeyPressed(mc.options.keyLeft);
      this.wasRightPressed = this.isKeyPressed(mc.options.keyRight);
      this.wasJumpPressed = this.isKeyPressed(mc.options.keyJump);
   }

   private boolean hasDirectionalMovementInput() {
      return mc.options != null
         && (
            this.isKeyPressed(mc.options.keyUp)
               || this.isKeyPressed(mc.options.keyDown)
               || this.isKeyPressed(mc.options.keyLeft)
               || this.isKeyPressed(mc.options.keyRight)
         );
   }

   private void closeDelayedScreen() {
      if (this.delayedClosePending && mc.player != null) {
         this.bypassCloseEvent = true;

         try {
            InventoryTask.closeScreen(false);
         } finally {
            this.bypassCloseEvent = false;
            this.delayedClosePending = false;
         }
      }
   }

   private boolean isKeyPressed(KeyMapping key) {
      if (mc.getWindow() == null) {
         return false;
      }

      Key inputKey = InputConstants.getKey(key.saveString());
      long handle = mc.getWindow().handle();

      return switch (inputKey.getType()) {
         case KEYSYM -> InputConstants.isKeyDown(mc.getWindow(), inputKey.getValue());
         case MOUSE -> GLFW.glfwGetMouseButton(handle, inputKey.getValue()) == 1;
         default -> false;
      };
   }

   private enum MovePhase {
      READY,
      SLOWING_DOWN,
      ALLOW_MOVEMENT,
      CLOSE_SCREEN,
      SPEEDING_UP,
      SEND_PACKETS,
      FINISHED;

      // $VF: synthetic method
      private static InventoryMoveFeature.MovePhase[] $values() {
         return new InventoryMoveFeature.MovePhase[]{READY, SLOWING_DOWN, ALLOW_MOVEMENT, CLOSE_SCREEN, SPEEDING_UP, SEND_PACKETS, FINISHED};
      }
   }
}

