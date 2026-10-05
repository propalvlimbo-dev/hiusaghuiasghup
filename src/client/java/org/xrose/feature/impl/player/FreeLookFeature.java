package org.xrose.feature.impl.player;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BindSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class FreeLookFeature extends Feature {
   private final BindSetting key = this.register(new BindSetting("Клавиша", 342));
   private boolean holding;
   private boolean engaged;
   private float yaw;
   private float pitch;
   private CameraType perspective;

   public FreeLookFeature() {
      super("FreeLook", "Свободный обзор камеры при удержании клавиши", FeatureCategory.PLAYER, -1);
   }

   public static boolean onMouseTurn(double yawDelta, double pitchDelta) {
      FreeLookFeature feature = FeatureManager.INSTANCE.getEnabled(FreeLookFeature.class);
      if (feature != null && feature.engaged) {
         feature.yaw += (float)yawDelta;
         feature.pitch = Mth.clamp(feature.pitch + (float)pitchDelta, -90.0F, 90.0F);
         return true;
      } else {
         return false;
      }
   }

   public static Float cameraYaw() {
      FreeLookFeature feature = FeatureManager.INSTANCE.getEnabled(FreeLookFeature.class);
      return feature != null && feature.engaged ? feature.yaw : null;
   }

   public static Float cameraPitch() {
      FreeLookFeature feature = FeatureManager.INSTANCE.getEnabled(FreeLookFeature.class);
      return feature != null && feature.engaged ? feature.pitch : null;
   }

   @Override
   protected void onDisable() {
      this.disengage(Minecraft.getInstance());
      this.holding = false;
   }

   @EventTarget
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (this.key.matches(event.getKey())) {
         this.holding = event.getAction() != 0;
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      if (this.key.matchesMouse(event.getButton())) {
         this.holding = event.getAction() != 0;
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player == null) {
         this.disengage(client);
      } else {
         boolean want = this.holding && client.gui.screen() == null;
         if (want) {
            this.engage(client, player);
         } else {
            this.disengage(client);
         }
      }
   }

   private void engage(Minecraft client, LocalPlayer player) {
      if (!this.engaged) {
         this.yaw = player.getYRot();
         this.pitch = player.getXRot();
         this.engaged = true;
      }

      if (this.perspective == null) {
         this.perspective = client.options.getCameraType();
      }

      if (client.options.getCameraType().isFirstPerson()) {
         client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      }
   }

   private void disengage(Minecraft client) {
      if (this.engaged || this.perspective != null) {
         if (this.perspective != null && client.options != null) {
            client.options.setCameraType(this.perspective);
         }

         this.perspective = null;
         this.engaged = false;
      }
   }
}

