package org.xrose.feature.impl.pve;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.DisconnectEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.pve.AutomationPriority;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.pve.PveFeature;
import org.xrose.pve.PveStateMachine;
import org.xrose.pve.economy.CommandCooldown;
import org.xrose.pve.economy.EconomyChat;
import org.xrose.pve.economy.EconomyCommands;
import org.xrose.pve.economy.EconomyItemText;
import org.xrose.pve.economy.EconomyMenus;
import org.xrose.pve.economy.EconomyTextParser;
import org.xrose.pve.server.ServerAdapter;
import org.xrose.pve.server.ServerAdapters;
import org.xrose.pve.server.ServerProfile;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AuctionRelistFeature extends PveFeature {
   private static final long CYCLE_TICKS = 1200L;
   private static final long MENU_TIMEOUT_TICKS = 80L;
   private final PveStateMachine<AuctionRelistFeature.State> machine = new PveStateMachine<>(AuctionRelistFeature.State.WAIT);
   private final CommandCooldown commandCooldown = new CommandCooldown();
   private long lastTick;
   private long nextCycleTick;
   private int ownedContainerId = -1;
   private boolean resourcesClaimed;

   public AuctionRelistFeature() {
      super("AuctionRelist", "Periodically relists expired auction items", -1, AutomationPriority.FEATURE);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client.player != null && client.level != null) {
         long tick = client.level.getGameTime();
         this.lastTick = tick;
         if (ServerAdapters.current().profile() == ServerProfile.FUNTIME) {
            switch ((AuctionRelistFeature.State)this.machine.state()) {
               case WAIT:
                  if (tick >= this.nextCycleTick && !isPlayerMoving(client)) {
                     this.beginCycle(tick);
                  }
                  break;
               case OPEN_AUCTION:
                  this.openAuction(client, tick);
                  break;
               case WAIT_AUCTION_MENU:
                  this.waitForAuctionMenu(client, tick);
                  break;
               case WAIT_STORAGE_MENU:
                  this.waitForStorageMenu(client, tick);
                  break;
               case CLOSING:
                  if (this.machine.ticksInState(tick) >= 6L) {
                     this.finishCycle(client, tick);
                  }
            }
         } else {
            if (this.resourcesClaimed || !this.machine.is(AuctionRelistFeature.State.WAIT)) {
               this.finishCycle(client, tick);
            }
         }
      }
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE) {
         String text = EconomyChat.incomingText(event.getPacket());
         if (text != null && EconomyTextParser.containsAny(text, "аукцион недоступен", "auction is unavailable", "не удалось открыть аукцион", "слишком часто")
            )
          {
            Minecraft.getInstance().execute(() -> {
               if (this.isEnabled()) {
                  this.finishCycle(Minecraft.getInstance(), this.lastTick);
               }
            });
         }
      }
   }

   @EventTarget
   public void onDisconnect(DisconnectEvent event) {
      this.resetRuntime(false);
   }

   @Override
   protected void onPveEnable() {
      this.resetRuntime(false);
      this.nextCycleTick = this.lastTick + 1200L;
   }

   @Override
   protected void onPveDisable() {
      this.resetRuntime(true);
   }

   @Override
   protected void onPvePreempted(PveAutomationCoordinator.RevocationReason reason) {
      this.resetRuntime(true);
   }

   private void beginCycle(long tick) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.gui.screen() == null && client.player.containerMenu == client.player.inventoryMenu) {
         if (!this.claim(AutomationResource.INVENTORY, AutomationResource.SCREEN, AutomationResource.CHAT)) {
            this.nextCycleTick = tick + 10L;
         } else {
            this.resourcesClaimed = true;
            this.machine.transition(AuctionRelistFeature.State.OPEN_AUCTION, tick);
         }
      } else {
         this.nextCycleTick = tick + 20L;
      }
   }

   private void openAuction(Minecraft client, long tick) {
      if (this.commandCooldown.ready(tick)) {
         ServerAdapter adapter = ServerAdapters.current();
         adapter.auctionCommand().flatMap(EconomyCommands::auctionRoot).ifPresentOrElse(command -> {
            adapter.sendCommand(client.player, command);
            this.commandCooldown.tryAcquire(tick, 40L);
            this.machine.transition(AuctionRelistFeature.State.WAIT_AUCTION_MENU, tick);
         }, () -> this.finishCycle(client, tick));
      }
   }

   private void waitForAuctionMenu(Minecraft client, long tick) {
      if (this.machine.ticksInState(tick) > 80L) {
         this.finishCycle(client, tick);
      } else if (EconomyMenus.titleContains(client, "аукцион", "auction")) {
         AbstractContainerMenu menu = client.player.containerMenu;
         int storage = EconomyMenus.findContainerSlot(
            menu, stack -> EconomyItemText.containsAny(stack, "хранилище", "storage", "истекшие", "expired", "снятые товары")
         );
         if (storage >= 0) {
            this.ownedContainerId = menu.containerId;
            if (EconomyMenus.click(client, menu, storage, 0, ContainerInput.PICKUP)) {
               this.machine.transition(AuctionRelistFeature.State.WAIT_STORAGE_MENU, tick);
            }
         }
      }
   }

   private void waitForStorageMenu(Minecraft client, long tick) {
      if (this.machine.ticksInState(tick) > 80L) {
         this.finishCycle(client, tick);
      } else if (EconomyMenus.titleContains(client, "хранилище", "storage")) {
         AbstractContainerMenu menu = client.player.containerMenu;
         int relist = EconomyMenus.findContainerSlot(
            menu, stack -> EconomyItemText.containsAny(stack, "перевыставить", "перевыстав", "выставить снова", "relist")
         );
         if (relist < 0) {
            if (this.machine.ticksInState(tick) >= 10L) {
               this.ownedContainerId = menu.containerId;
               this.machine.transition(AuctionRelistFeature.State.CLOSING, tick);
            }
         } else {
            this.ownedContainerId = menu.containerId;
            if (EconomyMenus.click(client, menu, relist, 0, ContainerInput.PICKUP)) {
               this.machine.transition(AuctionRelistFeature.State.CLOSING, tick);
            }
         }
      }
   }

   private void finishCycle(Minecraft client, long tick) {
      if (this.isRecognizedMenu(client)) {
         EconomyMenus.closeOwned(client, this.ownedContainerId);
      }

      this.ownedContainerId = -1;
      this.nextCycleTick = tick + 1200L;
      this.machine.transition(AuctionRelistFeature.State.WAIT, tick);
      if (this.resourcesClaimed) {
         this.resourcesClaimed = false;
         PveAutomationCoordinator.INSTANCE.release(this);
      }
   }

   private boolean isRecognizedMenu(Minecraft client) {
      return EconomyMenus.currentContainerId(client) == this.ownedContainerId
         && EconomyMenus.titleContains(client, "аукцион", "auction", "хранилище", "storage");
   }

   private static boolean isPlayerMoving(Minecraft client) {
      return client.player != null && client.player.getDeltaMovement().horizontalDistanceSqr() > 0.0025;
   }

   private void resetRuntime(boolean closeScreen) {
      Minecraft client = Minecraft.getInstance();
      if (closeScreen && this.isRecognizedMenu(client)) {
         EconomyMenus.closeOwned(client, this.ownedContainerId);
      }

      this.machine.reset(0L);
      this.commandCooldown.reset();
      this.ownedContainerId = -1;
      if (this.resourcesClaimed) {
         this.resourcesClaimed = false;
         PveAutomationCoordinator.INSTANCE.release(this);
      }

      this.nextCycleTick = 1200L;
      this.lastTick = 0L;
   }

   enum State {
      WAIT,
      OPEN_AUCTION,
      WAIT_AUCTION_MENU,
      WAIT_STORAGE_MENU,
      CLOSING;

      // $VF: synthetic method
      private static AuctionRelistFeature.State[] $values() {
         return new AuctionRelistFeature.State[]{WAIT, OPEN_AUCTION, WAIT_AUCTION_MENU, WAIT_STORAGE_MENU, CLOSING};
      }
   }
}

