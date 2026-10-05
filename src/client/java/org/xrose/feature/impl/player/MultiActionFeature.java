package org.xrose.feature.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class MultiActionFeature extends Feature {
   public MultiActionFeature() {
      super("MultiAction", "Attack and mine while using items", FeatureCategory.PLAYER, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null && client.level != null && client.gameMode != null) {
         if (player.isUsingItem() && client.options.keyAttack.isDown()) {
            HitResult hit = client.hitResult;
            if (hit instanceof EntityHitResult entityHit) {
               if (player.getAttackStrengthScale(0.0F) >= 1.0F) {
                  client.gameMode.attack(player, entityHit.getEntity());
                  player.swing(InteractionHand.MAIN_HAND);
               }
            } else {
               if (hit instanceof BlockHitResult blockHit && hit.getType() == Type.BLOCK) {
                  client.gameMode.continueDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
               }

               player.swing(InteractionHand.MAIN_HAND);
            }
         }
      }
   }
}

