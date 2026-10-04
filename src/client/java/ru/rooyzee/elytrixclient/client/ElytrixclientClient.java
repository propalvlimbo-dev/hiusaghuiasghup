package ru.rooyzee.elytrixclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.Elytrixclient;
import ru.rooyzee.elytrixclient.client.botmark.BotMarkRunner;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.soulfire.SoulFireController;
import ru.rooyzee.elytrixclient.client.ui.ElytrixScreen;
import ru.rooyzee.elytrixclient.client.ui.ElytrixTitleScreen;
import ru.rooyzee.elytrixclient.client.ui.WindowIcon;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

/**
 * Клиентский entrypoint: конфиг, общий лог, раннеры, горячая клавиша панели,
 * иконка окна и правки главного меню.
 *
 * Панель целиком построена на YACL (YetAnotherConfigLib) — 5 категорий-вкладок.
 */
public class ElytrixclientClient implements ClientModInitializer {
    public static final ElytrixConfig CONFIG = ElytrixConfig.load();
    public static final LogBuffer LOG = new LogBuffer();
    public static final BotMarkRunner BOTMARK = new BotMarkRunner(LOG);
    public static final SoulFireController SOULFIRE = new SoulFireController(LOG);

    /** Панель открывается правым Ctrl (см. README). */
    private static final int PANEL_KEY = GLFW.GLFW_KEY_RIGHT_CONTROL;

    private boolean panelKeyHeld = false;

    @Override
    public void onInitializeClient() {
        // Встроенный ресурспак мода: логотип «ELYTRIX CLIENT» в главном меню,
        // убранная надпись «JAVA EDITION» и свои сплэши.
        FabricLoader.getInstance().getModContainer(Elytrixclient.MOD_ID).ifPresent(container ->
                ResourceLoader.registerBuiltinPack(
                        Identifier.fromNamespaceAndPath(Elytrixclient.MOD_ID, Elytrixclient.MOD_ID),
                        container,
                        Component.literal("ElytrixClient"),
                        PackActivationType.DEFAULT_ENABLED));

        ElytrixTitleScreen.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WindowIcon.tick(client);

            // В 26.2 экранами управляет Minecraft.gui (не поле screen):
            // client.gui.screen() — текущий экран или null.
            boolean down = InputConstants.isKeyDown(client.getWindow(), PANEL_KEY);
            if (down && !panelKeyHeld && client.gui.screen() == null) {
                client.setScreenAndShow(ElytrixScreen.create(null));
            }
            panelKeyHeld = down;
        });

        LOG.add("[Elytrix] Мод загружен. Панель — правый Ctrl.");
    }
}
