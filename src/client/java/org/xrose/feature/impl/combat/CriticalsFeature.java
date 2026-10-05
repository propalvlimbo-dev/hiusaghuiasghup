package org.xrose.feature.impl.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.utils.combat.aura.AngleConnection;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class CriticalsFeature extends Feature {
   private static CriticalsFeature instance;
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "ReallyWorld", "ReallyWorld", "Legit"));

   public CriticalsFeature() {
      super("Criticals", "Forces critical hits.", FeatureCategory.COMBAT, -1);
      instance = this;
   }

   public static CriticalsFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(CriticalsFeature.class);
   }

   public void onAttack() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.level != null && !client.player.isInWater()) {
         if (this.mode.is("Legit")) {
            if (client.player.fallDistance == 0.0 && !client.player.onGround()) {
               sendLegitCrit(client);
            }
         } else if (this.mode.is("ReallyWorld")) {
            boolean hasSlowFalling = client.player.hasEffect(MobEffects.SLOW_FALLING);
            boolean isInCobweb = client.level.getBlockState(client.player.blockPosition()).is(Blocks.COBWEB);
            if (isInCobweb || hasSlowFalling) {
               if (isInCobweb && !client.player.onGround() && client.player.fallDistance == 0.0) {
                  client.player.fallDistance = random(1.0E-5F, 1.0E-4F);
                  client.player
                     .connection
                     .send(
                        new PosRot(
                           client.player.getX(),
                           client.player.getY() - client.player.fallDistance,
                           client.player.getZ(),
                           AngleConnection.INSTANCE.getPacketYaw(),
                           AngleConnection.INSTANCE.getPacketPitch(),
                           false,
                           false
                        )
                     );
               }

               if (hasSlowFalling && !client.player.onGround()) {
                  double y = client.player.getY();
                  if (y != (int)y) {
                     float fallDist = random(1.0E-7F, 1.0E-6F);
                     client.player.fallDistance = fallDist;
                     client.player
                        .connection
                        .send(
                           new PosRot(
                              client.player.getX(),
                              client.player.getY() - fallDist,
                              client.player.getZ(),
                              AngleConnection.INSTANCE.getPacketYaw(),
                              AngleConnection.INSTANCE.getPacketPitch(),
                              false,
                              false
                           )
                        );
                  }
               }
            }
         }
      }
   }

   private static void sendLegitCrit(Minecraft client) {
      client.player.fallDistance = 0.001F;
      client.player
         .connection
         .send(
            new PosRot(
               client.player.getX(),
               client.player.getY() - 9.99999999E-5,
               client.player.getZ(),
               AngleConnection.INSTANCE.getPacketYaw(),
               AngleConnection.INSTANCE.getPacketPitch(),
               false,
               client.player.horizontalCollision
            )
         );
   }

   private static float random(float min, float max) {
      return (float)ThreadLocalRandom.current().nextDouble(min, max);
   }
}

