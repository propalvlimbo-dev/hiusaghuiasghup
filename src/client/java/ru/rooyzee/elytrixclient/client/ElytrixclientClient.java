package ru.rooyzee.elytrixclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.botmark.BotMarkRunner;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
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
    public static final ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine OWN_BOTS = new ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine(LOG);

    /** Панель открывается правым Ctrl (см. настройки интерфейса). */
    private static final int PANEL_KEY = GLFW.GLFW_KEY_RIGHT_CONTROL;

    private boolean panelKeyHeld;

    /** Замер MSPT (мс на тик) для инфо-панели: START/END клиентского тика. */
    private static long tickStartNano;
    public static volatile float mspt;

    @Override
    public void onInitializeClient() {
        // Инициализация перенесённого ядра delta-26.2 (рендер-модули, события, шейдеры).
        try {
            new platform.client.Delta();
        } catch (Throwable t) {
            System.err.println("[Elytrix] Delta init failed: " + t);
        }
        // Baritone в чате не светится: глушим его logger.
        try {
            Object settings = baritone.api.BaritoneAPI.getSettings();
            Object loggerSetting = settings.getClass().getField("logger").get(settings);
            loggerSetting.getClass().getField("value").set(loggerSetting,
                    (java.util.function.Consumer<String>) msg -> { });
        } catch (Throwable ignored) {
        }
        // Прокси из конфига — сразу.
        try {
            ru.rooyzee.elytrixclient.client.ui.NetworkScreen.applyFromConfig();
        } catch (Throwable ignored) {
        }
        // Тема и тумблер анимаций из конфига — до первого кадра.
        UiTheme.applyPreset(CONFIG.themeIndex);
        UiWidget.ANIMATIONS = CONFIG.animations;
        // Семейство шрифтов интерфейса (MTSDF-атласы xrose_1) — тоже до первого кадра.
        // Сами атласы читаются лениво: ResourceManager сейчас ещё не готов.
        Fonts.setFamily(CONFIG.fontFamily);

        // Создаём папку .minecraft/elytrix/ для музыки, прокси и т.д.
        try {
            java.nio.file.Files.createDirectories(
                    net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("elytrix"));
        } catch (Exception ignored) {
        }

        // MSPT: замеряем длительность клиентского тика (START → END), сглаживаем.
        // World-render и удар игрока шьются миксинами AttackHookMixin / WorldRenderHookMixin
        // (в fabric-api 26.2 нет WorldRenderEvents и AttackEntityEvents).
        ClientTickEvents.START_CLIENT_TICK.register(client -> tickStartNano = System.nanoTime());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long dt = System.nanoTime() - tickStartNano;
            mspt = mspt * 0.8f + (dt / 1_000_000f) * 0.2f;
            WindowIcon.tick(client);
            ru.rooyzee.elytrixclient.client.features.render.Visuals.tick(client);
            ru.rooyzee.elytrixclient.client.features.EmbeddedGate.tick(client);

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
