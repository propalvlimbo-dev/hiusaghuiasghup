package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.xrose.event.EventTarget;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.mixin.accessor.MovePlayerPacketAccessor;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class NoFallFeature extends Feature {
   public final NumberSetting distance = this.register(new NumberSetting("Distance", 2.0, 0.5, 10.0, 0.5, " blocks"));

   public NoFallFeature() {
      super("NoFall", "Negates fall damage", FeatureCategory.MOVEMENT, -1);
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (event.getPhase() == PacketSendEvent.Phase.PRE
         && event.getPacket() instanceof ServerboundMovePlayerPacket packet
         && !((MovePlayerPacketAccessor)packet).isOnGroundFlag()) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null && !player.onGround() && !player.isFallFlying() && !player.isInWater() && !player.isInLava() && !player.onClimbable()) {
            if (!(player.fallDistance < this.distance.getFloat())) {
               ((MovePlayerPacketAccessor)packet).setOnGroundFlag(true);
            }
         }
      }
   }
}

