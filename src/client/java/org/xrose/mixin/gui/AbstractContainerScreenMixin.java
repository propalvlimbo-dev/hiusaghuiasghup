package org.xrose.mixin.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.feature.impl.misc.AuctionHelperFeature;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void renderAuctionHighlights(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
      AuctionHelperFeature.renderHighlights(graphics, screen.getTitle().getString(), screen);
   }

   @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
   private void augmentAuctionTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
      AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
      AuctionHelperFeature.augmentTooltip(screen.getTitle().getString(), stack, (List<Component>)cir.getReturnValue(), cir);
   }
}

