package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.Random;

/**
 * Анимированные фоны главного меню (выбор — иконка справа сверху в меню).
 * 0 — «Хакерский» ({@link ElytrixBackground}), остальные рисуются векторно:
 * «Аврора» (плавающие цветные облака), «Созвездие» (точки и связи, тянутся к
 * курсору), «Волны» (слои синусоид), «Звёздный путь» (полёт сквозь звёзды).
 * Все в цвете акцента.
 */
public final class MenuBackgrounds {
    private MenuBackgrounds() {
    }

    public static final String[] NAMES = {"Хакерский", "Аврора", "Созвездие", "Волны", "Звёздный путь"};

    private static final Random RANDOM = new Random(0xE1);
    private static long lastNanos;

    public static int current() {
        int i = ElytrixclientClient.CONFIG.menuBackground;
        return i < 0 || i >= NAMES.length ? 0 : i;
    }

    private static int shown = -1;
    private static int previous = -1;
    private static long switchedAt;

    /** Фон из настроек; при смене — плавный переход со старого на новый. */
    public static void render(GuiGraphicsExtractor g, int w, int h, float t, int accent, float alpha) {
        int want = current();
        long now = net.minecraft.util.Util.getMillis();
        if (shown < 0) {
            shown = want;
        } else if (want != shown) {
            previous = shown;
            shown = want;
            switchedAt = now;
        }
        float f = Math.min(1f, (now - switchedAt) / 700f);
        if (f < 1f && previous >= 0) {
            render(previous, g, w, h, t, accent, alpha);
            render(shown, g, w, h, t, accent, alpha * (f * f * (3f - 2f * f)));
        } else {
            render(shown, g, w, h, t, accent, alpha);
        }
    }

    public static void render(int kind, GuiGraphicsExtractor g, int w, int h, float t, int accent, float alpha) {
        if (kind == 0) {
            ElytrixBackground.render(g, w, h, t, accent, alpha);
            return;
        }
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        g.fillGradient(0, 0, w, h, a(0xFF0B0910, alpha), a(0xFF050407, alpha));
        switch (kind) {
            case 1 -> aurora(g, w, h, t, accent, alpha);
            case 2 -> constellation(g, w, h, dt, accent, alpha);
            case 3 -> waves(g, w, h, t, accent, alpha);
            default -> warp(g, w, h, dt, accent, alpha);
        }
        // мягкая виньетка — центр (кнопки) читается лучше
        g.fillGradient(0, 0, w, h / 4, a(0x99000000, alpha), 0x00000000);
        g.fillGradient(0, h - h / 3, w, h, 0x00000000, a(0xAA000000, alpha));
    }

    private static int a(int color, float mul) {
        return UiTheme.withAlpha(color, Math.max(0f, Math.min(1f, mul)));
    }

    // ── Аврора ───────────────────────────────────────────────────────────

    private static void aurora(GuiGraphicsExtractor g, int w, int h, float t, int accent, float alpha) {
        int[] colors = {
                accent,
                UiTheme.mix(accent, 0xFF7C5CFF, 0.55f),
                UiTheme.mix(accent, 0xFF38BDF8, 0.45f),
                UiTheme.mix(accent, 0xFFFFFFFF, 0.35f),
        };
        float base = Math.min(w, h);
        for (int b = 0; b < colors.length; b++) {
            float sp = 0.05f + b * 0.017f;
            float cx = w * (0.5f + 0.38f * (float) Math.sin(t * sp * 2.1f + b * 1.7f));
            float cy = h * (0.5f + 0.32f * (float) Math.cos(t * sp * 1.6f + b * 2.3f));
            float radius = base * (0.38f + 0.08f * (float) Math.sin(t * 0.21f + b));
            int layers = 16;
            for (int i = layers; i >= 1; i--) {
                float r = radius * i / layers;
                float k = 1f - (float) i / layers;
                int col = UiTheme.withAlpha(colors[b], (0.022f + 0.03f * k) * alpha);
                UiVector.roundRect(g, cx - r, cy - r, r * 2, r * 2, r, col);
            }
        }
    }

    // ── Созвездие ────────────────────────────────────────────────────────

    private static final int STARS = 80;
    private static final float[] px = new float[STARS];
    private static final float[] py = new float[STARS];
    private static final float[] vx = new float[STARS];
    private static final float[] vy = new float[STARS];
    private static boolean seeded;

    private static void constellation(GuiGraphicsExtractor g, int w, int h, float dt, int accent, float alpha) {
        if (!seeded) {
            for (int i = 0; i < STARS; i++) {
                px[i] = RANDOM.nextFloat();
                py[i] = RANDOM.nextFloat();
                double ang = RANDOM.nextDouble() * Math.PI * 2;
                float sp = 0.006f + RANDOM.nextFloat() * 0.012f;
                vx[i] = (float) Math.cos(ang) * sp;
                vy[i] = (float) Math.sin(ang) * sp;
            }
            seeded = true;
        }
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double my = mc.mouseHandler.getScaledYPos(mc.getWindow());
        float link = Math.min(w, h) * 0.2f;
        float[] sx = new float[STARS];
        float[] sy = new float[STARS];
        for (int i = 0; i < STARS; i++) {
            px[i] = (px[i] + vx[i] * dt + 1f) % 1f;
            py[i] = (py[i] + vy[i] * dt + 1f) % 1f;
            sx[i] = px[i] * w;
            sy[i] = py[i] * h;
            // точки рядом с курсором слегка притягиваются
            double dx = mx - sx[i];
            double dy = my - sy[i];
            double d = Math.hypot(dx, dy);
            if (d < link * 1.4 && d > 1) {
                float pull = (float) (1 - d / (link * 1.4)) * 10f;
                sx[i] += (float) (dx / d) * pull;
                sy[i] += (float) (dy / d) * pull;
            }
        }
        int soft = UiTheme.mix(accent, 0xFFFFFFFF, 0.35f);
        for (int i = 0; i < STARS; i++) {
            for (int j = i + 1; j < STARS; j++) {
                float dx = sx[i] - sx[j];
                float dy = sy[i] - sy[j];
                float d2 = dx * dx + dy * dy;
                if (d2 < link * link) {
                    float k = 1f - (float) Math.sqrt(d2) / link;
                    UiVector.line(g, sx[i], sy[i], sx[j], sy[j], 0.6f, UiTheme.withAlpha(soft, 0.35f * k * k * alpha));
                }
            }
            double d = Math.hypot(mx - sx[i], my - sy[i]);
            if (d < link) {
                float k = (float) (1 - d / link);
                UiVector.line(g, sx[i], sy[i], (float) mx, (float) my, 0.6f, UiTheme.withAlpha(accent, 0.45f * k * alpha));
            }
        }
        for (int i = 0; i < STARS; i++) {
            float r = 1.0f + (i % 3) * 0.35f;
            UiVector.roundRect(g, sx[i] - r, sy[i] - r, r * 2, r * 2, r, UiTheme.withAlpha(soft, 0.85f * alpha));
        }
    }

    // ── Волны ────────────────────────────────────────────────────────────

    private static void waves(GuiGraphicsExtractor g, int w, int h, float t, int accent, float alpha) {
        int lines = 7;
        int segs = Math.max(24, w / 8);
        for (int l = 0; l < lines; l++) {
            float k = l / (float) (lines - 1);
            float baseY = h * (0.42f + 0.32f * k);
            float amp = h * (0.05f + 0.035f * (float) Math.sin(t * 0.3f + l));
            float freq = 1.6f + l * 0.35f;
            float speed = 0.35f + l * 0.08f;
            int col = UiTheme.mix(accent, 0xFFFFFFFF, 0.15f + 0.5f * (1f - k));
            float prevX = 0;
            float prevY = 0;
            for (int s = 0; s <= segs; s++) {
                float x = w * s / (float) segs;
                float u = x / w;
                float y = baseY + amp * (float) (Math.sin(u * Math.PI * freq + t * speed + l * 0.9)
                        + 0.35 * Math.sin(u * Math.PI * freq * 2.3 - t * speed * 1.4));
                if (s > 0) {
                    UiVector.line(g, prevX, prevY, x, y, 3.0f, UiTheme.withAlpha(col, 0.05f * alpha));
                    UiVector.line(g, prevX, prevY, x, y, 0.8f, UiTheme.withAlpha(col, (0.25f + 0.35f * (1f - k)) * alpha));
                }
                prevX = x;
                prevY = y;
            }
        }
    }

    // ── Звёздный путь ────────────────────────────────────────────────────

    private static final int WARP = 170;
    private static final float[] wx = new float[WARP];
    private static final float[] wy = new float[WARP];
    private static final float[] wz = new float[WARP];
    private static boolean warpSeeded;

    private static void resetStar(int i, boolean anywhere) {
        wx[i] = (RANDOM.nextFloat() * 2f - 1f);
        wy[i] = (RANDOM.nextFloat() * 2f - 1f);
        wz[i] = anywhere ? 0.1f + RANDOM.nextFloat() * 0.9f : 1f;
    }

    private static void warp(GuiGraphicsExtractor g, int w, int h, float dt, int accent, float alpha) {
        if (!warpSeeded) {
            for (int i = 0; i < WARP; i++) {
                resetStar(i, true);
            }
            warpSeeded = true;
        }
        float cx = w / 2f;
        float cy = h / 2f;
        float scale = Math.max(w, h) * 0.5f;
        float speed = 0.16f;
        int soft = UiTheme.mix(accent, 0xFFFFFFFF, 0.55f);
        for (int i = 0; i < WARP; i++) {
            float z0 = wz[i];
            float z1 = z0 - speed * dt;
            if (z1 <= 0.04f) {
                resetStar(i, false);
                continue;
            }
            wz[i] = z1;
            float x0 = cx + wx[i] / z0 * scale * 0.5f;
            float y0 = cy + wy[i] / z0 * scale * 0.5f;
            float x1 = cx + wx[i] / z1 * scale * 0.5f;
            float y1 = cy + wy[i] / z1 * scale * 0.5f;
            // хвост длиннее у близких звёзд
            float tail = 6f;
            float tx = x1 + (x0 - x1) * tail;
            float ty = y1 + (y0 - y1) * tail;
            if (x1 < -20 || x1 > w + 20 || y1 < -20 || y1 > h + 20) {
                resetStar(i, false);
                continue;
            }
            float k = 1f - z1;
            int col = UiTheme.mix(accent, soft, k);
            UiVector.line(g, tx, ty, x1, y1, 0.5f + 1.1f * k, UiTheme.withAlpha(col, (0.15f + 0.75f * k) * alpha));
        }
    }
}
