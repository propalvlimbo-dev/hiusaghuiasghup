package org.xrose.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.impl.LinearConstructor;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.network.PacketUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoExplosionFeature extends Feature {
   private static final int ARM_WINDOW_TICKS = 40;
   public final ModeSetting trigger = this.register(new ModeSetting("Trigger", "Click", "Click", "Look"));
   public final NumberSetting range = this.register(new NumberSetting("Range", 4.5, 2.0, 6.0, 0.1, " blocks"));
   public final NumberSetting hits = this.register(new NumberSetting("Hits", 4.0, 1.0, 10.0, 1.0, ""));
   public final BooleanSetting autoSwitch = this.register(new BooleanSetting("Auto Switch", true));
   public final BooleanSetting rotate = this.register(new BooleanSetting("Rotate", true));
   private BlockPos pendingObsidian;
   private int armWindow;
   private BlockPos placedPos;
   private int remainingHits;
   private int previousSlot = -1;
   private boolean clickLatch;

   public AutoExplosionFeature() {
      super("AutoExplosion", "Auto crystal combo on clicked obsidian", FeatureCategory.COMBAT, -1);
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null && client.level != null && client.gameMode != null) {
         double maxRangeSq = this.range.getFloat() * this.range.getFloat();
         if (!this.attackPlacedCrystal(client, player, maxRangeSq)) {
            if (this.armWindow > 0 && --this.armWindow == 0) {
               this.pendingObsidian = null;
            }

            BlockHitResult blockHit = this.resolveObsidianHit(client, player, maxRangeSq);
            this.armFromCrosshair(client, blockHit);
            if (this.pendingObsidian != null && blockHit != null) {
               if (!client.level.getBlockState(this.pendingObsidian).is(Blocks.OBSIDIAN)) {
                  this.pendingObsidian = null;
               } else {
                  int crystalSlot = findCrystalSlot(player);
                  if (crystalSlot >= 0) {
                     if (!player.getMainHandItem().is(Items.END_CRYSTAL)) {
                        if (!this.autoSwitch.getValue()) {
                           return;
                        }

                        this.switchTo(player, crystalSlot);
                     }

                     Vec3 placementPoint = Vec3.atCenterOf(this.pendingObsidian).add(0.0, 0.5, 0.0);
                     if (this.rotate.getValue()) {
                        aimAt(client, placementPoint);
                     }

                     if (!(player.distanceToSqr(placementPoint) > maxRangeSq)) {
                        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, blockHit);
                        player.swing(InteractionHand.MAIN_HAND);
                        this.placedPos = this.pendingObsidian.above();
                        this.remainingHits = Math.max(1, Math.round(this.hits.getFloat()));
                        this.pendingObsidian = null;
                     }
                  }
               }
            }
         }
      } else {
         this.resetState();
      }
   }

   private boolean attackPlacedCrystal(Minecraft client, LocalPlayer player, double maxRangeSq) {
      if (this.placedPos == null) {
         return false;
      }

      EndCrystal crystal = findCrystalAt(client, this.placedPos);
      if (crystal != null && this.remainingHits > 0 && player.distanceToSqr(crystal) <= maxRangeSq && crystal.isAlive()) {
         if (this.rotate.getValue()) {
            aimAt(client, crystal.getBoundingBox().getCenter());
         }

         client.gameMode.attack(player, crystal);
         player.swing(InteractionHand.MAIN_HAND);
         this.remainingHits--;
         return true;
      } else {
         if (crystal == null || this.remainingHits <= 0) {
            this.placedPos = null;
            this.restoreSlot(player);
         }

         return false;
      }
   }

   private BlockHitResult resolveObsidianHit(Minecraft client, LocalPlayer player, double maxRangeSq) {
      if (client.hitResult instanceof BlockHitResult blockHit) {
         if (!client.level.getBlockState(blockHit.getBlockPos()).is(Blocks.OBSIDIAN)) {
            return null;
         }

         Vec3 center = Vec3.atCenterOf(blockHit.getBlockPos());
         return player.distanceToSqr(center) > maxRangeSq ? null : blockHit;
      } else {
         return null;
      }
   }

   private void armFromCrosshair(Minecraft client, BlockHitResult blockHit) {
      if (blockHit == null || !this.trigger.is("Look")) {
         boolean down = client.options.keyAttack.isDown();
         if (down && !this.clickLatch) {
            this.clickLatch = true;
         } else if (!down) {
            this.clickLatch = false;
            return;
         }

         if (!this.clickLatch || blockHit == null) {
            return;
         }
      }

      this.pendingObsidian = blockHit.getBlockPos();
      this.armWindow = 40;
   }

   private static EndCrystal findCrystalAt(Minecraft client, BlockPos pos) {
      for (Entity entity : client.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal crystal && entity.blockPosition().equals(pos)) {
            return crystal;
         }
      }

      return null;
   }

   private static int findCrystalSlot(LocalPlayer player) {
      for (int slot = 0; slot < 9; slot++) {
         if (player.getInventory().getItem(slot).is(Items.END_CRYSTAL)) {
            return slot;
         }
      }

      return -1;
   }

   private void switchTo(LocalPlayer player, int slot) {
      if (this.previousSlot < 0) {
         this.previousSlot = player.getInventory().getSelectedSlot();
      }

      player.getInventory().setSelectedSlot(slot);
      PacketUtil.sendHeldItemChange(slot);
   }

   private void restoreSlot(LocalPlayer player) {
      if (this.previousSlot >= 0 && this.previousSlot < 9) {
         player.getInventory().setSelectedSlot(this.previousSlot);
         PacketUtil.sendHeldItemChange(this.previousSlot);
      }

      this.previousSlot = -1;
   }

   private static void aimAt(Minecraft client, Vec3 point) {
      Angle angle = MathAngle.fromVec3d(point.subtract(client.player.getEyePosition()));
      AngleConnection.INSTANCE
         .rotateTo(angle, 1, new AngleConfig(new LinearConstructor(), true, false), TaskPriority.HIGH_IMPORTANCE_2, AutoExplosionFeature.class);
   }

   private void resetState() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         this.restoreSlot(player);
      }

      this.pendingObsidian = null;
      this.placedPos = null;
      this.armWindow = 0;
      this.remainingHits = 0;
      this.clickLatch = false;
   }
}

