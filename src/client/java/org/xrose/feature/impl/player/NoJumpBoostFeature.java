package org.xrose.feature.impl.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class NoJumpBoostFeature extends Feature {
   public NoJumpBoostFeature() {
      super("NoJumpBoost", "Removes jump boost effect", FeatureCategory.PLAYER, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && player.hasEffect(MobEffects.JUMP_BOOST)) {
         player.removeEffect(MobEffects.JUMP_BOOST);
      }
   }
}

