package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

import java.util.Random;

/**
 * «Хакерский» анимированный фон ElytrixClient: тёмная подложка, сетка, падающие столбцы
 * символов (дождь из глифов в бело-розовой гамме), сканлайны, редкие глитч-полосы и виньетка.
 *
 * <p><b>Производительность.</b> Фон рисуется на любом разрешении с ограниченным числом
 * примитивов: количество линий сетки, сканлайнов и столбцов «дождя» не зависит от размера
 * окна — оно вычисляется по сетке клеток и упирается в потолок. Столбцы «дождя» рисуются
 * в виртуальных координатах (масштаб задаётся позой), поэтому на 4K это те же ~200 глифов,
 * что и в окне 1280×720, а не в 9 раз больше. Тяжёлые эффекты (свечения, сканлайны, blur)
 * отключаются режимом качества — см. {@link ElytrixQuality}.
 */
public final class ElytrixBackground {
    /** Символы «дождя»: цифры, катакана и хакерская пунктуация. */
    private static final char[] GLYPHS =
            "01ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄ<>[]{}/\\|=+-*#$%&@!?".toCharArray();

    // ── потолки количества примитивов (не зависят от разрешения) ─────────
    private static final int MAX_COLUMNS = 40;
    private static final int MIN_COLUMNS = 10;
    private static final int MAX_ROWS = 30;
    private static final int MIN_ROWS = 10;
    private static final int TRAIL = 6;
    /** Виртуальная клетка «дождя» (глиф 8 px + зазор). */
    private static final float CELL = 10f;

    private static final Random RANDOM = new Random(0xE1711);

    private static int cachedW = -1;
    private static int cachedH = -1;
    private static int columns;
    private static int rows;
    private static float cachedRainScale = -1f;
    private static float[] head;
    private static float[] speed;
    private static float[] seed;

    private ElytrixBackground() {
    }

    /** Полный кадр фона. {@code t} — время в секундах, {@code alpha} — общая непрозрачность. */
    public static void render(GuiGraphicsExtractor g, int width, int height, float t, int accent, float alpha) {
        if (width <= 0 || height <= 0 || alpha <= 0.01f) {
            return;
        }
        ElytrixQuality.updateFrom(Minecraft.getInstance(), ElytrixclientClient.CONFIG);
        ensure(width, height);

        // 1. база: тёмный градиент с розовым отсветом сверху
        g.fillGradient(0, 0, width, height, col(0xFF140A18, alpha), col(0xFF0A060E, alpha));
        if (ElytrixQuality.glow()) {
            UiDraw.hGradient(g, 0, 0, width, Math.max(36, height / 6), col(accent, 0.10f * alpha), 0x00000000, 24);
        }

        // 2. сетка: одна заливка на линию (никакой поэлементной отрисовки)
        drawGrid(g, width, height, t, accent, alpha);

        // 3. столбцы символов — фиксированное число, в виртуальных координатах
        drawRain(g, width, height, t, accent, alpha);

        // 4. сканлайны
        if (ElytrixQuality.scanlines()) {
            int spacing = Math.max(4, height / 90);
            int line = col(0xFF000000, 0.16f * alpha);
            for (int y = (int) (t * 26f) % spacing; y < height; y += spacing) {
                g.fill(0, y, width, y + 1, line);
            }
        }

        // 5. редкие глитч-полосы
        if (ElytrixQuality.glitch()) {
            for (int i = 0; i < 3; i++) {
                float phase = (t * 0.6f + i * 0.37f) % 4f;
                if (phase < 0.12f) {
                    int y = (int) ((Math.abs(seed[i % seed.length]) * 37 + i * 97) % Math.max(1, height));
                    int w = (int) (width * (0.35f + 0.5f * ((seed[i % seed.length] * 7 % 10) / 10f)));
                    g.fill(0, y, Math.min(width, w), y + 1, col(0xFFFFFFFF, 0.14f * alpha));
                    g.fill(Math.max(0, width - w), y + 1, width, y + 2, col(accent, 0.12f * alpha));
                }
            }
        }

        // 6. виньетка
        g.fillGradient(0, 0, width, height / 3, col(0xFF000000, 0.40f * alpha), 0x00000000);
        g.fillGradient(0, height - height / 3, width, height, 0x00000000, col(0xFF000000, 0.47f * alpha));
    }

    private static void drawGrid(GuiGraphicsExtractor g, int width, int height, float t, int accent, float alpha) {
        int stepY = Math.max(26, height / 14);
        int stepX = Math.max(34, width / 16);
        int soft = col(accent, 0.055f * alpha);
        int strong = col(accent, 0.10f * alpha);

        int mid = (int) (height * 0.62f);
        for (int y = (int) (t * 14f) % stepY - stepY; y < height + stepY; y += stepY) {
            int ly = Mth.clamp(y, 0, height - 1);
            float dist = Math.abs(ly - mid) / Math.max(1f, mid);
            g.fill(0, ly, width, ly + 1, dist > 0.55f ? soft : strong);
        }
        for (int x = 0; x <= width; x += stepX) {
            g.fill(x, 0, Math.min(width, x + 1), height, soft);
        }
    }

    private static void drawRain(GuiGraphicsExtractor g, int width, int height, float t, int accent, float alpha) {
        Font font = Minecraft.getInstance().font;
        if (font == null || head == null) {
            return;
        }
        float virtualH = rows * CELL;
        float virtualW = columns * CELL;
        // виртуальная сетка вписывается в экран: масштаб один на все глифы
        float scaleX = width / virtualW;
        float scaleY = height / virtualH;
        float scale = Math.max(scaleX, scaleY);

        g.pose().pushMatrix();
        g.pose().scale(scale, scale);
        for (int i = 0; i < columns; i++) {
            float h = head[i];
            float x = i * CELL;
            if (x > virtualW) {
                break;
            }
            for (int k = 0; k < TRAIL; k++) {
                float y = h - k * CELL;
                if (y < -CELL || y > virtualH) {
                    continue;
                }
                float fade = 1f - (float) k / TRAIL;
                float a = 0.62f * fade * fade * alpha;
                if (a < 0.05f) {
                    continue;
                }
                int color = k == 0 ? col(0xFFFFFFFF, a)
                        : (k < 3 ? col(accent, a * 0.85f) : col(0xFFFFD9F2, a * 0.7f));
                String glyph = String.valueOf(GLYPHS[(int) (Math.abs(seed[i] + (int) (y / CELL)) % GLYPHS.length)]);
                UiDraw.text(g, font, glyph, (int) x, (int) y, color);
            }
            head[i] += speed[i] * (0.35f + 0.65f * ((i % 5) / 4f)) * 2.2f;
            if (head[i] - TRAIL * CELL > virtualH) {
                head[i] = -RANDOM.nextInt((int) virtualH / 2 + 12) - CELL;
                speed[i] = 0.6f + RANDOM.nextFloat() * 1.4f;
            }
        }
        g.pose().popMatrix();
    }

    private static int col(int color, float mul) {
        return UiTheme.withAlpha(color, Math.max(0f, Math.min(1f, mul)));
    }

    private static void ensure(int width, int height) {
        float scale = ElytrixQuality.rainScale();
        if (width == cachedW && height == cachedH && scale == cachedRainScale && head != null) {
            return;
        }
        cachedW = width;
        cachedH = height;
        cachedRainScale = scale;
        // число столбцов и строк ограничено потолком — на 4K рисуем столько же, сколько на 720p
        columns = Mth.clamp(Math.round(width / 26f * scale), MIN_COLUMNS, MAX_COLUMNS);
        rows = Mth.clamp(Math.round(height / 22f * scale), MIN_ROWS, MAX_ROWS);

        float virtualH = rows * CELL;
        head = new float[columns];
        speed = new float[columns];
        seed = new float[columns];
        for (int i = 0; i < columns; i++) {
            head[i] = -RANDOM.nextInt((int) virtualH + 30);
            speed[i] = 0.6f + RANDOM.nextFloat() * 1.4f;
            seed[i] = RANDOM.nextInt(97);
        }
    }

    /** Секунды с момента запуска — общая шкала времени для анимации. */
    public static float time() {
        return (Util.getMillis() % 3_600_000L) / 1000f;
    }
}
