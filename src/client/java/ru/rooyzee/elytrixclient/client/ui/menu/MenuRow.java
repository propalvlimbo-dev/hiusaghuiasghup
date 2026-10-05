package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.ui.UiSound;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static ru.rooyzee.elytrixclient.client.ui.menu.MenuKit.*;

/**
 * Строка внутри карточки меню. Координаты — в единицах панели (локальные).
 * Наследники: {@link Toggle}, {@link Slider}, {@link Mode}, {@link Info},
 * {@link Button}, {@link Text}.
 */
public abstract class MenuRow {
    protected final String label;
    protected float x;
    protected float y;
    protected float w;
    protected float h = 16;
    protected float hoverT;
    private BooleanSupplier visible = () -> true;
    private String desc;

    protected MenuRow(String label) {
        this.label = label;
    }

    /** Описание, всплывающее при наведении (что делает функция). */
    public MenuRow describe(String description) {
        this.desc = description;
        return this;
    }

    // Тултип рисуется поверх всего меню в конце кадра — строка лишь «запрашивает» его.
    private static String tipDesc;
    private static float tipX, tipY;

    /** Отрисовать запрошенный в этом кадре тултип поверх всего и сбросить. */
    public static void drawTooltip(GuiGraphicsExtractor g) {
        if (tipDesc == null) {
            return;
        }
        float tw = ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer
                .width(ru.rooyzee.elytrixclient.client.render.font.Fonts.REGULAR, tipDesc, 7f);
        float tx = tipX + 10f, ty = tipY + 12f;
        ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector
                .roundRect(g, tx, ty, tw + 12f, 15f, 4f, 0xF2141218);
        ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector
                .outline(g, tx, ty, tw + 12f, 15f, 4f, .5f, 0x33FFFFFF);
        ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer
                .draw(g, ru.rooyzee.elytrixclient.client.render.font.Fonts.REGULAR, tipDesc, tx + 6f, ty + 4f, 7f, 0xE6FFFFFF);
        tipDesc = null;
    }

    /** Показывать строку только при условии. */
    public MenuRow when(BooleanSupplier condition) {
        this.visible = condition;
        return this;
    }

    public boolean visible() {
        return visible.getAsBoolean();
    }

    public void layout(Font font, float x, float y, float w) {
        this.x = x;
        this.y = y;
        this.w = w;
    }

    public float height() {
        return h;
    }

    public boolean matches(String query) {
        return lower(label).contains(query);
    }

    protected boolean hover(double mx, double my) {
        return inside(mx, my, x, y, w, h);
    }

    /** Общий «ховер» строки: лёгкая подсветка под всей строкой. */
    protected void hoverBg(GuiGraphicsExtractor g, double mx, double my, float dt) {
        hoverT = approach(hoverT, hover(mx, my) ? 1f : 0f, 16f, dt);
        if (hoverT > 0.01f) {
            fill(g, x + 4, y + 1, w - 8, h - 2, 4, UiTheme.withAlpha(text(), 0.045f * hoverT));
        }
        if (desc != null && hoverT > .5f) {
            tipDesc = desc;
            tipX = (float) mx;
            tipY = (float) my;
        }
    }

    public abstract void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
    }

    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        return false;
    }

    /** Строка перехватывает клавиатуру (поле ввода в фокусе). */
    public boolean capturing() {
        return false;
    }

    public void blur() {
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Тумблер
    // ═════════════════════════════════════════════════════════════════════

    public static final class Toggle extends MenuRow {
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float t = -1f;

        public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(label);
            this.get = get;
            this.set = set;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
            boolean on = get.getAsBoolean();
            t = t < 0 ? (on ? 1f : 0f) : approach(t, on ? 1f : 0f, 14f, dt);
            hoverBg(g, mx, my, dt);

            float cy = y + h / 2f;
            text(g, font, trim(font, label, SMALL, w - 46), x + 10, ty(SMALL, cy),
                    UiTheme.mix(soft(), text(), Math.max(t, hoverT * 0.6f)), SMALL);

            float tw = 17;
            float th = 9;
            float tx = x + w - 10 - tw;
            float tyy = cy - th / 2f;
            fill(g, tx, tyy, tw, th, th / 2f, UiTheme.mix(UiTheme.mix(field(), text(), 0.08f), accent, t));
            float knob = 3.2f;
            float kx = tx + 4.5f + (tw - 9f) * t;
            disc(g, kx, cy, knob, UiTheme.mix(dim(), 0xFFFFFFFF, t));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && hover(mx, my)) {
                set.accept(!get.getAsBoolean());
                UiSound.play(get.getAsBoolean() ? UiSound.Event.ON : UiSound.Event.OFF);
                return true;
            }
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Слайдер
    // ═════════════════════════════════════════════════════════════════════

    public static final class Slider extends MenuRow {
        private final int min;
        private final int max;
        private final int step;
        private final String suffix;
        private final IntSupplier get;
        private final IntConsumer set;
        private boolean dragging;
        private float shown = -1f;

        public Slider(String label, int min, int max, int step, String suffix, IntSupplier get, IntConsumer set) {
            super(label);
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
            this.suffix = suffix == null ? "" : suffix;
            this.get = get;
            this.set = set;
            this.h = 23;
        }

        private float trackX() {
            return x + 10;
        }

        private float trackW() {
            return w - 20;
        }

        private void apply(double mx) {
            float t = (float) ((mx - trackX()) / trackW());
            t = t < 0 ? 0 : (t > 1 ? 1 : t);
            int raw = Math.round(min + (max - min) * t);
            int v = Math.round((raw - min) / (float) step) * step + min;
            set.accept(Math.max(min, Math.min(max, v)));
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
            if (dragging) {
                apply(mx);
            }
            hoverBg(g, mx, my, dt);
            int value = get.getAsInt();
            float t = (value - min) / (float) Math.max(1, max - min);
            shown = shown < 0 ? t : approach(shown, t, 20f, dt);

            text(g, font, trim(font, label, SMALL, w - 60), x + 10, ty(SMALL, y + 7), soft(), SMALL);
            textRight(g, font, value + suffix, x + w - 10, ty(MONO, y + 7), text(), MONO);

            float tyy = y + 15;
            fill(g, trackX(), tyy, trackW(), 3, 1.5f, field());
            float fw = trackW() * shown;
            if (fw > 0.5f) {
                fill(g, trackX(), tyy, fw, 3, 1.5f, accent);
            }
            float kx = trackX() + fw;
            if (dragging || hoverT > 0.01f) {
                disc(g, kx, tyy + 1.5f, 6f, UiTheme.withAlpha(accent, 0.18f * Math.max(hoverT, dragging ? 1f : 0f)));
            }
            disc(g, kx, tyy + 1.5f, 3.6f, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && hover(mx, my)) {
                dragging = true;
                apply(mx);
                UiSound.play(UiSound.Event.CLICK);
                return true;
            }
            return false;
        }

        @Override
        public void mouseReleased(double mx, double my, int button) {
            dragging = false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Выбор варианта — «чипы» с переносом строк (как сегменты в macOS)
    // ═════════════════════════════════════════════════════════════════════

    public static final class Mode extends MenuRow {
        private static final float CHIP_H = 12;
        private static final float GAP = 3;
        private final String[] options;
        private final IntSupplier get;
        private final IntConsumer set;
        private float[] cx = new float[0];
        private float[] cy = new float[0];
        private float[] cw = new float[0];
        private float[] sel = new float[0];
        private int hoveredChip = -1;

        public Mode(String label, String[] options, IntSupplier get, IntConsumer set) {
            super(label);
            this.options = options;
            this.get = get;
            this.set = set;
        }

        @Override
        public void layout(Font font, float x, float y, float w) {
            super.layout(font, x, y, w);
            int n = options.length;
            if (cx.length != n) {
                cx = new float[n];
                cy = new float[n];
                cw = new float[n];
                sel = new float[n];
                for (int i = 0; i < n; i++) {
                    sel[i] = -1f;
                }
            }
            float px = x + 10;
            float py = y + 14;
            float right = x + w - 10;
            for (int i = 0; i < n; i++) {
                float chip = width(font, options[i], SMALL) + 10;
                if (px + chip > right && px > x + 10) {
                    px = x + 10;
                    py += CHIP_H + GAP;
                }
                cx[i] = px;
                cy[i] = py;
                cw[i] = chip;
                px += chip + GAP;
            }
            this.h = (n == 0 ? 14 : (cy[n - 1] - y) + CHIP_H + 4);
        }

        @Override
        public boolean matches(String query) {
            if (super.matches(query)) {
                return true;
            }
            for (String o : options) {
                if (lower(o).contains(query)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
            text(g, font, label, x + 10, ty(SMALL, y + 7), soft(), SMALL);
            int current = get.getAsInt();
            boolean anyHover = false;
            for (int i = 0; i < options.length; i++) {
                boolean on = i == current;
                sel[i] = sel[i] < 0 ? (on ? 1f : 0f) : approach(sel[i], on ? 1f : 0f, 16f, dt);
                boolean hv = inside(mx, my, cx[i], cy[i], cw[i], CHIP_H);
                if (hv && hoveredChip != i) {
                    UiSound.play(UiSound.Event.HOVER);
                }
                if (hv) {
                    anyHover = true;
                    hoveredChip = i;
                }
                fill(g, cx[i], cy[i], cw[i], CHIP_H, 4, hv ? UiTheme.mix(field(), text(), 0.06f) : field());
                if (sel[i] > 0.01f) {
                    fill(g, cx[i], cy[i], cw[i], CHIP_H, 4, UiTheme.withAlpha(accent, sel[i]));
                }
                int col = UiTheme.mix(hv ? text() : soft(), 0xFFFFFFFF, sel[i]);
                textCenter(g, font, options[i], cx[i] + cw[i] / 2f, ty(SMALL, cy[i] + CHIP_H / 2f), col, SMALL);
            }
            if (!anyHover) {
                hoveredChip = -1;
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) {
                return false;
            }
            for (int i = 0; i < options.length; i++) {
                if (inside(mx, my, cx[i], cy[i], cw[i], CHIP_H)) {
                    set.accept(i);
                    UiSound.play(UiSound.Event.CLICK);
                    return true;
                }
            }
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Информация «ключ — значение»
    // ═════════════════════════════════════════════════════════════════════

    public static final class Info extends MenuRow {
        private final Supplier<String> value;
        private final int color;

        /** {@code color = 0} — обычный цвет текста. */
        public Info(String label, Supplier<String> value, int color) {
            super(label);
            this.value = value;
            this.color = color;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
            float cy = y + h / 2f;
            String v = value.get();
            int vw = width(font, v, MONO);
            text(g, font, trim(font, label, SMALL, w - 24 - vw), x + 10, ty(SMALL, cy), soft(), SMALL);
            if (color != 0) {
                disc(g, x + w - 10 - vw - 6, cy, 2f, color);
            }
            textRight(g, font, trim(font, v, MONO, w - 40), x + w - 10, ty(MONO, cy),
                    color != 0 ? text() : text(), MONO);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Кнопка
    // ═════════════════════════════════════════════════════════════════════

    public static final class Button extends MenuRow {
        public enum Kind { PRIMARY, SECONDARY, DANGER }

        private final Supplier<String> text;
        private final Kind kind;
        private final Runnable action;
        private float press;
        private boolean wasHover;

        public Button(String label, Kind kind, Runnable action) {
            this(() -> label, kind, action);
        }

        public Button(Supplier<String> label, Kind kind, Runnable action) {
            super("");
            this.text = label;
            this.kind = kind;
            this.action = action;
            this.h = 20;
        }

        @Override
        public boolean matches(String query) {
            return lower(text.get()).contains(query);
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
            float bx = x + 10;
            float by = y + 3;
            float bw = w - 20;
            float bh = 14;
            boolean hv = inside(mx, my, bx, by, bw, bh);
            if (hv && !wasHover) {
                UiSound.play(UiSound.Event.HOVER);
            }
            wasHover = hv;
            hoverT = approach(hoverT, hv ? 1f : 0f, 16f, dt);
            press = approach(press, 0f, 10f, dt);
            float s = press * 0.8f;
            bx += s;
            by += s * 0.5f;
            bw -= s * 2;
            bh -= s;
            int label;
            switch (kind) {
                case PRIMARY -> {
                    fill(g, bx, by, bw, bh, 4, UiTheme.mix(accent, 0xFFFFFFFF, 0.12f * hoverT));
                    label = 0xFFFFFFFF;
                }
                case DANGER -> {
                    fill(g, bx, by, bw, bh, 4, UiTheme.withAlpha(UiTheme.ERROR, 0.14f + 0.1f * hoverT));
                    outline(g, bx, by, bw, bh, 4, 0.7f, UiTheme.withAlpha(UiTheme.ERROR, 0.45f));
                    label = UiTheme.ERROR;
                }
                default -> {
                    fill(g, bx, by, bw, bh, 4, UiTheme.mix(field(), text(), 0.05f * hoverT));
                    outline(g, bx, by, bw, bh, 4, 0.7f, cardEdge());
                    label = UiTheme.mix(soft(), text(), hoverT);
                }
            }
            textCenter(g, font, trim(font, text.get(), SMALL, bw - 8), bx + bw / 2f, ty(SMALL, by + bh / 2f),
                    label, SMALL);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && inside(mx, my, x + 10, y + 3, w - 20, 14)) {
                press = 1f;
                UiSound.play(UiSound.Event.CLICK);
                action.run();
                return true;
            }
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Поле ввода
    // ═════════════════════════════════════════════════════════════════════

    public static final class Text extends MenuRow {
        private final Supplier<String> get;
        private final Consumer<String> set;
        private final int maxLength;
        private boolean focused;
        private float focusT;

        public Text(String label, int maxLength, Supplier<String> get, Consumer<String> set) {
            super(label);
            this.get = get;
            this.set = set;
            this.maxLength = maxLength;
            this.h = 28;
        }

        private float fx() {
            return x + 10;
        }

        private float fy() {
            return y + 12;
        }

        private float fw() {
            return w - 20;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
            focusT = approach(focusT, focused ? 1f : 0f, 16f, dt);
            text(g, font, label, x + 10, ty(SMALL, y + 6.5f), soft(), SMALL);
            float fh = 13;
            fill(g, fx(), fy(), fw(), fh, 4, field());
            outline(g, fx(), fy(), fw(), fh, 4, 0.7f, UiTheme.mix(cardEdge(), accent, focusT));

            String value = get.get();
            float maxW = fw() - 10;
            String shown = value;
            // показываем «хвост» строки, если она длиннее поля — как в терминале
            while (!shown.isEmpty() && width(font, shown, MONO) > maxW) {
                shown = shown.substring(1);
            }
            float tx = fx() + 5;
            float cy = fy() + fh / 2f;
            text(g, font, shown, tx, ty(MONO, cy), text(), MONO);
            if (focused && (Util.getMillis() / 530L) % 2L == 0L) {
                float cxp = tx + width(font, shown, MONO) + 0.5f;
                fill(g, cxp, cy - 3.5f, 0.8f, 7, 0, accent);
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            boolean inField = inside(mx, my, fx(), fy(), fw(), 13);
            focused = inField && button == 0;
            return inField;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!focused) {
                return false;
            }
            String value = get.get();
            int key = event.key();
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!value.isEmpty()) {
                    set.accept(event.hasControlDown() ? "" : value.substring(0, value.length() - 1));
                }
                return true;
            }
            if (event.isEscape() || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                focused = false;
                return true;
            }
            if (event.isPaste()) {
                String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clip != null) {
                    String add = clip.replaceAll("[\\r\\n\\t]", "").trim();
                    set.accept(clip(value + add));
                }
                return true;
            }
            if (event.isCopy()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(value);
                return true;
            }
            return true;
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            if (!focused) {
                return false;
            }
            if (event.isAllowedChatCharacter()) {
                set.accept(clip(get.get() + event.codepointAsString()));
            }
            return true;
        }

        private String clip(String s) {
            return s.length() > maxLength ? s.substring(0, maxLength) : s;
        }

        @Override
        public boolean capturing() {
            return focused;
        }

        @Override
        public void blur() {
            focused = false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Выбор цвета — кружки-образцы
    // ═════════════════════════════════════════════════════════════════════

    public static final class Swatches extends MenuRow {
        private static final float R = 5.5f;
        private static final float STEP = 17;
        private final int[] colors;
        private final IntSupplier get;
        private final IntConsumer set;
        private final float[] sel;
        private int hovered = -1;

        public Swatches(String label, int[] colors, IntSupplier get, IntConsumer set) {
            super(label);
            this.colors = colors;
            this.get = get;
            this.set = set;
            this.sel = new float[colors.length];
            java.util.Arrays.fill(sel, -1f);
            this.h = 30;
        }

        private float sx(int i) {
            return x + 10 + R + i * STEP;
        }

        private float sy() {
            return y + 20;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
            text(g, font, label, x + 10, ty(SMALL, y + 7), soft(), SMALL);
            int current = get.getAsInt();
            int hv = -1;
            for (int i = 0; i < colors.length; i++) {
                boolean on = i == current;
                sel[i] = sel[i] < 0 ? (on ? 1f : 0f) : approach(sel[i], on ? 1f : 0f, 16f, dt);
                float cx = sx(i);
                float cy = sy();
                boolean over = Math.hypot(mx - cx, my - cy) <= R + 2;
                if (over) {
                    hv = i;
                }
                if (sel[i] > 0.01f) {
                    float rr = R + 2.4f;
                    outline(g, cx - rr, cy - rr, rr * 2, rr * 2, rr, 1f, UiTheme.withAlpha(colors[i], 0.9f * sel[i]));
                }
                disc(g, cx, cy, R - (over && !on ? -0.4f : 0f), colors[i]);
            }
            if (hv >= 0 && hv != hovered) {
                UiSound.play(UiSound.Event.HOVER);
            }
            hovered = hv;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) {
                return false;
            }
            for (int i = 0; i < colors.length; i++) {
                if (Math.hypot(mx - sx(i), my - sy()) <= R + 2) {
                    set.accept(i);
                    UiSound.play(UiSound.Event.CLICK);
                    return true;
                }
            }
            return false;
        }
    }
}
