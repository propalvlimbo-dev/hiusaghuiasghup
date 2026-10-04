package ru.rooyzee.elytrixclient.client.ui.kit;

/**
 * Палитра и геометрия кастомного GUI ElytrixClient.
 *
 * <p>Две темы: <b>«Бело-розовая»</b> (по умолчанию — светлые панели, розовые акценты)
 * и <b>«Тёмная»</b>. Переключение — {@link #applyPreset(int)}; цвета лежат в изменяемых
 * полях, поэтому весь код виджетов просто читает {@code UiTheme.CARD} и т.п.
 *
 * <p>Цвета — ARGB (0xAARRGGBB), как их принимает {@code GuiGraphicsExtractor.fill(...)}.
 */
public final class UiTheme {
    private UiTheme() {
    }

    // ── темы ─────────────────────────────────────────────────────────────
    public static final String[] PRESET_NAMES = {"Бело-розовая", "Тёмная"};
    private static final int PRESET_LIGHT = 0;
    private static final int PRESET_DARK = 1;

    // ── поверхности (меняются темой) ─────────────────────────────────────
    public static int SCRIM;
    public static int PANEL;
    public static int SIDEBAR;
    public static int CARD;
    public static int CARD_HOVER;
    public static int ROW;
    public static int ROW_HOVER;
    public static int POPUP;
    public static int TRACK;
    public static int BORDER;
    public static int BORDER_SOFT;
    public static int DIVIDER;
    public static int SHADOW;

    // ── текст ────────────────────────────────────────────────────────────
    public static int TEXT;
    public static int TEXT_SOFT;
    public static int TEXT_DIM;

    // ── статусы (одинаковые в обеих темах) ───────────────────────────────
    public static final int OK = 0xFF34D399;
    public static final int WARN = 0xFFFBBF24;
    public static final int ERROR = 0xFFF87171;

    // ── размеры ──────────────────────────────────────────────────────────
    public static final int R_SM = 6;
    public static final int R_MD = 9;
    public static final int R_LG = 14;
    public static final int SIDEBAR_W = 124;
    public static final int HEADER_H = 46;
    public static final int PAD = 10;
    public static final int ROW_H = 28;
    public static final int ROW_H_TALL = 36;

    // ── акценты ──────────────────────────────────────────────────────────
    public static final String[] ACCENT_NAMES = {"Розовый", "Малиновый", "Сиреневый", "Голубой", "Зелёный", "Оранжевый"};
    public static final int[] ACCENTS = {0xFFFF4FC3, 0xFFFF5C8A, 0xFFA45CFF, 0xFF38BDF8, 0xFF34D399, 0xFFFFA34D};

    private static boolean light = true;

    static {
        applyPreset(PRESET_LIGHT);
    }

    /** 0 — бело-розовая, 1 — тёмная. */
    public static void applyPreset(int index) {
        if (index == PRESET_DARK) {
            light = false;
            SCRIM = 0xB0070A10;
            PANEL = 0xF513161D;
            SIDEBAR = 0xF50E111A;
            CARD = 0xFF181B23;
            CARD_HOVER = 0xFF1F242F;
            ROW = 0xFF1C2029;
            ROW_HOVER = 0xFF262C3A;
            POPUP = 0xFF14171E;
            TRACK = 0xFF2A2F3B;
            BORDER = 0x26FFFFFF;
            BORDER_SOFT = 0x14FFFFFF;
            DIVIDER = 0x16FFFFFF;
            SHADOW = 0x33000000;
            TEXT = 0xFFFFFFFF;
            TEXT_SOFT = 0xFFAAB0C0;
            TEXT_DIM = 0xFF6E7484;
        } else {
            light = true;
            SCRIM = 0x59FFFFFF;
            PANEL = 0xF7FFFFFF;
            SIDEBAR = 0xF7FFF6FB;
            CARD = 0xFFFFFFFF;
            CARD_HOVER = 0xFFFFF4FA;
            ROW = 0xFFFDF0F7;
            ROW_HOVER = 0xFFFBDFF0;
            POPUP = 0xFFFFFFFF;
            TRACK = 0xFFF4D6E8;
            BORDER = 0x33E24CB8;
            BORDER_SOFT = 0x22E24CB8;
            DIVIDER = 0x22E24CB8;
            SHADOW = 0x26000000;
            TEXT = 0xFF2A1C2E;
            TEXT_SOFT = 0xFF6D5A73;
            TEXT_DIM = 0xFF9C8BA3;
        }
    }

    public static boolean isLight() {
        return light;
    }

    public static int accent(int index) {
        if (index < 0 || index >= ACCENTS.length) {
            return ACCENTS[0];
        }
        return ACCENTS[index];
    }

    /** Светлая точка градиента (верх кнопки/переключателя). */
    public static int accentLight(int accent) {
        return mix(accent, 0xFFFFFFFF, 0.22f);
    }

    /** Тёмная точка градиента (низ кнопки). */
    public static int accentDark(int accent) {
        return mix(accent, 0xFF2A0B22, 0.30f);
    }

    /** Подложка под акцентный текст (чипы, выбранный пункт сайдбара). */
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
