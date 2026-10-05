package org.xrose.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class GrimCollideFeature extends Feature {
   public final NumberSetting boost = this.register(new NumberSetting("Boost", 0.08, 0.01, 0.08, 0.01, " b/t"));
   public final NumberSetting collisionRange = this.register(new NumberSetting("Collision Range", 0.5, 0.1, 2.0, 0.1, ""));

   public GrimCollideFeature() {
      super("GrimCollide", "Uses movement allowance near living entities", FeatureCategory.MOVEMENT, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && event.getClient().level != null && !player.input.getMoveVector().equals(Vec2.ZERO)) {
         AABB box = player.getBoundingBox().inflate(this.collisionRange.getValue());
         int collisions = event.getClient()
            .level
            .getEntities(player, box, entity -> this.isCollidableLivingEntity(player, entity))
            .stream()
            .filter(entity -> box.intersects(entity.getBoundingBox()))
            .toList()
            .size();
         if (collisions != 0) {
            double yaw = Math.toRadians(player.getYRot());
            double speed = this.boost.getValue() * collisions;
            player.push(-Math.sin(yaw) * speed, 0.0, Math.cos(yaw) * speed);
         }
      }
   }

   private boolean isCollidableLivingEntity(LocalPlayer player, Entity entity) {
      return entity != player && entity instanceof LivingEntity && !(entity instanceof ArmorStand);
   }
}

