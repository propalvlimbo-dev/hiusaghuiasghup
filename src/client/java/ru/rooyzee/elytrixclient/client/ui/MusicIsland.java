package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.music.CustomMusic;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiIcon;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

/**
 * «Динамический островок» — плавающая плашка вверху экрана с названием текущего трека,
 * в стиле iOS Dynamic Island. Появляется когда музыка играет, плавно исчезает когда нет.
 * Работает на всех экранах (в меню, в игре, в инвентаре и т.д.).
 */
public final class MusicIsland {
    private MusicIsland() {
    }

    private static final float PILL_H = 30;
    private static final float COLLAPSED_W = 70;
    private static final float EXPANDED_W = 220;
    private static final float PILL_RADIUS = 15;

    private static float openT;
    private static long lastFrame;
    private static final float[] eqBars = new float[5];
    private static float eqPhase;
    private static float hoverT;

    /**
     * Вызывается каждый кадр на любом экране.
     * Рисует островок по центру сверху экрана.
     */
    public static void render(GuiGraphicsExtractor g, int screenWidth, int screenHeight,
                               int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        boolean wantOpen = cfg != null && cfg.customMusic && CustomMusic.trackCount() > 0;

        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        // анимация открытия/закрытия
        float speed = wantOpen ? 5f : 8f;
        openT += ((wantOpen ? 1f : 0f) - openT) * (1f - (float) Math.exp(-speed * dt));
        if (openT < 0.005f && !wantOpen) {
            openT = 0f;
            return;
        }

        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);

        // размеры островка
        float currentW = COLLAPSED_W + (EXPANDED_W - COLLAPSED_W) * easeOut(openT);
        float cx = screenWidth / 2f - currentW / 2f;
        float cy = 8;

        // проверяем наведение мыши
        boolean hovered = mouseX >= cx && mouseX <= cx + currentW
                && mouseY >= cy && mouseY <= cy + PILL_H;
        hoverT += ((hovered ? 1f : 0f) - hoverT) * (1f - (float) Math.exp(-12f * dt));

        // тень
        UiVector.shadow(g, cx, cy, currentW, PILL_H, PILL_RADIUS,
                6f + 4f * hoverT, 0x40000000, 6);

        // фон — чёрный с лёгким акцентом при наведении
        int bg = UiTheme.mix(0xFF0A0A0A, UiTheme.withAlpha(accent, 0.15f), hoverT * 0.3f);
        UiVector.roundRect(g, cx, cy, currentW, PILL_H, PILL_RADIUS, bg);

        // тонкая обводка акцентом
        UiVector.outline(g, cx, cy, currentW, PILL_H, PILL_RADIUS, 0.6f,
                UiTheme.withAlpha(accent, 0.12f + 0.15f * hoverT));

        // иконка музыки слева (всегда видна при открытии)
        if (openT > 0.1f) {
            float iconAlpha = Math.min(1f, openT / 0.3f);
            float iconSize = 12;
            float iconX = cx + (PILL_H - iconSize) / 2f;
            float iconY = cy + (PILL_H - iconSize) / 2f;
            // маленький круг-подложка с акцентом
            UiVector.roundRect(g, iconX - 2, iconY - 2, iconSize + 4, iconSize + 4,
                    (iconSize + 4) / 2f, UiTheme.withAlpha(accent, 0.25f * iconAlpha));
            UiIcon.MUSIC.draw(g, (int) iconX, (int) iconY, (int) iconSize,
                    UiTheme.withAlpha(0xFFFFFFFF, iconAlpha));
        }

        // название трека (появляется с задержкой)
        if (openT > 0.25f) {
            float textAlpha = Math.min(1f, (openT - 0.25f) / 0.35f);
            Font font = mc.font;
            String track = CustomMusic.nowPlaying();
            if (track == null || track.equals("—")) track = "...";

            float textX = cx + PILL_H + 2;
            float maxTextW = currentW - PILL_H - 30;
            String shown = trimSmall(font, track, maxTextW);
            float textY = cy + PILL_H / 2f;

            int textColor = UiTheme.withAlpha(0xFFFFFFFF, textAlpha);
            UiText.draw(g, font, shown, Math.round(textX), Math.round(tySmall(font, textY)),
                    textColor, UiText.FACE, false);
        }

        // эквалайзер справа
        if (openT > 0.4f) {
            float eqAlpha = Math.min(1f, (openT - 0.4f) / 0.3f);
            updateEq(dt);

            int eqColor = UiTheme.withAlpha(accent, eqAlpha * (0.7f + 0.3f * hoverT));
            float eqBaseX = cx + currentW - 22;
            float eqBaseY = cy + PILL_H / 2f;
            for (int i = 0; i < eqBars.length; i++) {
                float barH = 4 + eqBars[i] * 11;
                float barW = 2.5f;
                float barX = eqBaseX + i * 4;
                float barY = eqBaseY - barH / 2f;
                UiVector.roundRect(g, barX, barY, barW, barH, barW / 2f, eqColor);
            }
        }
    }

    private static void updateEq(float dt) {
        eqPhase += dt * 2.5f;
        for (int i = 0; i < eqBars.length; i++) {
            float target = 0.2f + 0.8f * Math.abs((float) Math.sin(eqPhase * (1.0 + i * 0.7) + i * 2.1));
            eqBars[i] += (target - eqBars[i]) * Math.min(1f, dt * 10f);
        }
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float tySmall(Font font, float centerY) {
        return centerY + 3f;
    }

    private static String trimSmall(Font font, String text, float maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        String dots = "…";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + dots) > maxW) {
            end--;
        }
        return end > 0 ? text.substring(0, end) + dots : dots;
    }
}