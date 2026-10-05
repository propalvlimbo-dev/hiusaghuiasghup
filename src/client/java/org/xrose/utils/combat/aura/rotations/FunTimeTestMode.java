package org.xrose.utils.combat.aura.rotations;

import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.impl.RotateConstructor;

public final class FunTimeTestMode extends RotateConstructor {
   public static final FunTimeTestMode INSTANCE = new FunTimeTestMode();
   private final SecureRandom random = new SecureRandom();
   private final float[] flags = new float[11];
   private long lastElapsed = 0L;

   private FunTimeTestMode() {
      super("FunTimeTest");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      AuraFeature aura = AuraFeature.getInstance();
      StrikeManager attackHandler = aura.getAttackPerpetrator().getAttackHandler();
      float t = mc.player.tickCount + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      float yawNoise = (float)(Math.sin(t * 0.8F) * 11.0 + Math.sin(t * 0.04 + 17.2) * 1.5 + Math.sin(t * 0.11 + 5.8) * 3.0 + Math.sin(t * 0.07 + 12.3) * 1.0);
      float pitchNoise = (float)(Math.sin(t * 0.1) * 1.0 + Math.sin(t * 0.03 + 54.1) * 0.5);
      boolean paused = mc.gui.screen() != null;
      if (this.canAttackNextTick(attackHandler, entity, paused) || entity != null && attackHandler.canAttack(aura.getConfig(), 1)) {
         this.flags[4] = 1.0F;
      }

      long elapsed = attackHandler.getAttackTimer().elapsedTime();
      if (this.lastElapsed > elapsed) {
         this.flags[5] = this.randomInt(7, 10);
         this.flags[4] = 0.0F;
      }

      this.lastElapsed = elapsed;
      float naturalYaw = mc.player.getYRot();
      float naturalPitch = mc.player.getXRot();
      float step = (float)elapsed <= 400.0F ? Math.min((float)elapsed / 400.0F, 0.2F) : Math.min((float)elapsed / 550.0F, 0.35F);
      float pitch = smoothStep(naturalPitch, targetAngle.getPitch(), step);
      float yaw;
      if (this.flags[4] > 0.0F) {
         yawNoise /= 3.0F;
         pitchNoise /= 3.0F;
         yaw = targetAngle.getYaw();
         if (!this.rayTraceVisible(targetAngle.getYaw(), naturalPitch, 3.0F, entity, true)
            && this.rayTraceVisible(targetAngle.getYaw(), targetAngle.getPitch(), 3.0F, entity, true)
            && attackHandler.getAttackTimer().finished(500.0)) {
            pitch = targetAngle.getPitch();
         }
      } else {
         yaw = naturalYaw;
      }

      if (this.flags[5] > 0.0F) {
         yawNoise *= 1.5F;
         yaw = naturalYaw;
      }

      this.flags[4]--;
      this.flags[5]--;
      float outYaw = yaw + yawNoise;
      float outPitch = pitch + pitchNoise;
      float yawDelta = Mth.wrapDegrees(outYaw - naturalYaw);
      float pitchDelta = outPitch - naturalPitch;
      float total = Math.abs(yawDelta) + Math.abs(pitchDelta);
      if (total > 160.0F) {
         float yawCap = Math.abs(yawDelta / total) * 160.0F;
         float pitchCap = Math.abs(pitchDelta / total) * 160.0F;
         outYaw = naturalYaw + Mth.clamp(yawDelta, -yawCap, yawCap);
         outPitch = naturalPitch + Mth.clamp(pitchDelta, -pitchCap, pitchCap);
      }

      return new Angle(outYaw, outPitch);
   }

   private boolean canAttackNextTick(StrikeManager attackHandler, Entity entity, boolean paused) {
      if (!paused && entity != null) {
         Minecraft mc = Minecraft.getInstance();
         if (attackHandler.getAttackTimer().elapsedTime() < 480L) {
            return false;
         } else if (mc.player.getEyePosition().distanceTo(entity.getEyePosition()) > 3.0) {
            return false;
         } else {
            return mc.player.getAttackStrengthScale(0.5F) <= 0.7F ? false : this.willFallNextTick(mc);
         }
      } else {
         return false;
      }
   }

   private boolean willFallNextTick(Minecraft mc) {
      double velY = (mc.player.getDeltaMovement().y - 0.08) * 0.98;
      if (velY >= 0.0) {
         return false;
      }

      AABB box = mc.player.getBoundingBox().move(0.0, velY, 0.0);
      return !mc.level.getCollisions(mc.player, box).iterator().hasNext();
   }

   private boolean rayTraceVisible(float yaw, float pitch, float range, Entity entity, boolean walls) {
      if (walls) {
         return true;
      }

      Minecraft mc = Minecraft.getInstance();
      Vec3 from = mc.player.getEyePosition(1.0F);
      Vec3 dir = new Angle(yaw, pitch).toVector();
      Vec3 toTarget = entity.getBoundingBox().getCenter().subtract(from);
      double targetDist = toTarget.length();
      Vec3 end = from.add(dir.scale(targetDist));
      BlockHitResult hit = mc.level.clip(new ClipContext(from, end, Block.COLLIDER, Fluid.NONE, mc.player));
      return hit.getType() == Type.MISS || hit.getLocation().distanceTo(from) >= targetDist - 0.1;
   }

   private static float smoothStep(float from, float to, float delta) {
      float step = Mth.clamp(delta, 0.0F, 1.0F);
      float diff = Mth.wrapDegrees(to - from);
      if (Math.abs(diff) < 0.5F) {
         return to;
      }

      float value = Mth.wrapDegrees(from + diff * step);
      return Math.abs(Mth.wrapDegrees(to - value)) < 0.5F ? to : value;
   }

   private int randomInt(int min, int max) {
      return min + this.random.nextInt(max - min + 1);
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}

