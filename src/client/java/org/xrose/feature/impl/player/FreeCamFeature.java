package org.xrose.feature.impl.player;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.move.MoveUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class FreeCamFeature extends Feature {
   public static final int FAKE_PLAYER_ENTITY_ID = -81374659;
   private static FreeCamFeature instance;
   public final NumberSetting speedSetting = this.register(new NumberSetting("Speed", 2.0, 0.5, 5.0, 0.1, ""));
   public final BooleanSetting freezeSetting = this.register(new BooleanSetting("Freeze", false));
   public Vec3 pos;
   public Vec3 prevPos;
   private RemotePlayer fakePlayerEntity;

   public FreeCamFeature() {
      super("Free Cam", "Detached free camera", FeatureCategory.PLAYER, -1);
      instance = this;
   }

   public static FreeCamFeature getInstance() {
      return instance;
   }

   public static Vec3 getInterpolatedCameraPosition() {
      FreeCamFeature cam = instance;
      if (cam != null && cam.pos != null) {
         Vec3 prev = cam.prevPos;
         Vec3 current = cam.pos;
         if (isFinite(prev) && isFinite(current)) {
            float tickDelta = Mth.clamp(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), 0.0F, 1.0F);
            return prev.lerp(current, tickDelta);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean hasDetachedCameraState() {
      return isFinite(this.pos) || isFinite(this.prevPos) || this.fakePlayerEntity != null;
   }

   @Override
   protected void onEnable() {
      Minecraft client = Minecraft.getInstance();
      Vec3 cameraPos = client.gameRenderer != null && client.gameRenderer.mainCamera() != null
         ? client.gameRenderer.mainCamera().position()
         : (client.player != null ? client.player.getEyePosition() : Vec3.ZERO);
      this.prevPos = this.pos = isFinite(cameraPos) ? cameraPos : Vec3.ZERO;
      this.spawnFakePlayerEntity(client);
   }

   @Override
   protected void onDisable() {
      this.resetRuntimeState();
      this.removeFakePlayerEntity(Minecraft.getInstance());
   }

   @EventTarget
   private void onPacketSend(PacketSendEvent event) {
      if (event.getPacket() instanceof ServerboundMovePlayerPacket && this.freezeSetting.getValue()) {
         event.cancel();
      }
   }

   @EventTarget
   private void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPacket() instanceof ClientboundRespawnPacket || event.getPacket() instanceof ClientboundLoginPacket) {
         this.setEnabled(false);
      }
   }

   @EventTarget
   private void onInput(PlayerInputEvent event) {
      if (this.pos != null) {
         float speed = this.speedSetting.getFloat();
         Vec2 movement = event.getMoveVector();
         double[] motion = MoveUtil.calculateDirection(movement.y, movement.x, speed);
         this.prevPos = this.pos;
         this.pos = this.pos.add(motion[0], event.getKeyPresses().jump() ? speed : (event.getKeyPresses().shift() ? -speed : 0.0), motion[1]);
         event.inputNone();
      }
   }

   @EventTarget
   private void onTick(GameTickEvent event) {
      if (event.getClient().options != null) {
         event.getClient().options.setCameraType(CameraType.FIRST_PERSON);
      }
   }

   public void resetRuntimeState() {
      this.pos = null;
      this.prevPos = null;
   }

   private void spawnFakePlayerEntity(Minecraft client) {
      if (client.player != null && client.level != null) {
         this.removeFakePlayerEntity(client);
         GameProfile profile = new GameProfile(UUID.randomUUID(), resolveProfileName(client.player.getGameProfile()));
         RemotePlayer fake = new RemotePlayer(client.level, profile);
         fake.setId(-81374659);
         fake.copyPosition(client.player);
         fake.setYRot(client.player.getYRot());
         fake.setXRot(client.player.getXRot());
         fake.setYHeadRot(client.player.getYHeadRot());
         fake.setSprinting(client.player.isSprinting());
         fake.setShiftKeyDown(client.player.isShiftKeyDown());
         fake.setPose(client.player.getPose());
         fake.setHealth(client.player.getHealth());
         fake.setAbsorptionAmount(client.player.getAbsorptionAmount());

         for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            fake.getInventory().setItem(i, client.player.getInventory().getItem(i).copy());
         }

         fake.getInventory().setSelectedSlot(client.player.getInventory().getSelectedSlot());
         client.level.addEntity(fake);
         this.fakePlayerEntity = fake;
      }
   }

   private void removeFakePlayerEntity(Minecraft client) {
      if (client.level != null) {
         client.level.removeEntity(-81374659, RemovalReason.DISCARDED);
         if (this.fakePlayerEntity != null && this.fakePlayerEntity.getId() != -81374659) {
            client.level.removeEntity(this.fakePlayerEntity.getId(), RemovalReason.DISCARDED);
         }
      }

      if (this.fakePlayerEntity != null) {
         this.fakePlayerEntity.discard();
      }

      this.fakePlayerEntity = null;
   }

   private static boolean isFinite(Vec3 vec) {
      return vec != null && Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
   }

   private static String resolveProfileName(GameProfile profile) {
      Object value = invokeProfileMethod(profile, "name");
      if (!(value instanceof String)) {
         value = invokeProfileMethod(profile, "getName");
      }

      return value instanceof String name ? name : "FreeCam";
   }

   private static Object invokeProfileMethod(GameProfile profile, String methodName) {
      try {
         return GameProfile.class.getMethod(methodName).invoke(profile);
      } catch (ReflectiveOperationException ignored) {
         return null;
      }
   }
}

