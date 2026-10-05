package org.xrose.utils.combat.aura.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.xrose.utils.combat.aura.Angle;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public final class LegitCxMode extends DeltaStyleMode {
   public static final LegitCxMode INSTANCE = new LegitCxMode();

   private LegitCxMode() {
      super("Legit CX");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && entity != null) {
         this.updateState(entity, targetAngle.getPitch());
         double reach = this.reach();
         float yawToTarget = targetAngle.getYaw();
         float finalPitch = smoothStep(player.getXRot(), this.historyPitch(this.ticks()), rnd(0.1F, 0.5F));
         float finalYaw = smoothStep(player.getYRot(), yawToTarget, rnd(0.1F, 0.4F));
         if (this.attackWindow() >= 0.0F && this.switchCooldown() <= 0.0F) {
            if (!rayHitsBox(player, entity, player.getYRot(), player.getXRot(), reach)) {
               finalYaw = yawToTarget;
            }

            if (!rayHitsBox(player, entity, yawToTarget, finalPitch, reach)) {
               finalPitch = targetAngle.getPitch();
            }
         }

         if (this.ticks() <= 4 && this.parity() % 2 == 0) {
            finalYaw = player.getYRot();
         }

         float serverYaw = serverAngle(false, player);
         boolean needAuto = Math.abs(Mth.wrapDegrees(yawToTarget - serverYaw)) > 15.0F;
         float desiredYaw = needAuto ? finalYaw : serverYaw;
         float desiredPitch = finalPitch;
         Vec3 eye = player.getEyePosition();
         float minPitch = 90.0F;
         float maxPitch = -90.0F;
         float minYaw = 180.0F;
         float maxYaw = -180.0F;

         for (double x : CORNERS(entity.getBoundingBox().minX, entity.getBoundingBox().maxX)) {
            for (double y : CORNERS(entity.getBoundingBox().minY, entity.getBoundingBox().maxY)) {
               for (double z : CORNERS(entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ)) {
                  double dx = x - eye.x;
                  double dy = y - eye.y;
                  double dz = z - eye.z;
                  minPitch = Math.min(minPitch, (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)))));
                  maxPitch = Math.max(maxPitch, (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)))));
                  float yaw = Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
                  minYaw = Math.min(minYaw, yaw);
                  maxYaw = Math.max(maxYaw, yaw);
               }
            }
         }

         if (minPitch < maxPitch) {
            float inset = (maxPitch - minPitch) * 0.05F;
            float clampedMin = minPitch + inset;
            float clampedMax = maxPitch - inset;
            if (clampedMin > clampedMax) {
               float mid = (minPitch + maxPitch) * 0.5F;
               clampedMin = mid - 0.1F;
               clampedMax = mid + 0.1F;
            }

            desiredPitch = Mth.clamp(desiredPitch, clampedMin, clampedMax);
            desiredPitch = Mth.clamp(desiredPitch, -90.0F, 90.0F);
            if (minYaw < maxYaw) {
               float half = Math.max((maxYaw - minYaw) * 0.95F * 0.5F, 0.1F);
               float center = (minYaw + maxYaw) * 0.5F;
               float rel = Mth.wrapDegrees(Mth.wrapDegrees(desiredYaw) - center);
               desiredYaw = Mth.wrapDegrees(center + Mth.clamp(rel, -half, half));
            }
         }

         return limitSpeed(currentAngle, new Angle(desiredYaw, desiredPitch), 220.0F);
      } else {
         return currentAngle;
      }
   }

   private static double[] CORNERS(double min, double max) {
      return new double[]{min, max};
   }
}

