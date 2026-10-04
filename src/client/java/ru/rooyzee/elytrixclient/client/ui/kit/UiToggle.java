package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

/** Строка с переключателем-«пилюлей» (вкл/выкл) и необязательным описанием. */
public class UiToggle extends UiWidget {

    private final String label;
    private final String description;
    private final Consumer<Boolean> onChange;
    private boolean value;
    private float anim;

    public UiToggle(String label, boolean value, Consumer<Boolean> onChange) {
        this(label, null, value, onChange);
    }

    public UiToggle(String label, String description, boolean value, Consumer<Boolean> onChange) {
        super(description == null ? UiTheme.ROW_H : UiTheme.ROW_H_TALL);
        this.label = label;
        this.description = description;
        this.value = value;
        this.onChange = onChange;
        this.anim = value ? 1f : 0f;
    }

    public boolean value() {
        return value;
    }

    public void setValue(boolean v) {
        this.value = v;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!visible) {
            return;
        }
        hovered = enabled && contains(mouseX, mouseY);
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, hovered ? UiTheme.ROW_HOVER : UiTheme.ROW);

        var font = font();
        int textX = x + 10;
        if (description == null) {
            UiDraw.text(graphics, font, label, textX, y + (h - 8) / 2 + 1, hovered ? UiTheme.TEXT : UiTheme.TEXT_SOFT);
        } else {
            UiDraw.text(graphics, font, label, textX, y + 6, UiTheme.TEXT);
            UiDraw.text(graphics, font, description, textX, y + h - 14, UiTheme.TEXT_DIM);
        }

        anim += ((value ? 1f : 0f) - anim) * Math.min(1f, dt * 14f);
        int tw = 26;
        int th = 14;
        int tx = x + w - 10 - tw;
        int ty = y + (h - th) / 2;
        int track = UiTheme.mix(UiTheme.TRACK, accent, anim);
        UiDraw.roundRect(graphics, tx, ty, tw, th, th / 2, track);
        if (anim > 0.02f) {
            UiDraw.roundRect(graphics, tx + 1, ty + 1, tw - 2, th / 2 - 1, (th / 2) - 1,
                    UiTheme.withAlpha(0xFFFFFFFF, 0.16f * anim));
        }
        float knobX = tx + 7 + anim * (tw - 14);
        UiDraw.disc(graphics, knobX, ty + th / 2f, 5.2f, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || !visible || !contains(mx, my)) {
            return false;
        }
        value = !value;
        if (onChange != null) {
            onChange.accept(value);
        }
        return true;
    }
}
