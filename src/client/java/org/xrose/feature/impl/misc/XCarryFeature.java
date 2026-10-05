package org.xrose.feature.impl.misc;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.xrose.event.EventTarget;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class XCarryFeature extends Feature {
   private static final int PLAYER_INVENTORY_CONTAINER_ID = 0;

   public XCarryFeature() {
      super("XCarry", "Store items in the crafting grid", FeatureCategory.MISC, -1);
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (event.getPhase() == PacketSendEvent.Phase.PRE) {
         if (event.getPacket() instanceof ServerboundContainerClosePacket packet && packet.getContainerId() == 0) {
            event.cancel();
         }
      }
   }
}

