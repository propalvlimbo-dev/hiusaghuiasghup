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
        int bg = UiTheme.mix(UiTheme.ROW, UiTheme.ROW_HOVER, hoverT * 0.7f);
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, fadeIn(bg));

        var font = font();
        int textX = x + 10;
        if (icon != null) {
            icon.draw(graphics, textX, y + (h - 12) / 2, 12, fadeIn(UiTheme.TEXT_DIM));
            textX += 17;
        }
        UiDraw.text(graphics, font, key, textX, y + (h - 8) / 2 + 1, fadeIn(UiTheme.TEXT_DIM));

        String text = value == null ? "" : value.get();
        int vx = x + w - 10;
        if (dotColor != 0) {
            UiDraw.disc(graphics, vx - font.width(text) - 8f, y + h / 2f, 3f, fadeIn(dotColor));
        }
        UiDraw.textRight(graphics, font, trim(font, text, w - 60), vx, y + (h - 8) / 2 + 1, fadeIn(UiTheme.TEXT_SOFT));
        endFrame();
    }
}
