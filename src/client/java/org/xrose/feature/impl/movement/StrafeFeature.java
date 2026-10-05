package org.xrose.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.PlayerTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.move.MoveUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class StrafeFeature extends Feature {
   public final NumberSetting speed = this.register(new NumberSetting("Speed", 0.32, 0.1, 0.6, 0.01, ""));
   public final NumberSetting control = this.register(new NumberSetting("Air Control", 0.25, 0.05, 0.8, 0.05, ""));

   public StrafeFeature() {
      super("Strafe", "Full air control while jumping", FeatureCategory.MOVEMENT, -1);
   }

   @EventTarget
   public void onPlayerTick(PlayerTickEvent event) {
      if (event.isPost()) {
         LocalPlayer player = event.getPlayer();
         if (player != null && !player.onGround() && !player.isFallFlying() && !player.isInWater() && !player.isSwimming() && !player.onClimbable()) {
            if (MoveUtil.hasPlayerMovement()) {
               double[] wish = MoveUtil.forward(1.0);
               double wishLen = Math.sqrt(wish[0] * wish[0] + wish[1] * wish[1]);
               if (!(wishLen < 1.0E-4)) {
                  double dirX = wish[0] / wishLen;
                  double dirZ = wish[1] / wishLen;
                  Vec3 motion = player.getDeltaMovement();
                  double blend = this.control.getFloat();
                  double targetX = dirX * this.speed.getFloat();
                  double targetZ = dirZ * this.speed.getFloat();
                  player.setDeltaMovement(motion.x + (targetX - motion.x) * blend, motion.y, motion.z + (targetZ - motion.z) * blend);
               }
            }
         }
      }
   }
}

