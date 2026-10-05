package ru.rooyzee.elytrixclient.client.features.render.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;

/**
 * Particles (world-визуал в духе delta-26.2): пока включён, вокруг игрока в мире
 * каждую тик-итерацию поднимаются партиклы — аура. Меняет именно world-рендер,
 * а не HUD.
 */
public final class Particles extends VisualModule {

    public Particles() {
        super("Particles", false);
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        for (int i = 0; i < 3; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 0.5 + Math.random() * 0.5;
            double x = px + Math.cos(a) * r;
            double z = pz + Math.sin(a) * r;
            double y = py + Math.random() * 1.6;
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.06, 0);
        }
    }
}
