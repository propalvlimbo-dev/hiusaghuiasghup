package ru.rooyzee.elytrixclient.client.ui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Небольшие правки ванильного главного меню («чуть-чуть»):
 *  • кнопка «Elytrix Client» в левом верхнем углу (у TitleScreen там пусто) — открывает панель;
 *  • подсказка о горячей клавише рядом с кнопкой (ванильный логотип при этом не задет).
 *
 * Логотип («ELYTRIX CLIENT» вместо MINECRAFT), отсутствие «JAVA EDITION» и свои сплэши
 * приходят из встроенного ресурспака resourcepacks/elytrixclient (см. ElytrixclientClient).
 */
public final class ElytrixTitleScreen {
    private static final int MUTED = 0xFFA9A9BE;

    private static final int BUTTON_X = 6;
    private static final int BUTTON_Y = 6;
    private static final int BUTTON_W = 118;
    private static final int BUTTON_H = 20;

    private ElytrixTitleScreen() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) {
                return;
            }

            Screens.getWidgets(screen).add(
                    Button.builder(Component.literal("Elytrix Client"),
                                    button -> client.setScreenAndShow(ElytrixScreen.create(screen)))
                            .bounds(BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H)
                            .tooltip(Tooltip.create(Component.literal("Панель теста нагрузки (правый Ctrl)")))
                            .build());

            ScreenEvents.afterExtract(screen).register((ignored, graphics, mouseX, mouseY, tickProgress) -> {
                var font = client.font;
                int x = BUTTON_X + BUTTON_W + 6;
                graphics.text(font, "v" + version(), x, BUTTON_Y + 2, MUTED);
                graphics.text(font, "правый Ctrl — панель", x, BUTTON_Y + 11, MUTED);
            });
        });
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("elytrixclient")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }
}
