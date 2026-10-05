package org.xrose.event.events.game;

import lombok.Generated;
import org.xrose.event.Event;
import org.xrose.event.events.EventPhase;

public final class RotationUpdateEvent extends Event {
   private final EventPhase phase;

   public RotationUpdateEvent(EventPhase phase) {
      this.phase = phase;
   }

   public boolean isPre() {
      return this.phase == EventPhase.PRE;
   }

   public boolean isPost() {
      return this.phase == EventPhase.POST;
   }

   @Generated
   public EventPhase getPhase() {
      return this.phase;
   }
}

