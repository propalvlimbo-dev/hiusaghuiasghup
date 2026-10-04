package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

/**
 * Строка с переключателем-«пилюлей» (вкл/выкл) и необязательным описанием.
 * Ползунок плавно едет, дорожка перекрашивается в акцент, при включении вокруг
 * ползунка появляется мягкое свечение.
 */
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
    public void replay(float delay) {
        super.replay(delay);
        anim = value ? 1f : 0f;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!visible) {
            return;
        }
        beginFrame(mouseX, mouseY, dt);
        int bg = UiTheme.mix(UiTheme.ROW, UiTheme.ROW_HOVER, hoverT);
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, fadeIn(bg));
        if (hoverT > 0.01f) {
            UiDraw.roundRect(graphics, x, y, 2, h, 1, UiTheme.withAlpha(accent, 0.75f * hoverT * Math.max(0.05f, appear)));
        }

        var font = font();
        int textX = x + 10;
        int textW = w - 46;
        if (description == null) {
            int lh = (h - 8) / 2 + 1;
            UiDraw.text(graphics, font, trim(font, label, textW), textX, y + lh,
                    fadeIn(UiTheme.mix(UiTheme.TEXT_SOFT, UiTheme.TEXT, hoverT)));
        } else {
            UiDraw.text(graphics, font, trim(font, label, textW), textX, y + 6, fadeIn(UiTheme.TEXT));
            UiDraw.text(graphics, font, trim(font, description, textW), textX, y + h - 14, fadeIn(UiTheme.TEXT_DIM));
        }

        float target = value ? 1f : 0f;
        anim += (target - anim) * (ANIMATIONS ? Math.min(1f, dt * 13f) : 1f);
        if (Math.abs(target - anim) < 0.002f) {
            anim = target;
        }
        int tw = 28;
        int th = 15;
        int tx = x + w - 11 - tw;
        int ty = y + (h - th) / 2;
        int track = UiTheme.mix(UiTheme.TRACK, accent, anim);
        if (anim > 0.05f) {
            UiDraw.glow(graphics, tx, ty, tw, th, th / 2, accent, 0.35f * anim);
        }
        UiDraw.roundRect(graphics, tx, ty, tw, th, th / 2, fadeIn(track));
        UiDraw.roundRect(graphics, tx + 1, ty + 1, tw - 2, Math.max(1, th / 2 - 1), th / 2 - 1,
                UiTheme.withAlpha(0xFFFFFFFF, (0.10f + 0.14f * anim) * Math.max(0.05f, appear)));
        float knobX = tx + th / 2f + anim * (tw - th);
        UiDraw.disc(graphics, knobX, ty + th / 2f, 5.6f, fadeIn(0xFFFFFFFF));
        UiDraw.disc(graphics, knobX, ty + th / 2f, 3.4f, UiTheme.withAlpha(UiTheme.mix(0xFFD8DCE6, accent, anim), Math.max(0.05f, appear)));
        endFrame();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || !visible || !contains(mx, my)) {
            return false;
        }
        pressT = 1f;
        value = !value;
        if (onChange != null) {
            onChange.accept(value);
        }
        return true;
    }
}
