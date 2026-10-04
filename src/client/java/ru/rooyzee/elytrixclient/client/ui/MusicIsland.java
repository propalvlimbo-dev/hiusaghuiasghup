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

public final class MusicIsland {
    private MusicIsland() {}

    private static final float IH = 20, IH_EXP = 68, IR = 10;
    private static final float IW_EMPTY = 48, IW_PLAY = 155, IW_EXP = 230;
    private static final float CTRL_H = 22, CTRL_GAP = 6;
    private static final float MAIN_BTN_W = 50, SIDE_BTN_W = 38, VOL_BTN_W = 28;

    private static float openT, expandT, hoverT;
    private static long lastFrame;
    private static final float[] eq = new float[4];
    private static float eqPhase;
    private static boolean expanded, wasPressed;
    private static float sX, sY, sW, sH;
    private static final float[][] btnR = new float[5][4];

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        boolean wantOpen = cfg != null && cfg.customMusic && CustomMusic.trackCount() > 0;
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        openT += ((wantOpen ? 1f : 0f) - openT) * (1f - (float) Math.exp(-4.5f * dt));
        if (openT < 0.003f && !wantOpen) { openT = 0f; sW = 0; return; }
        if (!wantOpen) expanded = false;
        expandT += ((expanded ? 1f : 0f) - expandT) * (1f - (float) Math.exp(-7f * dt));

        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);
        float t = easeInOutCubic(expandT);
        float curH = IH + (IH_EXP - IH) * t;
        float wantW = expanded ? IW_EXP : (openT > 0.5f ? IW_PLAY : IW_EMPTY);
        float curW = IW_EMPTY + (wantW - IW_EMPTY) * easeOutQuad(Math.min(1f, openT * 2f));
        float cx = sw / 2f - curW / 2f;
        float cy = 6;
        float rad = IR + 3f * t;
        sX = cx; sY = cy; sW = curW; sH = curH;

        boolean over = mx >= cx && mx <= cx + curW && my >= cy && my <= cy + curH;
        hoverT += ((over ? 1f : 0f) - hoverT) * (1f - (float) Math.exp(-10f * dt));

        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;
        if (clicked) {
            if (expanded) {
                for (int i = 0; i < btnR.length; i++) {
                    float[] r = btnR[i];
                    if (r[2] > 0 && mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                        switch (i) {
                            case 0 -> CustomMusic.prev();
                            case 1 -> CustomMusic.togglePause();
                            case 2 -> CustomMusic.next();
                            case 3 -> CustomMusic.volumeDown();
                            case 4 -> CustomMusic.volumeUp();
                        }
                        clicked = false;
                    }
                }
            }
            if (clicked) {
                if (over) expanded = !expanded;
                else if (expanded) expanded = false;
            }
        }

        Font font = mc.font;

        // тень и фон
        UiVector.shadow(g, cx, cy, curW, curH, rad, 5f + 3f * hoverT, 0x30000000, 6);
        int bg = UiTheme.mix(0xFF0D0D0D, UiTheme.withAlpha(accent, 0.08f), hoverT * 0.15f);
        UiVector.roundRect(g, cx, cy, curW, curH, rad, bg);
        UiVector.outline(g, cx, cy, curW, curH, rad, 0.6f,
                UiTheme.withAlpha(accent, 0.06f + 0.12f * hoverT));

        // строка 1: иконка + трек + эквалайзер
        float row1Y = cy + (expanded ? 5 : (IH - 13) / 2f);
        float iconX = cx + 6;
        UiVector.roundRect(g, iconX - 1, row1Y - 1, 14, 14, 7,
                UiTheme.withAlpha(accent, 0.18f));
        UiIcon.MUSIC.draw(g, (int) iconX, (int) row1Y, 12,
                UiTheme.withAlpha(0xFFFFFFFF, 0.85f));

        if (openT > 0.15f) {
            float ta = Math.min(1f, (openT - 0.15f) / 0.25f);
            String track = CustomMusic.nowPlaying();
            if (track == null || track.equals("\u2014")) track = "\u2026";
            float maxTW = curW - 22 - (expanded ? 0 : 28);
            String shown = trim(font, track, maxTW);
            UiText.draw(g, font, shown, Math.round(cx + 22), Math.round(row1Y + 2),
                    UiTheme.withAlpha(0xFFFFFFFF, ta * 0.9f), UiText.FACE, false);
        }

        if (!expanded && openT > 0.25f) {
            updateEq(dt);
            float eqA = Math.min(1f, (openT - 0.25f) / 0.25f) * (0.55f + 0.35f * hoverT);
            int eqCol = UiTheme.withAlpha(accent, eqA);
            float eqX = cx + curW - 18;
            float eqY = cy + IH / 2f;
            for (int i = 0; i < eq.length; i++) {
                float bh = 3 + eq[i] * 9;
                UiVector.roundRect(g, eqX + i * 3.5f, eqY - bh / 2f, 2.2f, bh, 1f, eqCol);
            }
        }

        // строка 2: прогресс-бар
        if (expandT > 0.08f) {
            float barY = cy + IH + 1;
            float barX = cx + 10;
            float barW = curW - 20;
            float barA = expandT;
            UiVector.roundRect(g, barX, barY, barW, 3, 1.5f,
                    UiTheme.withAlpha(0xFFFFFFFF, barA * 0.1f));
            UiVector.roundRect(g, barX, barY, barW * 0.6f, 3, 1.5f,
                    UiTheme.withAlpha(accent, barA * 0.7f));
        }

        // строка 3: контролы
        if (expandT > 0.05f) {
            float a2 = easeInOutCubic(expandT);
            float by = cy + IH + 8;
            float totalW = SIDE_BTN_W * 2 + MAIN_BTN_W + CTRL_GAP * 2;
            float bx = cx + (curW - totalW) / 2f;
            boolean playing = CustomMusic.isPlaying();

            btnR[0] = new float[]{bx, by, SIDE_BTN_W, CTRL_H};
            drawCtrl(g, font, bx, by, SIDE_BTN_W, CTRL_H, "|<", accent, a2,
                    over(bx, by, SIDE_BTN_W, CTRL_H, mx, my), false);

            float px = bx + SIDE_BTN_W + CTRL_GAP;
            btnR[1] = new float[]{px, by, MAIN_BTN_W, CTRL_H};
            drawCtrl(g, font, px, by, MAIN_BTN_W, CTRL_H, playing ? "||" : ">", accent, a2,
                    over(px, by, MAIN_BTN_W, CTRL_H, mx, my), true);

            float nx = px + MAIN_BTN_W + CTRL_GAP;
            btnR[2] = new float[]{nx, by, SIDE_BTN_W, CTRL_H};
            drawCtrl(g, font, nx, by, SIDE_BTN_W, CTRL_H, ">|", accent, a2,
                    over(nx, by, SIDE_BTN_W, CTRL_H, mx, my), false);

            // строка 4: громкость
            float vy = by + CTRL_H + 5;
            float volTotal = VOL_BTN_W * 2 + 60 + CTRL_GAP * 2;
            float volX = cx + (curW - volTotal) / 2f;

            btnR[3] = new float[]{volX, vy, VOL_BTN_W, CTRL_H};
            drawCtrl(g, font, volX, vy, VOL_BTN_W, CTRL_H, "-", accent, a2,
                    over(volX, vy, VOL_BTN_W, CTRL_H, mx, my), false);

            float vBarX = volX + VOL_BTN_W + CTRL_GAP;
            float vBarY = vy + CTRL_H / 2f - 2;
            UiVector.roundRect(g, vBarX, vBarY, 60, 4, 2,
                    UiTheme.withAlpha(0xFFFFFFFF, a2 * 0.15f));
            UiVector.roundRect(g, vBarX, vBarY, 60 * 0.5f, 4, 2,
                    UiTheme.withAlpha(accent, a2 * 0.8f));

            float pux = vBarX + 60 + CTRL_GAP;
            btnR[4] = new float[]{pux, vy, VOL_BTN_W, CTRL_H};
            drawCtrl(g, font, pux, vy, VOL_BTN_W, CTRL_H, "+", accent, a2,
                    over(pux, vy, VOL_BTN_W, CTRL_H, mx, my), false);
        } else {
            for (float[] r : btnR) r[0] = r[1] = r[2] = r[3] = 0;
        }
    }

    public static void onClick(int mx, int my) {}

    private static void drawCtrl(GuiGraphicsExtractor g, Font font, float x, float y,
            float w, float h, String label, int accent, float alpha, boolean hover, boolean main) {
        int fill = main
                ? UiTheme.mix(0xFF1A1A1A, accent, (hover ? 1f : 0f) * 0.45f)
                : UiTheme.mix(0xFF141414, UiTheme.withAlpha(accent, 0.2f), hover ? 1f : 0f);
        UiVector.roundRect(g, x, y, w, h, 6, UiTheme.withAlpha(fill, alpha * 0.95f));
        UiVector.outline(g, x, y, w, h, 6, 0.5f,
                UiTheme.withAlpha(main ? accent : 0x44FFFFFF, alpha * (hover ? 0.6f : 0.2f)));
        int col = UiTheme.withAlpha(0xFFFFFFFF, alpha * (hover ? 1f : 0.65f));
        float tw = font.width(label);
        g.text(font, label, Math.round(x + w / 2f - tw / 2f), Math.round(y + h / 2f - 4), col, false);
    }

    private static void updateEq(float dt) {
        eqPhase += dt * 2.2f;
        for (int i = 0; i < eq.length; i++) {
            float t = 0.15f + 0.85f * Math.abs((float) Math.sin(eqPhase * (0.8 + i * 0.65) + i * 1.9));
            eq[i] += (t - eq[i]) * Math.min(1f, dt * 8f);
        }
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }
    private static float easeOutQuad(float t) { return 1f - (1f - t) * (1f - t); }
    private static boolean over(float x, float y, float w, float h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    private static String trim(Font font, String text, float maxW) {
        if (text == null) return "";
        if (font.width(text) <= maxW) return text;
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + "\u2026") > maxW) end--;
        return end > 0 ? text.substring(0, end) + "\u2026" : "\u2026";
    }
}