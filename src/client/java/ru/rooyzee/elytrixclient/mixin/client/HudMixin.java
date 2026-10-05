package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import platform.api.event.EventManager;
import platform.api.event.events.render.CrosshairEvent;
import platform.api.event.events.render.DrawEvent;
import platform.api.event.interfaces.IEvent;
import platform.client.features.modules.render.Animations;
import platform.client.utils.render.pipeline.DeltaRenderUtil;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
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

    // ---- delta-26.2 HudMixin: эффекты модуля Animations (перенесены, т.к. дельтовский HudMixin отключён) ----

    @Inject(method = "extractTabList", at = @At("RETURN"), require = 0)
    private void elytrix$tabListFadeOut(GuiGraphicsExtractor context, DeltaTracker delta, CallbackInfo ci) {
        try {
            Animations animations = platform.client.Delta.h().d().t().Q();
            if (animations.m() && animations.q().a("TAB").c().booleanValue() && animations.r().c() > 0.0f
                    && !platform.api.module.Interface.aM_.options.keyPlayerList.isDown()
                    && platform.api.module.Interface.aM_.player != null
                    && !(platform.api.module.Interface.aM_.isLocalServer()
                         && platform.api.module.Interface.aM_.player.connection.getListedOnlinePlayers().size() > 1)) {
                Scoreboard scoreboard = platform.api.module.Interface.aM_.level.getScoreboard();
                Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
                if (objective != null) {
                    context.nextStratum();
                    platform.api.module.Interface.aM_.gui.hud.getTabList()
                            .extractRenderState(context, context.guiWidth(), scoreboard, objective);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), require = 0)
    private void elytrix$hotbarRaiseHead(GuiGraphicsExtractor context, DeltaTracker delta, CallbackInfo ci) {
        try {
            Animations animations = platform.client.Delta.h().d().t().Q();
            if (animations.m() && animations.q().a("Поднятие хотбара").c().booleanValue()) {
                context.pose().pushMatrix();
                context.pose().translate(0.0f, (-16.0f) * animations.s().c());
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"), require = 0)
    private void elytrix$hotbarRaiseTail(GuiGraphicsExtractor context, DeltaTracker delta, CallbackInfo ci) {
        try {
            Animations animations = platform.client.Delta.h().d().t().Q();
            if (animations.m() && animations.q().a("Поднятие хотбара").c().booleanValue()) {
                context.pose().popMatrix();
            }
        } catch (Throwable ignored) {
        }
    }

    @ModifyArg(method = "extractItemHotbar", index = 2, require = 0,
            at = @At(value = "INVOKE", ordinal = 1,
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private int elytrix$hotbarSelectionSlot(int x) {
        try {
            Animations animations = platform.client.Delta.h().d().t().Q();
            if (animations.m() && animations.q().a("Слот хотбара").c().booleanValue()
                    && platform.api.module.Interface.aM_.player != null) {
                return Math.round((x - (platform.api.module.Interface.aM_.player.getInventory().getSelectedSlot() * 20))
                        + (animations.v() * 20.0f));
            }
        } catch (Throwable ignored) {
        }
        return x;
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
        ru.rooyzee.elytrixclient.client.features.render.Visuals.renderHud(g, sw, sh);
        MtsdfTextRenderer.flush(g);
    }
}