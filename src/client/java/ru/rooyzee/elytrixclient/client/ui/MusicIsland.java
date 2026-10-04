package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.music.CustomMusic;
import ru.rooyzee.elytrixclient.client.ui.kit.UiIcon;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

/**
 * Dynamic Island — компактный виджет музыки.
 * Свёрнутый: иконка + название + эквалайзер (~20px).
 * Развёрнутый: название + прогресс + контролы + громкость (~72px).
 */
public final class MusicIsland {
    private MusicIsland() {}

    /* ── размеры ── */
    private static final float COLLAPSED_H = 20;
    private static final float EXPANDED_H  = 72;
    private static final float RADIUS      = 10;
    private static final float W_EMPTY     = 48;
    private static final float W_COLLAPSED = 150;
    private static final float W_EXPANDED  = 220;

    /* ── контролы ── */
    private static final float BTN_H   = 18;
    private static final float BIG_W   = 44;
    private static final float SMALL_W = 32;
    private static final float TINY_W  = 24;
    private static final float GAP     = 4;

    /* ── анимация (spring-подобная) ── */
    private static final float OPEN_SPEED    = 5f;   // появление
    private static final float EXPAND_SPEED  = 8f;   // раскрытие
    private static final float HOVER_SPEED   = 12f;  // подсветка

    /* ── состояние ── */
    private static float openT, expandT, hoverT;
    private static long lastFrame;
    private static boolean expanded, wasPressed, dragging;
    private static float sX, sY, sW, sH;
    private static final float[][] btnR = new float[5][4]; // prev, play, next, vol-, vol+
    private static float seekHoverT;
    private static final float[] eq = new float[4];
    private static float eqPhase;

    /* ════════════════════════════════════════════════════ */

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        boolean wantOpen = cfg != null && cfg.customMusic && CustomMusic.trackCount() > 0;
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;

        /* ── плавные анимации (exponential decay) ── */
        openT   = lerp(openT,   wantOpen  ? 1f : 0f, dt, OPEN_SPEED);
        if (openT < 0.002f && !wantOpen) { openT = 0; sW = 0; return; }
        if (!wantOpen) expanded = false;

        expandT = lerp(expandT, expanded  ? 1f : 0f, dt, EXPAND_SPEED);

        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);

        /* ── размеры ── */
        float t = easeOutCubic(expandT);
        float curH = COLLAPSED_H + (EXPANDED_H - COLLAPSED_H) * t;
        float wantW = expanded ? W_EXPANDED : (openT > 0.4f ? W_COLLAPSED : W_EMPTY);
        float curW  = W_EMPTY + (wantW - W_EMPTY) * easeOutQuad(clamp01(openT * 2.5f));
        float cx = sw / 2f - curW / 2f;
        float cy = 6;
        float rad = RADIUS + 2f * t;
        sX = cx; sY = cy; sW = curW; sH = curH;

        boolean over = hit(cx, cy, curW, curH, mx, my);
        hoverT = lerp(hoverT, over ? 1f : 0f, dt, HOVER_SPEED);

        /* ── клик ── */
        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (clicked && expanded) {
            // проверяем кнопки
            for (int i = 0; i < btnR.length; i++) {
                if (hit(btnR[i], mx, my)) {
                    switch (i) {
                        case 0 -> CustomMusic.prev();
                        case 1 -> CustomMusic.togglePause();
                        case 2 -> CustomMusic.next();
                        case 3 -> CustomMusic.volumeDown();
                        case 4 -> CustomMusic.volumeUp();
                    }
                    clicked = false;
                    break;
                }
            }
            // проверяем клик по seek-бару
            if (clicked && hitSeekBar(cx, cy, curW, mx)) {
                clicked = false;
            }
        }
        if (clicked) {
            if (over) expanded = !expanded;
            else if (expanded) expanded = false;
        }

        /* ── dragging по seek-бару ── */
        if (pressed && expanded && dragging) {
            float barX = cx + 10;
            float barW = curW - 20;
            float frac = clamp01((mx - barX) / barW);
            CustomMusic.seekTo(frac);
        }
        if (!pressed) dragging = false;

        Font font = mc.font;

        /* ══ тень и фон ══ */
        UiVector.shadow(g, cx, cy, curW, curH, rad, 4f + 2f * hoverT, 0x28000000, 5);
        int bg = UiTheme.mix(0xFF0D0D0D, UiTheme.withAlpha(accent, 0.06f), hoverT * 0.12f);
        UiVector.roundRect(g, cx, cy, curW, curH, rad, bg);
        UiVector.outline(g, cx, cy, curW, curH, rad, 0.5f,
                UiTheme.withAlpha(accent, 0.05f + 0.1f * hoverT));

        /* ══ строка 1: иконка + трек + эквалайзер ══ */
        float row1Y = cy + (expanded ? 5 : (COLLAPSED_H - 13) / 2f);

        // иконка ноты
        float iconX = cx + 6;
        UiVector.roundRect(g, iconX - 1, row1Y - 1, 14, 14, 7,
                UiTheme.withAlpha(accent, 0.15f));
        UiIcon.MUSIC.draw(g, (int) iconX, (int) row1Y, 12,
                UiTheme.withAlpha(0xFFFFFFFF, 0.8f));

        // название трека
        if (openT > 0.12f) {
            float ta = clamp01((openT - 0.12f) / 0.2f);
            String track = CustomMusic.nowPlaying();
            if (track == null || track.equals("\u2014")) track = "\u2026";
            float maxTW = curW - 22 - (expanded ? 0 : 26);
            String shown = trim(font, track, maxTW);
            UiText.draw(g, font, shown, Math.round(cx + 22), Math.round(row1Y + 2),
                    UiTheme.withAlpha(0xFFFFFFFF, ta * 0.88f), UiText.FACE, false);
        }

        // эквалайзер (свёрнутый)
        if (!expanded && openT > 0.2f) {
            updateEq(dt);
            float eqA = clamp01((openT - 0.2f) / 0.2f) * (0.5f + 0.4f * hoverT);
            int eqCol = UiTheme.withAlpha(accent, eqA);
            float eqX = cx + curW - 17;
            float eqY = cy + COLLAPSED_H / 2f;
            for (int i = 0; i < eq.length; i++) {
                float bh = 2.5f + eq[i] * 8;
                UiVector.roundRect(g, eqX + i * 3.5f, eqY - bh / 2f, 2, bh, 1f, eqCol);
            }
        }

        /* ══ развёрнутая часть ══ */
        if (expandT < 0.03f) {
            for (float[] r : btnR) r[0] = r[1] = r[2] = r[3] = 0;
            return;
        }
        float a = easeOutCubic(expandT);

        /* ── строка 2: seek-бар ── */
        float barY = cy + COLLAPSED_H + 2;
        float barX = cx + 10;
        float barW = curW - 20;
        float barH = 4;
        boolean seekOver = my >= barY - 3 && my <= barY + barH + 3 && mx >= barX && mx <= barX + barW;
        seekHoverT = lerp(seekHoverT, seekOver ? 1f : 0f, dt, 10f);
        float barDrawH = barH + seekHoverT * 2;

        // фон
        UiVector.roundRect(g, barX, barY - seekHoverT, barW, barDrawH, barDrawH / 2f,
                UiTheme.withAlpha(0xFFFFFFFF, a * 0.1f));
        // заполненная часть
        float progress = CustomMusic.progress();
        UiVector.roundRect(g, barX, barY - seekHoverT, barW * progress, barDrawH, barDrawH / 2f,
                UiTheme.withAlpha(accent, a * 0.75f));
        // точка на seek-баре
        if (seekHoverT > 0.1f) {
            float dotX = barX + barW * progress;
            float dotR = 3 + seekHoverT * 2;
            UiVector.roundRect(g, dotX - dotR, barY + barH / 2f - dotR, dotR * 2, dotR * 2, dotR,
                    UiTheme.withAlpha(0xFFFFFFFF, a * seekHoverT));
        }

        if (clicked && seekOver) {
            dragging = true;
            float frac = clamp01((mx - barX) / barW);
            CustomMusic.seekTo(frac);
        }

        // время
        String timeStr = CustomMusic.timeString();
        if (timeStr != null) {
            UiText.draw(g, font, timeStr, Math.round(barX), Math.round(barY + barDrawH + 1),
                    UiTheme.withAlpha(0xFFFFFFFF, a * 0.4f), UiText.FACE, false);
        }

        /* ── строка 3: контролы ── */
        float ctrlY = cy + COLLAPSED_H + 16;
        boolean playing = CustomMusic.isPlaying();

        // prev | play/pause | next
        float mainTotal = SMALL_W + BIG_W + SMALL_W + GAP * 2;
        float mainX = cx + (curW - mainTotal) / 2f;

        btnR[0] = new float[]{mainX, ctrlY, SMALL_W, BTN_H};
        drawBtn(g, font, mainX, ctrlY, SMALL_W, BTN_H, "|<", accent, a, hit(btnR[0], mx, my), false);

        float playX = mainX + SMALL_W + GAP;
        btnR[1] = new float[]{playX, ctrlY, BIG_W, BTN_H};
        drawBtn(g, font, playX, ctrlY, BIG_W, BTN_H, playing ? "||" : ">", accent, a, hit(btnR[1], mx, my), true);

        float nextX = playX + BIG_W + GAP;
        btnR[2] = new float[]{nextX, ctrlY, SMALL_W, BTN_H};
        drawBtn(g, font, nextX, ctrlY, SMALL_W, BTN_H, ">|", accent, a, hit(btnR[2], mx, my), false);

        /* ── строка 4: громкость ── */
        float volY = ctrlY + BTN_H + 4;
        float volTotal = TINY_W + 50 + TINY_W + GAP * 2;
        float volX = cx + (curW - volTotal) / 2f;

        btnR[3] = new float[]{volX, volY, TINY_W, BTN_H};
        drawBtn(g, font, volX, volY, TINY_W, BTN_H, "-", accent, a, hit(btnR[3], mx, my), false);

        // полоска громкости
        float vBarX = volX + TINY_W + GAP;
        float vBarW = 50;
        float vBarY = volY + BTN_H / 2f - 2;
        UiVector.roundRect(g, vBarX, vBarY, vBarW, 3, 1.5f,
                UiTheme.withAlpha(0xFFFFFFFF, a * 0.12f));
        float vol = CustomMusic.volumeFrac();
        UiVector.roundRect(g, vBarX, vBarY, vBarW * vol, 3, 1.5f,
                UiTheme.withAlpha(accent, a * 0.75f));

        float plusX = vBarX + vBarW + GAP;
        btnR[4] = new float[]{plusX, volY, TINY_W, BTN_H};
        drawBtn(g, font, plusX, volY, TINY_W, BTN_H, "+", accent, a, hit(btnR[4], mx, my), false);
    }

    public static void onClick(int mx, int my) {}

    /* ══════ вспомогательные методы ══════ */

    private static void drawBtn(GuiGraphicsExtractor g, Font font, float x, float y,
            float w, float h, String label, int accent, float alpha, boolean hover, boolean main) {
        int fill = main
                ? UiTheme.mix(0xFF181818, accent, hover ? 0.4f : 0f)
                : UiTheme.mix(0xFF121212, UiTheme.withAlpha(accent, 0.18f), hover ? 1f : 0f);
        UiVector.roundRect(g, x, y, w, h, 5, UiTheme.withAlpha(fill, alpha * 0.95f));
        UiVector.outline(g, x, y, w, h, 5, 0.4f,
                UiTheme.withAlpha(main ? accent : 0x33FFFFFF, alpha * (hover ? 0.5f : 0.15f)));
        int col = UiTheme.withAlpha(0xFFFFFFFF, alpha * (hover ? 0.95f : 0.6f));
        float tw = font.width(label);
        g.text(font, label, Math.round(x + w / 2f - tw / 2f), Math.round(y + h / 2f - 4), col, false);
    }

    private static void updateEq(float dt) {
        eqPhase += dt * 2f;
        for (int i = 0; i < eq.length; i++) {
            float t = 0.12f + 0.88f * Math.abs((float) Math.sin(eqPhase * (0.7 + i * 0.6) + i * 1.8));
            eq[i] += (t - eq[i]) * Math.min(1f, dt * 7f);
        }
    }

    /* ── easing / math ── */
    private static float lerp(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }
    private static float easeOutCubic(float t) { float u = 1f - t; return 1f - u * u * u; }
    private static float easeOutQuad(float t)  { return 1f - (1f - t) * (1f - t); }
    private static float clamp01(float v)      { return Math.max(0, Math.min(1, v)); }

    private static boolean hit(float x, float y, float w, float h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    private static boolean hit(float[] r, int mx, int my) {
        return r[2] > 0 && hit(r[0], r[1], r[2], r[3], mx, my);
    }
    private static boolean hitSeekBar(float cx, float cy, float curW, int mx) {
        float barX = cx + 10;
        float barW = curW - 20;
        return mx >= barX && mx <= barX + barW;
    }
    private static String trim(Font font, String text, float maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + "\u2026") > maxW) end--;
        return end > 0 ? text.substring(0, end) + "\u2026" : "\u2026";
    }
}