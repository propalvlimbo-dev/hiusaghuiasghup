package ru.rooyzee.elytrixclient.client.ui.kit;

/**
 * Палитра и геометрия кастомного GUI ElytrixClient.
 *
 * <p>Стиль: тёмный фон, «карточки» со скруглениями и тонкой рамкой, мягкая тень,
 * акцентный градиент (розовый→фиолетовый по умолчанию). Всё рисуется вручную,
 * без ванильных виджетов — см. {@link UiDraw}.
 *
 * <p>Цвета — ARGB (0xAARRGGBB), как их принимает {@code GuiGraphicsExtractor.fill(...)}.
 */
public final class UiTheme {
    private UiTheme() {
    }

    // ── фон / поверхности ────────────────────────────────────────────────
    /** Затемнение всего экрана под панелью. */
    public static final int SCRIM = 0xB0070A10;
    public static final int PANEL = 0xF513161D;
    public static final int SIDEBAR = 0xF50F1219;
    public static final int CARD = 0xFF181B23;
    public static final int CARD_HOVER = 0xFF1F242F;
    public static final int ROW = 0xFF1C2029;
    public static final int ROW_HOVER = 0xFF262C3A;
    public static final int POPUP = 0xFF14171E;
    public static final int TRACK = 0xFF262B36;

    public static final int BORDER = 0x1FFFFFFF;
    public static final int BORDER_SOFT = 0x12FFFFFF;
    public static final int DIVIDER = 0x14FFFFFF;
    public static final int SHADOW = 0x2A000000;

    // ── текст ────────────────────────────────────────────────────────────
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_SOFT = 0xFFAAB0C0;
    public static final int TEXT_DIM = 0xFF6E7484;

    // ── статусы ──────────────────────────────────────────────────────────
    public static final int OK = 0xFF34D399;
    public static final int WARN = 0xFFFBBF24;
    public static final int ERROR = 0xFFF87171;

    // ── размеры ──────────────────────────────────────────────────────────
    public static final int R_SM = 4;
    public static final int R_MD = 7;
    public static final int R_LG = 10;
    public static final int SIDEBAR_W = 116;
    public static final int HEADER_H = 42;
    public static final int PAD = 10;
    public static final int ROW_H = 26;
    public static final int ROW_H_TALL = 34;

    // ── акценты (пресеты в настройках) ───────────────────────────────────
    public static final String[] ACCENT_NAMES = {"Розовый", "Фиолетовый", "Голубой", "Зелёный", "Оранжевый"};
    public static final int[] ACCENTS = {0xFFFF5CC8, 0xFF8B5CF6, 0xFF38BDF8, 0xFF34D399, 0xFFFBBF24};

    public static int accent(int index) {
        if (index < 0 || index >= ACCENTS.length) {
            return ACCENTS[0];
        }
        return ACCENTS[index];
    }

    /** Светлая точка градиента (верх кнопки/переключателя). */
    public static int accentLight(int accent) {
        return mix(accent, 0xFFFFFFFF, 0.24f);
    }

    /** Тёмная точка градиента (низ кнопки). */
    public static int accentDark(int accent) {
        return mix(accent, 0xFF120A22, 0.34f);
    }

    /** Приглушённая подложка под акцентный текст (чипы, выбранный пункт сайдбара). */
    public static int accentSoft(int accent, float amount) {
        return mix(CARD, accent, amount);
    }

    public static int withAlpha(int color, float mul) {
        int a = Math.round(((color >>> 24) & 0xFF) * clamp01(mul));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
