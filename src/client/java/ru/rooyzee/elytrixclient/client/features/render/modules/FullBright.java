package ru.rooyzee.elytrixclient.client.features.render.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;

/**
 * FullBright — скрещенный модуль: гамма/динамический свет берём из xrose
 * (FullBrightFeature, их lightmap-миксин), ночное зрение — из delta.
 */
public final class FullBright extends VisualModule {

    /** 0 = Гамма (xrose), 1 = Ночное зрение (delta). */
    public int mode = 0;

    public FullBright() {
        super("FullBright", false);
    }

    @Override
    protected void onEnable() {
        sync();
    }

    @Override
    protected void onDisable() {
        org.xrose.feature.impl.player.FullBrightFeature fb = xrose();
        if (fb != null && fb.isEnabled()) {
            fb.setEnabled(false);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void tick(Minecraft mc) {
        sync();
        if (mode == 1 && mc != null && mc.player != null) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 1, false, false, false));
        }
    }

    private void sync() {
        org.xrose.feature.impl.player.FullBrightFeature fb = xrose();
        if (fb == null) {
            return;
        }
        boolean want = enabled() && mode == 0;
        if (fb.isEnabled() != want) {
            fb.setEnabled(want);
        }
    }

    private static org.xrose.feature.impl.player.FullBrightFeature xrose() {
        try {
            for (org.xrose.feature.Feature f
                    : org.xrose.feature.FeatureManager.INSTANCE.getFeatures(org.xrose.feature.FeatureCategory.PLAYER)) {
                if (f instanceof org.xrose.feature.impl.player.FullBrightFeature fb) {
                    return fb;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
