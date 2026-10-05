package org.xrose.utils.math;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.MinecraftContext;

public final class BestPoint implements MinecraftContext {
   private static final BestPoint CONTEXT = new BestPoint();

   private BestPoint() {
   }

   public static Vec3 getNearestPoint(Entity entity) {
      if (entity != null && mc.player != null) {
         AABB box = entity.getBoundingBox();
         double step = 0.1;
         Vec3 bestVec = null;
         double closestDistance = Double.MAX_VALUE;
         Vec3 eyePos = mc.player.getEyePosition();

         for (double x = box.minX; x <= box.maxX; x += step) {
            for (double y = box.minY; y <= box.maxY; y += step) {
               for (double z = box.minZ; z <= box.maxZ; z += step) {
                  Vec3 sample = new Vec3(x, y, z);
                  double distance = eyePos.distanceTo(sample);
                  if (distance < closestDistance) {
                     closestDistance = distance;
                     bestVec = sample;
                  }
               }
            }
         }

         return bestVec != null ? bestVec : entity.position();
      } else {
         return null;
      }
   }

   public static Vec3 getPoint(Entity target) {
      AABB box = target.getBoundingBox();
      double width = box.maxX - box.minX;
      double height = box.maxY - box.minY;
      double depth = box.maxZ - box.minZ;
      double baseX = box.minX + width / 2.0;
      double baseY = box.minY + height * 0.7;
      double baseZ = box.minZ + depth / 2.0;
      double time = System.currentTimeMillis() / 50.0;
      int id = target.getId();
      double offsetX = Math.sin(time + id) * (width * 0.45);
      double offsetY = Math.cos(time * 0.8 + id) * (height * 0.1);
      double offsetZ = Math.cos(time * 1.2 + id) * (depth * 0.45);
      return new Vec3(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
   }

   public static Vec3 getPoint2(Entity target) {
      AABB box = target.getBoundingBox();
      double width = box.maxX - box.minX;
      double height = box.maxY - box.minY;
      double depth = box.maxZ - box.minZ;
      double baseX = box.minX + width / 2.0;
      double baseY = box.minY + height * 0.65;
      double baseZ = box.minZ + depth / 2.0;
      double time = System.currentTimeMillis() / 65.0;
      int id = target.getId();
      double offsetX = Math.sin(time + id) * (width * 0.7);
      double offsetY = Math.cos(time * 0.8 + id) * (height * 0.4);
      double offsetZ = Math.cos(time * 1.2 + id) * (depth * 0.7);
      return new Vec3(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
   }
}

