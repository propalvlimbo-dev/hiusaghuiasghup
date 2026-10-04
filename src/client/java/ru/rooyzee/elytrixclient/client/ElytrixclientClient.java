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
import ru.rooyzee.elytrixclient.client.ui.ElytrixLoadingScreen;
import ru.rooyzee.elytrixclient.client.ui.ElytrixScreen;
import ru.rooyzee.elytrixclient.client.ui.WindowIcon;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

/**
 * Клиентский entrypoint: конфиг, общий лог, раннеры, горячая клавиша панели,
 * иконка окна и свой экран загрузки.
 *
 * <p>Главное меню остаётся ванильным — мы в него ничего не добавляем.
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
    private boolean loadingShown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WindowIcon.tick(client);

            // Свой экран загрузки показываем один раз — вместо первого показа главного меню.
            if (!loadingShown && client.gui.screen() instanceof TitleScreen) {
                loadingShown = true;
                client.gui.setScreen(new ElytrixLoadingScreen());
                return;
            }

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
