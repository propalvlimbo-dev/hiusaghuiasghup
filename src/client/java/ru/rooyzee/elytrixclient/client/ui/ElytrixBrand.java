package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * Свой заголовок главного меню ElytrixClient — вместо ванильного логотипа Minecraft
 * (см. {@code mixin.client.TitleScreenMixin}: вызов ванильного {@code LogoRenderer}
 * подменяется на этот рендер, сплэш не показывается вовсе).
 *
 * <p>Стиль — «Apple + терминал»: знак клиента, название в разрядку своим шрифтом
 * и одна строка подписи моноширинным. Никаких «окон терминала», бейджей версии
 * и мигающих курсоров — минимум элементов.
 */
public final class ElytrixBrand {

    /** Размер, под который сгенерированы спрайты знака (logo_88_xN). */
    private static final int LOGO_SIZE = 88;

    private ElytrixBrand() {
    }

    /** Рисуется там же, где ванильный логотип: по центру сверху. */
    public static void draw(GuiGraphicsExtractor g, int screenWidth, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Font font = mc.font;
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        int cx = screenWidth / 2;

        int mark = 56;
        int markX = cx - mark / 2;
        int markY = 26;

        // знак с ореолом
        UiDraw.glow(g, markX, markY, mark, mark, 14, accent, 0.9f * alpha);
        int k = UiDraw.shapeScale();
        UiDraw.icon(g, UiDraw.shapeTexture("logo_" + LOGO_SIZE), markX, markY, mark, mark,
                LOGO_SIZE * k, LOGO_SIZE * k, UiTheme.withAlpha(0xFFFFFFFF, alpha));

        // название в разрядку своим шрифтом
        String name = "ELYTRIX";
        int spacing = 6;
        int nameW = UiDraw.spacedWidth(font, name, spacing, UiText.TITLE);
        UiDraw.textSpaced(g, font, name, cx - nameW / 2, markY + mark + 14, spacing,
                UiTheme.withAlpha(0xFFFFFFFF, alpha), false, UiText.TITLE);

        // тонкая линия и подпись
        int lineY = markY + mark + 40;
        int lineW = Math.min(160, screenWidth - 60);
        UiDraw.hLine(g, cx - lineW / 2, cx + lineW / 2, lineY, 1, UiTheme.withAlpha(UiTheme.DIVIDER, alpha));
        UiDraw.textCenter(g, font, "minecraft " + version() + " · fabric", cx, lineY + 8,
                UiTheme.withAlpha(accent, 0.9f * alpha), UiText.MONO);
    }

    private static String version() {
        return "26.2";
    }
}
