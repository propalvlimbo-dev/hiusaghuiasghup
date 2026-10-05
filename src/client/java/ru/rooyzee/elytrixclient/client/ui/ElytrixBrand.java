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
 * <p>Стиль — минималистичный знак клиента без текстовых подписей, бейджей версии
 * и декоративных терминальных элементов.
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

        // только знак клиента: без названия, версии и подписей, без «ореола»-плашки
        int mark = ElytrixMenuButtons.LOGO;
        int markX = ElytrixMenuButtons.left();
        // ближе к кнопкам: кнопки меню начинаются с height/4 + 48
        // над столбцом кнопок слева (см. ElytrixMenuButtons.layout)
        int markY = Math.max(8, ElytrixMenuButtons.logoY());
        int k = UiDraw.shapeScale();
        UiDraw.icon(g, UiDraw.shapeTexture("logo_" + LOGO_SIZE), markX, markY, mark, mark,
                LOGO_SIZE * k, LOGO_SIZE * k, UiTheme.withAlpha(0xFFFFFFFF, alpha));
    }

    private static String version() {
        return "26.2";
    }
}
