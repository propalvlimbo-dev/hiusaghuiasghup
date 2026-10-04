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
 * «Динамический островок» — компактная плашка вверху экрана с названием трека
 * и управлением (клик раскрывает контролы). Работает на всех экранах.
 */
public final class MusicIsland {
    private MusicIsland() {
    }

    // ── размеры ────────────────────────────────────────────────────────
    private static final float H = 20;          // высота свёрнутого
    private static final float H_EXPANDED = 48; // высота развёрнутого
    private static final float R = 10;          // радиус свёрнутого
    private static final float R_EXP = 12;      // радиус развёрнутого
    private static final float W_COLLAPSED = 48;
    private static final float W_PLAYING = 150;
    private static final float W_EXPANDED = 200;

    // ── состояние ──────────────────────────────────────────────────────
    private static float openT;        // 0=closed, 1=fully visible
    private static float expandT;      // 0=compact, 1=expanded with controls
    private static long lastFrame;
    private static final float[] eq = new float[4];
    private static float eqPhase;
    private static boolean wasPressed;
    private static boolean expanded;   // controls visible
    private static float hoverT;

    // кнопки в развёрнутом состоянии
    private static float btnHoverPrev, btnHoverPlay, btnHoverNext, btnHoverVolD, btnHoverVolU;

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        boolean wantOpen = cfg != null && cfg.customMusic && CustomMusic.trackCount() > 0;

        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        // анимация появления
        openT += ((wantOpen ? 1f : 0f) - openT) * (1f - (float) Math.exp(-6f * dt));
        if (openT < 0.005f && !wantOpen) { openT = 0f; return; }

        if (!wantOpen) expanded = false;
        expandT += ((expanded ? 1f : 0f) - expandT) * (1f - (float) Math.exp(-12f * dt));

        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);

        // ── геометрия ──────────────────────────────────────────────────
        float currentH = H + (H_EXPANDED - H) * easeOut(expandT);
        float targetW = expanded ? W_EXPANDED : (openT > 0.5f ? W_PLAYING : W_COLLAPSED);
        float currentW = W_COLLAPSED + (targetW - W_COLLAPSED) * easeOut(Math.min(1f, openT * 2f));
        float cx = sw / 2f - currentW / 2f;
        float cy = 6;
        float rad = R + (R_EXP - R) * expandT;

        // хит-тест
        boolean over = mx >= cx && mx <= cx + currentW && my >= cy && my <= cy + currentH;
        hoverT += ((over ? 1f : 0f) - hoverT) * (1f - (float) Math.exp(-14f * dt));

        // клик по островку — переключить развёрнутость
        boolean pressed = Minecraft.getInstance().mouseHandler.isLeftPressed();
        if (pressed && !wasPressed && over) {
            expanded = !expanded;
        }
        wasPressed = pressed;

        // ── рисование ──────────────────────────────────────────────────
        // тень
        UiVector.shadow(g, cx, cy, currentW, currentH, rad,
                4f + 3f * hoverT, 0x30000000, 5);

        // фон
        int bg = UiTheme.mix(0xFF0A0A0A, UiTheme.withAlpha(accent, 0.12f), hoverT * 0.25f);
        UiVector.roundRect(g, cx, cy, currentW, currentH, rad, bg);

        // обводка
        UiVector.outline(g, cx, cy, currentW, currentH, rad, 0.5f,
                UiTheme.withAlpha(accent, 0.1f + 0.12f * hoverT));

        Font font = mc.font;

        // ── строка 1: иконка + название + эквалайзер ───────────────────
        float row1Y = cy + (expanded ? 4 : (H - 12) / 2f);

        // иконка музыки
        float iconSize = 11;
        float iconX = cx + 5;
        float iconY = row1Y;
        UiVector.roundRect(g, iconX - 1, iconY - 1, iconSize + 2, iconSize + 2,
                (iconSize + 2) / 2f, UiTheme.withAlpha(accent, 0.2f));
        UiIcon.MUSIC.draw(g, (int) iconX, (int) iconY, (int) iconSize,
                UiTheme.withAlpha(0xFFFFFFFF, 0.9f));

        // название трека
        if (openT > 0.2f) {
            float textAlpha = Math.min(1f, (openT - 0.2f) / 0.3f);
            String track = CustomMusic.nowPlaying();
            if (track == null || track.equals("—")) track = "...";
            float textX = cx + iconSize + 9;
            float maxTextW = currentW - iconSize - (expanded ? 14 : 28);
            String shown = trim(font, track, maxTextW);
            UiText.draw(g, font, shown, Math.round(textX), Math.round(iconY + 2),
                    UiTheme.withAlpha(0xFFFFFFFF, textAlpha * 0.9f), UiText.FACE, false);
        }

        // эквалайзер (справа, только в свёрнутом режиме)
        if (!expanded && openT > 0.3f) {
            float eqAlpha = Math.min(1f, (openT - 0.3f) / 0.3f);
            updateEq(dt);
            int eqColor = UiTheme.withAlpha(accent, eqAlpha * (0.6f + 0.3f * hoverT));
            float eqX = cx + currentW - 17;
            float eqY = cy + H / 2f;
            for (int i = 0; i < eq.length; i++) {
                float bh = 3 + eq[i] * 8;
                UiVector.roundRect(g, eqX + i * 3.5f, eqY - bh / 2f, 2, bh, 1f, eqColor);
            }
        }

        // ── строка 2: контролы (только в развёрнутом режиме) ──────────
        if (expandT > 0.05f) {
            float alpha2 = expandT;
            float btnY = cy + H + 3;
            float btnH = 18;
            float btnW = 28;
            float gap = 4;
            float totalBtnW = btnW * 5 + gap * 4;
            float btnX = cx + (currentW - totalBtnW) / 2f;

            // кнопки: ◄◄ ▶ ►  🔈 🔊
            drawBtn(g, font, btnX, btnY, btnW, btnH, "⏮", accent, alpha2, btnHoverPrev, mx, my);
            drawBtn(g, font, btnX + (btnW + gap), btnY, btnW, btnH,
                    CustomMusic.isPlaying() ? "⏸" : "▶", accent, alpha2, btnHoverPlay, mx, my);
            drawBtn(g, font, btnX + (btnW + gap) * 2, btnY, btnW, btnH, "⏭", accent, alpha2, btnHoverNext, mx, my);
            drawBtn(g, font, btnX + (btnW + gap) * 3, btnY, btnW, btnH, "🔉", accent, alpha2, btnHoverVolD, mx, my);
            drawBtn(g, font, btnX + (btnW + gap) * 4, btnY, btnW, btnH, "🔊", accent, alpha2, btnHoverVolU, mx, my);

            // обработка кликов по кнопкам
            if (pressed && !wasPressed) {
                if (over(btnX, btnY, btnW, btnH, mx, my)) CustomMusic.prev();
                else if (over(btnX + (btnW + gap), btnY, btnW, btnH, mx, my)) CustomMusic.togglePause();
                else if (over(btnX + (btnW + gap) * 2, btnY, btnW, btnH, mx, my)) CustomMusic.next();
                else if (over(btnX + (btnW + gap) * 3, btnY, btnW, btnH, mx, my)) CustomMusic.volumeDown();
                else if (over(btnX + (btnW + gap) * 4, btnY, btnW, btnH, mx, my)) CustomMusic.volumeUp();
            }
        }
    }

    private static void drawBtn(GuiGraphicsExtractor g, Font font, float x, float y,
                                 float w, float h, String label, int accent,
                                 float alpha, float hover, int mx, int my) {
        boolean hv = over(x, y, w, h, mx, my);
        hover += ((hv ? 1f : 0f) - hover) * 0.3f;
        // обновляем hover-статус
        updateHover(label, hover);

        int fill = UiTheme.mix(0xFF1A1A1A, UiTheme.withAlpha(accent, 0.25f), hover);
        UiVector.roundRect(g, x, y, w, h, 4, UiTheme.withAlpha(fill, alpha * 0.9f));
        UiVector.outline(g, x, y, w, h, 4, 0.4f,
                UiTheme.withAlpha(0x33FFFFFF, alpha * (0.3f + 0.3f * hover)));

        int col = UiTheme.withAlpha(0xFFFFFFFF, alpha * (0.6f + 0.4f * hover));
        // рисуем символ по центру
        float tw = font.width(label);
        g.text(font, label, Math.round(x + w / 2f - tw / 2f), Math.round(y + h / 2f - 4), col, false);
    }

    private static void updateHover(String label, float value) {
        switch (label) {
            case "⏮" -> btnHoverPrev = value;
            case "▶", "⏸" -> btnHoverPlay = value;
            case "⏭" -> btnHoverNext = value;
            case "🔉" -> btnHoverVolD = value;
            case "🔊" -> btnHoverVolU = value;
        }
    }

    private static boolean over(float x, float y, float w, float h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void updateEq(float dt) {
        eqPhase += dt * 2.5f;
        for (int i = 0; i < eq.length; i++) {
            float t = 0.2f + 0.8f * Math.abs((float) Math.sin(eqPhase * (1.0 + i * 0.7) + i * 2.1));
            eq[i] += (t - eq[i]) * Math.min(1f, dt * 10f);
        }
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static String trim(Font font, String text, float maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        String dots = "…";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + dots) > maxW) end--;
        return end > 0 ? text.substring(0, end) + dots : dots;
    }
}