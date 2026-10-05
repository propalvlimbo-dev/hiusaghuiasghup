package org.xrose.utils.move;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.xrose.utils.combat.aura.AngleConnection;

public final class MoveUtil {
   private static final Minecraft MC = Minecraft.getInstance();

   private MoveUtil() {
   }

   public static boolean hasPlayerMovement() {
      if (MC.player != null && MC.player.input != null) {
         ClientInput input = MC.player.input;
         if (input.hasForwardImpulse()) {
            return true;
         }

         Vec2 movement = input.getMoveVector();
         return movement.x != 0.0F || movement.y != 0.0F;
      } else {
         return false;
      }
   }

   public static void setVelocity(double velocity) {
      if (MC.player != null) {
         double[] direction = calculateDirection(velocity);
         MC.player.setDeltaMovement(direction[0], MC.player.getDeltaMovement().y(), direction[1]);
      }
   }

   public static double[] forward(double distance) {
      if (MC.player != null && MC.player.input != null) {
         Vec2 movement = MC.player.input.getMoveVector();
         return calculateDirection(movement.y, movement.x, distance);
      } else {
         return new double[]{0.0, 0.0};
      }
   }

   public static double[] calculateDirection(double distance) {
      if (MC.player != null && MC.player.input != null) {
         Vec2 movement = MC.player.input.getMoveVector();
         return calculateDirection(movement.y, movement.x, distance);
      } else {
         return new double[]{0.0, 0.0};
      }
   }

   public static double[] calculateDirection(float forward, float sideways, double distance) {
      float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double x = forward * distance * cos + sideways * distance * sin;
      double z = forward * distance * sin - sideways * distance * cos;
      return new double[]{x, z};
   }

   public static boolean moveKeyPressed(int keyNumber) {
      if (MC.options == null) {
         return false;
      }

      boolean w = MC.options.keyUp.isDown();
      boolean a = MC.options.keyLeft.isDown();
      boolean s = MC.options.keyDown.isDown();
      boolean d = MC.options.keyRight.isDown();
      return keyNumber == 0 ? w : (keyNumber == 1 ? a : (keyNumber == 2 ? s : keyNumber == 3 && d));
   }

   public static boolean w() {
      return moveKeyPressed(0);
   }

   public static boolean a() {
      return moveKeyPressed(1);
   }

   public static boolean s() {
      return moveKeyPressed(2);
   }

   public static boolean d() {
      return moveKeyPressed(3);
   }
}

