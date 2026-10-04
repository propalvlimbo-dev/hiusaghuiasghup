package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

import java.util.Random;

/**
 * Живой фон ElytrixClient — «цифровой дождь» в бело-розовой гамме.
 *
 * <p>Три слоя глубины: дальний (мелкие тусклые цифры), средний и ближний
 * (крупные, яркие). По каждому столбцу бегут светлые «полосы»-головы, цифры
 * сами время от времени меняются. <b>Мышь оживляет экран:</b> вокруг курсора
 * цифры быстро перебираются и вспыхивают белым, а за курсором остаётся
 * затухающий «тепловой» след.
 *
 * <p>Шрифт — JetBrains Mono трёх размеров ({@code mx_s/mx_m/mx_l}), каждый
 * с вариантами под масштаб интерфейса, поэтому цифры чёткие без растяжения.
 * Пустые и очень тусклые клетки не рисуются; на «низком» качестве эффектов
 * дальний слой выключается.
 */
public final class ElytrixBackground {
    private static final char[] DIGITS = "0123456789".toCharArray();
    private static final Random RANDOM = new Random(0xE1711);

    /** Слой глубины. */
    private static final class Layer {
        final Identifier face;
        final float cell;
        final float alpha;
        final float speedMul;
        int cols;
        int rows;
        char[] glyph;
        float[] heat;
        float[] head;
        float[] speed;
        float[] len;
        /** Готовые компоненты цифр под текущий масштаб интерфейса (без аллокаций в кадре). */
        final net.minecraft.network.chat.Component[] digits = new net.minecraft.network.chat.Component[10];
        int digitsScale = -1;

        net.minecraft.network.chat.Component digit(char ch) {
            int k = Minecraft.getInstance().getWindow().getGuiScale();
            if (k != digitsScale) {
                for (int d = 0; d < 10; d++) {
                    digits[d] = UiText.of(String.valueOf(DIGITS[d]), face);
                }
                digitsScale = k;
            }
            return digits[ch - '0'];
        }

        Layer(String face, float cell, float alpha, float speedMul) {
            this.face = Identifier.fromNamespaceAndPath("elytrixclient", face);
            this.cell = cell;
            this.alpha = alpha;
            this.speedMul = speedMul;
        }

        void ensure(int width, int height) {
            int c = (int) Math.ceil(width / cell) + 1;
            int r = (int) Math.ceil(height / cell) + 1;
            if (c == cols && r == rows && glyph != null) {
                return;
            }
            cols = c;
            rows = r;
            glyph = new char[c * r];
            heat = new float[c * r];
            head = new float[c];
            speed = new float[c];
            len = new float[c];
            for (int i = 0; i < glyph.length; i++) {
                glyph[i] = DIGITS[RANDOM.nextInt(DIGITS.length)];
            }
            for (int i = 0; i < c; i++) {
                head[i] = RANDOM.nextFloat() * (r + 20) - 10;
                speed[i] = (2.5f + RANDOM.nextFloat() * 6f) * speedMul;
                len[i] = 6 + RANDOM.nextInt(14);
            }
        }
    }

    private static final Layer[] LAYERS = {
            new Layer("mx_s", 9f, 0.55f, 0.7f),
            new Layer("mx_m", 13f, 0.80f, 1.0f),
            new Layer("mx_l", 20f, 1.00f, 1.3f),
    };

    private static long lastNanos;
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    private ElytrixBackground() {
    }

    /** Полный кадр фона. {@code t} — время в секундах, {@code alpha} — общая непрозрачность. */
    public static void render(GuiGraphicsExtractor g, int width, int height, float t, int accent, float alpha) {
        if (width <= 0 || height <= 0 || alpha <= 0.01f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ElytrixQuality.updateFrom(mc, ElytrixclientClient.CONFIG);

        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;

        // база: почти чёрный с лёгким розовым оттенком
        g.fillGradient(0, 0, width, height, col(0xFF0D0810, alpha), col(0xFF070508, alpha));

        double mx = Double.NaN;
        double my = Double.NaN;
        if (mc.getWindow() != null) {
            mx = mc.mouseHandler.getScaledXPos(mc.getWindow());
            my = mc.mouseHandler.getScaledYPos(mc.getWindow());
        }

        Font font = mc.font;
        boolean full = ElytrixQuality.glow();
        for (int li = full ? 0 : 1; li < LAYERS.length; li++) {
            Layer layer = LAYERS[li];
            layer.ensure(width, height);
            stir(layer, mx, my, dt);
            drawLayer(g, font, layer, dt, accent, alpha, full);
        }
        lastMouseX = mx;
        lastMouseY = my;

        // виньетка: края темнее, центр (где кнопки) — спокойнее
        g.fillGradient(0, 0, width, height / 4, col(0xFF000000, 0.55f * alpha), 0x00000000);
        g.fillGradient(0, height - height / 3, width, height, 0x00000000, col(0xFF000000, 0.6f * alpha));
    }

    /** Мышь «нагревает» клетки вдоль пути курсора: там цифры перебираются и светятся. */
    private static void stir(Layer layer, double mx, double my, float dt) {
        float decay = (float) Math.exp(-dt * 1.6f);
        float[] heat = layer.heat;
        for (int i = 0; i < heat.length; i++) {
            if (heat[i] > 0.002f) {
                heat[i] *= decay;
            } else {
                heat[i] = 0f;
            }
        }
        if (Double.isNaN(mx) || Double.isNaN(my)) {
            return;
        }
        double fromX = Double.isNaN(lastMouseX) ? mx : lastMouseX;
        double fromY = Double.isNaN(lastMouseY) ? my : lastMouseY;
        double dist = Math.hypot(mx - fromX, my - fromY);
        // точки вдоль отрезка движения, чтобы быстрый рывок оставлял сплошной след
        int steps = Math.max(1, Math.min(24, (int) (dist / (layer.cell * 0.6))));
        float radius = 34f;
        float moving = (float) Math.min(1.0, 0.25 + dist / 12.0);
        for (int s = 1; s <= steps; s++) {
            double px = fromX + (mx - fromX) * s / steps;
            double py = fromY + (my - fromY) * s / steps;
            int c0 = Math.max(0, (int) ((px - radius) / layer.cell));
            int c1 = Math.min(layer.cols - 1, (int) ((px + radius) / layer.cell));
            int r0 = Math.max(0, (int) ((py - radius) / layer.cell));
            int r1 = Math.min(layer.rows - 1, (int) ((py + radius) / layer.cell));
            for (int r = r0; r <= r1; r++) {
                for (int c = c0; c <= c1; c++) {
                    double dx = (c + 0.5) * layer.cell - px;
                    double dy = (r + 0.5) * layer.cell - py;
                    double d = Math.sqrt(dx * dx + dy * dy) / radius;
                    if (d < 1.0) {
                        int i = r * layer.cols + c;
                        float add = (float) ((1.0 - d) * (1.0 - d)) * moving * (dt * 9f / steps + 0.02f);
                        heat[i] = Math.min(1f, heat[i] + add);
                    }
                }
            }
        }
    }

    private static void drawLayer(GuiGraphicsExtractor g, Font font, Layer layer, float dt,
                                  int accent, float alpha, boolean full) {
        int cols = layer.cols;
        int rows = layer.rows;
        int pinkSoft = UiTheme.mix(accent, 0xFFFFFFFF, 0.35f);
        // тусклая «подложка» из всех цифр — только для ближних слоёв и не на огромной сетке
        float base = full && layer.alpha > 0.6f && cols * rows < 4500 ? 0.07f : 0f;
        for (int c = 0; c < cols; c++) {
            layer.head[c] += layer.speed[c] * dt;
            if (layer.head[c] - layer.len[c] > rows) {
                layer.head[c] = -RANDOM.nextInt(Math.max(1, rows / 2));
                layer.speed[c] = (2.5f + RANDOM.nextFloat() * 6f) * layer.speedMul;
                layer.len[c] = 6 + RANDOM.nextInt(14);
            }
            float head = layer.head[c];
            float len = layer.len[c];
            int x = Math.round(c * layer.cell);
            for (int r = 0; r < rows; r++) {
                int i = r * cols + c;
                float d = head - r;
                float streak = d >= 0f && d < len ? 1f - d / len : 0f;
                float h = layer.heat[i];

                // цифры живут: редкая смена сама по себе, частая — под курсором и у головы
                float change = dt * (0.25f + streak * 2.5f + h * 30f);
                if (RANDOM.nextFloat() < change) {
                    layer.glyph[i] = DIGITS[RANDOM.nextInt(DIGITS.length)];
                }

                float b = Math.max(base, streak * streak);
                b = Math.min(1f, b + h * 0.9f);
                float a = b * layer.alpha * alpha;
                if (a < 0.035f) {
                    continue;
                }
                int color;
                if (d >= 0f && d < 1f) {
                    color = 0xFFFFFFFF;                       // голова полосы — белая
                } else if (h > 0.35f) {
                    color = UiTheme.mix(pinkSoft, 0xFFFFFFFF, Math.min(1f, (h - 0.35f) * 1.6f));
                } else {
                    color = UiTheme.mix(accent, pinkSoft, streak);
                }
                int y = Math.round(r * layer.cell);
                g.text(font, layer.digit(layer.glyph[i]), x, y,
                        UiTheme.withAlpha(color, Math.min(1f, a)), false);
            }
        }
    }

    private static int col(int color, float mul) {
        return UiTheme.withAlpha(color, Math.max(0f, Math.min(1f, mul)));
    }

    /** Секунды с момента запуска — общая шкала времени для анимации. */
    public static float time() {
        return (Util.getMillis() % 3_600_000L) / 1000f;
    }
}
