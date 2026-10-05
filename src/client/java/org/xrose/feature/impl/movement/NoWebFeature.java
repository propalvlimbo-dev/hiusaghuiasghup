package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.PlayerTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.move.MoveUtil;
import org.xrose.utils.network.PacketUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class NoWebFeature extends Feature {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Old", "Old", "GrimV2"));
   public final BooleanSetting climbUp = this.register(new BooleanSetting("Climb Up", true));
   public final NumberSetting speed = this.register(new NumberSetting("Speed", 0.1, 0.05, 0.8, 0.05, ""));
   private Vec3 preMotion;
   private boolean wasInWeb;
   private int webTicks;
   private boolean grimFlag;
   private int packetDelay;

   public NoWebFeature() {
      super("No Web", "Removes cobweb slowdown", FeatureCategory.MOVEMENT, -1);
      this.climbUp.visibleWhen(() -> this.mode.is("Old"));
      this.speed.visibleWhen(() -> this.mode.is("GrimV2"));
   }

   @EventTarget
   public void onPlayerTick(PlayerTickEvent event) {
      LocalPlayer player = event.getPlayer();
      if (player != null && !player.isFallFlying() && !player.isInWater()) {
         boolean inWeb = isInWeb(player);
         if (event.isPre()) {
            if (this.mode.is("GrimV2")) {
               if (inWeb) {
                  if (!this.wasInWeb) {
                     this.webTicks = 0;
                     this.grimFlag = false;
                     this.packetDelay = 0;
                  }

                  this.handleGrimV2(player);
               } else {
                  this.webTicks = 0;
                  this.grimFlag = false;
                  this.packetDelay = 0;
               }

               if (this.packetDelay > 0) {
                  this.packetDelay--;
               }
            }

            this.wasInWeb = inWeb;
            this.preMotion = player.getDeltaMovement();
         } else if (event.isPost() && this.wasInWeb && this.preMotion != null && this.mode.is("Old")) {
            double x = clamp(this.preMotion.x * 4.0, 1.5);
            double z = clamp(this.preMotion.z * 4.0, 1.5);
            double y = this.preMotion.y;
            if (y > -0.05 && this.climbUp.getValue() && Minecraft.getInstance().options.keyJump.isDown()) {
               y = 0.28;
            } else if (y < 0.0) {
               y = Math.max(y * 4.0, -0.35);
            }

            player.setDeltaMovement(new Vec3(x, y, z));
            this.preMotion = null;
         }
      }
   }

   private void handleGrimV2(LocalPlayer player) {
      if (player.getFoodData().getFoodLevel() >= 20) {
         if (this.webTicks == 0 && !this.grimFlag && !player.isSwimming()) {
            player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            PacketUtil.sendUseItem(InteractionHand.OFF_HAND, player.getYRot(), player.getXRot());
            this.grimFlag = true;
            this.packetDelay = 2;
         }

         if (this.webTicks > 0 && this.packetDelay == 0) {
            this.webTicks++;
            if (MoveUtil.hasPlayerMovement()) {
               double[] motion = MoveUtil.forward(this.speed.getValue());
               Vec3 delta = player.getDeltaMovement();
               player.setDeltaMovement(delta.add(motion[0], 0.0, motion[1]));
            }

            if (!player.onGround()) {
               Minecraft client = Minecraft.getInstance();
               if (client.options.keyJump.isDown()) {
                  player.setDeltaMovement(player.getDeltaMovement().x, 0.65, player.getDeltaMovement().z);
               } else if (client.options.keyShift.isDown()) {
                  player.setDeltaMovement(player.getDeltaMovement().x, -0.65, player.getDeltaMovement().z);
               }
            }
         } else {
            this.webTicks++;
         }
      }
   }

   @Override
   protected void onDisable() {
      this.preMotion = null;
      this.wasInWeb = false;
      this.webTicks = 0;
      this.grimFlag = false;
      this.packetDelay = 0;
   }

   private static boolean isInWeb(LocalPlayer player) {
      AABB box = player.getBoundingBox();

      for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
         if (player.level().getBlockState(pos).is(Blocks.COBWEB)) {
            return true;
         }
      }

      return false;
   }

   private static double clamp(double value, double limit) {
      return MoveUtil.hasPlayerMovement() ? Math.max(-limit, Math.min(limit, value)) : 0.0;
   }
}

