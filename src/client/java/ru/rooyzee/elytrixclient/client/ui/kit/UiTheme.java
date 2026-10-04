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
    /** 0 — «графит» (тёмная, стиль Apple + хакерская тема), 1 — светлая «бумага». */
    public static final String[] PRESET_NAMES = {"Графит", "Светлая"};
    private static final int PRESET_GRAPHITE = 0;
    private static final int PRESET_LIGHT = 1;

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
    public static final int R_SM = 8;
    public static final int R_MD = 10;
    public static final int R_LG = 12;
    public static final int R_XL = 16;
    public static final int SIDEBAR_W = 122;
    public static final int HEADER_H = 58;
    public static final int PAD = 12;
    public static final int ROW_H = 30;
    public static final int ROW_H_TALL = 42;

    // ── акценты ──────────────────────────────────────────────────────────
    public static final String[] ACCENT_NAMES = {"Розовый", "Малиновый", "Сиреневый", "Голубой", "Зелёный", "Оранжевый"};
    public static final int[] ACCENTS = {0xFFFF4FC3, 0xFFFF5C8A, 0xFFA45CFF, 0xFF38BDF8, 0xFF34D399, 0xFFFFA34D};

    private static boolean light = true;

    static {
        applyPreset(PRESET_GRAPHITE);
    }

    /**
     * 0 — «графит» (по умолчанию: тёмные поверхности, тонкие светящиеся хайрлайны,
     * акцент — розовый; ощущение тёмного терминала с аккуратной типографикой),
     * 1 — светлая «бумага» (розовый акцент остаётся).
     */
    public static void applyPreset(int index) {
        if (index == PRESET_LIGHT) {
            light = true;
            SCRIM = 0x59F4F4F7;
            PANEL = 0xFBFFFFFF;
            SIDEBAR = 0xFDF7F7FA;
            CARD = 0xFFFFFFFF;
            CARD_HOVER = 0xFFF7F7FB;
            ROW = 0xFFF6F6F9;
            ROW_HOVER = 0xFFEFEFF4;
            POPUP = 0xFFFFFFFF;
            TRACK = 0xFFE9E9F0;
            BORDER = 0x1F14141A;
            BORDER_SOFT = 0x1214141A;
            DIVIDER = 0x1414141A;
            SHADOW = 0x2E0B0B14;
            TEXT = 0xFF14141A;
            TEXT_SOFT = 0xFF4A4A55;
            TEXT_DIM = 0xFF8C8C99;
        } else {
            light = false;
            SCRIM = 0x59000000;
            PANEL = 0xF2141218;
            SIDEBAR = 0xF51A171F;
            CARD = 0x14FFFFFF;
            CARD_HOVER = 0x1FFFFFFF;
            ROW = 0x0FFFFFFF;
            ROW_HOVER = 0x1AFFFFFF;
            POPUP = 0xF21D1A23;
            TRACK = 0x1FFFFFFF;
            BORDER = 0x24FFFFFF;
            BORDER_SOFT = 0x12FFFFFF;
            DIVIDER = 0x14FFFFFF;
            SHADOW = 0x8C000000;
            TEXT = 0xFFF6F4F8;
            TEXT_SOFT = 0xE0FFFFFF;
            TEXT_DIM = 0x8AFFFFFF;
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
