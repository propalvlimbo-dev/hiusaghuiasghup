package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** «Пустое состояние» раздела: большая иконка в круге, заголовок и подпись. */
public class UiEmpty extends UiWidget {

    private final UiIcon icon;
    private final String title;
    private final String subtitle;

    public UiEmpty(UiIcon icon, String title, String subtitle) {
        super(92);
        this.icon = icon;
        this.title = title;
        this.subtitle = subtitle;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        tick(dt, false);
        UiDraw.roundRectBordered(graphics, x, y, w, h, UiTheme.R_LG,
                fadeIn(UiTheme.withAlpha(UiTheme.ROW, 0.55f)), fadeIn(UiTheme.BORDER_SOFT));
        int cx = x + w / 2;
        int cy = y + 30;
        UiDraw.disc(graphics, cx, cy, 17f, UiTheme.withAlpha(UiTheme.accentSoft(accent, 0.35f), Math.max(0.05f, appear)));
        UiDraw.ring(graphics, cx, cy, 17f, 1.4f, UiTheme.withAlpha(accent, 0.65f * Math.max(0.05f, appear)));
        icon.drawCentered(graphics, cx, cy, 17, UiTheme.withAlpha(UiTheme.mix(accent, 0xFFFFFFFF, 0.25f), Math.max(0.05f, appear)));

        var font = font();
        UiDraw.textCenter(graphics, font, title, cx, cy + 24, fadeIn(UiTheme.TEXT_SOFT));
        if (subtitle != null) {
            UiDraw.textCenter(graphics, font, subtitle, cx, cy + 36, fadeIn(UiTheme.TEXT_DIM));
        }
    }
}
