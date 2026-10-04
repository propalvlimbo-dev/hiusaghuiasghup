package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Примитивы отрисовки кастомного GUI: скруглённые прямоугольники и градиенты,
 * мягкая тень, круги/линии (для иконок), текст и прогресс-бар.
 *
 * <p>Ничего не знает о состоянии экрана — просто рисует в {@link GuiGraphicsExtractor}.
 * Так как игра рисует GUI «командами» (deferred render state), избыточные вызовы
 * {@code fill(...)} не страшны: скругление угла — это несколько тонких полосок.
 */
public final class UiDraw {
    private UiDraw() {
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Скругления и градиенты
    // ─────────────────────────────────────────────────────────────────────

    public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        if (h - 2 * r > 0) {
            g.fill(x, y + r, x + w, y + h - r, color);
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            inset = Math.max(0, Math.min(inset, r));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /** Скруглённый прямоугольник с вертикальным градиентом (сверху {@code top} → снизу {@code bottom}). */
    public static void roundRectGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int top, int bottom) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            int color = UiTheme.mix(top, bottom, t);
            int inset = 0;
            if (i < r) {
                double dy = r - i - 0.5;
                inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            } else if (i >= h - r) {
                double dy = r - (h - i) + 0.5;
                inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            }
            inset = Math.max(0, Math.min(inset, r));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }

    /** Рамка скруглённого прямоугольника толщиной 1: внешний контур цветом рамки + заливка внутри. */
    public static void roundRectBordered(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int fill, int border) {
        roundRect(g, x, y, w, h, radius, border);
        roundRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    /** Вертикальная тень-«облако» вокруг панели (несколько полупрозрачных слоёв). */
    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int layers, int color) {
        for (int i = layers; i >= 1; i--) {
            roundRect(g, x - i, y - i + 1, w + 2 * i, h + 2 * i, radius + i, color);
        }
    }

    /** Горизонтальный градиент — рисуем полосками (в 26.2 есть только вертикальный fillGradient). */
    public static void hGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int left, int right, int steps) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int n = Math.max(1, Math.min(steps, w));
        for (int i = 0; i < n; i++) {
            int x0 = x + w * i / n;
            int x1 = x + w * (i + 1) / n;
            if (x1 > x0) {
                g.fill(x0, y, x1, y + h, UiTheme.mix(left, right, (i + 0.5f) / n));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Линии и фигуры (иконки)
    // ─────────────────────────────────────────────────────────────────────

    public static void hLine(GuiGraphicsExtractor g, int x0, int x1, int y, int thickness, int color) {
        if (x1 < x0) {
            int t = x0;
            x0 = x1;
            x1 = t;
        }
        g.fill(x0, y, x1, y + Math.max(1, thickness), color);
    }

    public static void vLine(GuiGraphicsExtractor g, int x, int y0, int y1, int thickness, int color) {
        if (y1 < y0) {
            int t = y0;
            y0 = y1;
            y1 = t;
        }
        g.fill(x, y0, x + Math.max(1, thickness), y1, color);
    }

    /** Отрезок произвольной длины: раскладываем на маленькие квадраты вдоль линии. */
    public static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, float thickness, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        int steps = (int) Math.max(Math.abs(dx), Math.abs(dy)) + 1;
        int size = Math.max(1, Math.round(thickness));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int px = Math.round(x0 + dx * t);
            int py = Math.round(y0 + dy * t);
            g.fill(px - size / 2, py - size / 2, px - size / 2 + size, py - size / 2 + size, color);
        }
    }

    public static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
        if (r <= 0) {
            return;
        }
        int ri = (int) Math.ceil(r);
        int icx = Math.round(cx);
        int icy = Math.round(cy);
        for (int i = -ri; i <= ri; i++) {
            float y = i + 0.5f;
            if (Math.abs(y) > r) {
                continue;
            }
            int half = Math.round((float) Math.sqrt(Math.max(0.0, r * r - y * y)));
            if (half > 0) {
                g.fill(icx - half, icy + i, icx + half, icy + i + 1, color);
            }
        }
    }

    /** Кольцо (контур круга) — не требует знания цвета фона под ним. */
    public static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, float thickness, int color) {
        if (r <= 0) {
            return;
        }
        float inner = Math.max(0f, r - thickness);
        int ri = (int) Math.ceil(r);
        int icx = Math.round(cx);
        int icy = Math.round(cy);
        for (int i = -ri; i <= ri; i++) {
            float y = i + 0.5f;
            if (Math.abs(y) > r) {
                continue;
            }
            float outerW = (float) Math.sqrt(Math.max(0.0, r * r - y * y));
            float innerW = Math.abs(y) < inner ? (float) Math.sqrt(Math.max(0.0, inner * inner - y * y)) : 0f;
            int xl0 = Math.round(cx - outerW);
            int xl1 = Math.round(cx - innerW);
            int xr0 = Math.round(cx + innerW);
            int xr1 = Math.round(cx + outerW);
            if (xl1 > xl0) {
                g.fill(xl0, icy + i, xl1, icy + i + 1, color);
            }
            if (xr1 > xr0) {
                g.fill(xr0, icy + i, xr1, icy + i + 1, color);
            }
        }
    }

    /** Треугольник «play» вершиной вправо. */
    public static void playTriangle(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        int half = h / 2;
        for (int i = 0; i <= half; i++) {
            int len = Math.round((float) w * (half - i) / half);
            g.fill(x, y + i, x + len, y + i + 1, color);
            g.fill(x, y + h - 1 - i, x + len, y + h - i, color);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Текст
    // ─────────────────────────────────────────────────────────────────────

    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.text(font, s, x, y, color, false);
    }

    public static void textShadow(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.text(font, s, x, y, color, true);
    }

    public static void textCenter(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) {
        g.text(font, s, cx - font.width(s) / 2, y, color, false);
    }

    public static void textRight(GuiGraphicsExtractor g, Font font, String s, int right, int y, int color) {
        g.text(font, s, right - font.width(s), y, color, false);
    }

    public static void textCenterShadow(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) {
        g.text(font, s, cx - font.width(s) / 2, y, color, true);
    }

    public static void textComponentCenter(GuiGraphicsExtractor g, Font font, Component c, int cx, int y, int color) {
        g.centeredText(font, c, cx, y, color);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Прогресс
    // ─────────────────────────────────────────────────────────────────────

    /** Полоска прогресса: тёмный трек + заливка акцентным градиентом + блик. */
    public static void progress(GuiGraphicsExtractor g, int x, int y, int w, int h, float value, int accent) {
        float v = Math.max(0f, Math.min(1f, value));
        int r = h / 2;
        roundRect(g, x, y, w, h, Math.max(1, r), UiTheme.TRACK);
        int fillW = Math.round(w * v);
        if (fillW >= 2) {
            int light = UiTheme.mix(accent, 0xFFFFFFFF, 0.35f);
            int fillColor = UiTheme.mix(light, accent, 0.35f);
            roundRect(g, x, y, fillW, h, Math.max(1, r), fillColor);
            roundRect(g, x, y, fillW, Math.max(1, h / 2), Math.max(1, r), UiTheme.withAlpha(0xFFFFFFFF, 0.18f));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Иконки (рисуются кодом, без текстур)
    // ─────────────────────────────────────────────────────────────────────

    public enum Icon {
        HOME, BOTS, PROXY, CONSOLE, SETTINGS, CLOSE, CHEVRON, CHECK, SEARCH, PLUS, DOTS, BOLT
    }

    public static void icon(GuiGraphicsExtractor g, Icon icon, int x, int y, int size, int color) {
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float s = size;
        switch (icon) {
            case HOME -> {
                line(g, x + s * 0.10f, cy, cx, y + s * 0.16f, Math.max(1.6f, s * 0.13f), color);
                line(g, cx, y + s * 0.16f, x + s * 0.90f, cy, Math.max(1.6f, s * 0.13f), color);
                roundRect(g, Math.round(x + s * 0.22f), Math.round(cy - s * 0.02f), Math.round(s * 0.56f), Math.round(s * 0.44f), 2, color);
            }
            case BOTS -> {
                disc(g, cx, y + s * 0.27f, s * 0.16f, color);
                roundRect(g, Math.round(x + s * 0.18f), Math.round(y + s * 0.54f), Math.round(s * 0.64f), Math.round(s * 0.34f), Math.round(s * 0.17f), color);
                vLine(g, Math.round(cx), Math.round(y + s * 0.06f), Math.round(y + s * 0.14f), Math.max(1, Math.round(s * 0.07f)), color);
            }
            case PROXY -> {
                ring(g, cx, cy, s * 0.42f, Math.max(1.4f, s * 0.09f), color);
                hLine(g, Math.round(cx - s * 0.42f), Math.round(cx + s * 0.42f), Math.round(cy), Math.max(1, Math.round(s * 0.08f)), color);
                line(g, cx - s * 0.20f, cy - s * 0.36f, cx - s * 0.20f, cy + s * 0.36f, Math.max(1f, s * 0.07f), color);
                line(g, cx + s * 0.20f, cy - s * 0.36f, cx + s * 0.20f, cy + s * 0.36f, Math.max(1f, s * 0.07f), color);
            }
            case CONSOLE -> {
                roundRectBordered(g, Math.round(x + s * 0.06f), Math.round(y + s * 0.14f), Math.round(s * 0.88f), Math.round(s * 0.72f), 3, 0x00000000, color);
                line(g, x + s * 0.26f, y + s * 0.36f, x + s * 0.44f, y + s * 0.50f, Math.max(1.2f, s * 0.09f), color);
                line(g, x + s * 0.44f, y + s * 0.50f, x + s * 0.26f, y + s * 0.64f, Math.max(1.2f, s * 0.09f), color);
                hLine(g, Math.round(x + s * 0.56f), Math.round(x + s * 0.74f), Math.round(y + s * 0.64f), Math.max(1, Math.round(s * 0.09f)), color);
            }
            case SETTINGS -> {
                ring(g, cx, cy, s * 0.24f, Math.max(1.4f, s * 0.10f), color);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4.0;
                    int tx = Math.round(cx + (float) Math.cos(a) * s * 0.40f);
                    int ty = Math.round(cy + (float) Math.sin(a) * s * 0.40f);
                    int t = Math.max(2, Math.round(s * 0.14f));
                    roundRect(g, tx - t / 2, ty - t / 2, t, t, 1, color);
                }
            }
            case CLOSE -> {
                float t = Math.max(1.3f, s * 0.11f);
                line(g, x + s * 0.28f, y + s * 0.28f, x + s * 0.72f, y + s * 0.72f, t, color);
                line(g, x + s * 0.72f, y + s * 0.28f, x + s * 0.28f, y + s * 0.72f, t, color);
            }
            case CHEVRON -> {
                float t = Math.max(1.2f, s * 0.11f);
                line(g, x + s * 0.30f, y + s * 0.42f, cx, y + s * 0.62f, t, color);
                line(g, cx, y + s * 0.62f, x + s * 0.70f, y + s * 0.42f, t, color);
            }
            case CHECK -> {
                float t = Math.max(1.2f, s * 0.12f);
                line(g, x + s * 0.24f, y + s * 0.52f, x + s * 0.44f, y + s * 0.72f, t, color);
                line(g, x + s * 0.44f, y + s * 0.72f, x + s * 0.78f, y + s * 0.30f, t, color);
            }
            case SEARCH -> {
                float r = s * 0.32f;
                ring(g, cx - s * 0.06f, cy - s * 0.06f, r, Math.max(1.3f, s * 0.10f), color);
                line(g, cx + r * 0.62f, cy + r * 0.62f, x + s * 0.86f, y + s * 0.86f, Math.max(1.3f, s * 0.11f), color);
            }
            case PLUS -> {
                hLine(g, Math.round(x + s * 0.24f), Math.round(x + s * 0.76f), Math.round(cy), Math.max(1, Math.round(s * 0.12f)), color);
                vLine(g, Math.round(cx), Math.round(y + s * 0.24f), Math.round(y + s * 0.76f), Math.max(1, Math.round(s * 0.12f)), color);
            }
            case DOTS -> {
                disc(g, x + s * 0.24f, cy, s * 0.09f, color);
                disc(g, cx, cy, s * 0.09f, color);
                disc(g, x + s * 0.76f, cy, s * 0.09f, color);
            }
            case BOLT -> {
                line(g, x + s * 0.56f, y + s * 0.12f, x + s * 0.34f, y + s * 0.54f, Math.max(1.6f, s * 0.15f), color);
                line(g, x + s * 0.34f, y + s * 0.54f, x + s * 0.62f, y + s * 0.52f, Math.max(1.6f, s * 0.15f), color);
                line(g, x + s * 0.62f, y + s * 0.52f, x + s * 0.44f, y + s * 0.88f, Math.max(1.6f, s * 0.15f), color);
            }
        }
    }
}
