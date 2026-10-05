package org.xrose.context;

import lombok.Generated;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class RotationContext implements MinecraftContext {
   private static final float MOUSE_TURN_SCALE = 0.15F;
   private static float freeYaw;
   private static float freePitch;
   private static boolean active;
   private static boolean hasLastAngle;
   private static float serverYaw;
   private static float serverPitch;
   private static float lastYaw;
   private static float lastPitch;
   private static float lastHeadYaw;
   private static float lastBodyYaw;

   private RotationContext() {
   }

   public static void apply(float yawDelta, float pitchDelta) {
      LocalPlayer player = mc.player;
      if (player != null) {
         if (!active) {
            freeYaw = player.getYRot();
            freePitch = player.getXRot();
            serverYaw = player.getYRot();
            serverPitch = player.getXRot();
            active = true;
         }

         rememberRenderAngles(player);
         serverYaw += yawDelta;
         serverPitch = Mth.clamp(serverPitch + pitchDelta, -90.0F, 90.0F);
         player.setYRot(serverYaw);
         player.setXRot(serverPitch);
         player.yHeadRot = serverYaw;
         player.yBodyRot = serverYaw;
      }
   }

   public static void setRotation(float yaw, float pitch) {
      LocalPlayer player = mc.player;
      if (player != null) {
         if (!active) {
            freeYaw = player.getYRot();
            freePitch = player.getXRot();
            serverYaw = player.getYRot();
            serverPitch = player.getXRot();
            active = true;
         }

         float targetPitch = Mth.clamp(pitch, -90.0F, 90.0F);
         float deltaYaw = Mth.wrapDegrees(yaw - serverYaw);
         float deltaPitch = targetPitch - serverPitch;
         double sensitivity = (Double)mc.options.sensitivity().get();
         double f = sensitivity * 0.6 + 0.2;
         double gcdStep = f * f * f * 1.2;
         if (gcdStep >= 1.0E-4) {
            deltaYaw = (float)(Math.round(deltaYaw / gcdStep) * gcdStep);
            deltaPitch = (float)(Math.round(deltaPitch / gcdStep) * gcdStep);
         }

         apply(deltaYaw, deltaPitch);
      }
   }

   public static void rotateTo(LocalPlayer player, Entity target) {
      Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, horizontal)));
      setRotation(yaw, pitch);
   }

   public static void rotateToPosition(LocalPlayer player, Vec3 pos) {
      Vec3 delta = pos.subtract(player.getEyePosition());
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, horizontal)));
      setRotation(yaw, pitch);
   }

   public static float getFovToEntity(LocalPlayer player, Entity entity) {
      double diffX = entity.getX() - player.getX();
      double diffZ = entity.getZ() - player.getZ();
      float yaw = (float)(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
      float deltaYaw = Mth.wrapDegrees(yaw - (active ? serverYaw : player.getYRot()));
      return Math.abs(deltaYaw);
   }

   public static void syncFreeLook(float yaw, float pitch) {
      if (!active) {
         freeYaw = yaw;
         freePitch = pitch;
      }
   }

   public static boolean onMouseTurn(double yawInput, double pitchInput) {
      if (!active) {
         return false;
      }

      freeYaw += (float)yawInput * 0.15F;
      freePitch = Mth.clamp(freePitch + (float)pitchInput * 0.15F, -90.0F, 90.0F);
      return true;
   }

   public static void applyRenderInterpolation() {
      if (active && hasLastAngle) {
         LocalPlayer player = mc.player;
         if (player == null) {
            hasLastAngle = false;
         } else {
            player.yRotO = lastYaw;
            player.xRotO = lastPitch;
            player.yHeadRotO = lastHeadYaw;
            player.yBodyRotO = lastBodyYaw;
         }
      }
   }

   public static void disengage() {
      if (active) {
         LocalPlayer player = mc.player;
         if (player == null) {
            clear();
         } else {
            float deltaYaw = Mth.wrapDegrees(player.getYRot() - serverYaw);
            float deltaPitch = player.getXRot() - serverPitch;
            if (Math.abs(deltaYaw) < 2.0F && Math.abs(deltaPitch) < 2.0F) {
               clear();
            } else {
               rememberRenderAngles(player);
               serverYaw += deltaYaw * 0.4F;
               serverPitch = Mth.clamp(serverPitch + deltaPitch * 0.4F, -90.0F, 90.0F);
               player.setYRot(serverYaw);
               player.setXRot(serverPitch);
               player.yHeadRot = serverYaw;
               player.yBodyRot = serverYaw;
            }
         }
      }
   }

   public static void clear() {
      LocalPlayer player = mc.player;
      active = false;
      hasLastAngle = false;
      if (player != null) {
         serverYaw = player.getYRot();
         serverPitch = player.getXRot();
         freeYaw = player.getYRot();
         freePitch = player.getXRot();
      }
   }

   private static void rememberRenderAngles(LocalPlayer player) {
      lastYaw = player.getYRot();
      lastPitch = player.getXRot();
      lastHeadYaw = player.yHeadRot;
      lastBodyYaw = player.yBodyRot;
      hasLastAngle = true;
   }

   @Generated
   public static float getFreeYaw() {
      return freeYaw;
   }

   @Generated
   public static float getFreePitch() {
      return freePitch;
   }

   @Generated
   public static boolean isActive() {
      return active;
   }

   @Generated
   public static float getServerYaw() {
      return serverYaw;
   }

   @Generated
   public static float getServerPitch() {
      return serverPitch;
   }
}

