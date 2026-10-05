package org.xrose.mixin.gui;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.visual.HudFeature;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
   @Inject(method = "getScale", at = @At("HEAD"), cancellable = true)
   private void getScale(CallbackInfoReturnable<Double> cir) {
      try {
         FeatureManager fm = FeatureManager.INSTANCE;
         if (fm == null) {
            return;
         }

         HudFeature hud = fm.getFeature(HudFeature.class);
         if (hud != null && hud.isEnabled()) {
            cir.setReturnValue((double)HudFeature.chatScale());
         }
      } catch (Exception var4) {
      }
   }
}

