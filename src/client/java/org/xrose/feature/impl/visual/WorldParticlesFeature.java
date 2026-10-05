package org.xrose.feature.impl.visual;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ColorSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.particles.ProceduralParticleRenderer;
import org.xrose.utils.render.particles.ProceduralParticleState;
import sdk.api.optimize.optimize;

@optimize
public final class WorldParticlesFeature extends Feature implements MinecraftContext {
   private static final double SPAWN_PI = 3.1415928936382223;
   private static final int WORLD_BATCH_SIZE = 10;
   public final ModeSetting shape = this.register(
      new ModeSetting("Shape", ProceduralParticleRenderer.Shape.RANDOM.displayName(), ProceduralParticleRenderer.Shape.displayNames())
         .configKey("render.particles.texture")
   );
   public final NumberSetting spawnRate = this.register(new NumberSetting("Spawn Rate", 1.0, 1.0, 5.0, 1.0, " ticks").configKey("render.particles.spawnrate"));
   public final NumberSetting radius = this.register(new NumberSetting("Spawn Radius", 40.0, 10.0, 40.0, 1.0, " blocks").configKey("render.particles.radius"));
   public final NumberSetting lifetime = this.register(new NumberSetting("Lifetime", 60.0, 10.0, 200.0, 1.0, " ticks").configKey("render.particles.lifetime"));
   public final NumberSetting size = this.register(new NumberSetting("Size", 0.15, 0.05, 0.5, 0.05, " blocks").configKey("render.particles.size"));
   public final NumberSetting maxParticles = this.register(new NumberSetting("Max Particles", 100.0, 10.0, 200.0, 1.0, "").configKey("render.particles.max"));
   public final ColorSetting color = this.register(new ColorSetting("Color", -1).configKey("render.particles.color"));
   public final BooleanSetting natural = this.register(new BooleanSetting("Themed Colors", false).configKey("render.particles.natural"));
   public final NumberSetting glow = this.register(new NumberSetting("Glow", 0.0, 0.0, 1.0, 0.05, "").configKey("render.particles.glow"));
   private final List<ProceduralParticleState> particles = new ArrayList<>();
   private final List<ProceduralParticleRenderer.Sprite> sprites = new ArrayList<>();
   private final ProceduralParticleRenderer renderer = new ProceduralParticleRenderer();
   private final Random random = new Random();
   private int tickCounter;

   public WorldParticlesFeature() {
      super("WorldParticles", "Spawns procedural particles around the player", FeatureCategory.VISUAL, -1);
   }

   public static WorldParticlesFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(WorldParticlesFeature.class);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (mc.level != null && mc.player != null) {
         this.tickCounter++;
         int interval = this.spawnRate.getValue().intValue();
         if (interval <= 1 || this.tickCounter % interval == 0) {
            this.spawnWorldBatch();
         }

         if (!this.particles.isEmpty()) {
            float configuredSize = this.size.getValue().floatValue();
            Iterator<ProceduralParticleState> iterator = this.particles.iterator();

            while (iterator.hasNext()) {
               if (!iterator.next().update(mc.level, configuredSize, 1.0F, this.random)) {
                  iterator.remove();
               }
            }
         }
      } else {
         this.clear();
      }
   }

   @EventTarget
   public void onWorldLeave(WorldLeaveEvent event) {
      this.clear();
   }

   @Override
   protected void onDisable() {
      this.clear();
      this.renderer.release();
   }

   private void clear() {
      this.particles.clear();
      this.sprites.clear();
      this.tickCounter = 0;
   }

   public void renderWorld(CameraRenderState cameraState, float tickDelta) {
      if (mc.player != null && mc.level != null && !this.particles.isEmpty()) {
         int baseColor = this.color.getValue();
         float configuredSize = this.size.getValue().floatValue();
         this.sprites.clear();

         for (ProceduralParticleState particle : this.particles) {
            this.sprites
               .add(
                  new ProceduralParticleRenderer.Sprite(
                     particle.interpolatedPosition(tickDelta),
                     configuredSize,
                     ColorUtil.multiplyAlpha(baseColor, particle.opacityEnvelope()),
                     particle.shape(),
                     particle.renderRotation(tickDelta),
                     particle.normalizedLife(),
                     particle.seed(),
                     particle.phase()
                  )
               );
         }

         this.renderer.render(this.sprites, this.glow.getValue().floatValue(), this.natural.getValue());
      }
   }

   private void spawnWorldBatch() {
      int maximum = this.maxParticles.getValue().intValue();
      if (this.particles.size() < maximum) {
         Vec3 center = mc.player.position();
         double configuredRadius = this.radius.getValue();
         int configuredLifetime = this.lifetime.getValue().intValue();

         for (int index = 0; index < 10 && this.particles.size() < maximum; index++) {
            double angle = this.random.nextDouble() * 3.1415928936382223 * 2.0;
            double distance = this.random.nextDouble() * configuredRadius;
            Vec3 position = center.add(Math.cos(angle) * distance, (this.random.nextDouble() - 0.5) * distance, Math.sin(angle) * distance);
            if (!ProceduralParticleState.isPositionBlocked(mc.level, position)) {
               Vec3 velocity = ProceduralParticleState.randomVelocity(this.random);
               this.particles
                  .add(
                     new ProceduralParticleState(
                        position.x, position.y, position.z, velocity.x, velocity.y, velocity.z, configuredLifetime, this.selectedShape(), this.random
                     )
                  );
            }
         }
      }
   }

   private ProceduralParticleRenderer.Shape selectedShape() {
      return ProceduralParticleRenderer.Shape.fromDisplayName(this.shape.getValue()).resolve(this.random);
   }
}

