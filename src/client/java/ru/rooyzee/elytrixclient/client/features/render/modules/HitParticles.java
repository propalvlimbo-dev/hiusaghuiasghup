package ru.rooyzee.elytrixclient.client.features.render.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;

import java.util.Random;

/**
 * HitParticles — перенос xroses HitParticlesFeature: всплеск партиклов в месте
 * удара по цели. Вызывается из {@code Visuals.onAttack} (fabric AttackEntityEvents).
 */
public final class HitParticles extends VisualModule {

    public int shape = 2;
    /** Партиклов за удар. */
    public int amount = 15;

    private final Random random = new Random();

    public HitParticles() {
        super("HitParticles", false);
    }

    public void onAttack(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || !(target instanceof LivingEntity le)) {
            return;
        }
        Vec3 base = le.position();
        for (int i = 0; i < amount; i++) {
            double x = base.x + (random.nextDouble() - 0.5) * le.getBbWidth();
            double y = base.y + random.nextDouble() * le.getBbHeight();
            double z = base.z + (random.nextDouble() - 0.5) * le.getBbWidth();
            double vx = (random.nextDouble() - 0.5) * 0.2;
            double vy = random.nextDouble() * 0.15;
            double vz = (random.nextDouble() - 0.5) * 0.2;
            spawn(mc, x, y, z, vx, vy, vz);
        }
    }

    private void spawn(Minecraft mc, double x, double y, double z, double vx, double vy, double vz) {
        int s = shape;
        if (s <= 0 || s >= Particles.SHAPES.length) {
            s = 1 + random.nextInt(Particles.SHAPES.length - 1);
        }
        switch (s) {
            case 2 -> mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.CRIT, x, y, z, vx, vy, vz);
            case 3 -> mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, x, y, z, vx, vy, vz);
            case 4 -> mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, vx, vy, vz);
            case 5 -> mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, x, y, z, vx, vy, vz);
            default -> mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, x, y, z, vx, vy, vz);
        }
    }
}
