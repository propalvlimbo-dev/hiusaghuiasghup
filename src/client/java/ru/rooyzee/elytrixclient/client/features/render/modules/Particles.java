package ru.rooyzee.elytrixclient.client.features.render.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;

import java.util.Random;

/**
 * WorldParticles — перенос xroses WorldParticlesFeature: партиклы в воздухе
 * вокруг игрока (радиус/частота/лимит настраиваются). Отрисовка — ванильными
 * партиклами, без собственного GL-пайплайна.
 */
public final class Particles extends VisualModule {

    /** Типы партиклов (аналог xroses «Shape»). 0 = случайно. */
    public static final String[] SHAPES = {"Случайно", "Искры", "Криты", "Зачарование", "Дым", "Снег"};

    public int shape = 0;
    /** Интервал спавна, тиков. */
    public int spawnRate = 1;
    /** Радиус спавна, блоков. */
    public int radius = 4;
    /** Партиклов за батч. */
    public int amount = 3;
    /** Лимит партиклов в секунду. */
    public int maxParticles = 150;

    private final Random random = new Random();
    private int tickCounter;
    private int spawnedThisSecond;
    private long secondMark;

    public Particles() {
        super("WorldParticles", false);
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - secondMark > 1000L) {
            secondMark = now;
            spawnedThisSecond = 0;
        }
        tickCounter++;
        if (spawnRate > 1 && tickCounter % spawnRate != 0) {
            return;
        }
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        for (int i = 0; i < amount && spawnedThisSecond < maxParticles; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * radius;
            double x = px + Math.cos(angle) * dist;
            double z = pz + Math.sin(angle) * dist;
            double y = py + (random.nextDouble() - 0.5) * dist;
            spawn(mc, x, y, z);
            spawnedThisSecond++;
        }
    }

    protected void spawn(Minecraft mc, double x, double y, double z) {
        int s = shape;
        if (s <= 0 || s >= SHAPES.length) {
            s = 1 + random.nextInt(SHAPES.length - 1);
        }
        switch (s) {
            case 2 -> mc.level.addParticle(ParticleTypes.CRIT, x, y, z, 0, 0.05, 0);
            case 3 -> mc.level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, -0.05, 0);
            case 4 -> mc.level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.03, 0);
            case 5 -> mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.03, 0);
            default -> mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.06, 0);
        }
    }
}
