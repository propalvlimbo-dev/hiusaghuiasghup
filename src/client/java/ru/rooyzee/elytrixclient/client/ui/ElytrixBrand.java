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

    /** Масштаб логотипа: 1.5x от базового размера кнопок. */
    private static final float SCALE = 1.5f;

    private ElytrixBrand() {
    }

    /** Логотип убран — не вписывается. */
    public static void draw(GuiGraphicsExtractor g, int screenWidth, float alpha) {
        // не рисуем логотип E
    }

    private static String version() {
        return "26.2";
    }
}
