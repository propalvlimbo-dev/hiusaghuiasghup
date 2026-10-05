package org.xrose.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ParkourFeature extends Feature {
   private static final double EDGE_PROBE_DEPTH = 0.001;

   public ParkourFeature() {
      super("Parkour", "Automatically jumps at block edges", FeatureCategory.MOVEMENT, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && event.getClient().level != null) {
         if (player.onGround() && !player.isShiftKeyDown() && !player.getAbilities().flying) {
            if (this.isAtEdge(event, player)) {
               player.jumpFromGround();
            }
         }
      }
   }

   private boolean isAtEdge(GameTickEvent event, LocalPlayer player) {
      AABB probe = player.getBoundingBox().move(0.0, -0.001, 0.0);
      return !event.getClient().level.getBlockCollisions(player, probe).iterator().hasNext();
   }
}

