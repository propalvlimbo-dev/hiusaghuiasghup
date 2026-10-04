package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.ui.kit.UiIcon;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;
import ru.rooyzee.elytrixclient.client.ui.menu.MenuKit;

/**
 * Еле заметная иконка справа сверху в главном меню: по клику — список фонов
 * ({@link MenuBackgrounds#NAMES}) с плавным раскрытием. Выбор сохраняется в конфиг.
 */
public final class MenuBgPicker {
    private MenuBgPicker() {
    }

    private static final int ICON = 18;
    private static final int POP_W = 128;
    private static final int ROW_H = 18;
    private static final int PAD = 5;

    private static boolean open;
    private static float openT;
    private static float iconHover;
    private static final float[] rowHover = new float[MenuBackgrounds.NAMES.length];
    private static long last;

    private static int iconX(int w) {
        return w - ICON - 8;
    }

    private static int iconY() {
        return 8;
    }

    private static int popX(int w) {
        return w - POP_W - 8;
    }

    private static int popY() {
        return iconY() + ICON + 6;
    }

    private static int popH() {
        return PAD * 2 + MenuBackgrounds.NAMES.length * ROW_H;
    }

    private static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void render(GuiGraphicsExtractor g, int w, int h, int mouseX, int mouseY) {
        long now = Util.getMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        Minecraft mc = Minecraft.getInstance();

        float ix = iconX(w);
        float iy = iconY();
        boolean hv = in(mouseX, mouseY, ix, iy, ICON, ICON);
        if (hv && iconHover < 0.02f) {
            UiSound.play(UiSound.Event.HOVER);
        }
        iconHover = MenuKit.approach(iconHover, hv || open ? 1f : 0f, 12f, dt);
        openT = MenuKit.approach(openT, open ? 1f : 0f, 14f, dt);

        // иконка: почти незаметна, проявляется при наведении
        float ia = 0.28f + 0.6f * iconHover;
        if (iconHover > 0.01f) {
            UiVector.roundRect(g, ix, iy, ICON, ICON, ICON / 2f, UiTheme.withAlpha(0x33FFFFFF, 0.45f * iconHover));
        }
        UiIcon.IMAGE.draw(g, (int) ix + 4, (int) iy + 4, 10,
                UiTheme.withAlpha(UiTheme.mix(0xFFFFFFFF, accent, open ? 1f : 0f), ia));

        if (openT <= 0.01f) {
            return;
        }
        float e = 1f - (1f - openT) * (1f - openT) * (1f - openT);
        float px = popX(w);
        float py = popY() - (1f - e) * 6f;
        float ph = popH();
        g.pose().pushMatrix();
        float ox = px + POP_W;
        g.pose().translate(ox, py);
        g.pose().scale(0.94f + 0.06f * e, 0.94f + 0.06f * e);
        g.pose().translate(-ox, -py);

        UiVector.shadow(g, px, py, POP_W, ph, 9f, 14f, UiTheme.withAlpha(0x99000000, e), 18);
        UiVector.roundRect(g, px, py, POP_W, ph, 9f, UiTheme.withAlpha(0xF0141118, e));
        UiVector.outline(g, px, py, POP_W, ph, 9f, 0.5f, UiTheme.withAlpha(0x2EFFFFFF, e));

        int current = MenuBackgrounds.current();
        for (int i = 0; i < MenuBackgrounds.NAMES.length; i++) {
            float rx = px + PAD;
            float ry = py + PAD + i * ROW_H;
            float rw = POP_W - PAD * 2;
            boolean rh = open && in(mouseX, mouseY, rx, ry, rw, ROW_H);
            if (rh && rowHover[i] < 0.02f) {
                UiSound.play(UiSound.Event.HOVER);
            }
            rowHover[i] = MenuKit.approach(rowHover[i], rh ? 1f : 0f, 16f, dt);
            float ra = e * Math.max(0f, Math.min(1f, openT * 1.4f - i * 0.08f));
            if (rowHover[i] > 0.01f || i == current) {
                int bg = i == current ? UiTheme.withAlpha(accent, 0.22f) : 0x14FFFFFF;
                UiVector.roundRect(g, rx, ry + 1, rw, ROW_H - 2, 6f,
                        UiTheme.withAlpha(bg, ra * Math.max(rowHover[i], i == current ? 1f : 0f)));
            }
            swatch(g, i, rx + 5, ry + 4, 18, ROW_H - 8, accent, ra);
            int tc = UiTheme.mix(0xFFCFCAD6, 0xFFFFFFFF, Math.max(rowHover[i], i == current ? 1f : 0f));
            MenuKit.text(g, mc.font, MenuBackgrounds.NAMES[i], rx + 29, MenuKit.ty(MenuKit.BODY, ry + ROW_H / 2f),
                    UiTheme.withAlpha(tc, ra), MenuKit.BODY);
            if (i == current) {
                UiIcon.CHECK.draw(g, (int) (rx + rw - 14), (int) (ry + 4), 10, UiTheme.withAlpha(accent, ra));
            }
        }
        g.pose().popMatrix();
    }

    /** Мини-превью фона: маленькая плашка в характерных цветах. */
    private static void swatch(GuiGraphicsExtractor g, int kind, float x, float y, float w, float h, int accent, float a) {
        int c1 = switch (kind) {
            case 1 -> UiTheme.mix(accent, 0xFF7C5CFF, 0.5f);
            case 2 -> 0xFF15121C;
            case 3 -> UiTheme.mix(accent, 0xFF000000, 0.55f);
            case 4 -> 0xFF050407;
            default -> 0xFF0D0810;
        };
        int c2 = switch (kind) {
            case 1 -> UiTheme.mix(accent, 0xFF38BDF8, 0.45f);
            case 2 -> UiTheme.mix(accent, 0xFF000000, 0.6f);
            case 3 -> accent;
            case 4 -> UiTheme.mix(accent, 0xFF000000, 0.7f);
            default -> UiTheme.mix(accent, 0xFF000000, 0.65f);
        };
        UiVector.roundRectGradient(g, x, y, w, h, 3f, UiTheme.withAlpha(c1, a), UiTheme.withAlpha(c2, a));
        UiVector.outline(g, x, y, w, h, 3f, 0.5f, UiTheme.withAlpha(0x33FFFFFF, a));
    }

    /** @return true, если клик обработан пикером (дальше меню его не получает). */
    public static boolean click(double mx, double my, int w) {
        if (in(mx, my, iconX(w), iconY(), ICON, ICON)) {
            open = !open;
            UiSound.play(open ? UiSound.Event.OPEN : UiSound.Event.CLOSE);
            return true;
        }
        if (!open) {
            return false;
        }
        float px = popX(w);
        float py = popY();
        for (int i = 0; i < MenuBackgrounds.NAMES.length; i++) {
            if (in(mx, my, px + PAD, py + PAD + i * ROW_H, POP_W - PAD * 2, ROW_H)) {
                ElytrixConfig cfg = ElytrixclientClient.CONFIG;
                cfg.menuBackground = i;
                cfg.save();
                UiSound.play(UiSound.Event.CLICK);
                open = false;
                return true;
            }
        }
        open = false;
        return in(mx, my, px, py, POP_W, popH());
    }

    public static void close() {
        open = false;
        openT = 0f;
    }
}
