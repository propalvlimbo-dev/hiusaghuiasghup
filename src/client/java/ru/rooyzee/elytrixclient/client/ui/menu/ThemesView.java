package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

import static ru.rooyzee.elytrixclient.client.ui.menu.MenuKit.*;

/** Вкладка «Темы»: 4 акцента; акцент меняет и цвет фона панели. */
public final class ThemesView {
    private static final int COLS = 4;
    private static final float CARD_H = 50;
    private static final float GAP = 7;

    private final ElytrixConfig cfg = ElytrixclientClient.CONFIG;
    private final Runnable dirty;
    private final float[] sel = new float[UiTheme.ACCENTS.length];
    private final float[] hov = new float[UiTheme.ACCENTS.length];
    private final float[] themeSel = new float[UiTheme.PRESET_NAMES.length];
    private final float[] themeHov = new float[UiTheme.PRESET_NAMES.length];
    private float x;
    private float y;
    private float w;
    private float appear = 1f;

    public ThemesView(Runnable dirty) {
        this.dirty = dirty;
        for (int i = 0; i < sel.length; i++) {
            sel[i] = -1f;
        }
        for (int i = 0; i < themeSel.length; i++) {
            themeSel[i] = -1f;
        }
    }

    public void replay() {
        appear = 0f;
    }

    public void layout(float x, float y, float w) {
        this.x = x;
        this.y = y;
        this.w = w;
    }

    private float cardW() {
        return (w - GAP * (COLS - 1)) / COLS;
    }

    private float accentX(int i) {
        return x + (i % COLS) * (cardW() + GAP);
    }

    private float accentY(int i) {
        return y + 16 + (i / COLS) * (CARD_H + GAP);
    }

    private float themesTop() {
        int rows = (UiTheme.ACCENTS.length + COLS - 1) / COLS;
        return y + 16 + rows * (CARD_H + GAP) + 14;
    }

    private float themeW() {
        return (w - GAP) / 2f;
    }

    public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
        appear = approach(appear, 1f, 12f, dt);
        float saved = MenuKit.alpha;
        MenuKit.alpha = saved * appear;

        text(g, font, "Акцент", x, ty(SMALL, y + 6), dim(), SMALL);
        float cw = cardW();
        for (int i = 0; i < UiTheme.ACCENTS.length; i++) {
            float cx = accentX(i);
            float cy = accentY(i) + (1f - appear) * (4 + i);
            boolean on = i == cfg.accentIndex;
            boolean hv = inside(mx, my, cx, cy, cw, CARD_H);
            sel[i] = sel[i] < 0 ? (on ? 1f : 0f) : approach(sel[i], on ? 1f : 0f, 14f, dt);
            hov[i] = approach(hov[i], hv ? 1f : 0f, 16f, dt);

            int c1 = UiTheme.ACCENTS[i];
            int c2 = UiTheme.mix(c1, 0xFFFFFFFF, 0.42f);
            card(g, cx, cy, cw, CARD_H, 6, cardFill(), UiTheme.mix(cardEdge(), c1, Math.max(sel[i], 0.35f * hov[i])));
            // превью: градиент акцента и «кнопка» с текстом
            hgrad(g, cx + 4, cy + 4, cw - 8, 26, 4, c1, c2);
            fill(g, cx + 9, cy + 13, 26, 8, 4, 0x55FFFFFF);
            disc(g, cx + cw - 13, cy + 17, 4, 0xCCFFFFFF);
            text(g, font, UiTheme.ACCENT_NAMES[i], cx + 7, ty(SMALL, cy + 40), UiTheme.mix(soft(), text(), sel[i]), SMALL);
            if (sel[i] > 0.01f) {
                disc(g, cx + cw - 10, cy + 40, 3.5f * sel[i], c1);
            }
        }

        MenuKit.alpha = saved;
    }

    /** Миниатюра панели в цветах темы: сайдбар, пункт меню, пара карточек. */
    private void miniPanel(GuiGraphicsExtractor g, float px, float py, float pw, float ph, boolean light) {
        int side = light ? 0xFFF3F1F6 : 0xFF17141C;
        int body = light ? 0xFFFBFAFC : 0xFF121016;
        int cardC = light ? 0xFFFFFFFF : 0xFF1C1822;
        int edge = light ? 0x2414141A : 0xFF2A2430;
        int line = light ? 0x3314141A : 0x33FFFFFF;
        fill(g, px, py, pw, ph, 4, body);
        fill(g, px, py, pw * 0.26f, ph, 4, 0, 0, 4, side);
        hgrad(g, px + 3, py + 8, pw * 0.26f - 6, 5, 2, accent, accent2());
        for (int k = 0; k < 3; k++) {
            fill(g, px + 4, py + 17 + k * 7, pw * 0.26f - 12, 2, 1, line);
        }
        float cx = px + pw * 0.26f + 5;
        float cw = (pw * 0.74f - 15) / 2f;
        for (int k = 0; k < 2; k++) {
            float x0 = cx + k * (cw + 5);
            card(g, x0, py + 6, cw, ph - 12, 3, cardC, edge);
            fill(g, x0 + 4, py + 11, cw * 0.5f, 2, 1, line);
            fill(g, x0 + cw - 12, py + 18, 8, 4, 2, k == 0 ? accent : line);
            fill(g, x0 + 4, py + 19, cw * 0.4f, 2, 1, line);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        float cw = cardW();
        for (int i = 0; i < UiTheme.ACCENTS.length; i++) {
            if (inside(mx, my, accentX(i), accentY(i), cw, CARD_H)) {
                cfg.accentIndex = i;
                MenuKit.accent = UiTheme.accent(i);
                cfg.accent = String.format("#%06X", MenuKit.accent & 0xFFFFFF);
                dirty.run();
                return true;
            }
        }
        return false;
    }
}
