package ru.rooyzee.elytrixclient.client.features.misc;

import net.minecraft.client.Minecraft;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;

import java.lang.reflect.Method;

/**
 * Оптимизации: облака/взгляд покачивание/лимит FPS в меню.
 * Ванильные опции трогаем через reflection — если имя в 26.2 другое,
 * тумблер просто молча не сработает, клиент не упадёт.
 */
public final class Optimizations extends VisualModule {

    public boolean cloudsOff = true;
    public boolean bobOff = true;
    public boolean fpsMenu = true;

    private Object prevClouds;
    private Object prevBob;
    private Integer prevFps;
    private boolean fpsApplied;

    public Optimizations() {
        super("Optimizations", false);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        if (cloudsOff) {
            prevClouds = get(mc, "cloudStatus");
            setEnum(mc, "cloudStatus", "OFF");
        }
        if (bobOff) {
            prevBob = get(mc, "viewBobbing");
            set(mc, "viewBobbing", Boolean.FALSE);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        if (prevClouds != null) {
            setRaw(mc, "cloudStatus", prevClouds);
            prevClouds = null;
        }
        if (prevBob != null) {
            setRaw(mc, "viewBobbing", prevBob);
            prevBob = null;
        }
        if (fpsApplied && prevFps != null) {
            set(mc, "framerateLimit", prevFps);
            fpsApplied = false;
        }
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc == null || mc.options == null || !fpsMenu) {
            return;
        }
        boolean menuOpen = mc.gui != null && mc.gui.screen() instanceof ru.rooyzee.elytrixclient.client.ui.ElytrixScreen;
        if (menuOpen && !fpsApplied) {
            prevFps = (Integer) get(mc, "framerateLimit");
            set(mc, "framerateLimit", 60);
            fpsApplied = true;
        } else if (!menuOpen && fpsApplied) {
            if (prevFps != null) {
                set(mc, "framerateLimit", prevFps);
            }
            fpsApplied = false;
        }
    }

    private static Method optionMethod(Minecraft mc, String name) {
        try {
            return mc.options.getClass().getMethod(name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object get(Minecraft mc, String name) {
        try {
            Method m = optionMethod(mc, name);
            if (m == null) {
                return null;
            }
            Object option = m.invoke(mc.options);
            return option.getClass().getMethod("get").invoke(option);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void setRaw(Minecraft mc, String name, Object value) {
        try {
            Method m = optionMethod(mc, name);
            if (m == null) {
                return;
            }
            Object option = m.invoke(mc.options);
            option.getClass().getMethod("set", Object.class).invoke(option, value);
        } catch (Throwable ignored) {
        }
    }

    private static void set(Minecraft mc, String name, Object value) {
        setRaw(mc, name, value);
    }

    private static void setEnum(Minecraft mc, String optionName, String constant) {
        try {
            Method m = optionMethod(mc, optionName);
            if (m == null) {
                return;
            }
            Object option = m.invoke(mc.options);
            Class<?> type = m.getReturnType().getMethod("get").getReturnType();
            for (Object c : type.getEnumConstants()) {
                if (((Enum<?>) c).name().equals(constant)) {
                    setRaw(mc, optionName, c);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
