package org.xrose.event.events.lifecycle;

import lombok.Generated;
import net.minecraft.client.Minecraft;
import org.xrose.event.Event;

public final class ResourceReloadEvent extends Event {
   private Minecraft client;

   public ResourceReloadEvent set(Minecraft client) {
      this.client = client;
      return this;
   }

   @Generated
   public Minecraft getClient() {
      return this.client;
   }
}

