package org.xrose.feature.impl.misc;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.TextSetting;
import org.xrose.utils.FriendManager;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoDuelFeature extends Feature implements MinecraftContext {
   private static final List<String> DUEL_MARKERS = List.of("has sent you a duel request", "вызвал вас на дуэль", "duel request", "на дуэль");
   private static final long ACCEPT_COOLDOWN_MILLIS = 1000L;
   public final TextSetting command = this.register(new TextSetting("Command", "duel accept"));
   public final BooleanSetting friendsOnly = this.register(new BooleanSetting("Friends Only", false));
   private long lastAcceptAt;

   public AutoDuelFeature() {
      super("AutoDuel", "Automatically accepts duel requests", FeatureCategory.MISC, -1);
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE && this.player() != null) {
         if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
            String var7 = packet.content().getString().toLowerCase(Locale.ROOT);
            if (!DUEL_MARKERS.stream().noneMatch(var7::contains)) {
               if (!this.friendsOnly.getValue()
                  || !FriendManager.INSTANCE.getFriends().stream().map(name -> name.toLowerCase(Locale.ROOT)).noneMatch(var7::contains)) {
                  long now = System.currentTimeMillis();
                  if (now - this.lastAcceptAt >= 1000L) {
                     this.lastAcceptAt = now;
                     String cmd = this.command.getValue();
                     if (cmd != null && !cmd.isBlank()) {
                        this.player().connection.sendCommand(cmd.trim());
                     }
                  }
               }
            }
         }
      }
   }
}

