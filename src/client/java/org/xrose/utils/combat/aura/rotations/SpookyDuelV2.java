package org.xrose.utils.combat.aura.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public class SpookyDuelV2 extends RotateConstructor {
   public static final SpookyDuelV2 INSTANCE = new SpookyDuelV2();
   private long rotationTime = System.currentTimeMillis();

   private SpookyDuelV2() {
      super("SpookyDuelV2");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return currentAngle;
      }

      if (entity == null) {
         float playerYaw = mc.player.getYRot();
         float playerPitch = mc.player.getXRot();
         float yawDelta = Mth.wrapDegrees(playerYaw - currentAngle.getYaw());
         float pitchDelta = playerPitch - currentAngle.getPitch();
         float returnSpeed = 82.0F + ThreadLocalRandom.current().nextFloat(22.0F);
         float newYaw = currentAngle.getYaw() + Mth.clamp(yawDelta, -returnSpeed, returnSpeed);
         float newPitch = Mth.clamp(currentAngle.getPitch() + Mth.clamp(pitchDelta, -returnSpeed * 0.78F, returnSpeed * 0.78F), -89.9F, 89.9F);
         return new Angle(newYaw, newPitch);
      }

      if (entity instanceof LivingEntity livingEntity) {
         long currentTime = System.currentTimeMillis();
         Vec3 targetPos = livingEntity.position().add(0.0, livingEntity.getBbHeight() * 0.58, 0.0);
         Vec3 eyePos = mc.player.getEyePosition();
         Vec3 direction = targetPos.subtract(eyePos);
         float targetYaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
         float targetPitch = (float)Math.toDegrees(Math.asin(-direction.y / direction.length()));
         float yawDiff = Mth.wrapDegrees(targetYaw - currentAngle.getYaw());
         float pitchDiff = Mth.wrapDegrees(targetPitch - currentAngle.getPitch());
         float maxSpeed = 82.0F + ThreadLocalRandom.current().nextFloat(22.0F);
         float yawChange = Mth.clamp(yawDiff, -maxSpeed, maxSpeed);
         float pitchChange = Mth.clamp(pitchDiff, -maxSpeed * 0.78F, maxSpeed * 0.78F);
         float newYaw = currentAngle.getYaw() + yawChange;
         float newPitch = currentAngle.getPitch() + pitchChange;
         float noise = (float)(Math.sin(currentTime / 94.0) * 0.1 + Math.cos(currentTime / 172.0) * 0.07 + Math.sin(currentTime / 311.0) * 0.04);
         noise += (ThreadLocalRandom.current().nextFloat() - 0.5F) * 5.0F;
         float distance = mc.player.distanceTo(livingEntity);
         noise *= distance < 3.0F ? 0.55F : 1.0F;
         newYaw += noise;
         newPitch += noise * 0.68F;
         if (ThreadLocalRandom.current().nextFloat() < 0.07F) {
            newYaw += (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.18F;
            newPitch += (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.12F;
         }

         newPitch = Mth.clamp(newPitch, -89.9F, 89.9F);
         this.rotationTime = currentTime;
         return new Angle(newYaw, newPitch);
      } else {
         return currentAngle;
      }
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.0, 0.0, 0.0);
   }
}

