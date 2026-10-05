package org.xrose.utils.combat;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import org.xrose.event.EventTarget;
import org.xrose.event.events.lifecycle.WorldJoinEvent;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.event.events.packet.PacketSendEvent;

public final class ServerSprintTracker {
   public static final ServerSprintTracker INSTANCE = new ServerSprintTracker();
   private volatile boolean sprinting;

   private ServerSprintTracker() {
   }

   public static boolean isServerSprinting() {
      return INSTANCE.sprinting;
   }

   public static void resetServerState() {
      INSTANCE.sprinting = false;
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (event.getPhase() == PacketSendEvent.Phase.PRE && event.getPacket() instanceof ServerboundPlayerCommandPacket packet) {
         if (packet.getAction() == Action.START_SPRINTING) {
            if (this.sprinting) {
               event.cancel();
               return;
            }

            this.sprinting = true;
         } else if (packet.getAction() == Action.STOP_SPRINTING) {
            if (!this.sprinting) {
               event.cancel();
               return;
            }

            this.sprinting = false;
         }
      }
   }

   @EventTarget
   public void onWorldJoin(WorldJoinEvent event) {
      this.sprinting = false;
      LocalPlayerHistory.reset();
   }

   @EventTarget
   public void onWorldLeave(WorldLeaveEvent event) {
      this.sprinting = false;
      LocalPlayerHistory.reset();
   }
}

