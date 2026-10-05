package org.xrose.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.PlayerContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class WaterSpeedFeature extends Feature implements PlayerContext {
   public final NumberSetting boost = this.register(new NumberSetting("Boost", 1.05, 1.01, 1.2, 0.01, "x"));

   public WaterSpeedFeature() {
      super("WaterSpeed", "Swim faster", FeatureCategory.MOVEMENT, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && player.isSwimming() && this.isMoving()) {
         Vec3 movement = player.getDeltaMovement();
         player.setDeltaMovement(movement.x * this.boost.getValue(), movement.y, movement.z * this.boost.getValue());
      }
   }
}

