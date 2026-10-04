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

    private static final float IH = 20, IH_EXP = 56, IR = 10;
    private static final float IW_EMPTY = 48, IW_PLAY = 150, IW_EXP = 220;
    private static final float BW = 36, BH = 20, BGAP = 6;

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
        openT += ((wantOpen ? 1f : 0f) - openT) * (1f - (float) Math.exp(-6f * dt));
        if (openT < 0.005f && !wantOpen) { openT = 0f; sW = 0; return; }
        if (!wantOpen) expanded = false;
        expandT += ((expanded ? 1f : 0f) - expandT) * (1f - (float) Math.exp(-12f * dt));
        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);

        float curH = IH + (IH_EXP - IH) * easeOut(expandT);
        float wantW = expanded ? IW_EXP : (openT > 0.5f ? IW_PLAY : IW_EMPTY);
        float curW = IW_EMPTY + (wantW - IW_EMPTY) * easeOut(Math.min(1f, openT * 2f));
        float cx = sw / 2f - curW / 2f;
        float cy = 6;
        float rad = IR + 2f * expandT;
        sX = cx; sY = cy; sW = curW; sH = curH;
        boolean over = mx >= cx && mx <= cx + curW && my >= cy && my <= cy + curH;
        hoverT += ((over ? 1f : 0f) - hoverT) * (1f - (float) Math.exp(-14f * dt));

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
        UiVector.shadow(g, cx, cy, curW, curH, rad, 4f + 2f * hoverT, 0x30000000, 5);
        int bg = UiTheme.mix(0xFF0A0A0A, UiTheme.withAlpha(accent, 0.1f), hoverT * 0.2f);
        UiVector.roundRect(g, cx, cy, curW, curH, rad, bg);
        UiVector.outline(g, cx, cy, curW, curH, rad, 0.5f, UiTheme.withAlpha(accent, 0.08f + 0.1f * hoverT));

        // строка 1: иконка + трек + эквалайзер
        float row1Y = cy + (expanded ? 4 : (IH - 12) / 2f);
        float iconX = cx + 5;
        UiVector.roundRect(g, iconX - 1, row1Y - 1, 13, 13, 6, UiTheme.withAlpha(accent, 0.2f));
        UiIcon.MUSIC.draw(g, (int) iconX, (int) row1Y, 11, UiTheme.withAlpha(0xFFFFFFFF, 0.9f));

        if (openT > 0.2f) {
            float ta = Math.min(1f, (openT - 0.2f) / 0.3f);
            String track = CustomMusic.nowPlaying();
            if (track == null || track.equals("\u2014")) track = "...";
            String shown = trim(font, track, curW - 15 - (expanded ? 14 : 28));
            UiText.draw(g, font, shown, Math.round(cx + 20), Math.round(row1Y + 2),
                    UiTheme.withAlpha(0xFFFFFFFF, ta * 0.9f), UiText.FACE, false);
        }

        if (!expanded && openT > 0.3f) {
            updateEq(dt);
            int eqCol = UiTheme.withAlpha(accent, Math.min(1f, (openT - 0.3f) / 0.3f) * (0.6f + 0.3f * hoverT));
            float eqX = cx + curW - 17;
            float eqY = cy + IH / 2f;
            for (int i = 0; i < eq.length; i++) {
                float bh = 3 + eq[i] * 8;
                UiVector.roundRect(g, eqX + i * 3.5f, eqY - bh / 2f, 2, bh, 1f, eqCol);
            }
        }

        // строка 2: контролы (текстовые метки вместо Unicode-иконок)
        if (expandT > 0.05f) {
            float a2 = expandT;
            float by = cy + IH + 4;
            float totalW = BW * 5 + BGAP * 4;
            float bx = cx + (curW - totalW) / 2f;
            String[] lbl = {"|<", CustomMusic.isPlaying() ? "||" : ">", ">|", "-", "+"};
            for (int i = 0; i < 5; i++) {
                float x = bx + i * (BW + BGAP);
                btnR[i] = new float[]{x, by, BW, BH};
                boolean hv = over(x, by, BW, BH, mx, my);
                drawBtn(g, font, x, by, BW, BH, lbl[i], accent, a2, hv);
            }
        } else {
            for (float[] r : btnR) r[0] = r[1] = r[2] = r[3] = 0;
        }
    }

    public static void onClick(int mx, int my) {}

    private static void drawBtn(GuiGraphicsExtractor g, Font font, float x, float y,
            float w, float h, String label, int accent, float alpha, boolean hover) {
        int fill = UiTheme.mix(0xFF1A1A1A, UiTheme.withAlpha(accent, 0.3f), hover ? 1f : 0f);
        UiVector.roundRect(g, x, y, w, h, 5, UiTheme.withAlpha(fill, alpha * 0.95f));
        UiVector.outline(g, x, y, w, h, 5, 0.5f, UiTheme.withAlpha(0x44FFFFFF, alpha * (hover ? 0.8f : 0.4f)));
        int col = UiTheme.withAlpha(0xFFFFFFFF, alpha * (hover ? 1f : 0.7f));
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
        while (end > 0 && font.width(text.substring(0, end) + "\u2026") > maxW) end--;
        return end > 0 ? text.substring(0, end) + "\u2026" : "\u2026";
    }
}