package org.xrose.feature.impl.player;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.feature.setting.TextSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoRespawnFeature extends Feature {
   public final NumberSetting delay = this.register(new NumberSetting("Respawn Delay", 0.0, 0.0, 100.0, 1.0, " ticks"));
   public final BooleanSetting teleportHome = this.register(new BooleanSetting("Teleport Home", false));
   public final TextSetting command = this.register(new TextSetting("Command", "/home", 128).visibleWhen(this.teleportHome::getValue));
   private int deathTicks;
   private int commandDelay = -1;

   public AutoRespawnFeature() {
      super("AutoRespawn", "Respawns automatically after death", FeatureCategory.PLAYER, -1);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player == null) {
         this.reset();
      } else if (event.getClient().gui.screen() instanceof DeathScreen) {
         if (this.deathTicks++ > this.delay.getValue().intValue()) {
            player.respawn();
            event.getClient().gui.setScreen(null);
            this.deathTicks = 0;
            this.commandDelay = this.teleportHome.getValue() ? 1 : -1;
         }
      } else {
         this.deathTicks = 0;
         if (this.commandDelay > 0) {
            this.commandDelay--;
         } else {
            if (this.commandDelay == 0) {
               this.commandDelay = -1;
               String value = this.command.getValue().trim();
               if (value.startsWith("/")) {
                  value = value.substring(1);
               }

               if (!value.isBlank()) {
                  player.connection.sendCommand(value);
               }
            }
         }
      }
   }

   @EventTarget
   public void onWorldLeave(WorldLeaveEvent event) {
      this.reset();
   }

   private void reset() {
      this.deathTicks = 0;
      this.commandDelay = -1;
   }
}

