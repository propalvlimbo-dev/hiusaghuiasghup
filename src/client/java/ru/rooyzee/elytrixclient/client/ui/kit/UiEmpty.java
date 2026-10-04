package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** «Пустое состояние» раздела: большая иконка, заголовок и подпись. */
public class UiEmpty extends UiWidget {

    private final UiDraw.Icon icon;
    private final String title;
    private final String subtitle;

    public UiEmpty(UiDraw.Icon icon, String title, String subtitle) {
        super(86);
        this.icon = icon;
        this.title = title;
        this.subtitle = subtitle;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_LG, UiTheme.withAlpha(0xFFFFFFFF, 0.025f));
        int cx = x + w / 2;
        int cy = y + 26;
        UiDraw.disc(graphics, cx, cy, 15f, UiTheme.accentSoft(accent, 0.22f));
        UiDraw.icon(graphics, icon, cx - 8, cy - 8, 16, UiTheme.mix(accent, 0xFFFFFFFF, 0.35f));

        var font = font();
        UiDraw.textCenter(graphics, font, title, cx, cy + 22, UiTheme.TEXT_SOFT);
        if (subtitle != null) {
            UiDraw.textCenter(graphics, font, subtitle, cx, cy + 34, UiTheme.TEXT_DIM);
        }
    }
}
