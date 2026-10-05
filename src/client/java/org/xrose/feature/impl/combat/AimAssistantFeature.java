package org.xrose.feature.impl.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.game.RotationUpdateEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.FriendManager;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public final class AimAssistantFeature extends Feature implements MinecraftContext {
   public final MultiSelectSetting targets = this.register(new MultiSelectSetting("Targets", Set.of("Players"), "Players", "Friends", "Animals", "Mobs"));
   public final BooleanSetting throughWalls = this.register(new BooleanSetting("Through Walls", false));
   public final NumberSetting threshold = this.register(new NumberSetting("Threshold", 5.0, 1.0, 5.0, 0.25, ""));
   public final BooleanSetting weaponOnly = this.register(new BooleanSetting("Weapon Only", true));
   private LivingEntity aimTarget;
   private Vec3 smoothed;

   public AimAssistantFeature() {
      super("Aim Assistant", "Smoothly pulls the crosshair toward nearby targets", FeatureCategory.COMBAT, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.isEnabled() && mc.player != null && mc.level != null) {
         TriggerBotFeature trigger = TriggerBotFeature.getEnabled();
         LivingEntity found = trigger != null ? trigger.getCurrentTarget() : null;
         if (found == null) {
            found = this.scanTarget();
         }

         if (found != this.aimTarget) {
            this.smoothed = null;
         }

         this.aimTarget = found;
      } else {
         this.aimTarget = null;
      }
   }

   @EventTarget
   public void onRotationUpdate(RotationUpdateEvent event) {
      if (event.isPre() && this.isEnabled() && this.aimTarget != null && mc.player != null) {
         if (!mc.player.isUsingItem() && !this.auraHasTarget()) {
            if (!this.weaponOnly.getValue() || this.holdingWeapon()) {
               this.applyAim();
            }
         }
      }
   }

   private void applyAim() {
      Vec3 point = this.findAimPoint(this.aimTarget);
      if (point != Vec3.ZERO) {
         this.smoothed = this.smoothed == null ? point : this.smoothed.lerp(point, 0.2);
         Vec3 rel = this.smoothed.subtract(mc.player.getEyePosition());
         float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(rel.z, rel.x)) - 90.0);
         float pitch = (float)(-Math.toDegrees(Math.atan2(rel.y, Math.hypot(rel.x, rel.z))));
         float deltaYaw = Mth.wrapDegrees(yaw - mc.player.getYRot());
         float deltaPitch = pitch - mc.player.getXRot();
         if (Math.abs(deltaPitch) <= 13.0F && Math.abs(deltaYaw) < 8.0F && canHit(mc, this.aimTarget, mc.player.getYRot(), mc.player.getXRot(), 3.0)) {
            deltaPitch = 0.0F;
         }

         float frame = mc.getDeltaTracker().getRealtimeDeltaTicks();
         float ease = Mth.clamp((float)Math.hypot(deltaYaw, deltaPitch) / 4.0F, 0.0F, 1.0F);
         float speed = this.threshold.getFloat() * frame * ease;
         if (!(speed <= 0.0F)) {
            float step = Math.min(1.0F, speed / Math.max(Math.abs(deltaYaw), Math.abs(deltaPitch) * 2.0F));
            mc.player.setYRot(Mth.wrapDegrees(mc.player.getYRot() + deltaYaw * step));
            if (deltaPitch != 0.0F) {
               mc.player.setXRot(Mth.clamp(mc.player.getXRot() + deltaPitch * step, -90.0F, 90.0F));
            }
         }
      }
   }

   private Vec3 findAimPoint(LivingEntity target) {
      AABB box = target.getBoundingBox();
      Vec3 eye = mc.player.getEyePosition();
      double cx = box.getCenter().x;
      double cz = box.getCenter().z;
      List<Vec3> points = new ArrayList<>(45);

      for (int i = 0; i < 9; i++) {
         double y = Mth.lerp(i / 8.0, box.minY, box.maxY);
         points.add(new Vec3(cx, y, cz));
         points.add(new Vec3(box.minX, y, cz));
         points.add(new Vec3(box.maxX, y, cz));
         points.add(new Vec3(cx, y, box.minZ));
         points.add(new Vec3(cx, y, box.maxZ));
      }

      Vec3 best = null;
      double bestDistSq = Double.MAX_VALUE;

      for (Vec3 point : points) {
         if (this.throughWalls.getValue() || clearLine(eye, point)) {
            double distSq = eye.distanceToSqr(point);
            if (distSq < bestDistSq) {
               bestDistSq = distSq;
               best = point;
            }
         }
      }

      return best == null ? Vec3.ZERO : best;
   }

   private LivingEntity scanTarget() {
      if (mc.player != null && mc.level != null) {
         double maxReach = 4.0 + mc.player.getDeltaMovement().length() * 3.0;
         Vec3 eye = mc.player.getEyePosition();
         Vec3 look = Vec3.directionFromRotation(mc.player.getXRot(), mc.player.getYRot());
         LivingEntity best = null;
         double bestAngle = Double.MAX_VALUE;

         for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(128.0))) {
            if (this.isValidTarget(entity, maxReach) && (this.throughWalls.getValue() || clearLine(eye, entity.getBoundingBox().getCenter()))) {
               Vec3 toCenter = entity.getBoundingBox().getCenter().subtract(eye).normalize();
               double angle = Math.acos(Mth.clamp(look.dot(toCenter), -1.0, 1.0));
               if (angle < bestAngle) {
                  bestAngle = angle;
                  best = entity;
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private boolean isValidTarget(LivingEntity entity, double maxReach) {
      if (entity != mc.player && entity.isAlive() && !entity.isRemoved()) {
         if (mc.player.getEyePosition().distanceTo(entity.position()) > maxReach) {
            return false;
         } else if (entity instanceof Player player) {
            boolean friend = FriendManager.INSTANCE.isFriend(player.getGameProfile().name());
            return friend ? this.targets.isSelected("Friends") : this.targets.isSelected("Players");
         } else {
            return entity instanceof Mob ? this.targets.isSelected("Mobs") : entity instanceof Animal && this.targets.isSelected("Animals");
         }
      } else {
         return false;
      }
   }

   private boolean holdingWeapon() {
      Item item = mc.player.getMainHandItem().getItem();
      return mc.player.getMainHandItem().has(DataComponents.WEAPON) || item instanceof AxeItem || item instanceof MaceItem;
   }

   private boolean auraHasTarget() {
      AuraFeature aura = AuraFeature.getEnabled();
      return aura != null && aura.getCurrentTarget() != null;
   }

   private static boolean clearLine(Vec3 from, Vec3 to) {
      Minecraft mc = Minecraft.getInstance();
      return mc.level != null && mc.player != null
         ? mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player)).getType() == Type.MISS
         : false;
   }

   private static boolean canHit(Minecraft client, LivingEntity target, float yaw, float pitch, double reach) {
      if (client.player != null && client.level != null) {
         Vec3 from = client.player.getEyePosition();
         Vec3 dir = Vec3.directionFromRotation(new Vec2(pitch, yaw)).scale(reach);
         AABB box = target.getBoundingBox();
         if (!box.contains(from)) {
            Vec3 end = from.add(dir);
            Vec3 hit = (Vec3)box.clip(from, end).orElse(null);
            if (hit == null) {
               return false;
            }

            if (client.level.clip(new ClipContext(from, hit, Block.COLLIDER, Fluid.NONE, client.player)).getType() != Type.MISS) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }
}

