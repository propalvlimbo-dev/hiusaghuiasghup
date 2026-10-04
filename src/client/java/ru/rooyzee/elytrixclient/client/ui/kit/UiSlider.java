package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

/**
 * Слайдер в стиле панели: строка с подписью, «чипом» значения справа и дорожкой снизу.
 * Ползунок плавно догоняет значение, при наведении подсвечивается акцентом.
 */
public class UiSlider extends UiWidget {

    private final String label;
    private final String suffix;
    private final int min;
    private final int max;
    private final Consumer<Integer> onChange;
    private int value;
    private boolean dragging;
    private float shownRatio = -1f;

    public UiSlider(String label, String suffix, int min, int max, int value, Consumer<Integer> onChange) {
        super(UiTheme.ROW_H_TALL);
        this.label = label;
        this.suffix = suffix == null ? "" : suffix;
        this.min = min;
        this.max = max;
        this.value = Math.max(min, Math.min(max, value));
        this.onChange = onChange;
    }

    public int value() {
        return value;
    }

    public void setValue(int v) {
        this.value = Math.max(min, Math.min(max, v));
    }

    private float ratio() {
        return max == min ? 0f : (float) (value - min) / (max - min);
    }

    private int trackX() {
        return x + 10;
    }

    private int trackW() {
        return Math.max(20, w - 20);
    }

    private int trackY() {
        return y + h - 11;
    }

    private void updateFromMouse(double mx) {
        float t = (float) ((mx - trackX()) / (double) trackW());
        t = Math.max(0f, Math.min(1f, t));
        int v = min + Math.round(t * (max - min));
        if (v != value) {
            value = v;
            if (onChange != null) {
                onChange.accept(value);
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!visible) {
            return;
        }
        beginFrame(mouseX, mouseY, dt);
        int bg = UiTheme.mix(UiTheme.ROW, UiTheme.ROW_HOVER, Math.max(hoverT, dragging ? 1f : 0f));
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, fadeIn(bg));

        var font = font();
        UiDraw.text(graphics, font, trim(font, label, w - 70), x + 10, y + 6, fadeIn(UiTheme.TEXT));

        String text = value + suffix;
        int chipW = UiDraw.width(font, text) + 14;
        int chipX = x + w - 10 - chipW;
        UiDraw.roundRect(graphics, chipX, y + 4, chipW, 14, UiTheme.R_SM, fadeIn(UiTheme.accentSoft(accent, 0.22f)));
        UiDraw.textCenter(graphics, font, text, chipX + chipW / 2, y + 7,
                fadeIn(UiTheme.mix(accent, 0xFFFFFFFF, 0.35f)));

        int tx = trackX();
        int tw = trackW();
        int ty = trackY();
        UiDraw.roundRect(graphics, tx, ty, tw, 4, 2, fadeIn(UiTheme.TRACK));

        float target = ratio();
        if (shownRatio < 0f) {
            shownRatio = target;
        }
        shownRatio += (target - shownRatio) * (ANIMATIONS ? Math.min(1f, dt * 16f) : 1f);
        if (Math.abs(target - shownRatio) < 0.0015f) {
            shownRatio = target;
        }

        int fillW = Math.round(tw * shownRatio);
        if (fillW > 0) {
            UiDraw.roundRect(graphics, tx, ty, Math.max(2, fillW), 4, 2, fadeIn(accent));
        }
        float knobX = tx + tw * shownRatio;
        float kr = 6.5f + 1.5f * Math.max(hoverT, dragging ? 1f : 0f);
        if (dragging || hoverT > 0.05f) {
            UiDraw.disc(graphics, knobX, ty + 2f, kr + 3.5f, UiTheme.withAlpha(accent, 0.25f * Math.max(hoverT, dragging ? 1f : 0f)));
        }
        UiDraw.disc(graphics, knobX, ty + 2f, kr, fadeIn(0xFFFFFFFF));
        UiDraw.disc(graphics, knobX, ty + 2f, kr - 2.2f, fadeIn(accent));
        endFrame();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || !visible || !contains(mx, my)) {
            return false;
        }
        dragging = true;
        updateFromMouse(mx);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button) {
        if (dragging) {
            updateFromMouse(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }
}
