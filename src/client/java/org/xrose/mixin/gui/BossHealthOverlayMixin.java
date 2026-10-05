package org.xrose.mixin.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.feature.impl.visual.RemovalsFeature;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
   @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
   private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, CallbackInfo ci) {
      if (RemovalsFeature.shouldRemoveBossBar()) {
         ci.cancel();
      }
   }
}

