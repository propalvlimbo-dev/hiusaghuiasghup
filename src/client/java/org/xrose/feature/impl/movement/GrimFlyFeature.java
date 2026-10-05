package org.xrose.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class GrimFlyFeature extends Feature {
   public final NumberSetting speed = this.register(new NumberSetting("Speed", 0.18, 0.05, 0.3, 0.01, ""));
   public final NumberSetting verticalSpeed = this.register(new NumberSetting("Vertical Speed", 0.08, 0.02, 0.15, 0.01, ""));

   public GrimFlyFeature() {
      super("GrimFly", "Low-speed flight", FeatureCategory.MOVEMENT, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && !player.isSpectator() && !player.getAbilities().flying && !player.isFallFlying()) {
         Vec2 input = player.input.getMoveVector();
         player.setDeltaMovement(Vec3.ZERO);
         if (input.lengthSquared() > 0.0F) {
            player.moveRelative(this.speed.getValue().floatValue(), new Vec3(input.x, 0.0, input.y));
         }

         double vertical = event.getClient().options.keyJump.isDown()
            ? this.verticalSpeed.getValue()
            : (event.getClient().options.keyShift.isDown() ? -this.verticalSpeed.getValue() : 0.0);
         Vec3 movement = player.getDeltaMovement();
         player.setDeltaMovement(movement.x, vertical, movement.z);
      }
   }
}

