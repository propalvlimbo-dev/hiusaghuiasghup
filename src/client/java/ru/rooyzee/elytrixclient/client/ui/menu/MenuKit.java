package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.Locale;

/**
 * Общие вещи для меню-панели: шрифты, палитра, общая прозрачность (анимация
 * открытия) и короткие обёртки над векторным слоем {@link UiVector}.
 *
 * <p>Все размеры — в GUI-единицах игры: панель 450×350 сама следует за
 * настройкой «Масштаб интерфейса».
 */
public final class MenuKit {
    private MenuKit() {
    }

    // ── шрифты (assets/elytrixclient/font/menu*.json) ───────────────────
    /** Основной текст, Onest 8. */
    public static final Identifier BODY = id("menu");
    /** Подписи и значения, Onest 7. */
    public static final Identifier SMALL = id("menu_s");
    /** Логотип/заголовки, Onest 10. */
    public static final Identifier TITLE = id("menu_t");
    /** Консоль, адреса и числа — Onest в компактном кегле. */
    public static final Identifier MONO = id("mono_s");

    /** Общая прозрачность кадра (анимация открытия/закрытия панели). */
    public static float alpha = 1f;
    /** Текущий акцент (розовый по умолчанию). */
    public static int accent = UiTheme.ACCENTS[0];

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("elytrixclient", path);
    }

    // ── палитра ─────────────────────────────────────────────────────────

    /** Вторая точка градиента акцента — бело-розовая. */
    public static int accent2() {
        return UiTheme.mix(accent, 0xFFFFFFFF, 0.42f);
    }

    public static int sidebar() {
        return UiTheme.isLight() ? 0xFFF3F1F6 : 0xFF17141C;
    }

    public static int content() {
        return UiTheme.isLight() ? 0xFFFBFAFC : 0xFF121016;
    }

    public static int cardFill() {
        return UiTheme.isLight() ? 0xF5FFFFFF : 0xE6161319;
    }

    public static int cardEdge() {
        return UiTheme.isLight() ? 0x1F14141A : 0x26FFFFFF;
    }

    public static int field() {
        return UiTheme.isLight() ? 0xFFEDEBF1 : 0xFF0D0B10;
    }

    public static int divider() {
        return UiTheme.isLight() ? 0x1414141A : 0x1AFFFFFF;
    }

    public static int text() {
        return UiTheme.isLight() ? 0xFF15131A : 0xFFFFFFFF;
    }

    public static int soft() {
        return UiTheme.isLight() ? 0xFF55525E : 0xFFCFCAD6;
    }

    public static int dim() {
        return UiTheme.isLight() ? 0xFF8E8A98 : 0xFF928B9C;
    }

    // ── прозрачность ────────────────────────────────────────────────────

    /** Цвет с учётом общей прозрачности кадра. */
    public static int a(int color) {
        return UiTheme.withAlpha(color, alpha);
    }

    public static int a(int color, float mul) {
        return UiTheme.withAlpha(color, alpha * mul);
    }

    // ── анимация ────────────────────────────────────────────────────────

    /** Экспоненциальное приближение, не зависящее от FPS. */
    public static float approach(float current, float target, float speed, float dt) {
        if (!UiWidget.ANIMATIONS) {
            return target;
        }
        float k = 1f - (float) Math.exp(-speed * dt);
        float v = current + (target - current) * k;
        return Math.abs(v - target) < 0.001f ? target : v;
    }

    // ── фигуры ──────────────────────────────────────────────────────────

    public static void fill(GuiGraphicsExtractor g, float x, float y, float w, float h, float r, int color) {
        UiVector.roundRect(g, x, y, w, h, r, a(color));
    }

    public static void fill(GuiGraphicsExtractor g, float x, float y, float w, float h,
                            float tl, float tr, float br, float bl, int color) {
        UiVector.roundRect(g, x, y, w, h, tl, tr, br, bl, a(color));
    }

    /** Горизонтальный градиент слева направо. */
    public static void hgrad(GuiGraphicsExtractor g, float x, float y, float w, float h, float r,
                             int left, int right) {
        UiVector.roundRectBilinear(g, x, y, w, h, r, r, r, r, a(left), a(right), a(right), a(left));
    }

    public static void vgrad(GuiGraphicsExtractor g, float x, float y, float w, float h, float r,
                             int top, int bottom) {
        UiVector.roundRectGradient(g, x, y, w, h, r, a(top), a(bottom));
    }

    public static void outline(GuiGraphicsExtractor g, float x, float y, float w, float h, float r,
                               float t, int color) {
        UiVector.outline(g, x, y, w, h, r, t, a(color));
    }

    public static void outlineGrad(GuiGraphicsExtractor g, float x, float y, float w, float h, float r,
                                   float t, int left, int right) {
        UiVector.outlineGradient(g, x, y, w, h, r, t, a(left), a(right));
    }

    /** Карточка: тонкая рамка + заливка. */
    public static void card(GuiGraphicsExtractor g, float x, float y, float w, float h, float r,
                            int fill, int edge) {
        fill(g, x, y, w, h, r, fill);
        outline(g, x, y, w, h, r, 0.5f, edge);
    }

    public static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
        UiVector.roundRect(g, cx - r, cy - r, r * 2, r * 2, r, a(color));
    }

    public static void shadow(GuiGraphicsExtractor g, float x, float y, float w, float h, float r,
                              float spread, int color) {
        UiVector.shadow(g, x, y, w, h, r, spread, a(color), Math.max(8, Math.min(32, Math.round(spread * 1.4f))));
    }

    public static void hline(GuiGraphicsExtractor g, float x, float y, float w, int color) {
        UiVector.rect(g, x, y, w, 0.5f, a(color));
    }

    // ── текст ───────────────────────────────────────────────────────────

    public static void text(GuiGraphicsExtractor g, Font font, String s, float x, float y, int color,
                            Identifier face) {
        if (s == null || s.isEmpty()) {
            return;
        }
        int c = a(color);
        if (((c >>> 24) & 0xFF) < 6) {
            return;
        }
        int o = UiDraw.scaleOverride;
        if (o > 0) {
            // панель в нецелом масштабе: ставим текст точно в сетку пикселей экрана
            float sx = Math.round(x * o) / (float) o;
            float sy = Math.round(y * o) / (float) o;
            g.pose().pushMatrix();
            g.pose().translate(sx, sy);
            g.text(font, UiText.of(s, face), 0, 0, c, false);
            g.pose().popMatrix();
            return;
        }
        g.text(font, UiText.of(s, face), Math.round(x), Math.round(y), c, false);
    }

    public static void textRight(GuiGraphicsExtractor g, Font font, String s, float right, float y, int color,
                                 Identifier face) {
        text(g, font, s, right - width(font, s, face), y, color, face);
    }

    public static void textCenter(GuiGraphicsExtractor g, Font font, String s, float cx, float y, int color,
                                  Identifier face) {
        text(g, font, s, cx - width(font, s, face) / 2f, y, color, face);
    }

    /**
     * Y для отрисовки, чтобы заглавные буквы встали по центру {@code centerY}.
     * В наших font providers с нулевым shift базовая линия TTF-глифа — {@code y + 7};
     * высота заглавных у Onest — примерно 0.73 размера шрифта.
     */
    public static float ty(Identifier face, float centerY) {
        float size = face == TITLE ? 10f : (face == BODY ? 8f : 7f);
        // shift = 0 → базовая линия ровно на y + 7; координата целая → глиф в сетке пикселей
        return Math.round(centerY + size * 0.73f / 2f - 7f);
    }

    public static int width(Font font, String s, Identifier face) {
        return s == null || s.isEmpty() ? 0 : font.width(UiText.of(s, face));
    }

    /** Обрезать строку с «…», чтобы влезла в {@code max}. */
    public static String trim(Font font, String s, Identifier face, float max) {
        if (s == null || width(font, s, face) <= max) {
            return s;
        }
        String dots = "…";
        int end = s.length();
        while (end > 0 && width(font, s.substring(0, end) + dots, face) > max) {
            end--;
        }
        return s.substring(0, end) + dots;
    }

    // ── прочее ──────────────────────────────────────────────────────────

    public static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
