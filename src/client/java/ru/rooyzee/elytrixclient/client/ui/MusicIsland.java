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
 * «Динамический островок» — компактная плашка вверху экрана.
 * Клик по островку раскрывает контролы (плей/пауза, переключение, громкость).
 */
public final class MusicIsland {
    private MusicIsland() {
    }

    // ── размеры ────────────────────────────────────────────────────────
    private static final float H = 20;
    private static final float H_EXP = 46;
    private static final float R = 10;
    private static final float W_EMPTY = 48;
    private static final float W_PLAY = 150;
    private static final float W_EXP = 200;
    private static final float BTN_W = 28;
    private static final float BTN_H = 16;
    private static final float BTN_GAP = 4;

    // ── состояние ──────────────────────────────────────────────────────
    private static float openT, expandT, hoverT;
    private static long lastFrame;
    private static final float[] eq = new float[4];
    private static float eqPhase;
    private static boolean expanded;

    // сохранённая позиция для onClick
    private static float sX, sY, sW, sH;
    private static float[][] btnRects = new float[5][4]; // x,y,w,h для 5 кнопок

    /**
     * Рендер — вызывается каждый кадр из ScreenMixin / LoadingOverlayMixin.
     */
    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        boolean wantOpen = cfg != null && cfg.customMusic && CustomMusic.trackCount() > 0;

        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        openT += ((wantOpen ? 1f : 0f) - openT) * (1f - (float) Math.exp(-6f * dt));
        if (openT < 0.005f && !wantOpen) { openT = 0f; sW = 0; return; }
        if (!wantOpen) expanded = false;
        expandT += ((expanded ? 1f : 0f) - expandT) * (1f - (float) Math.exp(-12f * dt));

        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);

        // ── геометрия ──────────────────────────────────────────────────
        float curH = H + (H_EXP - H) * easeOut(expandT);
        float wantW = expanded ? W_EXP : (openT > 0.5f ? W_PLAY : W_EMPTY);
        float curW = W_EMPTY + (wantW - W_EMPTY) * easeOut(Math.min(1f, openT * 2f));
        float cx = sw / 2f - curW / 2f;
        float cy = 6;
        float rad = R + (2f) * expandT;

        // сохраняем для onClick
        sX = cx; sY = cy; sW = curW; sH = curH;

        boolean over = mx >= cx && mx <= cx + curW && my >= cy && my <= cy + curH;
        hoverT += ((over ? 1f : 0f) - hoverT) * (1f - (float) Math.exp(-14f * dt));

        // ── рисование ──────────────────────────────────────────────────
        UiVector.shadow(g, cx, cy, curW, curH, rad, 4f + 2f * hoverT, 0x30000000, 5);
        int bg = UiTheme.mix(0xFF0A0A0A, UiTheme.withAlpha(accent, 0.1f), hoverT * 0.2f);
        UiVector.roundRect(g, cx, cy, curW, curH, rad, bg);
        UiVector.outline(g, cx, cy, curW, curH, rad, 0.5f,
                UiTheme.withAlpha(accent, 0.08f + 0.1f * hoverT));

        Font font = mc.font;

        // ── строка 1: иконка + название + эквалайзер ───────────────────
        float row1Y = cy + (expanded ? 4 : (H - 12) / 2f);
        float iconSz = 11;
        float iconX = cx + 5;
        UiVector.roundRect(g, iconX - 1, row1Y - 1, iconSz + 2, iconSz + 2,
                (iconSz + 2) / 2f, UiTheme.withAlpha(accent, 0.2f));
        UiIcon.MUSIC.draw(g, (int) iconX, (int) row1Y, (int) iconSz,
                UiTheme.withAlpha(0xFFFFFFFF, 0.9f));

        if (openT > 0.2f) {
            float ta = Math.min(1f, (openT - 0.2f) / 0.3f);
            String track = CustomMusic.nowPlaying();
            if (track == null || track.equals("—")) track = "...";
            String shown = trim(font, track, curW - iconSz - (expanded ? 14 : 28));
            UiText.draw(g, font, shown, Math.round(cx + iconSz + 9), Math.round(row1Y + 2),
                    UiTheme.withAlpha(0xFFFFFFFF, ta * 0.9f), UiText.FACE, false);
        }

        // эквалайзер (свёрнутый режим)
        if (!expanded && openT > 0.3f) {
            updateEq(dt);
            int eqCol = UiTheme.withAlpha(accent, Math.min(1f, (openT - 0.3f) / 0.3f) * (0.6f + 0.3f * hoverT));
            float eqX = cx + curW - 17;
            float eqY = cy + H / 2f;
            for (int i = 0; i < eq.length; i++) {
                float bh = 3 + eq[i] * 8;
                UiVector.roundRect(g, eqX + i * 3.5f, eqY - bh / 2f, 2, bh, 1f, eqCol);
            }
        }

        // ── строка 2: контролы ─────────────────────────────────────────
        if (expandT > 0.05f) {
            float alpha2 = expandT;
            float by = cy + H + 3;
            float totalW = BTN_W * 5 + BTN_GAP * 4;
            float bx = cx + (curW - totalW) / 2f;

            String[] labels = {"⏮", CustomMusic.isPlaying() ? "⏸" : "▶", "⏭", "🔉", "🔊"};
            for (int i = 0; i < 5; i++) {
                float x = bx + i * (BTN_W + BTN_GAP);
                btnRects[i] = new float[]{x, by, BTN_W, BTN_H};
                boolean hv = over(x, by, BTN_W, BTN_H, mx, my);
                drawBtn(g, font, x, by, BTN_W, BTN_H, labels[i], accent, alpha2, hv);
            }
        } else {
            for (float[] r : btnRects) r[0] = r[1] = r[2] = r[3] = 0;
        }
    }

    /**
     * Обработка клика — вызывается из ScreenMixin.mouseClicked.
     */
    public static void onClick(int mx, int my) {
        if (sW < 1f) return;
        boolean over = mx >= sX && mx <= sX + sW && my >= sY && my <= sY + sH;

        if (expanded) {
            // проверяем кнопки
            for (int i = 0; i < btnRects.length; i++) {
                float[] r = btnRects[i];
                if (r[2] > 0 && mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    switch (i) {
                        case 0 -> CustomMusic.prev();
                        case 1 -> CustomMusic.togglePause();
                        case 2 -> CustomMusic.next();
                        case 3 -> CustomMusic.volumeDown();
                        case 4 -> CustomMusic.volumeUp();
                    }
                    return;
                }
            }
        }

        if (over) {
            expanded = !expanded;
        } else if (expanded) {
            expanded = false;
        }
    }

    private static void drawBtn(GuiGraphicsExtractor g, Font font, float x, float y,
                                 float w, float h, String label, int accent, float alpha, boolean hover) {
        int fill = UiTheme.mix(0xFF1A1A1A, UiTheme.withAlpha(accent, 0.25f), hover ? 1f : 0f);
        UiVector.roundRect(g, x, y, w, h, 4, UiTheme.withAlpha(fill, alpha * 0.9f));
        UiVector.outline(g, x, y, w, h, 4, 0.4f,
                UiTheme.withAlpha(0x33FFFFFF, alpha * (hover ? 0.6f : 0.3f)));
        int col = UiTheme.withAlpha(0xFFFFFFFF, alpha * (hover ? 1f : 0.6f));
        float tw = font.width(label);
        g.text(font, label, Math.round(x + w / 2f - tw / 2f), Math.round(y + h / 2f - 4), col, false);
    }

    private static void updateEq(float dt) {
        eqPhase += dt * 2.5f;
        for (int i = 0; i < eq.length; i++) {
            float t = 0.2f + 0.8f * Math.abs((float) Math.sin(eqPhase * (1.0 + i * 0.7) + i * 2.1));
            eq[i] += (t - eq[i]) * Math.min(1f, dt * 10f);
        }
    }

    private static float easeOut(float t) { float u = 1f - t; return 1f - u * u * u; }

    private static boolean over(float x, float y, float w, float h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String trim(Font font, String text, float maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + "…") > maxW) end--;
        return end > 0 ? text.substring(0, end) + "…" : "…";
    }
}