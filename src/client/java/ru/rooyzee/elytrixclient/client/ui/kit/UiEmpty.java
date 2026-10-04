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
        int cx = x + w / 2;
        int cy = y + 30;
        float a = Math.max(0.05f, appear);
        UiDraw.disc(graphics, cx, cy, 20f, UiTheme.withAlpha(UiTheme.accentSoft(accent, 0.30f), a));
        icon.drawCentered(graphics, cx, cy, 16,
                UiTheme.withAlpha(UiTheme.mix(accent, 0xFFFFFFFF, 0.30f), a));

        var font = font();
        UiDraw.textCenter(graphics, font, title, cx, cy + 26, fadeIn(UiTheme.TEXT_SOFT));
        if (subtitle != null) {
            UiDraw.textCenter(graphics, font, subtitle, cx, cy + 40, fadeIn(UiTheme.TEXT_DIM));
        }
    }
}
