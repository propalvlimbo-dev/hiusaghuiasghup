package ru.rooyzee.elytrixclient.client.ui.kit.gfx;

import java.util.ArrayDeque;
import java.util.Deque;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import ru.rooyzee.elytrixclient.mixin.client.GuiGraphicsExtractorAccessor;

/**
 * Векторная отрисовка форм для кастомного GUI.
 *
 * <p>Зачем: спрайты и полоски из прямоугольников дают «лестницу» на скруглениях
 * и градиентах, а на больших разрешениях — ещё и мыло от растягивания текстуры.
 * Здесь формы считаются геометрией (float-координаты, реальные дуги) и кладутся
 * в {@code GuiRenderState} как собственный {@link GuiElementRenderState} — так же,
 * как это делает ванильная заливка. Никаких текстур, никакого масштабирования:
 * край остаётся гладким при любом GUI Scale и любом размере панели.
 *
 * <p>Пайплайн {@code RenderPipelines.GUI} рисует четверками вершин
 * ({@code VertexFormat.Mode.QUADS}), поэтому вся геометрия собирается в квады:
 * дуги — «пирогами» из двух треугольников в одном кваде, кольца — полосами
 * между внешним и внутренним контуром.
 *
 * <p>Все формы одного цвета/без текстуры попадают в один меш в
 * {@code GuiRenderer}, поэтому десятки скруглений — это один draw call.
 */
public final class UiVector {

    /** Зеркало ванильного scissor-стека: ванильный приватный, а формам нужен их прямоугольник. */
    private static final Deque<ScreenRectangle> SCISSOR = new ArrayDeque<>();
    private static final int MAX_SEGMENTS = 12;

    private UiVector() {
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Scissor
    // ─────────────────────────────────────────────────────────────────────

    /** Обрезка: делает то же, что {@code graphics.enableScissor}, но ещё и запоминает прямоугольник для форм. */
    public static void scissor(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        g.enableScissor(x0, y0, x1, y1);
        // как в ванили: прямоугольник обрезки хранится в экранных координатах,
        // т.е. уже с учётом текущей матрицы (translate/scale панели)
        ScreenRectangle rect = new ScreenRectangle(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0))
                .transformAxisAligned(g.pose());
        SCISSOR.push(clip(SCISSOR.peek(), rect));
    }

    /** Снять обрезку (парно {@link #scissor}). */
    public static void unscissor(GuiGraphicsExtractor g) {
        if (!SCISSOR.isEmpty()) {
            SCISSOR.pop();
        }
        g.disableScissor();
    }

    /** Сброс зеркала — на случай, если кадр закончился с незакрытой обрезкой. */
    public static void resetScissor() {
        SCISSOR.clear();
    }

    private static ScreenRectangle clip(ScreenRectangle outer, ScreenRectangle inner) {
        if (outer == null) {
            return inner;
        }
        ScreenRectangle result = outer.intersection(inner);
        return result == null ? ScreenRectangle.empty() : result;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Формы
    // ─────────────────────────────────────────────────────────────────────

    public static void rect(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
        roundRect(g, x, y, w, h, 0f, color);
    }

    public static void roundRect(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                 float radius, int color) {
        roundRect(g, x, y, w, h, radius, radius, radius, radius, color);
    }

    /** Прямоугольник с разными радиусами углов: порядок — ЛВ, ПВ, ПН, ЛН. */
    public static void roundRect(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                 float rTL, float rTR, float rBR, float rBL, int color) {
        roundRectBilinear(g, x, y, w, h, rTL, rTR, rBR, rBL, color, color, color, color);
    }

    /** Скруглённый прямоугольник с вертикальным градиентом. */
    public static void roundRectGradient(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                         float radius, int top, int bottom) {
        roundRectBilinear(g, x, y, w, h, radius, radius, radius, radius, top, top, bottom, bottom);
    }

    /**
     * Скруглённый прямоугольник с цветом в каждом углу (билинейная интерполяция):
     * {@code cTL, cTR, cBR, cBL} — левый верх, правый верх, правый низ, левый низ.
     */
    public static void roundRectBilinear(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                         float rTL, float rTR, float rBR, float rBL,
                                         int cTL, int cTR, int cBR, int cBL) {
        if (g == null || w <= 0.05f || h <= 0.05f) {
            return;
        }
        float[] r = radii(x, y, w, h, rTL, rTR, rBR, rBL);
        int[] seg = segments(r);
        float top = Math.max(r[0], r[1]);
        float bottom = Math.max(r[3], r[2]);
        Mesh mesh = new Mesh(x, y, w, h, cTL, cTR, cBR, cBL);

        if (h - top - bottom > 0.05f) {
            mesh.quad(x, y + top, x + w, y + top, x + w, y + h - bottom, x, y + h - bottom);
        }
        if (top > 0.05f) {
            if (w - r[0] - r[1] > 0.05f) {
                mesh.quad(x + r[0], y, x + w - r[1], y, x + w - r[1], y + top, x + r[0], y + top);
            }
            if (r[0] < top - 0.05f) {
                mesh.quad(x, y + r[0], x + r[0], y + r[0], x + r[0], y + top, x, y + top);
            }
            if (r[1] < top - 0.05f) {
                mesh.quad(x + w - r[1], y + r[1], x + w, y + r[1], x + w, y + top, x + w - r[1], y + top);
            }
        }
        if (bottom > 0.05f) {
            if (w - r[3] - r[2] > 0.05f) {
                mesh.quad(x + r[3], y + h - bottom, x + w - r[2], y + h - bottom,
                        x + w - r[2], y + h, x + r[3], y + h);
            }
            if (r[3] < bottom - 0.05f) {
                mesh.quad(x, y + h - bottom, x + r[3], y + h - bottom, x + r[3], y + h - r[3], x, y + h - r[3]);
            }
            if (r[2] < bottom - 0.05f) {
                mesh.quad(x + w - r[2], y + h - bottom, x + w, y + h - bottom, x + w, y + h - r[2],
                        x + w - r[2], y + h - r[2]);
            }
        }

        mesh.pie(x + r[0], y + r[0], r[0], 180f, 270f, seg[0]);
        mesh.pie(x + w - r[1], y + r[1], r[1], 270f, 360f, seg[1]);
        mesh.pie(x + w - r[2], y + h - r[2], r[2], 0f, 90f, seg[2]);
        mesh.pie(x + r[3], y + h - r[3], r[3], 90f, 180f, seg[3]);

        mesh.emit(g);
    }

    /** Рамка скруглённого прямоугольника (кольцо), не требует фона под собой. */
    public static void outline(GuiGraphicsExtractor g, float x, float y, float w, float h,
                               float radius, float thickness, int color) {
        outline(g, x, y, w, h, radius, thickness, color, color, false);
    }

    /** Рамка с горизонтальным градиентом цвета. */
    public static void outlineGradient(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                       float radius, float thickness, int left, int right) {
        outline(g, x, y, w, h, radius, thickness, left, right, true);
    }

    private static void outline(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                float radius, float thickness, int left, int right, boolean horizontal) {
        if (g == null || w <= 0.05f || h <= 0.05f || thickness <= 0.05f) {
            return;
        }
        float t = Math.min(thickness, Math.min(w, h) * 0.5f);
        float[] outerRadii = radii(x, y, w, h, radius, radius, radius, radius);
        int[] seg = segments(outerRadii);
        float[] outer = boundary(x, y, w, h, outerRadii, seg);
        float[] inner = boundary(x + t, y + t, w - 2 * t, h - 2 * t,
                radii(x + t, y + t, w - 2 * t, h - 2 * t,
                        outerRadii[0] - t, outerRadii[1] - t, outerRadii[2] - t, outerRadii[3] - t), seg);
        int n = Math.min(outer.length, inner.length) / 2;
        if (n < 3) {
            return;
        }
        Mesh mesh = new Mesh(x, y, w, h, left, horizontal ? right : left, horizontal ? right : left, left);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            mesh.quad(outer[i * 2], outer[i * 2 + 1], outer[j * 2], outer[j * 2 + 1],
                    inner[j * 2], inner[j * 2 + 1], inner[i * 2], inner[i * 2 + 1]);
        }
        mesh.emit(g);
    }

    /** Мягкая тень/свечение: несколько расширяющихся слоёв с падающей альфой. */
    public static void shadow(GuiGraphicsExtractor g, float x, float y, float w, float h,
                              float radius, float spread, int color, int layers) {
        if (g == null || w <= 0.05f || h <= 0.05f || spread <= 0.05f || layers <= 0) {
            return;
        }
        int n = Math.max(1, Math.min(layers, 12));
        float base = ((color >>> 24) & 0xFF) / 255f;
        if (base <= 0f) {
            return;
        }
        int rgb = color & 0x00FFFFFF;
        for (int i = n; i >= 1; i--) {
            float t = (float) i / n;
            float grow = spread * t;
            float alpha = base * (1f - t * t) * 1.6f / n;
            int col = (Math.round(255f * Math.min(1f, alpha)) << 24) | rgb;
            roundRect(g, x - grow, y - grow, w + 2 * grow, h + 2 * grow, radius + grow, col);
        }
    }

    /** Отрезок произвольной толщины (один квад). */
    public static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1,
                            float thickness, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f || thickness <= 0f) {
            return;
        }
        float half = thickness * 0.5f;
        float nx = -dy / len * half;
        float ny = dx / len * half;
        Mesh mesh = new Mesh(0f, 0f, 1f, 1f, color, color, color, color);
        mesh.quad(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny);
        mesh.emit(g);
    }

    /** Треугольник (для иконок вроде «play»). */
    public static void triangle(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1,
                                float x2, float y2, int color) {
        Mesh mesh = new Mesh(0f, 0f, 1f, 1f, color, color, color, color);
        mesh.quad(x0, y0, x1, y1, x2, y2, x2, y2);
        mesh.emit(g);
    }

    /** Смешать два ARGB-цвета. */
    public static int mix(int a, int b, float t) {
        float k = t < 0f ? 0f : (t > 1f ? 1f : t);
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return (Math.round(aa + (ba - aa) * k) << 24)
                | (Math.round(ar + (br - ar) * k) << 16)
                | (Math.round(ag + (bg - ag) * k) << 8)
                | Math.round(ab + (bb - ab) * k);
    }

    /** Цвет с заданной альфой (0..1). */
    public static int alpha(int color, float a) {
        int value = Math.round(255f * (a < 0f ? 0f : (a > 1f ? 1f : a)));
        return (value << 24) | (color & 0x00FFFFFF);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Геометрия
    // ─────────────────────────────────────────────────────────────────────

    private static float[] radii(float x, float y, float w, float h,
                                 float rTL, float rTR, float rBR, float rBL) {
        float max = Math.min(w, h) * 0.5f;
        return new float[] {
                clamp(rTL, 0f, max), clamp(rTR, 0f, max), clamp(rBR, 0f, max), clamp(rBL, 0f, max)
        };
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    /** Число сегментов на угол: адаптивно радиусу, но не больше {@link #MAX_SEGMENTS}. */
    private static int[] segments(float[] r) {
        return new int[] { segments(r[0]), segments(r[1]), segments(r[2]), segments(r[3]) };
    }

    private static int segments(float radius) {
        if (radius <= 0.05f) {
            return 0;
        }
        return (int) clamp((float) Math.ceil(radius * 0.35f) + 2f, 3f, MAX_SEGMENTS);
    }

    /**
     * Контур скруглённого прямоугольника по часовой стрелке.
     * Порядок точек: верх, правый верх, право, правый низ, низ, левый низ, лево, левый верх.
     */
    private static float[] boundary(float x, float y, float w, float h, float[] r, int[] seg) {
        Buf out = new Buf();
        out.add(x + r[0], y);
        out.add(x + w - r[1], y);
        arc(out, x + w - r[1], y + r[1], r[1], seg[1], 270f, 360f);
        out.add(x + w, y + h - r[2]);
        arc(out, x + w - r[2], y + h - r[2], r[2], seg[2], 0f, 90f);
        out.add(x + r[3], y + h);
        arc(out, x + r[3], y + h - r[3], r[3], seg[3], 90f, 180f);
        out.add(x, y + r[0]);
        arc(out, x + r[0], y + r[0], r[0], seg[0], 180f, 270f);
        return out.trim();
    }

    /** Точки дуги, начиная со следующей за начальной (конечная точка входит). */
    private static void arc(Buf out, float cx, float cy, float radius, int seg, float a0, float a1) {
        if (radius <= 0.05f || seg <= 0) {
            return;
        }
        for (int i = 1; i <= seg; i++) {
            double a = Math.toRadians(a0 + (a1 - a0) * i / seg);
            out.add(cx + (float) Math.cos(a) * radius, cy + (float) Math.sin(a) * radius);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Меш и состояние отрисовки
    // ─────────────────────────────────────────────────────────────────────

    /** Набор вершин в порядке отрисовки (пайплайн GUI — квады, 4 вершины на фигуру). */
    private static final class Mesh {
        private final float x;
        private final float y;
        private final float w;
        private final float h;
        private final int cTL;
        private final int cTR;
        private final int cBR;
        private final int cBL;

        private float[] xy = new float[128];
        private int[] colors = new int[64];
        private int count;
        private float minX = Float.MAX_VALUE;
        private float minY = Float.MAX_VALUE;
        private float maxX = -Float.MAX_VALUE;
        private float maxY = -Float.MAX_VALUE;

        private Mesh(float x, float y, float w, float h, int cTL, int cTR, int cBR, int cBL) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.cTL = cTL;
            this.cTR = cTR;
            this.cBR = cBR;
            this.cBL = cBL;
        }

        /** Пирог: сектор угла как один квад (центр, начало, середина дуги, конец). */
        private void pie(float cx, float cy, float radius, float a0, float a1, int seg) {
            if (radius <= 0.05f || seg <= 0) {
                return;
            }
            for (int i = 0; i < seg; i++) {
                double s = Math.toRadians(a0 + (a1 - a0) * i / seg);
                double m = Math.toRadians(a0 + (a1 - a0) * (i + 0.5f) / seg);
                double e = Math.toRadians(a0 + (a1 - a0) * (i + 1) / seg);
                quad(cx, cy,
                        cx + (float) Math.cos(s) * radius, cy + (float) Math.sin(s) * radius,
                        cx + (float) Math.cos(m) * radius, cy + (float) Math.sin(m) * radius,
                        cx + (float) Math.cos(e) * radius, cy + (float) Math.sin(e) * radius);
            }
        }

        /** Квад из четырёх точек; цвет каждой вершины берётся из градиента фигуры. */
        private void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
            // Пайплайн GUI отсекает задние грани (cull = true). Ваниль кладёт вершины
            // в порядке TL -> BL -> BR -> TR, т.е. в экранных координатах (Y вниз)
            // ориентированная площадь отрицательна. Приводим любой квад к этому обходу,
            // иначе GPU молча выбрасывает фигуру.
            float area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1)
                    + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
            if (Math.abs(area) < 1.0e-6f) {
                return;
            }
            if (area > 0f) {
                vertex(x0, y0, color(x0, y0));
                vertex(x3, y3, color(x3, y3));
                vertex(x2, y2, color(x2, y2));
                vertex(x1, y1, color(x1, y1));
            } else {
                vertex(x0, y0, color(x0, y0));
                vertex(x1, y1, color(x1, y1));
                vertex(x2, y2, color(x2, y2));
                vertex(x3, y3, color(x3, y3));
            }
        }

        private void vertex(float vx, float vy, int color) {
            if (count * 2 + 2 > xy.length) {
                xy = java.util.Arrays.copyOf(xy, xy.length * 2);
            }
            if (count + 1 > colors.length) {
                colors = java.util.Arrays.copyOf(colors, colors.length * 2);
            }
            xy[count * 2] = vx;
            xy[count * 2 + 1] = vy;
            colors[count] = color;
            if (vx < minX) {
                minX = vx;
            }
            if (vy < minY) {
                minY = vy;
            }
            if (vx > maxX) {
                maxX = vx;
            }
            if (vy > maxY) {
                maxY = vy;
            }
            count++;
        }

        private int color(float px, float py) {
            float tx = w <= 0.001f ? 0f : clamp((px - x) / w, 0f, 1f);
            float ty = h <= 0.001f ? 0f : clamp((py - y) / h, 0f, 1f);
            int top = mix(cTL, cTR, tx);
            int bottom = mix(cBL, cBR, tx);
            return mix(top, bottom, ty);
        }

        private void emit(GuiGraphicsExtractor g) {
            if (count < 4) {
                return;
            }
            float[] px = java.util.Arrays.copyOf(xy, count * 2);
            int[] pc = java.util.Arrays.copyOf(colors, count);
            Matrix3x2f pose = new Matrix3x2f(g.pose());
            ScreenRectangle bounds = new ScreenRectangle(
                    (int) Math.floor(minX) - 1,
                    (int) Math.floor(minY) - 1,
                    (int) Math.ceil(maxX - minX) + 3,
                    (int) Math.ceil(maxY - minY) + 3).transformMaxBounds(pose);
            ScreenRectangle scissor = SCISSOR.peek();
            if (scissor != null) {
                bounds = clip(scissor, bounds);
            }
            ((GuiGraphicsExtractorAccessor) g).elytrix$guiRenderState()
                    .addGuiElement(new MeshState(pose, px, pc, scissor, bounds));
        }
    }

    /** Готовый элемент GUI: вершины уже посчитаны, остаётся отдать их пайплайну. */
    public record MeshState(
            Matrix3x2fc pose,
            float[] xy,
            int[] colors,
            ScreenRectangle scissorArea,
            ScreenRectangle bounds
    ) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int i = 0; i < colors.length; i++) {
                consumer.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(colors[i]);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }

    /** Растущий массив пар координат для контуров. */
    private static final class Buf {
        private float[] data = new float[64];
        private int size;

        private void add(float x, float y) {
            if (size * 2 + 2 > data.length) {
                data = java.util.Arrays.copyOf(data, data.length * 2);
            }
            data[size * 2] = x;
            data[size * 2 + 1] = y;
            size++;
        }

        private float[] trim() {
            return java.util.Arrays.copyOf(data, size * 2);
        }
    }
}
