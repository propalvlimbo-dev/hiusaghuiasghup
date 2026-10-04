package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ui.MusicIsland;

/**
 * Рендерит MusicIsland поверх HUD во время игры.
 * Пробует render() с GuiGraphicsExtractor — require=0 если метод не существует.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    /** render(GuiGraphicsExtractor, DeltaTracker) */
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void elytrix$musicIslandA(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        renderIsland(g);
    }

    /** render(GuiGraphicsExtractor, float) */
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void elytrix$musicIslandB(GuiGraphicsExtractor g, float pt, CallbackInfo ci) {
        renderIsland(g);
    }

    private static void renderIsland(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui.screen() != null) return;
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        // mouseHandler.xypos() в оконных координатах, нужен масштаб в GUI
        MouseHandler mouse = mc.mouseHandler;
        double scaleX = (double) sw / mc.getWindow().getWidth();
        double scaleY = (double) sh / mc.getWindow().getHeight();
        int mx = (int)(mouse.xpos() * scaleX);
        int my = (int)(mouse.ypos() * scaleY);
        MusicIsland.render(g, sw, sh, mx, my);
    }
}