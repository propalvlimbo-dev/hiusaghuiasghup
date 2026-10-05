package org.xrose.event.events.game;

import net.minecraft.world.entity.LivingEntity;
import org.xrose.event.Event;

public class FireworkEvent extends Event {
   private final LivingEntity boostedEntity;
   private float speed;

   public FireworkEvent(LivingEntity boostedEntity, float speed) {
      this.boostedEntity = boostedEntity;
      this.speed = speed;
   }

   public LivingEntity getBoostedEntity() {
      return this.boostedEntity;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }
}

