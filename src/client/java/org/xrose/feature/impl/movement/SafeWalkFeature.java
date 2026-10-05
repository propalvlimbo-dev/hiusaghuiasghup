package org.xrose.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class SafeWalkFeature extends Feature implements MinecraftContext {
   public SafeWalkFeature() {
      super("SafeWalk", "Prevents walking off block edges", FeatureCategory.MOVEMENT, -1);
   }

   @EventTarget
   public void onInput(PlayerInputEvent event) {
      LocalPlayer player = this.player();
      if (player != null && this.level() != null && player.onGround()) {
         BlockPos below = player.blockPosition().below();
         if (this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty()) {
            event.setShift(true);
         }
      }
   }
}

