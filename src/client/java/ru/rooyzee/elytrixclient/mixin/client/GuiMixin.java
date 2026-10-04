package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.MusicIsland;

/**
 * Рисует «Динамический островок» музыки поверх HUD в игре (когда нет экрана).
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void elytrix$musicIsland(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
        if (this.minecraft.gui.screen() != null) {
            return; // экран открыт — островок рисуется через ScreenMixin
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int mx = (int) (this.minecraft.mouseHandler.xpos() * w / this.minecraft.getWindow().getWidth());
        int my = (int) (this.minecraft.mouseHandler.ypos() * h / this.minecraft.getWindow().getHeight());
        MusicIsland.render(graphics, w, h, mx, my);
    }
}