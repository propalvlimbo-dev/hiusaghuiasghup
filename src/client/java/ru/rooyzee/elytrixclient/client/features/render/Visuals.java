package ru.rooyzee.elytrixclient.client.features.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.rooyzee.elytrixclient.client.features.render.modules.ArmorHud;
import ru.rooyzee.elytrixclient.client.features.render.modules.FullBright;

import java.util.ArrayList;
import java.util.List;

/**
 * Реестр визуальных модулей («Визуалы»). Порт системы render-модулей delta-26.2:
 * здесь регистрируются все визуальные функции клиента, меню «Визуалы» строится
 * из {@link #all()}.
 */
public final class Visuals {
    private static final List<VisualModule> MODULES = new ArrayList<>();

    static {
        register(new FullBright());
        register(new ArmorHud());
    }

    private Visuals() {
    }

    public static void register(VisualModule module) {
        MODULES.add(module);
    }

    public static List<VisualModule> all() {
        return MODULES;
    }

    public static void tick(Minecraft mc) {
        for (VisualModule m : MODULES) {
            if (m.enabled()) {
                m.tick(mc);
            }
        }
    }

    public static void renderHud(GuiGraphicsExtractor g, int sw, int sh) {
        for (VisualModule m : MODULES) {
            if (m.enabled()) {
                m.renderHud(g, sw, sh);
            }
        }
    }

    public static void renderWorld(Object ctx) {
        for (VisualModule m : MODULES) {
            if (m.enabled()) {
                m.renderWorld(ctx);
            }
        }
    }
}
