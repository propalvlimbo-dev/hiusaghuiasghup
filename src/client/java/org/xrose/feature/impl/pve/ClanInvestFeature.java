package org.xrose.feature.impl.pve;

import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.DisconnectEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.feature.setting.TextSetting;
import org.xrose.pve.AutomationPriority;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.pve.PveFeature;
import org.xrose.pve.PveStateMachine;
import org.xrose.pve.economy.CommandCooldown;
import org.xrose.pve.economy.EconomyChat;
import org.xrose.pve.economy.EconomyCommands;
import org.xrose.pve.economy.EconomyTextParser;
import org.xrose.pve.economy.ScoreboardBalance;
import org.xrose.pve.server.ServerAdapter;
import org.xrose.pve.server.ServerAdapters;
import org.xrose.pve.server.ServerProfile;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ClanInvestFeature extends PveFeature {
   private static final long CONFIRMATION_TIMEOUT_TICKS = 80L;
   private static final long COMMAND_COOLDOWN_TICKS = 100L;
   public final TextSetting currencyThreshold = this.register(new TextSetting("Currency Threshold", "1000000"));
   public final NumberSetting investPercentage = this.register(new NumberSetting("Invest Percentage", 30.0, 1.0, 100.0, 1.0, "%"));
   private final PveStateMachine<ClanInvestFeature.State> machine = new PveStateMachine<>(ClanInvestFeature.State.ARMED);
   private final CommandCooldown commandCooldown = new CommandCooldown();
   private long lastTick;
   private long pendingAmount;

   public ClanInvestFeature() {
      super("ClanInvest", "Invests a percentage of the displayed balance after a threshold", -1, AutomationPriority.FEATURE);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client.player != null && client.level != null) {
         long tick = client.level.getGameTime();
         this.lastTick = tick;
         if (ServerAdapters.current().profile() == ServerProfile.FUNTIME) {
            if (this.machine.is(ClanInvestFeature.State.WAITING_CONFIRMATION) && this.machine.ticksInState(tick) >= 80L) {
               this.pendingAmount = 0L;
               this.machine.transition(ClanInvestFeature.State.ARMED, tick);
            }

            int threshold = this.positiveThreshold();
            if (threshold > 0) {
               OptionalLong balanceResult = ScoreboardBalance.read(client.player);
               if (!balanceResult.isEmpty()) {
                  long balance = balanceResult.getAsLong();
                  if (balance < threshold) {
                     this.pendingAmount = 0L;
                     this.machine.transition(ClanInvestFeature.State.ARMED, tick);
                  } else if (this.machine.is(ClanInvestFeature.State.ARMED) && this.commandCooldown.ready(tick)) {
                     long boundedBalance = Math.min(2147483647L, balance);
                     long amount = Math.max(1L, Math.min(2147483647L, boundedBalance * Math.round(this.investPercentage.getValue()) / 100L));
                     EconomyCommands.clanInvest(amount).ifPresent(command -> {
                        if (!this.claim(AutomationResource.CHAT)) {
                           this.commandCooldown.defer(tick, 5L);
                        } else {
                           try {
                              ServerAdapter adapter = ServerAdapters.current();
                              adapter.sendCommand(client.player, command);
                              this.pendingAmount = amount;
                              this.machine.transition(ClanInvestFeature.State.WAITING_CONFIRMATION, tick);
                              this.commandCooldown.tryAcquire(tick, 100L);
                           } finally {
                              PveAutomationCoordinator.INSTANCE.release(this);
                           }
                        }
                     });
                  }
               }
            }
         }
      }
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE) {
         String text = EconomyChat.incomingText(event.getPacket());
         if (text != null) {
            String normalized = EconomyTextParser.normalize(text);
            Minecraft.getInstance().execute(() -> this.handleChat(normalized));
         }
      }
   }

   @EventTarget
   public void onDisconnect(DisconnectEvent event) {
      this.resetRuntime();
   }

   @Override
   protected void onPveEnable() {
      this.resetRuntime();
   }

   @Override
   protected void onPveDisable() {
      this.resetRuntime();
   }

   @Override
   protected void onPvePreempted(PveAutomationCoordinator.RevocationReason reason) {
      this.resetRuntime();
   }

   private void handleChat(String text) {
      if (this.isEnabled()) {
         if (!EconomyTextParser.containsAny(text, "/clan create - создать клан", "you are not in a clan")
            && !EconomyTextParser.containsAny(text, "вы не можете пополнить баланс клана", "cannot deposit to the clan")) {
            if (this.machine.is(ClanInvestFeature.State.WAITING_CONFIRMATION)
               && EconomyTextParser.containsAny(text, "пополнил баланс казны", "clan treasury", "clan balance")) {
               this.pendingAmount = 0L;
               this.machine.transition(ClanInvestFeature.State.LATCHED, this.lastTick);
            }
         } else {
            this.setEnabled(false);
         }
      }
   }

   private int positiveThreshold() {
      String value = this.currencyThreshold.getValue();
      if (value != null && value.matches("\\d+")) {
         try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : -1;
         } catch (NumberFormatException ignored) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private void resetRuntime() {
      this.machine.reset(0L);
      this.commandCooldown.reset();
      this.pendingAmount = 0L;
      this.lastTick = 0L;
   }

   enum State {
      ARMED,
      WAITING_CONFIRMATION,
      LATCHED;

      // $VF: synthetic method
      private static ClanInvestFeature.State[] $values() {
         return new ClanInvestFeature.State[]{ARMED, WAITING_CONFIRMATION, LATCHED};
      }
   }
}

