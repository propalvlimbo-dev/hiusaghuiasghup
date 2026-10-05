package org.xrose.event.events.lifecycle;

import lombok.Generated;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.xrose.event.Event;

public final class WorldLeaveEvent extends Event {
   private Minecraft client;
   private ClientLevel level;

   public WorldLeaveEvent set(Minecraft client, ClientLevel level) {
      this.client = client;
      this.level = level;
      return this;
   }

   @Generated
   public Minecraft getClient() {
      return this.client;
   }

   @Generated
   public ClientLevel getLevel() {
      return this.level;
   }
}

