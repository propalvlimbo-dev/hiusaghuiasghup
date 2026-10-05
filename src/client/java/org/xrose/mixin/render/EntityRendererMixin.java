package org.xrose.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.feature.impl.visual.KillEffectFeature;
import org.xrose.feature.impl.visual.NameTagsFeature;
import org.xrose.utils.text.NameProtectUtil;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
   @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
   private void protectNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
      if (entity instanceof Player && NameTagsFeature.shouldHideVanillaTag()) {
         cir.setReturnValue(null);
      } else {
         cir.setReturnValue(NameProtectUtil.protect((Component)cir.getReturnValue()));
      }
   }

   @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
   private void killEffectHideCorpse(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
      if (entity instanceof LivingEntity) {
         KillEffectFeature killEffect = KillEffectFeature.getEnabled();
         if (killEffect != null && killEffect.hidesEntity(entity)) {
            cir.setReturnValue(false);
         }
      }
   }
}

