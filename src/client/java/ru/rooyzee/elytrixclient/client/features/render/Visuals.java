package ru.rooyzee.elytrixclient.client.features.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import ru.rooyzee.elytrixclient.client.features.render.modules.FullBright;
import ru.rooyzee.elytrixclient.client.features.render.modules.HitParticles;
import ru.rooyzee.elytrixclient.client.features.render.modules.Particles;

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
        register(new ru.rooyzee.elytrixclient.client.features.misc.AutoRegister());
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

    /** Удар игрока по цели — для HitParticles (fabric AttackEntityEvents). */
    public static void onAttack(Entity target) {
        for (VisualModule m : MODULES) {
            if (m.enabled() && m instanceof HitParticles hp) {
                hp.onAttack(target);
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
