package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * Свой заголовок главного меню ElytrixClient — «терминальное окно» вместо ванильного
 * логотипа Minecraft (см. {@code mixin.client.TitleScreenMixin}: вызов ванильного
 * {@code LogoRenderer} подменяется на этот рендер, сплэш не показывается вовсе).
 */
public final class ElytrixBrand {

    private ElytrixBrand() {
    }

    /** Рисуется там же, где ванильный логотип: по центру сверху. */
    public static void draw(GuiGraphicsExtractor g, int screenWidth, float alpha) {
        Font font = Minecraft.getInstance().font;
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        float t = ElytrixBackground.time();

        int w = 306;
        int h = 58;
        int x = screenWidth / 2 - w / 2;
        int y = 22;

        // «стекло» окна терминала
        UiDraw.shadow(g, x, y, w, h, 12, 5, UiTheme.withAlpha(0xFF000000, 0.35f * alpha));
        UiDraw.roundRectBordered(g, x, y, w, h, 12,
                UiTheme.withAlpha(0xFF0B0710, 0.80f * alpha),
                UiTheme.withAlpha(accent, 0.55f * alpha));

        // «светофор» окна
        UiDraw.disc(g, x + 15, y + 13, 3.2f, UiTheme.withAlpha(0xFFFF5C8A, alpha));
        UiDraw.disc(g, x + 27, y + 13, 3.2f, UiTheme.withAlpha(0xFFFFA34D, alpha));
        UiDraw.disc(g, x + 39, y + 13, 3.2f, UiTheme.withAlpha(0xFF34D399, alpha));
        UiDraw.hLine(g, x + 1, x + w - 1, y + 22, 1, UiTheme.withAlpha(accent, 0.28f * alpha));

        // имя клиента в разрядку
        UiDraw.textSpaced(g, font, "ELYTRIX", screenWidth / 2 - UiDraw.spacedWidth(font, "ELYTRIX", 6) / 2, y + 27, 6,
                UiTheme.withAlpha(0xFFFFFFFF, alpha), true);
        UiDraw.textCenter(g, font, "client " + ElytrixLoader.version(), screenWidth / 2, y + 41,
                UiTheme.withAlpha(accent, 0.9f * alpha));

        // «командная строка» с мигающим курсором
        String cmd = "elytrix:~$ ready";
        int cmdX = x + 12;
        int cmdY = y + 41;
        UiDraw.text(g, font, cmd, cmdX, cmdY, UiTheme.withAlpha(accent, 0.75f * alpha));
        if ((int) (t * 2f) % 2 == 0) {
            UiDraw.roundRect(g, cmdX + font.width(cmd) + 3, cmdY, 5, 9, 1, UiTheme.withAlpha(0xFFFFFFFF, 0.85f * alpha));
        }
    }

    /** Маленький бейдж в левом нижнем углу — вместо ванильной строки «Minecraft 26.2». */
    public static void drawVersionChip(GuiGraphicsExtractor g, int screenHeight, float alpha) {
        Font font = Minecraft.getInstance().font;
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        String text = "Elytrix Client v" + ElytrixLoader.version();
        int w = font.width(text) + 14;
        UiDraw.roundRect(g, 2, screenHeight - 16, w, 14, UiTheme.R_SM, UiTheme.withAlpha(0xFF0B0710, 0.85f * alpha));
        UiDraw.roundRect(g, 2, screenHeight - 16, 3, 14, UiTheme.R_SM, UiTheme.withAlpha(accent, alpha));
        UiDraw.text(g, font, text, 9, screenHeight - 12, UiTheme.withAlpha(UiTheme.TEXT_SOFT, alpha));
    }
}
