package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

/**
 * Слайдер в стиле панели: строка с подписью, «чипом» значения справа и дорожкой снизу.
 * Значение целочисленное, меняется перетаскиванием или кликом по дорожке.
 */
public class UiSlider extends UiWidget {

    private final String label;
    private final String suffix;
    private final int min;
    private final int max;
    private final Consumer<Integer> onChange;
    private int value;
    private boolean dragging;

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
        hovered = enabled && contains(mouseX, mouseY);
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, hovered || dragging ? UiTheme.ROW_HOVER : UiTheme.ROW);

        var font = font();
        UiDraw.text(graphics, font, label, x + 10, y + 6, UiTheme.TEXT);

        String text = value + suffix;
        int chipW = font.width(text) + 14;
        int chipX = x + w - 10 - chipW;
        UiDraw.roundRect(graphics, chipX, y + 4, chipW, 14, UiTheme.R_SM, UiTheme.accentSoft(accent, 0.22f));
        UiDraw.textCenter(graphics, font, text, chipX + chipW / 2, y + 7, UiTheme.mix(accent, 0xFFFFFFFF, 0.35f));

        int tx = trackX();
        int tw = trackW();
        int ty = trackY();
        UiDraw.roundRect(graphics, tx, ty, tw, 4, 2, UiTheme.TRACK);
        int fillW = Math.round(tw * ratio());
        if (fillW > 0) {
            UiDraw.roundRect(graphics, tx, ty, Math.max(2, fillW), 4, 2, accent);
        }
        float knobX = tx + fillW;
        UiDraw.disc(graphics, knobX, ty + 2f, 7f, UiTheme.mix(accent, 0xFFFFFFFF, 0.15f));
        UiDraw.disc(graphics, knobX, ty + 2f, 5.5f, 0xFFFFFFFF);
        UiDraw.disc(graphics, knobX, ty + 2f, 3.2f, accent);
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
