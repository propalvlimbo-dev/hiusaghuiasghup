package org.xrose.feature.impl.visual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.ColorSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.mixin.accessor.ModelPartAccessor;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.particles.ProceduralParticleRenderer;
import sdk.api.optimize.optimize;

@optimize
public final class KillEffectFeature extends Feature implements MinecraftContext {
   private static final int HARD_PARTICLE_CAP = 450;
   private static final long ATTACK_TIMEOUT_MS = 5000L;
   private static final long MIN_HIDE_MS = 1250L;
   public final NumberSetting amount = this.register(new NumberSetting("Amount", 90.0, 20.0, 1000.0, 10.0, "").configKey("visual.killeffect.amount"));
   public final NumberSetting holdTime = this.register(new NumberSetting("Hold Time", 0.8, 0.2, 3.0, 0.1, " s").configKey("visual.killeffect.hold"));
   public final NumberSetting fadeTime = this.register(new NumberSetting("Fade Time", 1.0, 0.4, 2.5, 0.1, " s").configKey("visual.killeffect.fade"));
   public final NumberSetting scatterForce = this.register(new NumberSetting("Scatter Force", 1.0, 0.2, 3.0, 0.05, "").configKey("visual.killeffect.scatter"));
   public final NumberSetting gravity = this.register(new NumberSetting("Gravity", 1.2, 0.0, 4.0, 0.1, "").configKey("visual.killeffect.gravity"));
   public final NumberSetting size = this.register(new NumberSetting("Size", 0.09, 0.03, 0.25, 0.01, " blocks").configKey("visual.killeffect.size"));
   public final ColorSetting color = this.register(new ColorSetting("Color", -9635408).configKey("visual.killeffect.color"));
   public final NumberSetting glow = this.register(new NumberSetting("Glow", 0.75, 0.0, 1.0, 0.05, "").configKey("visual.killeffect.glow"));
   private final List<KillEffectFeature.Effect> effects = new ArrayList<>();
   private final Map<Integer, KillEffectFeature.PendingMark> pendingKills = new HashMap<>();
   private final List<ProceduralParticleRenderer.Sprite> sprites = new ArrayList<>();
   private final ProceduralParticleRenderer renderer = new ProceduralParticleRenderer();
   private final Random random = new Random();

   public KillEffectFeature() {
      super("KillEffect", "Rebuilds killed entities from glow spheres that then scatter", FeatureCategory.VISUAL, -1);
   }

   public static KillEffectFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(KillEffectFeature.class);
   }

   public void onAttack(Entity target) {
      if (target instanceof LivingEntity living && living != mc.player && !living.isDeadOrDying()) {
         long now = System.currentTimeMillis();
         this.pendingKills.values().removeIf(mark -> now - mark.atMs() > 5000L);
         this.pendingKills.put(living.getId(), new KillEffectFeature.PendingMark(now, living));
      }
   }

   public boolean hidesEntity(Entity entity) {
      long now = System.currentTimeMillis();
      return this.effects.stream().anyMatch(e -> e.entityId() == entity.getId() && now < e.hideUntilMs());
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (mc.level == null) {
         this.clear();
      } else {
         long now = System.currentTimeMillis();
         this.pendingKills.entrySet().removeIf(entry -> {
            KillEffectFeature.PendingMark mark = entry.getValue();
            if (now - mark.atMs() > 5000L) {
               return true;
            } else {
               Entity entity = mc.level.getEntity(entry.getKey());
               if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
                  this.spawnEffect(living);
                  return true;
               } else if (entity == null) {
                  if (now - mark.atMs() <= 2000L) {
                     this.spawnEffect(mark.entity());
                  }

                  return true;
               } else {
                  return false;
               }
            }
         });
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

   public void renderWorld() {
      if (mc.player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         this.effects.removeIf(effectx -> now > effectx.endMs() + 60L);
         this.sprites.clear();
         float halfSize = this.size.getValue().floatValue();
         float gravity = this.gravity.getValue().floatValue();
         int baseColor = this.color.getValue();

         for (KillEffectFeature.Effect effect : this.effects) {
            float t = (float)(now - effect.startMs()) / 1000.0F;
            boolean scattering = t >= effect.holdSec();
            float ts = scattering ? t - (float)effect.holdSec() : 0.0F;
            float k = scattering ? Math.min(1.0F, ts / (float)effect.fadeSec()) : 0.0F;
            float ramp = Math.min(1.0F, t / 0.22F);
            float appear = 0.3F + 0.7F * ramp * ramp * (3.0F - 2.0F * ramp);
            float env = scattering ? (float)Math.pow(1.0F - k, 1.5) : 1.0F;
            float sizeMul = scattering ? 1.0F - 0.55F * k : 0.35F + 0.65F * appear;
            float[] px = effect.pointsX();
            float[] py = effect.pointsY();
            float[] pz = effect.pointsZ();
            float[] vx = effect.velX();
            float[] vy = effect.velY();
            float[] vz = effect.velZ();
            float[] seeds = effect.seeds();
            float[] swirls = effect.swirls();

            for (int i = 0; i < effect.count(); i++) {
               float x = px[i];
               float y = py[i];
               float z = pz[i];
               if (scattering) {
                  float angle = swirls[i] * ts;
                  float cos = (float)Math.cos(angle);
                  float sin = (float)Math.sin(angle);
                  x += (vx[i] * cos - vz[i] * sin) * ts;
                  y += vy[i] * ts - 0.5F * gravity * ts * ts;
                  z += (vx[i] * sin + vz[i] * cos) * ts;
               }

               float shimmer = 0.92F + 0.08F * (float)Math.sin(now * 0.006 + seeds[i] * 17.0);
               float alpha = env * appear * shimmer;
               if (!(alpha <= 0.02F)) {
                  this.sprites
                     .add(
                        new ProceduralParticleRenderer.Sprite(
                           new Vec3(effect.originX() + x, effect.originY() + y, effect.originZ() + z),
                           halfSize * sizeMul,
                           ColorUtil.multiplyAlpha(baseColor, Math.min(1.0F, alpha)),
                           ProceduralParticleRenderer.Shape.GLOW,
                           seeds[i] * 6.2831F + ts * 2.2F,
                           k,
                           seeds[i],
                           seeds[i] * 13.7F % 1.0F
                        )
                     );
               }
            }
         }

         if (!this.sprites.isEmpty()) {
            this.renderer.render(this.sprites, this.glow.getValue().floatValue(), false);
         }
      }
   }

   private void spawnEffect(LivingEntity entity) {
      int count = Math.min(this.amount.getValue().intValue(), Math.max(0, 450 - this.activeParticleCount()));
      if (count > 0) {
         Vec3 origin = entity.position();
         long now = System.currentTimeMillis();
         float[] offsets = this.sampleModelOffsets(entity, count);
         float width = entity.getBbWidth();
         float height = entity.getBbHeight();
         float[] pointsX = new float[count];
         float[] pointsY = new float[count];
         float[] pointsZ = new float[count];
         float[] velX = new float[count];
         float[] velY = new float[count];
         float[] velZ = new float[count];
         float[] seeds = new float[count];
         float[] swirls = new float[count];

         for (int index = 0; index < count; index++) {
            double dx;
            double dy;
            double dz;
            if (offsets != null) {
               dx = offsets[index * 3];
               dy = offsets[index * 3 + 1];
               dz = offsets[index * 3 + 2];
            } else {
               double roll = this.random.nextDouble();
               double angle = this.random.nextDouble() * Math.PI * 2.0;
               if (roll < 0.16) {
                  dy = height * (0.8 + 0.16 * this.random.nextDouble());
                  double radius = Math.sqrt(this.random.nextDouble()) * width * 0.26;
                  dx = Math.cos(angle) * radius;
                  dz = Math.sin(angle) * radius;
               } else if (roll < 0.67) {
                  dy = height * (0.44 + 0.31 * this.random.nextDouble());
                  dx = (this.random.nextDouble() - 0.5) * width * (roll < 0.5 ? 0.46 : 0.53);
                  dz = (this.random.nextDouble() - 0.5) * width * 0.3;
               } else {
                  dy = height * (0.04 + 0.4 * this.random.nextDouble());
                  dx = (this.random.nextDouble() - 0.5) * width * 0.42;
                  dz = (this.random.nextDouble() - 0.5) * width * 0.24;
               }
            }

            pointsX[index] = (float)dx;
            pointsY[index] = (float)dy;
            pointsZ[index] = (float)dz;
            double radial = Math.sqrt(dx * dx + dz * dz);
            double angle = (radial > 0.04 ? Math.atan2(dz, dx) : this.random.nextDouble() * Math.PI * 2.0) + (this.random.nextDouble() - 0.5) * 1.1;
            double force = this.scatterForce.getValue() * (0.55 + 0.9 * this.random.nextDouble());
            velX[index] = (float)(Math.cos(angle) * force * 1.7);
            velZ[index] = (float)(Math.sin(angle) * force * 1.7);
            velY[index] = (float)((0.45 + 0.85 * this.random.nextDouble()) * force + 0.3);
            seeds[index] = this.random.nextFloat();
            swirls[index] = (this.random.nextBoolean() ? 1.0F : -1.0F) * (0.8F + 1.4F * this.random.nextFloat());
         }

         long holdMs = (long)(this.holdTime.getValue() * 1000.0);
         long fadeMs = (long)(this.fadeTime.getValue() * 1000.0);
         long endMs = now + holdMs + fadeMs;
         this.effects
            .add(
               new KillEffectFeature.Effect(
                  entity.getId(),
                  origin.x,
                  origin.y,
                  origin.z,
                  count,
                  pointsX,
                  pointsY,
                  pointsZ,
                  velX,
                  velY,
                  velZ,
                  seeds,
                  swirls,
                  now,
                  endMs,
                  Math.max(endMs, now + 1250L),
                  holdMs / 1000.0,
                  fadeMs / 1000.0
               )
            );
      }
   }

   private float[] sampleModelOffsets(LivingEntity entity, int count) {
      try {
         if (!(mc.getEntityRenderDispatcher().getRenderer(entity) instanceof LivingEntityRenderer livingRenderer)) {
            return null;
         } else {
            LivingEntityRenderState livingState = (LivingEntityRenderState)livingRenderer.createRenderState(entity, 1.0F);
            livingRenderer.extractRenderState(entity, livingState, 1.0F);
            livingState.deathTime = 0.0F;
            EntityModel model = livingRenderer.getModel();
            model.resetPose();
            model.setupAnim(livingState);
            List<KillEffectFeature.PartFrame> frames = new ArrayList<>();
            this.walkPart(model.root(), new Quaternionf(), new Vector3f(), frames);
            List<Object[]> cubes = new ArrayList<>();
            float totalArea = 0.0F;

            for (KillEffectFeature.PartFrame frame : frames) {
               ModelPart part = frame.part();
               if (part.visible && !part.skipDraw) {
                  for (Cube cube : ((ModelPartAccessor)(Object)part).xrose$getCubes()) {
                     float area = 2.0F
                        * (
                           (cube.maxX - cube.minX) * (cube.maxY - cube.minY)
                              + (cube.maxY - cube.minY) * (cube.maxZ - cube.minZ)
                              + (cube.maxX - cube.minX) * (cube.maxZ - cube.minZ)
                        );
                     if (!(area <= 0.001F)) {
                        cubes.add(new Object[]{frame, cube, area});
                        totalArea += area;
                     }
                  }
               }
            }

            if (!cubes.isEmpty() && !(totalArea <= 0.0F)) {
               float scale = livingState.scale * livingState.ageScale;
               float yaw = livingState.bodyRot * (float) (Math.PI / 180.0);
               float cosYaw = (float)Math.cos(yaw);
               float sinYaw = (float)Math.sin(yaw);
               float[] out = new float[count * 3];
               Vector3f point = new Vector3f();

               for (int index = 0; index < count; index++) {
                  Object[] chosen = cubes.get(cubes.size() - 1);
                  float target = this.random.nextFloat() * totalArea;

                  for (Object[] entry : cubes) {
                     target -= (Float)entry[2];
                     if (target <= 0.0F) {
                        chosen = entry;
                        break;
                     }
                  }

                  KillEffectFeature.PartFrame frame = (KillEffectFeature.PartFrame)chosen[0];
                  Cube cube = (Cube)chosen[1];
                  ModelPart part = frame.part();
                  float sizeX = cube.maxX - cube.minX;
                  float sizeY = cube.maxY - cube.minY;
                  float sizeZ = cube.maxZ - cube.minZ;
                  float u = this.random.nextFloat();
                  float v = this.random.nextFloat();
                  float face = this.random.nextFloat() * 2.0F * (sizeX * sizeY + sizeY * sizeZ + sizeX * sizeZ);
                  float px;
                  float py;
                  float pz;
                  if ((face = face - 2.0F * sizeX * sizeY) <= 0.0F) {
                     px = this.random.nextBoolean() ? cube.minX : cube.maxX;
                     py = cube.minY + v * sizeY;
                     pz = cube.minZ + u * sizeZ;
                  } else if ((face = face - 2.0F * sizeY * sizeZ) <= 0.0F) {
                     py = this.random.nextBoolean() ? cube.minY : cube.maxY;
                     px = cube.minX + u * sizeX;
                     pz = cube.minZ + v * sizeZ;
                  } else {
                     pz = this.random.nextBoolean() ? cube.minZ : cube.maxZ;
                     px = cube.minX + u * sizeX;
                     py = cube.minY + v * sizeY;
                  }

                  point.set(px * part.xScale, py * part.yScale, pz * part.zScale).mul(0.0625F);
                  frame.rotation().transform(point);
                  point.add(frame.position());
                  out[index * 3] = (cosYaw * point.x + sinYaw * point.z) * scale;
                  out[index * 3 + 1] = (-point.y - -1.501F) * scale;
                  out[index * 3 + 2] = (sinYaw * point.x - cosYaw * point.z) * scale;
               }

               return out;
            } else {
               return null;
            }
         }
      } catch (Throwable ignored) {
         return null;
      }
   }

   private void walkPart(ModelPart part, Quaternionf parentRotation, Vector3f parentPosition, List<KillEffectFeature.PartFrame> frames) {
      Vector3f position = new Vector3f(parentPosition).add(parentRotation.transform(new Vector3f(part.x, part.y, part.z).mul(0.0625F)));
      Quaternionf rotation = new Quaternionf(parentRotation);
      if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
         rotation.rotateZYX(part.zRot, part.yRot, part.xRot);
      }

      frames.add(new KillEffectFeature.PartFrame(position, rotation, part));

      for (ModelPart child : ((ModelPartAccessor)(Object)part).xrose$getChildren().values()) {
         this.walkPart(child, rotation, position, frames);
      }
   }

   private int activeParticleCount() {
      return this.effects.stream().mapToInt(KillEffectFeature.Effect::count).sum();
   }

   private void clear() {
      this.effects.clear();
      this.pendingKills.clear();
      this.sprites.clear();
   }

   private record Effect(
      int entityId,
      double originX,
      double originY,
      double originZ,
      int count,
      float[] pointsX,
      float[] pointsY,
      float[] pointsZ,
      float[] velX,
      float[] velY,
      float[] velZ,
      float[] seeds,
      float[] swirls,
      long startMs,
      long endMs,
      long hideUntilMs,
      double holdSec,
      double fadeSec
   ) {
   }

   private record PartFrame(Vector3f position, Quaternionf rotation, ModelPart part) {
   }

   private record PendingMark(long atMs, LivingEntity entity) {
   }
}

