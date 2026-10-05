package org.xrose.utils.combat.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.RotationContext;

public final class ReallyworldRotation implements AuraRotation {
   private static final float LERP_SPEED = 0.45F;
   private float smoothedYaw;
   private float smoothedPitch;
   private boolean initialized;

   @Override
   public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
      if (target != null && target.isAlive()) {
         Vec3 eye = player.getEyePosition();
         Vec3 delta = targetEyePos.subtract(eye);
         double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
         float idealYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
         float idealPitch = Mth.clamp((float)(-Math.toDegrees(Math.atan2(delta.y, horizontal))), -90.0F, 90.0F);
         if (!this.initialized) {
            this.smoothedYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
            this.smoothedPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
            this.initialized = true;
         }

         float deltaYaw = Mth.wrapDegrees(idealYaw - this.smoothedYaw);
         float deltaPitch = idealPitch - this.smoothedPitch;
         this.smoothedYaw = Mth.wrapDegrees(this.smoothedYaw + deltaYaw * 0.45F);
         this.smoothedPitch = Mth.clamp(this.smoothedPitch + deltaPitch * 0.45F, -90.0F, 90.0F);
         ThreadLocalRandom random = ThreadLocalRandom.current();
         float noiseYaw = (float)random.nextDouble(-3.0, 3.0);
         float noisePitch = (float)random.nextDouble(-2.0, 2.0);
         float finalYaw = this.smoothedYaw + noiseYaw;
         float finalPitch = Mth.clamp(this.smoothedPitch + noisePitch, -90.0F, 90.0F);
         RotationContext.setRotation(finalYaw, finalPitch);
      } else {
         this.reset();
         RotationContext.clear();
      }
   }

   @Override
   public void reset() {
      this.initialized = false;
   }
}

