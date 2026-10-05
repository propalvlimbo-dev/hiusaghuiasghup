package ru.rooyzee.elytrixclient.client.features.misc;

import net.minecraft.client.Minecraft;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;

import java.util.Locale;

/**
 * AutoRegister — автоматическая регистрация на серверах: при входе шлёт
 * /register и /login с паролем, а также реагирует на подсказки чата
 * («register», «reg», «login», «логин», «пароль»).
 */
public final class AutoRegister extends VisualModule {

    public String password = "elytrix123";

    private int joinDelay = -1;
    private long lastAttempt = 0L;
    private Object lastLevel = null;

    public AutoRegister() {
        super("AutoRegister", false);
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            joinDelay = -1;
            lastLevel = null;
            return;
        }
        if (lastLevel != mc.level) {
            lastLevel = mc.level;
            joinDelay = 40; // ~2 сек после входа
        }
        if (joinDelay > 0) {
            joinDelay--;
        } else if (joinDelay == 0) {
            joinDelay = -1;
            attempt(mc);
        }
    }

    /** Уже виденные строки — чтобы не слать команды каждый кадр. */
    private final java.util.Set<String> seen = new java.util.HashSet<>();

    /**
     * Вызывается из NameProtectUtil.protect — через него проходят ВСЕ
     * отрисованные строки, включая строки чата (входящие сообщения сервера).
     */
    public static void onDrawnText(String text) {
        Minecraft mc = Minecraft.getInstance();
        AutoRegister self = instance();
        if (self == null || !self.enabled() || mc == null || mc.player == null
                || text == null || text.length() > 512) {
            return;
        }
        String low = text.toLowerCase(Locale.ROOT);
        if ((low.contains("register") || low.contains("/reg") || low.contains("login")
                || low.contains("логин") || low.contains("пароль")) && self.seen.add(low)) {
            self.attempt(mc);
        }
    }

    private void attempt(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < 5000L) {
            return;
        }
        lastAttempt = now;
        try {
            mc.player.connection.sendCommand("register " + password + " " + password);
            mc.player.connection.sendCommand("login " + password);
            mc.player.connection.sendCommand("l " + password);
        } catch (Throwable ignored) {
        }
    }

    private static AutoRegister instance() {
        for (ru.rooyzee.elytrixclient.client.features.render.VisualModule m
                : ru.rooyzee.elytrixclient.client.features.render.Visuals.all()) {
            if (m instanceof AutoRegister ar) {
                return ar;
            }
        }
        return null;
    }
}
