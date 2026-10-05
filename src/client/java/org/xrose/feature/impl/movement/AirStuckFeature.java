package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AirStuckFeature extends Feature {
   private static final double RELEASE_FALL_VELOCITY = -0.0784;
   private static AirStuckFeature instance;
   public final NumberSetting auraRange = this.register(new NumberSetting("Aura Range", 3.0, 1.0, 6.0, 0.1, ""));
   private Vec3 stuckPosition;

   public AirStuckFeature() {
      super("AirStuck", "Freeze the player in mid-air", FeatureCategory.MOVEMENT, -1);
      instance = this;
   }

   public static AirStuckFeature getInstance() {
      return instance;
   }

   public float getAuraRange() {
      return this.auraRange.getFloat();
   }

   @Override
   protected void onEnable() {
      this.capturePosition();
      this.freezePlayer();
   }

   @Override
   protected void onDisable() {
      this.releasePlayer();
      this.stuckPosition = null;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      this.freezePlayer();
   }

   @EventTarget
   public void onInput(PlayerInputEvent event) {
      event.inputNone();
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (event.getPhase() == PacketSendEvent.Phase.PRE) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
            event.cancel();
         }
      }
   }

   private void freezePlayer() {
      LocalPlayer player = this.player();
      if (player != null && player.level() != null) {
         if (this.stuckPosition == null) {
            this.capturePosition();
         }

         player.setDeltaMovement(Vec3.ZERO);
         player.setPos(this.stuckPosition.x, this.stuckPosition.y, this.stuckPosition.z);
         player.setSprinting(false);
         player.fallDistance = 0.0;
      } else {
         this.stuckPosition = null;
      }
   }

   private void capturePosition() {
      LocalPlayer player = this.player();
      if (player != null) {
         this.stuckPosition = player.position();
      }
   }

   private void releasePlayer() {
      LocalPlayer player = this.player();
      if (player != null && player.level() != null) {
         double fallVelocity = Math.min(player.getDeltaMovement().y, -0.0784);
         double releaseMoveY = this.getReleaseMoveY(player, fallVelocity);
         if (releaseMoveY != 0.0) {
            player.setPos(player.getX(), player.getY() + releaseMoveY, player.getZ());
         }

         player.setDeltaMovement(0.0, fallVelocity, 0.0);
         player.setSprinting(false);
      }
   }

   private double getReleaseMoveY(LocalPlayer player, double moveY) {
      AABB movedBox = player.getBoundingBox().move(0.0, moveY, 0.0);
      return player.level().noCollision(player, movedBox) ? moveY : 0.0;
   }

   private LocalPlayer player() {
      return Minecraft.getInstance().player;
   }
}

