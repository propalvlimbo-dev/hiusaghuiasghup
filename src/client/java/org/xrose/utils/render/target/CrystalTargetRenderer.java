package org.xrose.utils.render.target;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.math.Animation;
import org.xrose.utils.render.HurtUtil;
import org.xrose.utils.render.Render3DUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.particles.WorldParticleRenderer;
import sdk.api.optimize.optimize;

@optimize
public final class CrystalTargetRenderer {
   private static final int SHARD_COUNT = 13;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder().withUniform("WorldParticleUniforms", UniformType.UNIFORM_BUFFER).build();
   private static final RenderPipeline FILLED_PIPELINE = buildPipeline("crystal_target_filled", new ColorTargetState(BlendFunction.TRANSLUCENT));
   private static final RenderPipeline GLOW_PIPELINE = buildPipeline(
      "crystal_target_glow", new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
   );
   private final Animation appear = new Animation(650L, Animation.Easing.EASE_OUT_EXPO);
   private final Animation inner = new Animation(850L, Animation.Easing.EASE_IN_OUT_QUAD);
   private final WorldParticleRenderer glowRenderer = new WorldParticleRenderer();
   private final List<WorldParticleRenderer.Sprite> glowSprites = new ArrayList<>(80);
   private final List<CrystalTargetRenderer.Shard> shards = new ArrayList<>(13);
   private LivingEntity lastTarget;
   private boolean secondVariant;
   private boolean animationTarget;
   private int shardTargetId = Integer.MIN_VALUE;
   private long spawnStartMillis;
   private long lastFrameNanos;
   private float clock;
   private float rotationAngle;
   private Vec3 smoothedPos;

   private static RenderPipeline buildPipeline(String name, ColorTargetState colorTarget) {
      return RenderPipeline.builder(new Snippet[0])
         .withLocation(Identifier.parse("xrose:pipeline/world/" + name))
         .withVertexShader(Identifier.parse("xrose:core/world_particle"))
         .withFragmentShader(Identifier.parse("xrose:core/crystal_shard"))
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(LAYOUT)
         .withColorTargetState(colorTarget)
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
         .withCull(false)
         .build();
   }

   public void render(LivingEntity activeTarget, float tickDelta, int color, boolean second) {
      long nowNanos = System.nanoTime();
      float dt = this.lastFrameNanos == 0L ? 0.016F : Math.min(0.1F, (float)(nowNanos - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = nowNanos;
      long nowMillis = System.currentTimeMillis();
      boolean present = valid(activeTarget);
      if (present) {
         this.lastTarget = activeTarget;
      }

      this.updateAnimations(present);
      if (present && (second != this.secondVariant || activeTarget.getId() != this.shardTargetId)) {
         this.secondVariant = second;
         this.shardTargetId = activeTarget.getId();
         this.spawnStartMillis = nowMillis;
         this.rotationAngle = 0.0F;
         this.smoothedPos = null;
         this.rebuildShards(activeTarget);
      }

      float aV = this.appear.getValue();
      float iV = this.inner.getValue();
      this.glowSprites.clear();
      if (this.lastTarget != null && !(aV <= 0.01F) && !(iV <= 0.001F)) {
         this.clock += dt * 1000.0F;
         float t = this.clock % 80000.0F / 1000.0F;
         this.rotationAngle += (second ? 48.0F : 30.0F) * dt;
         if (this.rotationAngle >= 360.0F) {
            this.rotationAngle -= 360.0F;
         }

         Minecraft mc = Minecraft.getInstance();
         Camera camera = mc.gameRenderer.mainCamera();
         Vec3 cameraPos = camera.position();
         Vec3 targetPos = Render3DUtil.interpolatedPosition(this.lastTarget, tickDelta);
         float smoothing = Math.min(1.0F, Math.max(0.12F, tickDelta * 1.5F));
         this.smoothedPos = this.smoothedPos == null
            ? targetPos
            : new Vec3(
               this.smoothedPos.x + (targetPos.x - this.smoothedPos.x) * smoothing,
               this.smoothedPos.y + (targetPos.y - this.smoothedPos.y) * smoothing,
               this.smoothedPos.z + (targetPos.z - this.smoothedPos.z) * smoothing
            );
         AABB dangerZone = this.lastTarget.getBoundingBox().inflate(0.35);
         double distance = cameraPos.distanceTo(this.smoothedPos);
         float fadeStart = Math.max(1.25F, this.lastTarget.getBbWidth() * 1.6F);
         float cameraFade = Mth.clamp((float)((distance - fadeStart) / 0.9), 0.0F, 1.0F);
         if (dangerZone.contains(cameraPos)) {
            cameraFade = 0.0F;
         }

         float alpha = Math.min(0.95F, iV * 1.6F) * aV * cameraFade;
         if (!(alpha <= 0.01F)) {
            int baseColor = HurtUtil.blend(color, this.lastTarget, 1.0F);
            float spawnElapsed = (float)(nowMillis - this.spawnStartMillis) / 600.0F;
            List<CrystalTargetRenderer.Tri3D> filledTris = new ArrayList<>(700);
            List<CrystalTargetRenderer.Tri3D> glowTris = new ArrayList<>(500);
            if (second) {
               this.renderOrbitShards(filledTris, this.glowSprites, cameraPos, baseColor, alpha);
            } else {
               this.renderClusterShards(filledTris, glowTris, this.glowSprites, baseColor, alpha, t, spawnElapsed);
            }

            Matrix4f viewPose = Render3DUtil.cameraViewPose(camera);
            this.drawMesh(mc, filledTris, FILLED_PIPELINE, "Crystal Target Filled", cameraPos, viewPose);
            this.drawMesh(mc, glowTris, GLOW_PIPELINE, "Crystal Target Glow", cameraPos, viewPose);
            this.glowRenderer.render(this.glowSprites, 1.0F, false, true, Textures.Shader.PARTICLE_GLOW);
         }
      } else {
         if (!present && aV <= 0.01F) {
            this.lastTarget = null;
            this.shards.clear();
            this.shardTargetId = Integer.MIN_VALUE;
            this.smoothedPos = null;
         }
      }
   }

   private void rebuildShards(LivingEntity target) {
      this.shards.clear();
      Random rng = new Random(target.getId());
      float minRadius = 0.45F;
      float maxRadius = 0.85F;
      List<Vec3> placed = new ArrayList<>();
      int attempts = 0;

      while (placed.size() < 13 && attempts < 1200) {
         attempts++;
         double angle = rng.nextDouble() * Math.PI * 2.0;
         float radius = minRadius + (float)(rng.nextDouble() * (maxRadius - minRadius));
         Vec3 candidate = new Vec3(radius * Math.cos(angle), 0.55F + (float)(rng.nextDouble() * 0.95), radius * Math.sin(angle));
         boolean tooClose = false;
         Iterator var12 = placed.iterator();

         while (true) {
            if (var12.hasNext()) {
               Vec3 p = (Vec3)var12.next();
               if (!(p.distanceTo(candidate) < 0.34F)) {
                  continue;
               }

               tooClose = true;
            }

            if (!tooClose) {
               placed.add(candidate);
               this.shards
                  .add(new CrystalTargetRenderer.Shard(candidate, -30.0F + rng.nextFloat() * 60.0F, -30.0F + rng.nextFloat() * 60.0F, placed.size() - 1));
            }
            break;
         }
      }
   }

   private void renderClusterShards(
      List<CrystalTargetRenderer.Tri3D> filled,
      List<CrystalTargetRenderer.Tri3D> glow,
      List<WorldParticleRenderer.Sprite> halos,
      int baseColor,
      float alpha,
      float t,
      float spawnElapsed
   ) {
      for (CrystalTargetRenderer.Shard shard : this.shards) {
         float staggerDelay = shard.index * 0.07F;
         float spawnT = Mth.clamp((spawnElapsed - staggerDelay) / 0.65F, 0.0F, 1.0F);
         if (!(spawnT <= 0.0F)) {
            float bob = (float)Math.sin(t * 2.1 + shard.bobOffset) * 0.055F;
            float spin = t * shard.spinSpeed * 60.0F;
            Matrix4f m = new Matrix4f()
               .translation((float)this.smoothedPos.x, (float)this.smoothedPos.y, (float)this.smoothedPos.z)
               .rotateY((float)Math.toRadians(this.rotationAngle))
               .translate((float)shard.position.x, (float)(shard.position.y + bob), (float)shard.position.z);
            m.scale(spawnT, spawnT, spawnT);
            m.rotateX((float)Math.toRadians(shard.tiltX));
            m.rotateZ((float)Math.toRadians(shard.tiltZ));
            m.rotateY((float)Math.toRadians(spin));
            addHexShard(filled, m, baseColor, 0.28F * spawnT * 0.9F, alpha, 1.0F);
            addHexShard(glow, m, baseColor, 0.28F * spawnT * 0.22F, alpha, 1.18F);
            addHexShard(glow, m, baseColor, 0.28F * spawnT * 0.08F, alpha, 1.35F);
            Vector3f worldOrigin = m.transformPosition(new Vector3f());
            Vec3 haloPos = new Vec3(worldOrigin.x, worldOrigin.y, worldOrigin.z);
            int ba = (int)(13.75F * alpha * spawnT);
            halos.add(new WorldParticleRenderer.Sprite(haloPos, 0.3F, withAlpha(baseColor, ba)));
            halos.add(new WorldParticleRenderer.Sprite(haloPos, 0.15F, withAlpha(lighten(baseColor, 1.4F), ba / 3), (float) (Math.PI / 4)));
         }
      }
   }

   private void renderOrbitShards(
      List<CrystalTargetRenderer.Tri3D> filled, List<WorldParticleRenderer.Sprite> glows, Vec3 cameraPos, int baseColor, float alpha
   ) {
      float entityWidth = this.lastTarget.getBbWidth();
      float entityHeight = this.lastTarget.getBbHeight();
      float width = entityWidth * 1.5F;
      float orbitScale = 1.2F - 0.5F * alpha;
      float centerY = entityHeight / 2.0F;
      int glowColor = ColorUtil.multiplyAlpha(baseColor, alpha * 0.28F);
      float glowBaseSize = 4.5F + entityWidth * 3.0F;

      for (int i = 0; i < 360; i += 20) {
         float angleRad = (float)Math.toRadians(i + this.rotationAngle);
         float ox = (float)(Math.sin(angleRad) * width * orbitScale);
         float oz = (float)(Math.cos(angleRad) * width * orbitScale);
         float oy = 0.1F + entityHeight * Mth.abs(Mth.sin(i));
         float dirX = -ox;
         float dirY = centerY - oy;
         float dirZ = -oz;
         float length = (float)Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
         if (!(length < 0.001F)) {
            dirX /= length;
            dirY /= length;
            dirZ /= length;
            Matrix4f m = new Matrix4f()
               .translation((float)this.smoothedPos.x, (float)this.smoothedPos.y, (float)this.smoothedPos.z)
               .translate(ox, oy, oz)
               .rotate(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(dirX, dirY, dirZ)));
            addTetraShard(filled, m, ColorUtil.multiplyAlpha(baseColor, alpha));
            Vec3 glowPos = this.smoothedPos.add(ox, oy, oz);
            double dist = cameraPos.distanceTo(glowPos);
            float distScale = (float)Math.max(0.1, dist * 0.007);
            float outerRotation = (float)Math.toRadians(this.rotationAngle + i);
            float innerRotation = (float)Math.toRadians(-(this.rotationAngle + i * 0.5F));
            glows.add(
               new WorldParticleRenderer.Sprite(glowPos, glowBaseSize * 1.28F * distScale * 0.5F, ColorUtil.multiplyAlpha(glowColor, 0.24F), outerRotation)
            );
            glows.add(new WorldParticleRenderer.Sprite(glowPos, glowBaseSize * distScale * 0.5F, glowColor, innerRotation));
         }
      }
   }

   private static void addHexShard(List<CrystalTargetRenderer.Tri3D> tris, Matrix4f m, int baseColor, float alphaMultiplier, float anim, float scale) {
      float r = 0.0547F;
      float hTop = 0.1037F;
      float hBot = 0.0691F;
      float waist = 0.0259F;
      int faces = 6;
      Vector3f[] ring = new Vector3f[faces];
      Vector3f[] waistRing = new Vector3f[faces];

      for (int i = 0; i < faces; i++) {
         float ang = (float)((Math.PI * 2) * i / faces);
         ring[i] = new Vector3f((float)(r * Math.cos(ang)) * scale, 0.0F, (float)(r * Math.sin(ang)) * scale);
         float wa = ang + (float)(Math.PI / faces);
         waistRing[i] = new Vector3f((float)(waist * Math.cos(wa)) * scale, 0.0F, (float)(waist * Math.sin(wa)) * scale);
      }

      Vector3f apex = new Vector3f(0.0F, hTop * scale, 0.0F);
      Vector3f nadir = new Vector3f(0.0F, -hBot * scale, 0.0F);
      int fa = (int)(alphaMultiplier * 255.0F * anim);
      if (fa > 2) {
         int c0 = withAlpha(baseColor, fa);
         int c1 = withAlpha(darken(baseColor, 0.6F), fa);
         int c2 = withAlpha(lighten(baseColor, 1.3F), fa);

         for (int i = 0; i < faces; i++) {
            Vector3f a0 = ring[i];
            Vector3f a1 = ring[(i + 1) % faces];
            int col = i % 2 == 0 ? c2 : c0;
            addTri(tris, m, apex, a1, a0, col);
         }

         for (int i = 0; i < faces; i++) {
            Vector3f a0 = ring[i];
            Vector3f a1 = ring[(i + 1) % faces];
            Vector3f w0 = waistRing[i];
            int col = i % 2 == 0 ? c0 : c1;
            addTri(tris, m, a0, a1, w0, col);
            addTri(tris, m, a1, waistRing[(i + 1) % faces], w0, col);
         }

         for (int i = 0; i < faces; i++) {
            Vector3f a0 = ring[i];
            Vector3f w0 = waistRing[i];
            int col = i % 2 == 0 ? c1 : c0;
            addTri(tris, m, nadir, a0, w0, col);
            addTri(tris, m, nadir, w0, ring[(i + 1) % faces], col);
         }
      }
   }

   private static void addTetraShard(List<CrystalTargetRenderer.Tri3D> tris, Matrix4f m, int color) {
      float w = 0.06F;
      float h = 0.2F;
      Vector3f top = new Vector3f(0.0F, h, 0.0F);
      Vector3f bottom = new Vector3f(0.0F, -h, 0.0F);
      Vector3f px = new Vector3f(w, 0.0F, 0.0F);
      Vector3f pz = new Vector3f(0.0F, 0.0F, w);
      Vector3f nx = new Vector3f(-w, 0.0F, 0.0F);
      Vector3f nz = new Vector3f(0.0F, 0.0F, -w);
      addTri(tris, m, top, px, pz, color);
      addTri(tris, m, top, pz, nx, color);
      addTri(tris, m, top, nx, nz, color);
      addTri(tris, m, top, nz, px, color);
      addTri(tris, m, bottom, pz, px, color);
      addTri(tris, m, bottom, nx, pz, color);
      addTri(tris, m, bottom, nz, nx, color);
      addTri(tris, m, bottom, px, nz, color);
   }

   private static void addTri(List<CrystalTargetRenderer.Tri3D> tris, Matrix4f m, Vector3f v1, Vector3f v2, Vector3f v3, int color) {
      Vector3f p1 = m.transformPosition(new Vector3f(v1));
      Vector3f p2 = m.transformPosition(new Vector3f(v2));
      Vector3f p3 = m.transformPosition(new Vector3f(v3));
      tris.add(new CrystalTargetRenderer.Tri3D(new Vec3(p1.x, p1.y, p1.z), new Vec3(p2.x, p2.y, p2.z), new Vec3(p3.x, p3.y, p3.z), color));
   }

   private void drawMesh(Minecraft mc, List<CrystalTargetRenderer.Tri3D> tris, RenderPipeline pipeline, String label, Vec3 cameraPos, Matrix4f viewPose) {
      if (!tris.isEmpty() && mc.level != null) {
         RenderTarget target = mc.gameRenderer.mainRenderTarget();
         GpuTextureView colorView = target != null ? target.getColorTextureView() : null;
         if (colorView != null) {
            BufferBuilder builder = new BufferBuilder(
               ByteBufferBuilder.exactlySized(tris.size() * 3 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize()),
               PrimitiveTopology.TRIANGLES,
               DefaultVertexFormat.POSITION_TEX_COLOR
            );

            for (CrystalTargetRenderer.Tri3D tri : tris) {
               Vector4f v0 = Render3DUtil.toViewSpace(tri.a(), cameraPos, viewPose);
               Vector4f v1 = Render3DUtil.toViewSpace(tri.b(), cameraPos, viewPose);
               Vector4f v2 = Render3DUtil.toViewSpace(tri.c(), cameraPos, viewPose);
               builder.addVertex(v0.x, v0.y, v0.z).setUv(0.0F, 0.0F).setColor(tri.color());
               builder.addVertex(v1.x, v1.y, v1.z).setUv(0.0F, 0.0F).setColor(tri.color());
               builder.addVertex(v2.x, v2.y, v2.z).setUv(0.0F, 0.0F).setColor(tri.color());
            }

            MeshData meshData = builder.build();
            if (meshData != null) {
               GpuDevice device = RenderSystem.getDevice();
               GpuBuffer vertexBuffer = null;
               GpuBuffer uniformBuffer = null;

               try {
                  vertexBuffer = device.createBuffer(() -> "XRose " + label + " Vertices", 40, meshData.vertexBuffer());
                  uniformBuffer = this.uploadUniform();
                  GpuTextureView depthView = target.getDepthTextureView();
                  RenderPass pass = depthView != null
                     ? device.createCommandEncoder()
                        .createRenderPass(() -> "XRose " + label + " Pass", colorView, Optional.empty(), depthView, OptionalDouble.empty())
                     : device.createCommandEncoder().createRenderPass(() -> "XRose " + label + " Pass", colorView, Optional.empty());

                  try {
                     pass.setPipeline(pipeline);
                     pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     pass.setUniform("WorldParticleUniforms", uniformBuffer);
                     pass.setVertexBuffer(0, vertexBuffer.slice());
                     pass.draw(tris.size() * 3, 1, 0, 0);
                  } finally {
                     pass.close();
                  }
               } finally {
                  if (uniformBuffer != null) {
                     uniformBuffer.close();
                  }

                  if (vertexBuffer != null) {
                     vertexBuffer.close();
                  }

                  meshData.close();
               }
            }
         }
      }
   }

   private GpuBuffer uploadUniform() {
      GpuDevice device = RenderSystem.getDevice();
      GpuBuffer buffer = device.createBuffer(() -> "XRose Crystal Target UBO", 136, UNIFORM_SIZE);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE).putVec4(1.0F, 0.0F, 0.0F, 0.0F).get();
         device.createCommandEncoder().writeToBuffer(buffer.slice(), data);
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }

      return buffer;
   }

   private void updateAnimations(boolean present) {
      if (present != this.animationTarget) {
         this.animationTarget = present;
         float to = present ? 1.0F : 0.0F;
         this.appear.animate(this.appear.getValue(), to, present ? 650L : 450L, present ? Animation.Easing.EASE_OUT_EXPO : Animation.Easing.EASE_OUT_QUAD);
         this.inner.animate(this.inner.getValue(), to, present ? 850L : 500L, present ? Animation.Easing.EASE_IN_OUT_QUAD : Animation.Easing.EASE_OUT_QUAD);
      }
   }

   private static boolean valid(LivingEntity entity) {
      return entity != null && entity.isAlive() && !entity.isRemoved();
   }

   private static int withAlpha(int argb, int a) {
      return a << 24 | argb & 16777215;
   }

   private static int darken(int argb, float factor) {
      int a = argb >> 24 & 0xFF;
      int r = (int)((argb >> 16 & 0xFF) * factor);
      int g = (int)((argb >> 8 & 0xFF) * factor);
      int b = (int)((argb & 0xFF) * factor);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int lighten(int argb, float factor) {
      int a = argb >> 24 & 0xFF;
      int r = Math.min(255, (int)((argb >> 16 & 0xFF) * factor));
      int g = Math.min(255, (int)((argb >> 8 & 0xFF) * factor));
      int b = Math.min(255, (int)((argb & 0xFF) * factor));
      return a << 24 | r << 16 | g << 8 | b;
   }

   public void release() {
      this.lastTarget = null;
      this.shards.clear();
      this.shardTargetId = Integer.MIN_VALUE;
      this.smoothedPos = null;
      this.glowSprites.clear();
   }

   private static final class Shard {
      final Vec3 position;
      final float tiltX;
      final float tiltZ;
      final float spinSpeed;
      final float bobOffset;
      final int index;

      Shard(Vec3 position, float tiltX, float tiltZ, int index) {
         this.position = position;
         this.tiltX = tiltX;
         this.tiltZ = tiltZ;
         this.index = index;
         this.spinSpeed = 0.8F + (float)(Math.random() * 1.2);
         this.bobOffset = (float)(Math.random() * Math.PI * 2.0);
      }
   }

   private record Tri3D(Vec3 a, Vec3 b, Vec3 c, int color) {
   }
}

