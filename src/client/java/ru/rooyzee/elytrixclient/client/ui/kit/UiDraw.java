package ru.rooyzee.elytrixclient.client.ui.kit;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

/**
 * Примитивы отрисовки кастомного GUI: скруглённые прямоугольники и градиенты,
 * мягкая тень, круги/линии (для иконок), текст и прогресс-бар.
 *
 * <p>Ничего не знает о состоянии экрана — просто рисует в {@link GuiGraphicsExtractor}.
 *
 * <p>Скругления, круги, тени и свечения берутся из спрайтов, посчитанных по SDF
 * (мягкий край вместо «лестницы» из прямоугольников). Спрайты есть под каждый
 * масштаб интерфейса игры ({@code _x1}...{@code _x4}) и рисуются 1:1 — поэтому
 * сглаживание сохраняется и на GUI Scale 2–4, где всё остальное обычно «квадратится».
 */
public final class UiDraw {
    /** Рисовать ли «свечения» (glow) — выключается режимом качества на больших разрешениях. */
    public static boolean GLOW = true;

    /**
     * Векторный рендер форм ({@link UiVector}) вместо спрайтов.
     *
     * <p>Спрайты давали мягкий край, только если рисовались ровно 1:1 в пиксель экрана; при
     * любом дробном размере панели или масштабе они «квадратились». Векторные формы считаются
     * геометрией и сглаживаются всегда, поэтому включены по умолчанию. Флаг оставлен как
     * аварийный переключатель: {@code false} — вернуться на старую отрисовку спрайтами.
     */
    public static boolean VECTOR = true;

    // ─────────────────────────────────────────────────────────────────────
    //  Спрайты форм (сгенерированы scripts/make-ui-shapes.py)
    //
    //  Почему так: скругления и круги из прямоугольников дают «лестницу» на
    //  краях. Здесь формы посчитаны как SDF и покрыты по альфе — край мягкий.
    //  Углы скруглений рисуются из спрайта 1:1 (без масштабирования текстуры),
    //  поэтому сглаживание сохраняется на любом размере панели.
    // ─────────────────────────────────────────────────────────────────────

    private static final String SHAPE_DIR = "textures/gui/shapes/";
    private static final int[] SPRITE_RADII = {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 24, 28};
    private static final int[] DISC_SIZES = {4, 6, 8, 10, 12, 16, 20, 24, 32, 48, 64};
    private static final int SHADOW_TEX = 64;
    /** Отступ формы внутри спрайта тени (ширина размытия) + радиус. */
    private static final int SHADOW_NINE = 26;
    /** Спрайты есть под каждый масштаб интерфейса: _x1 (1:1) ... _x4. */
    private static final int K_MAX = 4;
    private static final Map<String, Identifier> TEX_CACHE = new HashMap<>();

    /**
     * Текущий масштаб интерфейса игры (1..4). Игра умножает единицы интерфейса
     * на этот множитель, поэтому спрайт нужного размера берём под него — тогда
     * сглаженный край ложится ровно в пиксели, без «ступенек» из квадратов.
     */
    /**
     * Пикселей на единицу внутри панели, если она нарисована в нецелом масштабе
     * (0 — как масштаб интерфейса). Тогда шрифты/иконки/формы берутся под это значение.
     */
    public static int scaleOverride = 0;

    public static int shapeScale() {
        if (scaleOverride > 0) {
            return Math.max(1, Math.min(K_MAX, scaleOverride));
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return 1;
        }
        return Math.max(1, Math.min(K_MAX, mc.getWindow().getGuiScale()));
    }

    private static Identifier shape(String name, int scale) {
        String key = name + "_x" + Math.max(1, Math.min(K_MAX, scale));
        Identifier id = TEX_CACHE.get(key);
        if (id == null) {
            id = Identifier.fromNamespaceAndPath("elytrixclient", SHAPE_DIR + key + ".png");
            TEX_CACHE.put(key, id);
        }
        return id;
    }

    /** Текстура из папки спрайтов форм под текущий масштаб интерфейса. */
    public static Identifier shapeTexture(String name) {
        return shape(name, shapeScale());
    }

    private static int nearest(int[] values, int want) {
        int best = values[0];
        int bestDelta = Integer.MAX_VALUE;
        for (int v : values) {
            int d = Math.abs(v - want);
            if (d < bestDelta) {
                bestDelta = d;
                best = v;
            }
        }
        return best;
    }

    private static void blitShape(GuiGraphicsExtractor g, Identifier texture, int x, int y, int u, int v,
                                  int w, int h, int srcW, int srcH, int texSize, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, srcW, srcH, texSize, texSize, color);
    }

    /** Растягивание спрайта по 9 частям: углы не искажаются, края и центр тянутся. */
    private static void nineSlice(GuiGraphicsExtractor g, Identifier texture, int texSize, int m,
                                  int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int k = shapeScale();
        int texPx = texSize * k;
        int mPx = m * k;
        int midPx = texPx - 2 * mPx;
        if (w < 2 * m + 1 || h < 2 * m + 1) {
            // мало места — тянем целиком (мягкие края всё равно сглажены)
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, texPx, texPx, texPx, texPx, color);
            return;
        }
        blitShape(g, texture, x, y, 0, 0, m, m, mPx, mPx, texPx, color);
        blitShape(g, texture, x + w - m, y, mPx + midPx, 0, m, m, mPx, mPx, texPx, color);
        blitShape(g, texture, x, y + h - m, 0, mPx + midPx, m, m, mPx, mPx, texPx, color);
        blitShape(g, texture, x + w - m, y + h - m, mPx + midPx, mPx + midPx, m, m, mPx, mPx, texPx, color);
        blitShape(g, texture, x + m, y, mPx, 0, w - 2 * m, m, midPx, mPx, texPx, color);
        blitShape(g, texture, x + m, y + h - m, mPx, mPx + midPx, w - 2 * m, m, midPx, mPx, texPx, color);
        blitShape(g, texture, x, y + m, 0, mPx, m, h - 2 * m, mPx, midPx, texPx, color);
        blitShape(g, texture, x + w - m, y + m, mPx + midPx, mPx, m, h - 2 * m, mPx, midPx, texPx, color);
        blitShape(g, texture, x + m, y + m, mPx, mPx, w - 2 * m, h - 2 * m, midPx, midPx, texPx, color);
    }

    private UiDraw() {
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Скругления и градиенты
    // ─────────────────────────────────────────────────────────────────────

    public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (color == 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (VECTOR) {
            UiVector.roundRect(g, x, y, w, h, r, color);
            return;
        }
        if (r < 2) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        int k = shapeScale();
        int sr = nearest(SPRITE_RADII, r);
        Identifier tex = shape("round_" + sr, k);
        int src = sr * k;                 // сторона угла в пикселях спрайта
        int off = src + 2 * k;            // смещение до правого/нижнего угла
        int ts = (2 * sr + 2) * k;        // размер всего спрайта
        if (w - 2 * r > 0) {
            g.fill(x + r, y, x + w - r, y + h, color);
        }
        if (h - 2 * r > 0) {
            g.fill(x, y + r, x + r, y + h - r, color);
            g.fill(x + w - r, y + r, x + w, y + h - r, color);
        }
        blitShape(g, tex, x, y, 0, 0, r, r, src, src, ts, color);
        blitShape(g, tex, x + w - r, y, off, 0, r, r, src, src, ts, color);
        blitShape(g, tex, x, y + h - r, 0, off, r, r, src, src, ts, color);
        blitShape(g, tex, x + w - r, y + h - r, off, off, r, r, src, src, ts, color);
    }

    /** Скруглённый прямоугольник с вертикальным градиентом (сверху {@code top} → снизу {@code bottom}). */
    public static void roundRectGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int top, int bottom) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (VECTOR) {
            UiVector.roundRectGradient(g, x, y, w, h, r, top, bottom);
            return;
        }
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            int color = UiTheme.mix(top, bottom, t);
            if (r >= 2 && (i < r || i >= h - r)) {
                // угловые полосы: прямую часть рисуем заливкой, углы — спрайтом с AA
                if (w - 2 * r > 0) {
                    g.fill(x + r, y + i, x + w - r, y + i + 1, color);
                }
            } else {
                g.fill(x, y + i, x + w, y + i + 1, color);
            }
        }
        if (r >= 2) {
            int k = shapeScale();
            int sr = nearest(SPRITE_RADII, r);
            Identifier tex = shape("round_" + sr, k);
            int src = sr * k;
            int off = src + 2 * k;
            int ts = (2 * sr + 2) * k;
            int tc = UiTheme.mix(top, bottom, Math.min(1f, (r * 0.5f) / Math.max(1, h - 1)));
            int bc = UiTheme.mix(top, bottom, Math.max(0f, 1f - (r * 0.5f) / Math.max(1, h - 1)));
            blitShape(g, tex, x, y, 0, 0, r, r, src, src, ts, tc);
            blitShape(g, tex, x + w - r, y, off, 0, r, r, src, src, ts, tc);
            blitShape(g, tex, x, y + h - r, 0, off, r, r, src, src, ts, bc);
            blitShape(g, tex, x + w - r, y + h - r, off, off, r, r, src, src, ts, bc);
        }
    }

    /** Рамка скруглённого прямоугольника толщиной 1: внешний контур цветом рамки + заливка внутри. */
    public static void roundRectBordered(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int fill, int border) {
        if (VECTOR) {
            UiVector.roundRect(g, x, y, w, h, radius, fill);
            UiVector.outline(g, x, y, w, h, radius, 1f, border);
            return;
        }
        roundRect(g, x, y, w, h, radius, border);
        roundRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    /** Вертикальная тень-«облако» вокруг панели (несколько полупрозрачных слоёв). */
    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int layers, int color) {
        if (color == 0) {
            return;
        }
        if (VECTOR) {
            float spread = Math.max(4f, 2f + layers * 3f);
            UiVector.shadow(g, x, y + 2, w, h, radius, spread, color, Math.max(3, Math.min(layers, 8)));
            return;
        }
        // один размытый спрайт вместо стопки жёстких прямоугольников
        int spread = Math.max(4, 2 + layers * 3);
        nineSlice(g, shapeTexture("shadow"), SHADOW_TEX, SHADOW_NINE,
                x - spread, y - spread + 2, w + 2 * spread, h + 2 * spread, color);
    }

    /** Горизонтальный градиент — рисуем полосками (в 26.2 есть только вертикальный fillGradient). */
    public static void hGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int left, int right, int steps) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (VECTOR) {
            UiVector.roundRectBilinear(g, x, y, w, h, 0f, 0f, 0f, 0f, left, right, right, left);
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

    /** Отрезок произвольной длины: раскладываем на квадраты вдоль линии (шаг = толщина). */
    public static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, float thickness, int color) {
        if (VECTOR) {
            UiVector.line(g, x0, y0, x1, y1, Math.max(0.5f, thickness), color);
            return;
        }
        float dx = x1 - x0;
        float dy = y1 - y0;
        int size = Math.max(1, Math.round(thickness));
        // шаг по толщине, но не меньше 1 px и не больше 96 квадратов — иначе это уже не иконка
        int steps = Math.min(96, Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy)) / size)));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int px = Math.round(x0 + dx * t);
            int py = Math.round(y0 + dy * t);
            g.fill(px - size / 2, py - size / 2, px - size / 2 + size, py - size / 2 + size, color);
        }
    }

    public static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
        if (r <= 0 || color == 0) {
            return;
        }
        if (VECTOR) {
            UiVector.roundRect(g, cx - r, cy - r, r * 2f, r * 2f, r, color);
            return;
        }
        int d = Math.max(2, Math.round(r * 2f));
        if (d < 4) {
            g.fill(Math.round(cx - r), Math.round(cy - r), Math.round(cx - r) + d, Math.round(cy - r) + d, color);
            return;
        }
        int k = shapeScale();
        int size = nearest(DISC_SIZES, d);
        blitShape(g, shape("disc_" + size, k), Math.round(cx - r), Math.round(cy - r), 0, 0,
                d, d, size * k, size * k, size * k, color);
    }

    /** Кольцо (контур круга) — не требует знания цвета фона под ним. */
    public static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, float thickness, int color) {
        if (r <= 0) {
            return;
        }
        if (VECTOR) {
            UiVector.outline(g, cx - r, cy - r, r * 2f, r * 2f, r, thickness, color);
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
        if (VECTOR) {
            UiVector.triangle(g, x, y, x + w, y + h / 2f, x, y + h, color);
            return;
        }
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

    /** Ширина строки в текущем шрифте интерфейса (не в ванильном bitmap). */
    public static int width(Font font, String s) {
        return UiText.width(font, s, UiText.FACE);
    }

    /** Ширина строки в конкретном шрифте (заголовки — {@link UiText#TITLE}, консоль — {@link UiText#MONO}). */
    public static int width(Font font, String s, Identifier face) {
        return UiText.width(font, s, face);
    }

    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        UiText.draw(g, font, s, x, y, color, UiText.FACE, false);
    }

    /** Текст своим шрифтом (заголовки, моноширинные значения). */
    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color, Identifier face) {
        UiText.draw(g, font, s, x, y, color, face, false);
    }

    public static void textShadow(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        UiText.draw(g, font, s, x, y, color, UiText.FACE, true);
    }

    public static void textCenter(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) {
        textCenter(g, font, s, cx, y, color, UiText.FACE);
    }

    public static void textCenter(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color,
                                  Identifier face) {
        UiText.draw(g, font, s, cx - UiText.width(font, s, face) / 2, y, color, face, false);
    }

    public static void textRight(GuiGraphicsExtractor g, Font font, String s, int right, int y, int color) {
        UiText.draw(g, font, s, right - UiText.width(font, s, UiText.FACE), y, color, UiText.FACE, false);
    }

    /** Текст, прижатый вправо, своим шрифтом (моно для чисел и адресов). */
    public static void textRight(GuiGraphicsExtractor g, Font font, String s, int right, int y, int color,
                                 Identifier face) {
        UiText.draw(g, font, s, right - UiText.width(font, s, face), y, color, face, false);
    }

    /** Текст с увеличенным межбуквенным интервалом (заголовки «в стиле интерфейса»). */
    public static void textSpaced(GuiGraphicsExtractor g, Font font, String s, int x, int y, int spacing,
                                  int color, boolean shadow) {
        textSpaced(g, font, s, x, y, spacing, color, shadow, UiText.FACE);
    }

    public static void textSpaced(GuiGraphicsExtractor g, Font font, String s, int x, int y, int spacing,
                                  int color, boolean shadow, Identifier face) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            int cw = UiText.width(font, ch, face);
            if (ch.equals(" ")) {
                cx += Math.max(3, cw) + spacing;
                continue;
            }
            UiText.draw(g, font, ch, cx, y, color, face, shadow);
            cx += cw + spacing;
        }
    }

    public static int spacedWidth(Font font, String s, int spacing) {
        return spacedWidth(font, s, spacing, UiText.FACE);
    }

    public static int spacedWidth(Font font, String s, int spacing, Identifier face) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            int cw = UiText.width(font, ch, face);
            w += (ch.equals(" ") ? Math.max(3, cw) : cw) + spacing;
        }
        return Math.max(0, w - spacing);
    }

    public static void textCenterShadow(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) {
        UiText.draw(g, font, s, cx - UiText.width(font, s, UiText.FACE) / 2, y, color, UiText.FACE, true);
    }

    /** Обрезает строку так, чтобы она влезла в {@code maxWidth} (с «…»), — для узких мест. */
    public static String trim(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (UiText.width(font, text, UiText.FACE) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int ew = UiText.width(font, ellipsis, UiText.FACE);
        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int cw = UiText.width(font, ch, UiText.FACE);
            if (w + cw + ew > maxWidth) {
                break;
            }
            sb.append(text.charAt(i));
            w += cw;
        }
        return sb + ellipsis;
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
        if (VECTOR && fillW >= 1) {
            UiDraw.roundRect(g, x, y, fillW, h, Math.max(1, r), UiTheme.mix(UiTheme.mix(accent, 0xFFFFFFFF, 0.35f), accent, 0.35f));
            if (h >= 4) {
                roundRect(g, x, y, fillW, Math.max(1, h / 2), Math.max(1, r), UiTheme.withAlpha(0xFFFFFFFF, 0.18f));
            }
            return;
        }
        if (fillW >= 2) {
            int light = UiTheme.mix(accent, 0xFFFFFFFF, 0.35f);
            int fillColor = UiTheme.mix(light, accent, 0.35f);
            roundRect(g, x, y, fillW, h, Math.max(1, r), fillColor);
            roundRect(g, x, y, fillW, Math.max(1, h / 2), Math.max(1, r), UiTheme.withAlpha(0xFFFFFFFF, 0.18f));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Иконки-текстуры (PNG 16×16, тонируются цветом)
    // ─────────────────────────────────────────────────────────────────────

    /** Текстура 16×16, растянутая в квадрат {@code size}×{@code size}, с тонировкой {@code argb}. */
    public static void icon(GuiGraphicsExtractor g, Identifier texture, int x, int y, int size, int argb) {
        if (size <= 0) {
            return;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size, 16, 16, 16, 16, argb);
    }

    /** Текстура произвольного размера с тонировкой. */
    public static void icon(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h,
                            int texW, int texH, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, texW, texH, texW, texH, argb);
    }

    /** Мягкое «свечение» вокруг прямоугольника — несколько полупрозрачных слоёв акцента. */
    public static void glow(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int accent, float strength) {
        if (!GLOW || strength <= 0.02f) {
            return;
        }
        if (VECTOR) {
            float spread = Math.max(3f, 9f * strength);
            UiVector.shadow(g, x, y, w, h, radius, spread,
                    withAlpha(accent, Math.min(0.5f, 0.22f * strength)), 6);
            return;
        }
        // мягкое свечение — размытый спрайт акцентного цвета
        int spread = Math.max(3, Math.round(9 * strength));
        int col = withAlpha(accent, Math.min(0.5f, 0.22f * strength));
        nineSlice(g, shapeTexture("shadow"), SHADOW_TEX, SHADOW_NINE,
                x - spread, y - spread, w + 2 * spread, h + 2 * spread, col);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Обрезка (нужна векторным формам: они не знают про ванильный scissor-стек)
    // ─────────────────────────────────────────────────────────────────────

    /** Включить обрезку: то же, что {@code graphics.enableScissor}, плюс запоминание прямоугольника. */
    public static void scissor(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        UiVector.scissor(g, x0, y0, x1, y1);
    }

    /** Снять обрезку (парно {@link #scissor}). */
    public static void unscissor(GuiGraphicsExtractor g) {
        UiVector.unscissor(g);
    }

    /** Сбросить зеркало обрезки — вызывать в начале кадра, если что-то осталось незакрытым. */
    public static void resetScissor() {
        UiVector.resetScissor();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Формы с дробными координатами (для анимаций: без «прилипания» к пикселю)
    // ─────────────────────────────────────────────────────────────────────

    /** Скруглённый прямоугольник с float-координатами. */
    public static void roundRectF(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                  float radius, int color) {
        if (VECTOR) {
            UiVector.roundRect(g, x, y, w, h, radius, color);
        } else {
            roundRect(g, Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(radius), color);
        }
    }

    /** Скруглённый прямоугольник с вертикальным градиентом и float-координатами. */
    public static void roundRectGradientF(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                          float radius, int top, int bottom) {
        if (VECTOR) {
            UiVector.roundRectGradient(g, x, y, w, h, radius, top, bottom);
        } else {
            roundRectGradient(g, Math.round(x), Math.round(y), Math.round(w), Math.round(h),
                    Math.round(radius), top, bottom);
        }
    }

    /** Рамка скруглённого прямоугольника (float). */
    public static void outlineF(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                float radius, float thickness, int color) {
        if (VECTOR) {
            UiVector.outline(g, x, y, w, h, radius, thickness, color);
        }
    }

    /** Рамка с горизонтальным градиентом (float). */
    public static void outlineGradientF(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                        float radius, float thickness, int left, int right) {
        if (VECTOR) {
            UiVector.outlineGradient(g, x, y, w, h, radius, thickness, left, right);
        }
    }

    /** Тень/свечение с float-координатами. */
    public static void shadowF(GuiGraphicsExtractor g, float x, float y, float w, float h,
                               float radius, float spread, int color, int layers) {
        if (VECTOR) {
            UiVector.shadow(g, x, y, w, h, radius, spread, color, layers);
        } else {
            shadow(g, Math.round(x), Math.round(y), Math.round(w), Math.round(h),
                    Math.round(radius), Math.round(spread / 3f), color);
        }
    }

    /** Круг (float) — используется для точек-индикаторов. */
    public static void discF(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
        disc(g, cx, cy, r, color);
    }

    /** Смешать два цвета (дубль {@link UiTheme#mix} для удобства вызовов из gfx-кода). */
    public static int mix(int a, int b, float t) {
        return UiVector.mix(a, b, t);
    }

    private static int withAlpha(int color, float a) {
        int alpha = Math.round(255 * Math.max(0f, Math.min(1f, a)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
