package org.xrose.feature.impl.combat;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class VelocityFeature extends Feature implements MinecraftContext {
   public VelocityFeature() {
      super("Velocity", "Removes knockback", FeatureCategory.COMBAT, -1);
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE && this.player() != null) {
         if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.id() == this.player().getId()) {
            event.cancel();
         }
      }
   }
}

