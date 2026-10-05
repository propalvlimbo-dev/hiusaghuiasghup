package org.xrose.feature.impl.movement;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.game.PlayerTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.move.MoveUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class SpeedFeature extends Feature {
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "HolyWorld", "HolyWorld", "Grim", "Vanilla", "FunTime", "Polar"));
   public final NumberSetting collisionRadius = this.register(
      new NumberSetting("Collision Radius", 0.3, 0.0, 1.0, 0.01, "").visibleWhen(() -> this.mode.is("HolyWorld"))
   );
   public final NumberSetting speed = this.register(new NumberSetting("Speed", 0.8, 0.0, 2.0, 0.01, "").visibleWhen(() -> this.mode.is("Vanilla")));
   public final BooleanSetting onlyAura = this.register(new BooleanSetting("Only Aura", true).visibleWhen(() -> this.mode.is("HolyWorld")));
   private int cooldown = 0;
   private boolean suppressSprint = false;
   private double polarTimerAccumulator = 0.0;

   public SpeedFeature() {
      super("Speed", "Changes player movement speed", FeatureCategory.MOVEMENT, -1);
   }

   public static boolean shouldSkipBaseTick() {
      return false;
   }

   public static boolean consumeExtraTick() {
      SpeedFeature speed = FeatureManager.INSTANCE.getEnabled(SpeedFeature.class);
      LocalPlayer player = Minecraft.getInstance().player;
      if (speed == null || player == null || !speed.mode.is("Polar") || !MoveUtil.hasPlayerMovement()) {
         return false;
      }

      if (!player.isFallFlying() && !player.isInWater() && !player.isSwimming() && !player.isUnderWater()) {
         double rate = player.onGround() ? 1.0865 : 1.0213;
         speed.polarTimerAccumulator += rate - 1.0;
         if (speed.polarTimerAccumulator < 1.0) {
            return false;
         }

         speed.polarTimerAccumulator--;
         return true;
      } else {
         speed.polarTimerAccumulator = 0.0;
         return false;
      }
   }

   @EventTarget
   private void onTick(GameTickEvent event) {
      if (this.cooldown > 0) {
         this.cooldown--;
      }

      if (this.mode.is("HolyWorld")) {
         Minecraft client = event.getClient();
         if (client.player != null && client.level != null) {
            if (this.onlyAura.getValue()) {
               AuraFeature aura = FeatureManager.INSTANCE.getEnabled(AuraFeature.class);
               if (aura == null || aura.getTarget() == null || !aura.getTarget().isAlive()) {
                  return;
               }
            }

            this.handleHolyWorldMode(client);
         }
      }
   }

   @EventTarget
   private void onMotion(PlayerTickEvent event) {
      if (event.isPost()) {
         Minecraft client = Minecraft.getInstance();
         LocalPlayer player = event.getPlayer();
         if (player != null && client.level != null && MoveUtil.hasPlayerMovement()) {
            if (this.mode.is("Vanilla")) {
               MoveUtil.setVelocity(this.speed.getValue() / 3.0);
            } else if (this.mode.is("FunTime")) {
               this.handleFunTimeMode(client);
            } else if (this.mode.is("Grim")) {
               this.handleGrimMode(client);
            } else if (this.mode.is("Polar")) {
               this.handlePolarMode(player);
            }
         }
      }
   }

   private void handleHolyWorldMode(Minecraft client) {
      LocalPlayer player = client.player;
      if (this.cooldown <= 0) {
         boolean wasSprinting = player.isSprinting();
         if (wasSprinting) {
            player.setSprinting(false);
         }

         AABB expandedBox = player.getBoundingBox().inflate(this.collisionRadius.getFloat());
         Entity targetEntity = null;
         if (this.onlyAura.getValue()) {
            AuraFeature aura = FeatureManager.INSTANCE.getEnabled(AuraFeature.class);
            LivingEntity auraTarget = aura == null ? null : aura.getTarget();
            if (auraTarget != null && auraTarget.isAlive() && expandedBox.intersects(auraTarget.getBoundingBox())) {
               targetEntity = auraTarget;
            }
         } else {
            for (Entity entity : client.level.entitiesForRendering()) {
               if (entity != player
                  && (entity instanceof LivingEntity || entity.getType().toString().toLowerCase(Locale.ROOT).contains("boat"))
                  && expandedBox.intersects(entity.getBoundingBox())) {
                  targetEntity = entity;
                  break;
               }
            }
         }

         if (wasSprinting) {
            player.setSprinting(true);
         }

         if (targetEntity instanceof Player) {
            Vec3 targetCenter = targetEntity.getBoundingBox().getCenter();
            Vec3 playerPos = player.position();
            double dx = targetCenter.x - playerPos.x;
            double dz = targetCenter.z - playerPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            double maxVelocity = player.isInWater() ? 0.1 : 0.03;
            double clampedVelocity = Math.min(maxVelocity, Math.max(0.0, distance));
            double pushX = 0.0;
            double pushZ = 0.0;
            if (distance > 0.0) {
               pushX = dx / distance * clampedVelocity;
               pushZ = dz / distance * clampedVelocity;
            }

            player.push(pushX, 0.0, pushZ);
            if (wasSprinting) {
               player.push(-pushX, 0.0, -pushZ);
            }

            player.setSprinting(false);
            this.suppressSprint = true;
            if (player.getBoundingBox().intersects(targetEntity.getBoundingBox())) {
               this.cooldown = 3;
            }
         } else {
            this.cooldown = 0;
         }
      }
   }

   private void handleFunTimeMode(Minecraft client) {
      LocalPlayer player = client.player;
      if (!player.isSwimming() && !player.isFallFlying() && !player.isShiftKeyDown() && player.getBoundingBox().getYsize() < 1.5) {
         MoveUtil.setVelocity(player.hasEffect(MobEffects.SPEED) ? 0.32F : 0.28F);
      }
   }

   private void handleGrimMode(Minecraft client) {
      LocalPlayer player = client.player;
      int collisions = 0;

      for (Entity entity : client.level.entitiesForRendering()) {
         if (this.isValidCollisionBoostEntity(client, entity) && player.getBoundingBox().inflate(0.5).intersects(entity.getBoundingBox())) {
            collisions++;
         }
      }

      double[] motion = MoveUtil.forward(0.07 * collisions);
      player.push(motion[0], 0.0, motion[1]);
   }

   private void handlePolarMode(LocalPlayer player) {
      if (player.onGround()) {
         player.jumpFromGround();
         double[] strafe = MoveUtil.forward(0.002);
         player.push(strafe[0], 0.0, strafe[1]);
         Vec3 motion = player.getDeltaMovement();
         player.setDeltaMovement(motion.x * 1.004, motion.y, motion.z * 1.004);
      }
   }

   private boolean isValidCollisionBoostEntity(Minecraft client, Entity entity) {
      return entity != null && entity != client.player && !(entity instanceof ArmorStand)
         ? entity instanceof LivingEntity || entity.getType().toString().toLowerCase(Locale.ROOT).contains("boat")
         : false;
   }

   @Override
   protected void onDisable() {
      this.cooldown = 0;
      this.suppressSprint = false;
      this.polarTimerAccumulator = 0.0;
   }

   @Override
   protected void onEnable() {
      this.cooldown = 0;
      this.suppressSprint = false;
      this.polarTimerAccumulator = 0.0;
   }
}

