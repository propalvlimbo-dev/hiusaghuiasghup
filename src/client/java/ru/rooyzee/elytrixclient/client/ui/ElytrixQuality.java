package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;

/**
 * Качество эффектов ElytrixClient: решает, что можно рисовать без просадки FPS
 * на больших разрешениях, и раздаёт это флаги в {@link UiDraw} и {@link ElytrixBackground}.
 *
 * <p>Почему это вообще нужно: дорогие вещи на 1440p/4K — это размытие фона
 * ({@code blurBeforeThisStratum}, полноэкранный blur) и плотные эффекты
 * (свечения, «дождь» символов, сканлайны). На маленьком окне всё это бесплатно,
 * на большом — заметно бьёт по кадрам, поэтому режим «Авто» отключает тяжёлое
 * по размеру кадра, а не по желанию пользователя.
 *
 * <p>Режимы ({@link ElytrixConfig#effectsQuality}): 0 — авто, 1 — низкое, 2 — высокое.
 * Любое значение можно переопределить в Настройках → «Качество эффектов».
 */
public final class ElytrixQuality {
    public static final String[] NAMES = {"Авто", "Низкое", "Высокое"};

    /** От ~2560×1440 (3.6 Мпикс) — выключаем полноэкранный blur. */
    private static final int PIXELS_NO_BLUR = 3_600_000;
    /** От ~4K (6 Мпикс) — режем свечения и плотность «дождя». */
    private static final int PIXELS_HEAVY = 6_000_000;

    private static int cachedLevel = Integer.MIN_VALUE;
    private static int cachedPixels = -1;
    private static int cachedFps = -1;

    private static boolean blur = true;
    private static boolean glow = true;
    private static boolean scanlines = true;
    private static boolean glitch = true;
    private static float rainScale = 1f;

    private ElytrixQuality() {
    }

    /** Пересчитывает режим (дёшево: если ничего не изменилось — выходим сразу). */
    public static void update(ElytrixConfig cfg, int framebufferPixels, int fps) {
        int level = cfg == null ? 2 : cfg.effectsQuality;
        if (level == cachedLevel && framebufferPixels == cachedPixels && fps == cachedFps) {
            return;
        }
        cachedLevel = level;
        cachedPixels = framebufferPixels;
        cachedFps = fps;

        boolean low = level == 1;
        boolean high = level == 2;
        boolean auto = !low && !high;

        boolean big = framebufferPixels >= PIXELS_NO_BLUR;
        boolean huge = framebufferPixels >= PIXELS_HEAVY;
        // на слабом железе (низкий FPS) тоже сбрасываем тяжёлое, если режим «Авто»
        boolean slow = auto && fps > 0 && fps < 45;

        blur = high || (!low && !(auto && big));
        glow = high || (!low && !(auto && (huge || slow)));
        scanlines = !low;
        glitch = !low;
        rainScale = low ? 0.55f : (auto && (huge || slow) ? 0.7f : 1f);

        // свечения рисуются слоями скруглённых прямоугольников — самый дорогой виджет-эффект
        UiDraw.GLOW = glow;
    }

    /** Удобная обёртка: берёт размер кадра и FPS из клиента. */
    public static void updateFrom(Minecraft client, ElytrixConfig cfg) {
        int pixels = 0;
        int fps = 0;
        if (client != null && client.getWindow() != null) {
            pixels = Math.max(0, client.getWindow().getWidth()) * Math.max(0, client.getWindow().getHeight());
        }
        if (client != null) {
            fps = client.getFps();
        }
        update(cfg, pixels, fps);
    }

    public static boolean blur() {
        return blur;
    }

    public static boolean glow() {
        return glow;
    }

    public static boolean scanlines() {
        return scanlines;
    }

    public static boolean glitch() {
        return glitch;
    }

    public static float rainScale() {
        return rainScale;
    }

    /** Текущая строка для интерфейса: показывает, что именно включено. */
    public static String summary() {
        return (blur ? "blur " : "") + (glow ? "glow " : "") + (scanlines ? "scan " : "")
                + "rain×" + String.format("%.2f", rainScale);
    }
}
