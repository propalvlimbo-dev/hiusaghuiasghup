package ru.rooyzee.elytrixclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.botmark.BotMarkRunner;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.soulfire.SoulFireController;
import ru.rooyzee.elytrixclient.client.ui.ElytrixScreen;
import ru.rooyzee.elytrixclient.client.ui.MusicIsland;
import ru.rooyzee.elytrixclient.client.ui.WindowIcon;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

/**
 * Клиентский entrypoint: конфиг, общий лог, раннеры, горячая клавиша панели и иконка окна.
 *
 * <p>Фон главного меню и экран загрузки рисует миксин-слой ({@code mixin.client}):
 * панорама заменяется на анимированный фон Elytrix, ванильный красный лоадер — на свой.
 * Панель ({@link ElytrixScreen}) целиком рисуется своим кодом, см. пакет {@code ui.kit}.
 */
public class ElytrixclientClient implements ClientModInitializer {
    public static final ElytrixConfig CONFIG = ElytrixConfig.load();
    public static final LogBuffer LOG = new LogBuffer();
    public static final BotMarkRunner BOTMARK = new BotMarkRunner(LOG);
    public static final SoulFireController SOULFIRE = new SoulFireController(LOG);

    /** Панель открывается правым Ctrl (см. настройки интерфейса). */
    private static final int PANEL_KEY = GLFW.GLFW_KEY_RIGHT_CONTROL;

    private boolean panelKeyHeld;

    @Override
    public void onInitializeClient() {
        // Тема и тумблер анимаций из конфига — до первого кадра.
        UiTheme.applyPreset(CONFIG.themeIndex);
        UiWidget.ANIMATIONS = CONFIG.animations;

        // Создаём папку .minecraft/elytrix/ для музыки, прокси и т.д.
        try {
            java.nio.file.Files.createDirectories(
                    net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("elytrix"));
        } catch (Exception ignored) {
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WindowIcon.tick(client);
            ru.rooyzee.elytrixclient.client.music.CustomMusic.tick(client);

            if (!CONFIG.panelKey) {
                panelKeyHeld = false;
                return;
            }

            // В 26.2 экранами управляет Minecraft.gui (не поле screen):
            // client.gui.screen() — текущий экран или null.
            Screen current = client.gui.screen();
            boolean down = InputConstants.isKeyDown(client.getWindow(), PANEL_KEY);
            if (down && !panelKeyHeld && (current == null || current instanceof TitleScreen)) {
                client.gui.setScreen(ElytrixScreen.create(current));
            }
            panelKeyHeld = down;
        });

        LOG.add("[Elytrix] Мод загружен. Панель — правый Ctrl.");
    }
}
