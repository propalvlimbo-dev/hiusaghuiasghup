package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Supplier;

/** Строка «ключ → значение» (значение берётся лямбдой, можно показывать живые данные). */
public class UiInfo extends UiWidget {

    private final String key;
    private final Supplier<String> value;
    private final int dotColor;
    private UiIcon icon;

    public UiInfo(String key, Supplier<String> value, int dotColor) {
        super(UiTheme.ROW_H);
        this.key = key;
        this.value = value;
        this.dotColor = dotColor;
    }

    public UiInfo icon(UiIcon i) {
        this.icon = i;
        return this;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        beginFrame(mouseX, mouseY, dt);
        if (hoverT > 0.01f) {
            UiDraw.roundRect(graphics, x - 2, y + 1, w + 4, h - 2, UiTheme.R_SM,
                    fadeIn(UiTheme.withAlpha(UiTheme.ROW_HOVER, 0.55f * hoverT)));
        }

        var font = font();
        int textX = x + 4;
        if (icon != null) {
            icon.draw(graphics, textX, y + (h - 16) / 2, 16, fadeIn(UiTheme.TEXT_DIM));
            textX += 22;
        }
        UiDraw.text(graphics, font, key, textX, y + (h - 8) / 2 + 1, fadeIn(UiTheme.TEXT_SOFT));

        String text = value == null ? "" : value.get();
        String shown = trim(font, text, Math.max(40, w - 110));
        int vx = x + w - 4;
        if (dotColor != 0) {
            UiDraw.disc(graphics, vx - UiDraw.width(font, shown, UiText.MONO) - 10f, y + h / 2f, 3f,
                    fadeIn(dotColor));
        }
        UiDraw.textRight(graphics, font, shown, vx, y + (h - 8) / 2 + 1, fadeIn(UiTheme.TEXT), UiText.MONO);
        endFrame();
    }
}
