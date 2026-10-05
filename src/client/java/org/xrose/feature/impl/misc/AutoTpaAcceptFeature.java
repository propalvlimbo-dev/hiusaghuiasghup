package org.xrose.feature.impl.misc;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.pve.PvpStateTracker;
import org.xrose.utils.FriendManager;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoTpaAcceptFeature extends Feature implements MinecraftContext {
   private static final List<String> REQUEST_MARKERS = List.of("has requested teleport", "телепортироваться");
   private static final long ACCEPT_COOLDOWN_MILLIS = 1000L;
   public final BooleanSetting friendsOnly = this.register(new BooleanSetting("Friends Only", false));
   public final BooleanSetting avoidCombat = this.register(new BooleanSetting("Avoid Combat", true));
   private long lastAcceptAt;

   public AutoTpaAcceptFeature() {
      super("AutoTpaAccept", "Automatically accepts teleport requests", FeatureCategory.MISC, -1);
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE && this.player() != null) {
         if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
            String text = packet.content().getString();
            String normalized = text.toLowerCase(Locale.ROOT);
            if (!REQUEST_MARKERS.stream().noneMatch(normalized::contains)) {
               if (!this.avoidCombat.getValue() || !this.isInCombat()) {
                  if (!this.friendsOnly.getValue()
                     || !FriendManager.INSTANCE.getFriends().stream().map(name -> name.toLowerCase(Locale.ROOT)).noneMatch(normalized::contains)) {
                     long now = System.currentTimeMillis();
                     if (now - this.lastAcceptAt >= 1000L) {
                        this.lastAcceptAt = now;
                        this.player().connection.sendCommand("tpaccept");
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isInCombat() {
      if (!PvpStateTracker.INSTANCE.isActive() && this.player().hurtTime <= 0) {
         AuraFeature aura = FeatureManager.INSTANCE.getFeature(AuraFeature.class);
         return aura != null && aura.getCurrentTarget() != null;
      } else {
         return true;
      }
   }
}

