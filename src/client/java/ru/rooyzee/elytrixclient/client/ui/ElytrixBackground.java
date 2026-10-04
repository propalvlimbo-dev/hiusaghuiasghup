package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.Random;

import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * «Хакерский» анимированный фон ElytrixClient: тёмная подложка, перспективная сетка,
 * падающие столбцы символов (дождь из глифов в бело-розовой гамме), сканлайны,
 * редкие глитч-полосы и виньетка.
 *
 * <p>Рисуется и под главным меню (перекрывает ванильную панораму), и на экране загрузки.
 * {@code alpha} домножает прозрачность всего кадра — так работает плавное появление
 * и исчезновение загрузочного экрана.
 */
public final class ElytrixBackground {
    /** Символы «дождя»: цифры, катакана и хакерская пунктуация. */
    private static final char[] GLYPHS =
            "01ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄ<>[]{}/\\|=+-*#$%&@!?".toCharArray();

    private static final int CELL_W = 11;
    private static final int CELL_H = 11;
    private static final int TRAIL = 9;
    private static final Random RANDOM = new Random(0xE1711);

    private static int cachedW = -1;
    private static int cachedH = -1;
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
        ensure(width, height);

        // 1. база: тёмный градиент с розовым отсветом сверху
        g.fillGradient(0, 0, width, height, col(0xFF140A18, alpha), col(0xFF0A060E, alpha));
        UiDraw.hGradient(g, 0, 0, width, Math.max(40, height / 5), col(accent, 0.10f * alpha), 0x00000000, 24);

        // 2. перспективная сетка
        drawGrid(g, width, height, t, accent, alpha);

        // 3. столбцы символов
        Font font = Minecraft.getInstance().font;
        int columns = head.length;
        for (int i = 0; i < columns; i++) {
            float h = head[i];
            int x = i * CELL_W + 2;
            if (x > width - 4) {
                break;
            }
            for (int k = 0; k < TRAIL; k++) {
                float y = h - k * CELL_H;
                if (y < -CELL_H || y > height) {
                    continue;
                }
                float fade = 1f - (float) k / TRAIL;
                float a = 0.62f * fade * fade * alpha;
                if (a < 0.04f) {
                    continue;
                }
                int color = k == 0 ? col(0xFFFFFFFF, a)
                        : (k < 3 ? col(accent, a * 0.85f)
                        : col(0xFFFFD9F2, a * 0.7f));
                String glyph = String.valueOf(GLYPHS[(int) (Math.abs(seed[i] + (int) (y / CELL_H)) % GLYPHS.length)]);
                UiDraw.text(g, font, glyph, x, (int) y, color);
            }
            head[i] += speed[i] * (0.35f + 0.65f * ((i % 5) / 4f)) * 2.2f;
            if (head[i] - TRAIL * CELL_H > height) {
                head[i] = -RANDOM.nextInt(height / 2 + 20) - CELL_H;
                speed[i] = 0.6f + RANDOM.nextFloat() * 1.4f;
            }
        }

        // 4. сканлайны
        int line = col(0xFF000000, 0.16f * alpha);
        for (int y = (int) (t * 26f) % 4; y < height; y += 4) {
            g.fill(0, y, width, y + 1, line);
        }

        // 5. глитч-полосы (редко и коротко)
        for (int i = 0; i < 3; i++) {
            float phase = (t * 0.6f + i * 0.37f) % 4f;
            if (phase < 0.12f) {
                int y = (int) ((Math.abs(seed[i % seed.length]) * 37 + i * 97) % Math.max(1, height));
                int w = (int) (width * (0.35f + 0.5f * ((seed[i % seed.length] * 7 % 10) / 10f)));
                g.fill(0, y, Math.min(width, w), y + 1, col(0xFFFFFFFF, 0.14f * alpha));
                g.fill(Math.max(0, width - w), y + 1, width, y + 2, col(accent, 0.12f * alpha));
            }
        }

        // 6. виньетка
        g.fillGradient(0, 0, width, height / 3, col(0xFF000000, 0.40f * alpha), 0x00000000);
        g.fillGradient(0, height - height / 3, width, height, 0x00000000, col(0xFF000000, 0.47f * alpha));
    }

    private static void drawGrid(GuiGraphicsExtractor g, int width, int height, float t, int accent, float alpha) {
        int step = 34;
        int offset = (int) ((t * 14f) % step);
        int col = col(accent, 0.055f * alpha);
        int colStrong = col(accent, 0.10f * alpha);

        for (int y = offset - step; y < height + step; y += step) {
            int ly = Mth.clamp(y, 0, height);
            float dist = Math.abs(ly - height * 0.62f) / Math.max(1f, height * 0.62f);
            g.fill(0, ly, width, ly + 1, dist > 0.55f ? col : colStrong);
        }
        for (int x = 0; x <= width; x += step) {
            int cx = width / 2;
            int spread = (int) ((x - cx) * 0.18f);
            UiDraw.line(g, x, 0, x + spread, height, 1f, col);
        }
    }

    private static int col(int color, float mul) {
        return UiTheme.withAlpha(color, Math.max(0f, Math.min(1f, mul)));
    }

    private static void ensure(int width, int height) {
        if (width == cachedW && height == cachedH && head != null) {
            return;
        }
        cachedW = width;
        cachedH = height;
        int columns = Math.max(1, width / CELL_W + 1);
        head = new float[columns];
        speed = new float[columns];
        seed = new float[columns];
        for (int i = 0; i < columns; i++) {
            head[i] = -RANDOM.nextInt(height + 60);
            speed[i] = 0.6f + RANDOM.nextFloat() * 1.4f;
            seed[i] = RANDOM.nextInt(97);
        }
    }

    /** Секунды с момента запуска — общая шкала времени для анимации. */
    public static float time() {
        return (Util.getMillis() % 3_600_000L) / 1000f;
    }
}
