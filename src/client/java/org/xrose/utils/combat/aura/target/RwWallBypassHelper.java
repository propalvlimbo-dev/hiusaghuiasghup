package org.xrose.utils.combat.aura.target;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.feature.impl.combat.AuraFeature;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class RwWallBypassHelper {
   private RwWallBypassHelper() {
   }

   private static Minecraft mc() {
      return Minecraft.getInstance();
   }

   public static boolean isTargetBehindWall(LivingEntity target) {
      if (target == null || mc().player == null || mc().level == null) {
         return false;
      } else {
         return !mc().player.hasLineOfSight(target) ? true : hasNarrowRwWallGap(target);
      }
   }

   public static boolean hasNarrowRwWallGap(LivingEntity target) {
      if (target != null && mc().player != null && mc().level != null) {
         AuraFeature aura = AuraFeature.getInstance();
         if (aura != null && aura.rwWallBypass.getValue()) {
            Vec3 eyePos = mc().player.getEyePosition();
            AABB box = target.getBoundingBox();
            Vec3 center = box.getCenter();
            double centerX = center.x;
            double centerZ = center.z;
            double height = box.getYsize();
            Vec3[] samplePoints = new Vec3[]{
               center,
               getStableBodyPoint(target),
               new Vec3(centerX, box.minY - 0.08, centerZ),
               new Vec3(centerX, box.maxY + 0.12, centerZ),
               new Vec3(box.minX + 0.04, box.minY + height * 0.55, centerZ),
               new Vec3(box.maxX - 0.04, box.minY + height * 0.55, centerZ),
               new Vec3(centerX, box.minY + height * 0.55, box.minZ + 0.04),
               new Vec3(centerX, box.minY + height * 0.55, box.maxZ - 0.04)
            };
            int missCount = 0;
            int hitCount = 0;

            for (Vec3 point : samplePoints) {
               BlockHitResult result = mc().level.clip(new ClipContext(eyePos, point, Block.COLLIDER, Fluid.NONE, mc().player));
               if (result != null && result.getType() != Type.MISS) {
                  missCount++;
               } else {
                  hitCount++;
               }
            }

            return missCount > 0 && hitCount >= missCount;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static Vec3 getStableBodyPoint(LivingEntity entity) {
      AABB box = entity.getBoundingBox();
      Vec3 center = box.getCenter();
      return new Vec3(center.x, box.minY + box.getYsize() * 0.72, center.z);
   }

   public static EntityHitResult getAttackRaycastResult() {
      if (mc().player != null && mc().level != null) {
         Vec3 from = mc().player.getEyePosition(1.0F);
         Vec3 lookVec = mc().player.getLookAngle();
         AuraFeature aura = AuraFeature.getInstance();
         float range = aura != null ? aura.attackDistance() : 3.0F;
         float rayRange = range * 2.0F;
         Vec3 to = from.add(lookVec.scale(rayRange));
         return ProjectileUtil.getEntityHitResult(
            mc().player,
            from,
            to,
            mc().player.getBoundingBox().expandTowards(lookVec.scale(rayRange)).inflate(1.0),
            e -> e != mc().player && e.isAlive(),
            rayRange * rayRange
         );
      } else {
         return null;
      }
   }

   public static void tryBreakRwWallBlockPacket() {
      AuraFeature aura = AuraFeature.getInstance();
      if (aura != null && aura.rwWallBypass.getValue()) {
         tryBreakWallBlockPacket(AuraFeature.target);
      }
   }

   public static void tryBreakWallBlockPacket(LivingEntity target) {
      if (target != null && mc().player != null && mc().level != null) {
         if (!mc().player.hasLineOfSight(target)) {
            if (mc().player.connection != null) {
               Vec3 eyePos = mc().player.getEyePosition();
               Vec3 targetCenter = target.getBoundingBox().getCenter();
               BlockHitResult hitResult = mc().level.clip(new ClipContext(eyePos, targetCenter, Block.COLLIDER, Fluid.NONE, mc().player));
               if (hitResult != null && hitResult.getType() == Type.BLOCK) {
                  BlockPos blockPos = hitResult.getBlockPos();
                  BlockState blockState = mc().level.getBlockState(blockPos);
                  if (!blockState.isAir()) {
                     if (!(blockState.getDestroySpeed(mc().level, blockPos) < 0.0F)) {
                        Direction direction = hitResult.getDirection() != null ? hitResult.getDirection() : Direction.UP;
                        mc().player.connection.send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction));
                        mc().player.connection.send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction));
                     }
                  }
               }
            }
         }
      }
   }
}

