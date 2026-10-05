package ru.rooyzee.elytrixclient.client.features;

import net.minecraft.client.Minecraft;

import java.util.Set;

/**
 * Ворота для встроенных ядер xrose/delta: они встроены целиком (иначе
 * whitelisted-функции не работают), но всё, чего нет в списке пользователя,
 * принудительно выключается через ~5 секунд после запуска — включая то, что
 * могло включиться из их конфигов.
 */
public final class EmbeddedGate {

    private static final Set<String> XROSE_ALLOWED = Set.of(
            "BlockOutline", "Removals", "Chams", "AtmoDawnFog", "NameProtect");

    private static int ticks = -1;
    private static boolean done = false;

    private EmbeddedGate() {
    }

    public static void tick(Minecraft mc) {
        updateHooks();
        if (done) {
            return;
        }
        if (ticks < 0) {
            ticks = 0;
            return;
        }
        if (++ticks < 100) {
            return;
        }
        done = true;
        disableDelta();
        disableXrose();
    }

    /** Дешёвый флаг: нужны ли построчные хуки текста (иначе protect() — мгновенный return). */
    private static void updateHooks() {
        try {
            boolean np = false;
            for (org.xrose.feature.Feature f : org.xrose.feature.FeatureManager.INSTANCE.getFeatures(org.xrose.feature.FeatureCategory.MISC)) {
                if (f instanceof org.xrose.feature.impl.misc.NameProtectFeature n && n.isEnabled()) {
                    np = true;
                    break;
                }
            }
            boolean sm = false;
            platform.client.Delta delta = platform.client.Delta.h();
            if (delta != null && delta.d() != null && delta.d().t() != null) {
                for (platform.api.module.Module mod : delta.d().t().d()) {
                    if (mod != null && "Streamer Mode".equals(mod.j())) {
                        sm = mod.m();
                        break;
                    }
                }
            }
            boolean ar = false;
            for (ru.rooyzee.elytrixclient.client.features.render.VisualModule vm
                    : ru.rooyzee.elytrixclient.client.features.render.Visuals.all()) {
                if (vm instanceof ru.rooyzee.elytrixclient.client.features.misc.AutoRegister a && a.enabled()) {
                    ar = true;
                    break;
                }
            }
            org.xrose.utils.text.NameProtectUtil.hooksActive = np || sm || ar;
        } catch (Throwable ignored) {
        }
    }

    private static void disableDelta() {
        try {
            platform.client.Delta delta = platform.client.Delta.h();
            if (delta == null || delta.d() == null || delta.d().t() == null) {
                return;
            }
            for (platform.api.module.Module mod : delta.d().t().d()) {
                if (mod == null || !mod.m()) {
                    continue;
                }
                boolean allowed =
                        (mod.l() == platform.api.module.Category.Render
                                && ru.rooyzee.elytrixclient.client.ui.menu.MenuContent.VISUAL_DELTA.contains(mod.j()))
                        || (mod.l() == platform.api.module.Category.Misc
                                && ru.rooyzee.elytrixclient.client.ui.menu.MenuContent.MISC_DELTA.contains(mod.j()));
                if (!allowed) {
                    mod.a(false);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void disableXrose() {
        try {
            for (org.xrose.feature.FeatureCategory cat : org.xrose.feature.FeatureCategory.values()) {
                for (org.xrose.feature.Feature f : org.xrose.feature.FeatureManager.INSTANCE.getFeatures(cat)) {
                    if (f == null || !f.isEnabled()) {
                        continue;
                    }
                    if (f instanceof org.xrose.feature.impl.player.FullBrightFeature) {
                        // Разрешена только пока включён наш FullBright в режиме «Гамма».
                        if (!fullBrightGammaWanted()) {
                            f.setEnabled(false);
                        }
                    } else if (!XROSE_ALLOWED.contains(f.getName())) {
                        f.setEnabled(false);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean fullBrightGammaWanted() {
        for (ru.rooyzee.elytrixclient.client.features.render.VisualModule vm
                : ru.rooyzee.elytrixclient.client.features.render.Visuals.all()) {
            if (vm instanceof ru.rooyzee.elytrixclient.client.features.render.modules.FullBright fb) {
                return fb.enabled() && fb.mode == 0;
            }
        }
        return false;
    }
}
