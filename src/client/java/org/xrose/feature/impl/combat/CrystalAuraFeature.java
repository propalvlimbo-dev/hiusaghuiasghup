package org.xrose.feature.impl.combat;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.FriendManager;
import org.xrose.utils.combat.StopWatch;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.impl.LinearConstructor;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.network.PacketUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class CrystalAuraFeature extends Feature {
   private static final double EXPLOSION_POWER = 6.0;
   private static final float PLACE_ANGLE_TOLERANCE = 12.0F;
   private final NumberSetting targetRange = this.register(new NumberSetting("Target Range", 10.0, 3.0, 16.0, 0.5, " blocks"));
   private final NumberSetting placeRange = this.register(new NumberSetting("Place Range", 4.5, 1.0, 6.0, 0.1, " blocks"));
   private final NumberSetting hitRange = this.register(new NumberSetting("Hit Range", 4.0, 2.0, 6.0, 0.1, " blocks"));
   private final NumberSetting minDamage = this.register(new NumberSetting("Min Damage", 6.0, 0.5, 20.0, 0.5, " dmg"));
   private final NumberSetting maxSelfDamage = this.register(new NumberSetting("Max Self Damage", 8.0, 0.5, 20.0, 0.5, " dmg"));
   private final NumberSetting placeDelay = this.register(new NumberSetting("Place Delay", 200.0, 0.0, 1000.0, 10.0, " ms"));
   private final MultiSelectSetting options = this.register(
      new MultiSelectSetting("Options", List.of("Auto Switch", "Sync Cooldown", "No Suicide"), "Auto Switch", "Ignore Walls", "Sync Cooldown", "No Suicide")
   );
   private final StopWatch placeTimer = new StopWatch();
   private LivingEntity currentTarget;
   private Vec3 pendingPlacement;
   private int restoreSlot = -1;

   public CrystalAuraFeature() {
      super("CrystalAura", "Places and detonates end crystals.", FeatureCategory.COMBAT, -1);
   }

   public static CrystalAuraFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(CrystalAuraFeature.class);
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @EventTarget
   public void onWorldLeave(WorldLeaveEvent event) {
      this.resetState();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft mc = event.getClient();
      LocalPlayer player = mc.player;
      ClientLevel level = mc.level;
      if (player != null && level != null && mc.gameMode != null) {
         if (this.restoreSlot != -1 && player.getInventory().getSelectedSlot() != this.restoreSlot) {
            switchSlot(player, this.restoreSlot);
            this.restoreSlot = -1;
         }

         this.currentTarget = this.findTarget(player, level);
         if (this.currentTarget == null) {
            this.pendingPlacement = null;
         } else {
            if (!this.attackBestCrystal(mc, player)) {
               this.tryPlaceCrystal(mc, player, level);
            }
         }
      }
   }

   private void resetState() {
      this.currentTarget = null;
      this.pendingPlacement = null;
      this.restoreSlot = -1;
   }

   private LivingEntity findTarget(LocalPlayer player, ClientLevel level) {
      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;
      double rangeSq = this.targetRange.getValue() * this.targetRange.getValue();

      for (Player candidate : level.players()) {
         if (candidate != player
            && candidate.isAlive()
            && !candidate.isSpectator()
            && !candidate.isCreative()
            && !FriendManager.INSTANCE.isFriend(candidate.getGameProfile().name())) {
            double distSq = player.distanceToSqr(candidate);
            if (!(distSq > rangeSq) && !(distSq >= bestDist)) {
               best = candidate;
               bestDist = distSq;
            }
         }
      }

      return best;
   }

   private boolean attackBestCrystal(Minecraft mc, LocalPlayer player) {
      LivingEntity target = this.currentTarget;
      if (target == null) {
         return false;
      }

      if (this.options.isSelected("Sync Cooldown") && player.getAttackStrengthScale(0.0F) < 0.9F) {
         return false;
      }

      EndCrystal best = null;
      float bestDamage = -1.0F;

      for (EndCrystal crystal : mc.level.getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(this.hitRange.getValue()))) {
         if (crystal.isAlive()) {
            Vec3 center = crystal.position();
            float selfDamage = explosionDamage(player, center);
            if (this.selfDamageAllowed(player, selfDamage)) {
               float damage = explosionDamage(target, center);
               if (!(damage < this.minDamage.getValue().floatValue()) && !(damage <= bestDamage)) {
                  best = crystal;
                  bestDamage = damage;
               }
            }
         }
      }

      if (best == null) {
         return false;
      }

      mc.gameMode.attack(player, best);
      player.swing(InteractionHand.MAIN_HAND);
      return true;
   }

   private void tryPlaceCrystal(Minecraft mc, LocalPlayer player, ClientLevel level) {
      if (this.placeTimer.finished(this.placeDelay.getValue())) {
         CrystalAuraFeature.Placement placement = this.findBestPlacement(player, level);
         if (placement != null) {
            this.rotateTo(mc, player, placement.facePoint());
            if (!(angleTo(player, placement.facePoint()) > 12.0F)) {
               if (this.switchToCrystal(mc, player)) {
                  BlockHitResult hit = new BlockHitResult(placement.facePoint(), Direction.UP, placement.support(), false);
                  mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                  player.swing(InteractionHand.MAIN_HAND);
                  this.finishSwitch(player);
                  this.placeTimer.reset();
               }
            }
         }
      }
   }

   private CrystalAuraFeature.Placement findBestPlacement(LocalPlayer player, ClientLevel level) {
      LivingEntity target = this.currentTarget;
      if (target == null) {
         return null;
      }

      int r = Mth.ceil(this.placeRange.getValue());
      BlockPos playerPos = player.blockPosition();
      boolean ignoreWalls = this.options.isSelected("Ignore Walls");
      CrystalAuraFeature.Placement best = null;
      float bestDamage = Math.max(this.minDamage.getValue().floatValue(), 0.0F);

      for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-r, -2, -r), playerPos.offset(r, 2, r))) {
         if (this.canPlaceCrystal(level, pos, ignoreWalls)) {
            Vec3 explosion = Vec3.atLowerCornerOf(pos).add(0.5, 1.0, 0.5);
            if ((ignoreWalls || this.isVisible(player, explosion))
               && (
                  !(player.position().distanceTo(explosion) > this.placeRange.getValue())
                     || !(player.getEyePosition().distanceTo(explosion) > this.placeRange.getValue())
               )) {
               float selfDamage = explosionDamage(player, explosion);
               if (this.selfDamageAllowed(player, selfDamage)) {
                  float damage = explosionDamage(target, explosion);
                  if (!(damage <= bestDamage)) {
                     best = new CrystalAuraFeature.Placement(pos.immutable(), explosion);
                     bestDamage = damage;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean canPlaceCrystal(ClientLevel level, BlockPos pos, boolean ignoreWalls) {
      BlockState state = level.getBlockState(pos);
      if (!state.isAir() && !state.canBeReplaced()) {
         return false;
      }

      BlockState support = level.getBlockState(pos.below());
      if (!support.is(Blocks.OBSIDIAN) && !support.is(Blocks.CRYING_OBSIDIAN) && !support.is(Blocks.BEDROCK)) {
         return false;
      }

      AABB box = new AABB(pos);

      for (Entity entity : level.getEntities(null, box.inflate(0.05))) {
         if (entity instanceof LivingEntity living && living.isAlive()) {
            return false;
         }
      }

      return true;
   }

   private boolean isVisible(LocalPlayer player, Vec3 point) {
      HitResult result = Minecraft.getInstance().level.clip(new ClipContext(player.getEyePosition(), point, Block.COLLIDER, Fluid.NONE, player));
      return result.getType() == Type.MISS;
   }

   private void rotateTo(Minecraft mc, LocalPlayer player, Vec3 point) {
      double dx = point.x - player.getX();
      double dy = point.y - (player.getY() + player.getEyeHeight());
      double dz = point.z - player.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
      AngleConnection.INSTANCE.rotateTo(new Angle(yaw, pitch), 1, new AngleConfig(new LinearConstructor(), true, true), TaskPriority.HIGH_IMPORTANCE_2, this);
   }

   private static float angleTo(LocalPlayer player, Vec3 point) {
      double dx = point.x - player.getX();
      double dy = point.y - (player.getY() + player.getEyeHeight());
      double dz = point.z - player.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
      float dYaw = Math.abs(Mth.wrapDegrees(yaw - player.getYRot()));
      float dPitch = Math.abs(pitch - player.getXRot());
      return Math.max(dYaw, dPitch);
   }

   private boolean switchToCrystal(Minecraft mc, LocalPlayer player) {
      if (player.getMainHandItem().is(Items.END_CRYSTAL)) {
         return true;
      }

      if (!this.options.isSelected("Auto Switch")) {
         return false;
      }

      int crystalSlot = -1;

      for (int slot = 0; slot < 9; slot++) {
         if (player.getInventory().getItem(slot).is(Items.END_CRYSTAL)) {
            crystalSlot = slot;
            break;
         }
      }

      if (crystalSlot == -1) {
         return false;
      }

      this.restoreSlot = player.getInventory().getSelectedSlot();
      switchSlot(player, crystalSlot);
      return true;
   }

   private void finishSwitch(LocalPlayer player) {
      if (this.restoreSlot != -1) {
         switchSlot(player, this.restoreSlot);
         this.restoreSlot = -1;
      }
   }

   private static void switchSlot(LocalPlayer player, int slot) {
      if (slot >= 0 && slot <= 8 && player.getInventory().getSelectedSlot() != slot) {
         player.getInventory().setSelectedSlot(slot);
         PacketUtil.sendHeldItemChange(slot);
      }
   }

   private boolean selfDamageAllowed(LocalPlayer player, float selfDamage) {
      return selfDamage > this.maxSelfDamage.getValue().floatValue() ? false : !this.options.isSelected("No Suicide") || selfDamage < player.getHealth() - 0.5F;
   }

   private static float explosionDamage(LivingEntity target, Vec3 explosion) {
      Vec3 center = target.getBoundingBox().getCenter();
      double dist = center.distanceTo(explosion);
      double radius = 12.0;
      if (dist >= radius) {
         return 0.0F;
      }

      double impact = (1.0 - dist / radius) * exposure(target, explosion, center);
      double raw = (impact * impact + impact) / 2.0 * 7.0 * 6.0 * 2.0 + 1.0;
      float armor = target.getArmorValue();
      float toughness = (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      float f = 2.0F + toughness / 4.0F;
      float clamped = Mth.clamp(armor - (float)raw / f, armor * 0.2F, 20.0F);
      return (float)raw * (1.0F - clamped / 25.0F);
   }

   private static float exposure(LivingEntity target, Vec3 explosion, Vec3 center) {
      HitResult result = target.level().clip(new ClipContext(explosion, center, Block.COLLIDER, Fluid.NONE, target));
      return result.getType() == Type.MISS ? 1.0F : 0.7F;
   }

   private record Placement(BlockPos support, Vec3 facePoint) {
   }
}

