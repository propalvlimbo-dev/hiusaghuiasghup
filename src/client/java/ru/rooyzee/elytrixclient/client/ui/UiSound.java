package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;

/**
 * Звуки меню. Свои наборы синтезированы скриптом {@code scripts/make-ui-sounds.py}
 * ({@code assets/elytrixclient/sounds/ui/<set>/<event>.ogg}, описаны в {@code sounds.json}).
 * Набор, громкость и включение — в настройках панели.
 */
public final class UiSound {
    private UiSound() {
    }

    public static final String[] SET_NAMES = {"Мягкий", "Стеклянный", "Ванильный"};
    private static final String[] SET_IDS = {"soft", "glass"};

    public enum Event {
        CLICK("click"), HOVER("hover"), ON("on"), OFF("off"), OPEN("open"), CLOSE("close");

        final String id;

        Event(String id) {
            this.id = id;
        }
    }

    private static final SoundEvent[][] EVENTS = new SoundEvent[SET_IDS.length][Event.values().length];
    private static long lastHover;

    static {
        for (int s = 0; s < SET_IDS.length; s++) {
            for (Event e : Event.values()) {
                EVENTS[s][e.ordinal()] = SoundEvent.createVariableRangeEvent(
                        Identifier.fromNamespaceAndPath("elytrixclient", "ui." + SET_IDS[s] + "." + e.id));
            }
        }
    }

    public static void play(Event event) {
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        if (cfg == null || !cfg.menuSounds || cfg.soundVolume <= 0) {
            return;
        }
        if (event == Event.HOVER) {
            if (!cfg.hoverSounds) {
                return;
            }
            long now = Util.getMillis();
            if (now - lastHover < 45L) {
                return;
            }
            lastHover = now;
        }
        float volume = cfg.soundVolume / 100f;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) {
            return;
        }
        try {
            int set = cfg.soundSet;
            if (set >= 0 && set < SET_IDS.length) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(EVENTS[set][event.ordinal()], 1.0f, volume));
                return;
            }
            // «Ванильный»: стандартный щелчок кнопки с разной высотой
            float pitch = switch (event) {
                case HOVER -> 0f;
                case ON, OPEN -> 1.15f;
                case OFF, CLOSE -> 0.85f;
                default -> 1.0f;
            };
            if (pitch > 0f) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch,
                        volume * 0.6f));
            }
        } catch (Throwable ignored) {
            // звук не должен ломать интерфейс
        }
    }
}
