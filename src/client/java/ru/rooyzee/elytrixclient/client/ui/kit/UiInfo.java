package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Supplier;

/** Строка «ключ → значение» (значение берётся лямбдой, можно показывать живые данные). */
public class UiInfo extends UiWidget {

    private final String key;
    private final Supplier<String> value;
    private final int dotColor;

    public UiInfo(String key, Supplier<String> value, int dotColor) {
        super(UiTheme.ROW_H);
        this.key = key;
        this.value = value;
        this.dotColor = dotColor;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, UiTheme.ROW);
        var font = font();
        UiDraw.text(graphics, font, key, x + 10, y + (h - 8) / 2 + 1, UiTheme.TEXT_DIM);

        String text = value == null ? "" : value.get();
        int vx = x + w - 10;
        if (dotColor != 0) {
            UiDraw.disc(graphics, vx - font.width(text) - 7f, y + h / 2f, 3f, dotColor);
        }
        UiDraw.textRight(graphics, font, text, vx, y + (h - 8) / 2 + 1, UiTheme.TEXT_SOFT);
    }
}
