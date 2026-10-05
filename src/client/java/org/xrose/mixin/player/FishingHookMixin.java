package org.xrose.mixin.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.feature.impl.movement.NoPushFeature;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
   @Inject(method = "pullEntity(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void cancelFishingHookPull(Entity entity, CallbackInfo ci) {
      if (NoPushFeature.shouldCancelFishingHookPull((FishingHook)(Object)this, entity)) {
         ci.cancel();
      }
   }
}

