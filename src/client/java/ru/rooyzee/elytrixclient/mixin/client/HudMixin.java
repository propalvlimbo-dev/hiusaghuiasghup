package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import platform.api.event.EventManager;
import platform.api.event.events.render.CrosshairEvent;
import platform.api.event.events.render.DrawEvent;
import platform.api.event.interfaces.IEvent;
import platform.client.utils.render.pipeline.DeltaRenderUtil;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.DeltaHud;
import ru.rooyzee.elytrixclient.client.ui.MusicIsland;

/**
 * Рендерит MusicIsland поверх HUD во время игры (без экранов)
 * и рассылает HUD-события delta-26.2 (DrawEvent D2D / CrosshairEvent) —
 * дельтовский HudMixin отключён, поэтому события шьём здесь, теми же точками.
 * Hud — ванильный класс 26.2, принимает GuiGraphicsExtractor.
 */
@Mixin(Hud.class)
public abstract class HudMixin {

    /** delta-26.2: DrawEvent D2D в HEAD extractRenderState — HUD-отрисовка включённых модулей delta. */
    @Inject(method = "extractRenderState", at = @At("HEAD"), require = 0)
    private void elytrix$deltaDrawEvent(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        try {
            EventManager.a((IEvent) new DrawEvent(g, dt.getGameTimeDeltaPartialTick(false), DrawEvent.a.D2D));
        } catch (Throwable ignored) {
        }
    }

    /** delta-26.2: CrosshairEvent в HEAD extractCrosshair — модуль Crosshair гасит ванильный прицел. */
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$deltaCrosshairEvent(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        try {
            CrosshairEvent event = new CrosshairEvent(g, dt.getGameTimeDeltaPartialTick(false));
            EventManager.a((IEvent) event);
            if (event.a()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    /** delta-26.2: сброс очереди дельтовского рендера в GuiRenderState (в TAIL, как у delta). */
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void elytrix$deltaFlush(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        try {
            DeltaRenderUtil.flush(g);
        } catch (Throwable ignored) {
        }
    }

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