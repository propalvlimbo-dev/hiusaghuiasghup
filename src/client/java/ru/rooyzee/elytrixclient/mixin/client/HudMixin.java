package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.DeltaHud;
import ru.rooyzee.elytrixclient.client.ui.MusicIsland;

/**
 * Рендерит MusicIsland поверх HUD во время игры (без экранов).
 * Hud — ванильный класс 26.2, принимает GuiGraphicsExtractor.
 */
@Mixin(Hud.class)
public abstract class HudMixin {

    /** Пробуем extractRenderState(GuiGraphicsExtractor, DeltaTracker) */
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void elytrix$musicIslandA(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        renderIsland(g);
    }

    /** Пробуем render(GuiGraphicsExtractor, DeltaTracker) */
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void elytrix$musicIslandB(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        renderIsland(g);
    }

    private static void renderIsland(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui.screen() != null) return;
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        double scaleX = (double) sw / mc.getWindow().getWidth();
        double scaleY = (double) sh / mc.getWindow().getHeight();
        int mx = (int) (mc.mouseHandler.xpos() * scaleX);
        int my = (int) (mc.mouseHandler.ypos() * scaleY);
        MtsdfTextRenderer.beginFrame();
        MusicIsland.render(g, sw, sh, mx, my);
        DeltaHud.render(g, sw, sh);
        ru.rooyzee.elytrixclient.client.features.render.Visuals.renderHud(g, sw, sh);
        MtsdfTextRenderer.flush(g);
    }
}