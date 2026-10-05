package org.xrose.utils.combat.aura.rotations;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.RotationContext;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
abstract class DeltaStyleMode extends RotateConstructor {
   private static final float DECAY_FLOOR = -90.0F;
   private final float[] pitchHistory = new float[30];
   private int lastTargetId = Integer.MIN_VALUE;
   private int ticks;
   private int parity;
   private float attackWindow = -1.0F;
   private float switchCooldown = -1.0F;

   protected DeltaStyleMode(String name) {
      super(name);
   }

   protected final void updateState(Entity entity, float aimPitch) {
      Minecraft mc = Minecraft.getInstance();
      int id = entity != null ? entity.getId() : Integer.MIN_VALUE;
      if (id != this.lastTargetId) {
         this.lastTargetId = id;
         this.ticks = 0;
         this.attackWindow = -1.0F;
         this.switchCooldown = 2.0F;
         this.parity = ThreadLocalRandom.current().nextInt(-10, 11);
         Arrays.fill(this.pitchHistory, mc.player != null ? mc.player.getXRot() : 0.0F);
      } else {
         this.ticks++;
         this.parity++;
         this.switchCooldown = Math.max(-90.0F, this.switchCooldown - 1.0F);
      }

      System.arraycopy(this.pitchHistory, 0, this.pitchHistory, 1, this.pitchHistory.length - 1);
      this.pitchHistory[0] = aimPitch;
      AuraFeature aura = AuraFeature.getInstance();
      if (entity != null && aura != null && aura.getAttackPerpetrator().getAttackHandler().canAttack(aura.getConfig(), 1)) {
         this.attackWindow = 1.0F;
      } else {
         this.attackWindow = Math.max(-90.0F, this.attackWindow - 1.0F);
      }
   }

   protected final int ticks() {
      return this.ticks;
   }

   protected final int parity() {
      return this.parity;
   }

   protected final float attackWindow() {
      return this.attackWindow;
   }

   protected final float switchCooldown() {
      return this.switchCooldown;
   }

   protected final float historyPitch(int ticksSinceAcquire) {
      return this.pitchHistory[Mth.clamp(10 - ticksSinceAcquire, 0, this.pitchHistory.length - 1)];
   }

   protected final double reach() {
      AuraFeature aura = AuraFeature.getInstance();
      return aura != null ? aura.attackDistance() : 3.0;
   }

   protected static boolean rayHitsBox(LocalPlayer player, Entity entity, float yaw, float pitch, double reach) {
      Vec3 from = player.getEyePosition();
      Vec3 dir = Vec3.directionFromRotation(new Vec2(pitch, yaw)).scale(reach);
      AABB box = entity.getBoundingBox();
      return box.contains(from) || box.clip(from, from.add(dir)).isPresent();
   }

   protected static float smoothStep(float start, float end, float amount) {
      float step = Mth.clamp(amount, 0.0F, 1.0F);
      float diff = Mth.wrapDegrees(end - start);
      if (Math.abs(diff) < 0.5F) {
         return end;
      }

      float value = Mth.wrapDegrees(start + diff * step);
      return Math.abs(Mth.wrapDegrees(end - value)) < 0.5F ? end : value;
   }

   protected static float serverAngle(boolean pitch, LocalPlayer player) {
      return RotationContext.isActive()
         ? (pitch ? RotationContext.getServerPitch() : RotationContext.getServerYaw())
         : (pitch ? player.getXRot() : player.getYRot());
   }

   protected static float rnd(float min, float max) {
      return ThreadLocalRandom.current().nextFloat(min, max);
   }

   protected static Angle limitSpeed(Angle from, Angle to, float degreesPerSecond) {
      float maxStep = degreesPerSecond / 20.0F;
      float yaw = from.getYaw() + Mth.clamp(Mth.wrapDegrees(to.getYaw() - from.getYaw()), -maxStep, maxStep);
      float pitch = Mth.clamp(from.getPitch() + Mth.clamp(to.getPitch() - from.getPitch(), -maxStep, maxStep), -90.0F, 90.0F);
      return new Angle(yaw, pitch);
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}

