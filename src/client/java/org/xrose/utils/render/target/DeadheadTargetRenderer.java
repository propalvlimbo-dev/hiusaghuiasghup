package org.xrose.utils.render.target;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
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
public final class DeadheadTargetRenderer {
   private static final int SKULL_COUNT = 3;
   private static final int SEAL_PARTICLES = 16;
   private static final int WAVE_RINGS = 3;
   private static final int WAVE_SEGMENTS = 22;
   private static final Random RNG = new Random();
   private static long lastBiteTime = 0L;
   private static int biteSkullIndex = 0;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
      .withSampler("BloomSampler")
      .withUniform("WorldParticleUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline ALPHA_THROUGH_WALLS = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.parse("xrose:pipeline/world/deadhead_alpha"))
      .withVertexShader(Identifier.parse("xrose:core/world_particle"))
      .withFragmentShader(Identifier.parse("xrose:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private final Animation appear = new Animation(650L, Animation.Easing.EASE_OUT_EXPO);
   private final Animation inner = new Animation(850L, Animation.Easing.EASE_IN_OUT_QUAD);
   private final WorldParticleRenderer glowRenderer = new WorldParticleRenderer();
   private final TargetDeathDissolve deathDissolve = new TargetDeathDissolve();
   private final List<WorldParticleRenderer.Sprite> glowSprites = new ArrayList<>(120);
   private LivingEntity lastTarget;
   private int dissolvedTargetId = Integer.MIN_VALUE;
   private boolean animationTarget;
   private long lastFrameNanos;
   private float clock;

   public static void triggerBite() {
      lastBiteTime = System.currentTimeMillis();
      biteSkullIndex = RNG.nextInt(3);
   }

   public void render(LivingEntity activeTarget, float tickDelta, int color) {
      long nowNanos = System.nanoTime();
      long nowMillis = System.currentTimeMillis();
      float dt = this.lastFrameNanos == 0L ? 0.016F : Math.min(0.1F, (float)(nowNanos - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = nowNanos;
      boolean present = valid(activeTarget);
      if (present) {
         this.lastTarget = activeTarget;
         if (activeTarget.getId() == this.dissolvedTargetId) {
            this.dissolvedTargetId = Integer.MIN_VALUE;
         }
      }

      if (dead(this.lastTarget) && this.lastTarget.getId() != this.dissolvedTargetId) {
         this.deathDissolve.burst(this.glowSprites, this.lastTarget, color, nowMillis);
         this.dissolvedTargetId = this.lastTarget.getId();
         present = false;
      }

      this.updateAnimations(present);
      float aV = this.appear.getValue();
      float iV = this.inner.getValue();
      if (this.lastTarget != null && !(aV <= 0.01F) && !(iV <= 0.001F)) {
         this.clock += dt * 1000.0F;
         float t = this.clock / 1000.0F;
         Vec3 base = Render3DUtil.interpolatedPosition(this.lastTarget, tickDelta);
         float alpha = Math.min(0.95F, iV * 1.6F) * aV;
         int baseColor = HurtUtil.blend(color, this.lastTarget, 1.0F);
         int mintColor = ColorUtil.lerp(baseColor, -16711800, 0.4F);
         this.glowSprites.clear();
         float sealR = (0.85F + 0.12F * Mth.sin(t * 3.0F)) * iV;

         for (int i = 0; i < 16; i++) {
            float angle = t * 1.8F + (float)(i * Math.PI * 2.0 / 16.0);
            this.glowSprites
               .add(
                  new WorldParticleRenderer.Sprite(
                     new Vec3(base.x + sealR * Mth.sin(angle), base.y + 0.05 + 0.03 * Mth.sin(t * 4.0F + i), base.z + sealR * Mth.cos(angle)),
                     0.08F * iV,
                     ColorUtil.multiplyAlpha(mintColor, alpha * 0.45F)
                  )
               );
         }

         Vec3 headCenter = new Vec3(base.x, base.y + this.lastTarget.getBbHeight() * 0.9F, base.z);
         float hurt = HurtUtil.easedFactor(this.lastTarget);
         int waveColor = ColorUtil.lerp(mintColor, -44459, hurt * 0.75F);

         for (int w = 0; w < 3; w++) {
            float cycle = (t * (0.55F + hurt * 0.6F) % 1.0F + w / 3.0F) % 1.0F;
            float pulse = 1.0F - cycle;
            float radius = (0.3F + pulse * (0.85F + hurt * 0.35F)) * iV;
            float ringAlpha = alpha * pulse * pulse * (0.35F + hurt * 0.25F);

            for (int s = 0; s < 22; s++) {
               float ang = (float)(s * Math.PI * 2.0 / 22.0) + t * 1.5F;
               float wobble = 0.05F * Mth.sin(t * 5.0F + ang * 3.0F);
               float segAlpha = (0.5F + 0.5F * Mth.sin(ang * 2.0F - t * 8.0F)) * ringAlpha;
               if (!(segAlpha <= 0.004F)) {
                  this.glowSprites
                     .add(
                        new WorldParticleRenderer.Sprite(
                           new Vec3(headCenter.x + radius * Mth.sin(ang), headCenter.y + wobble, headCenter.z + radius * Mth.cos(ang)),
                           0.11F * iV,
                           ColorUtil.multiplyAlpha(waveColor, segAlpha)
                        )
                     );
               }
            }
         }

         if (hurt > 0.02F) {
            this.glowSprites
               .add(
                  new WorldParticleRenderer.Sprite(
                     headCenter.add(0.0, 0.05F, 0.0), 0.55F * iV * (1.0F + hurt * 0.5F), ColorUtil.multiplyAlpha(-44459, alpha * hurt * 0.45F)
                  )
               );
         }

         long biteAge = nowMillis - lastBiteTime;
         float biteT = biteAge >= 0L && biteAge <= 380L ? (float)biteAge / 380.0F : -1.0F;
         Vec3 chestPos = base.add(0.0, this.lastTarget.getBbHeight() * 0.55, 0.0);
         Minecraft mc = Minecraft.getInstance();
         Camera camera = mc.gameRenderer.mainCamera();
         Vec3 cameraPos = camera.position();
         Matrix4f viewPose = Render3DUtil.cameraViewPose(camera);
         List<DeadheadTargetRenderer.Quad3D> meshQuads = new ArrayList<>(180);
         float orbitR = 1.85F * (0.8F + 0.2F * iV);

         for (int si = 0; si < 3; si++) {
            float phase = (float)(si * Math.PI * 2.0 / 3.0);
            float orbitA = t * 2.2F + phase;
            double bob = 0.4 * Mth.sin(t * 3.6F + phase * 1.4F);
            Vec3 skullCenter = new Vec3(
               base.x + orbitR * Mth.sin(orbitA), base.y + this.lastTarget.getBbHeight() * 0.52 + bob, base.z + orbitR * Mth.cos(orbitA)
            );
            float scale = 1.56F * iV;
            float jawOpen = 0.0F;
            float yawAngle = -orbitA + (float) (Math.PI / 2);
            if (biteT >= 0.0F && si == biteSkullIndex) {
               float lunge = (float)Math.sin(biteT * Math.PI);
               skullCenter = skullCenter.lerp(chestPos, lunge * 0.88);
               scale *= 1.0F + lunge * 0.6F;
               jawOpen = 0.18F * lunge;
               if (biteT >= 0.35F && biteT <= 0.65F) {
                  for (int b = 0; b < 4; b++) {
                     this.glowSprites
                        .add(
                           new WorldParticleRenderer.Sprite(
                              chestPos.add((RNG.nextFloat() - 0.5) * 0.4, (RNG.nextFloat() - 0.5) * 0.4, (RNG.nextFloat() - 0.5) * 0.4),
                              0.12F * iV,
                              ColorUtil.multiplyAlpha(-16711800, alpha)
                           )
                        );
                  }
               }
            }

            for (int ti = 1; ti <= 8; ti++) {
               float tA = orbitA - ti * 0.08F;
               double ttBob = 0.4 * Mth.sin(t * 3.6F + phase * 1.4F - ti * 0.1F);
               float tAlpha = alpha * (1.0F - ti / 8.0F) * 0.35F;
               if (tAlpha > 0.005F) {
                  this.glowSprites
                     .add(
                        new WorldParticleRenderer.Sprite(
                           new Vec3(base.x + orbitR * Mth.sin(tA), base.y + this.lastTarget.getBbHeight() * 0.52 + ttBob, base.z + orbitR * Mth.cos(tA)),
                           scale * (1.0F - ti * 0.04F),
                           ColorUtil.multiplyAlpha(mintColor, tAlpha)
                        )
                     );
               }
            }

            int boneColor = packARGB((int)(alpha * 230.0F), 215, 255, 225);
            int darkSocket = packARGB((int)(alpha * 240.0F), 10, 20, 15);
            int toothCol = packARGB((int)(alpha * 240.0F), 245, 255, 250);
            add3DBox(meshQuads, skullCenter, scale * 0.32F, scale * 0.28F, scale * 0.32F, 0.0F, scale * 0.1F, 0.0F, yawAngle, boneColor);
            add3DBox(meshQuads, skullCenter, scale * 0.24F, scale * 0.14F, scale * 0.18F, 0.0F, -scale * 0.08F, scale * 0.12F, yawAngle, boneColor);
            add3DBox(meshQuads, skullCenter, scale * 0.08F, scale * 0.08F, scale * 0.05F, -scale * 0.08F, scale * 0.1F, scale * 0.14F, yawAngle, darkSocket);
            add3DBox(meshQuads, skullCenter, scale * 0.08F, scale * 0.08F, scale * 0.05F, scale * 0.08F, scale * 0.1F, scale * 0.14F, yawAngle, darkSocket);
            add3DBox(meshQuads, skullCenter, scale * 0.05F, scale * 0.06F, scale * 0.04F, 0.0F, scale * 0.01F, scale * 0.15F, yawAngle, darkSocket);
            add3DBox(meshQuads, skullCenter, scale * 0.22F, scale * 0.1F, scale * 0.24F, 0.0F, -scale * (0.2F + jawOpen), scale * 0.06F, yawAngle, boneColor);
            add3DBox(meshQuads, skullCenter, scale * 0.18F, scale * 0.04F, scale * 0.02F, 0.0F, -scale * 0.14F, scale * 0.2F, yawAngle, toothCol);
            add3DBox(meshQuads, skullCenter, scale * 0.16F, scale * 0.04F, scale * 0.02F, 0.0F, -scale * (0.16F + jawOpen), scale * 0.17F, yawAngle, toothCol);
            Vec3 leftEyeWorld = rotateOffset(skullCenter, -scale * 0.08F, scale * 0.1F, scale * 0.16F, yawAngle);
            Vec3 rightEyeWorld = rotateOffset(skullCenter, scale * 0.08F, scale * 0.1F, scale * 0.16F, yawAngle);
            this.glowSprites.add(new WorldParticleRenderer.Sprite(leftEyeWorld, scale * 0.18F, ColorUtil.multiplyAlpha(-16711732, alpha * 0.9F)));
            this.glowSprites.add(new WorldParticleRenderer.Sprite(rightEyeWorld, scale * 0.18F, ColorUtil.multiplyAlpha(-16711732, alpha * 0.9F)));
         }

         this.render3DMesh(mc, meshQuads, cameraPos, viewPose);
         this.glowRenderer.render(this.glowSprites, 1.0F, true, true);
         this.deathDissolve.render(dt, nowMillis);
      } else {
         if (!present && aV <= 0.01F) {
            this.lastTarget = null;
            this.glowSprites.clear();
         }

         this.deathDissolve.render(dt, nowMillis);
      }
   }

   private static Vec3 rotateOffset(Vec3 center, float ox, float oy, float oz, float yaw) {
      float cos = Mth.cos(yaw);
      float sin = Mth.sin(yaw);
      double rx = ox * cos - oz * sin;
      double rz = ox * sin + oz * cos;
      return center.add(rx, oy, rz);
   }

   private static void add3DBox(
      List<DeadheadTargetRenderer.Quad3D> quads, Vec3 center, float w, float h, float d, float ox, float oy, float oz, float yaw, int color
   ) {
      float hW = w * 0.5F;
      float hH = h * 0.5F;
      float hD = d * 0.5F;
      Vec3[] v = new Vec3[]{
         rotateOffset(center, ox - hW, oy - hH, oz - hD, yaw),
         rotateOffset(center, ox + hW, oy - hH, oz - hD, yaw),
         rotateOffset(center, ox + hW, oy + hH, oz - hD, yaw),
         rotateOffset(center, ox - hW, oy + hH, oz - hD, yaw),
         rotateOffset(center, ox - hW, oy - hH, oz + hD, yaw),
         rotateOffset(center, ox + hW, oy - hH, oz + hD, yaw),
         rotateOffset(center, ox + hW, oy + hH, oz + hD, yaw),
         rotateOffset(center, ox - hW, oy + hH, oz + hD, yaw)
      };
      quads.add(new DeadheadTargetRenderer.Quad3D(v[4], v[5], v[6], v[7], color));
      quads.add(new DeadheadTargetRenderer.Quad3D(v[1], v[0], v[3], v[2], darken(color, 0.8F)));
      quads.add(new DeadheadTargetRenderer.Quad3D(v[3], v[2], v[6], v[7], lighten(color, 1.15F)));
      quads.add(new DeadheadTargetRenderer.Quad3D(v[4], v[5], v[1], v[0], darken(color, 0.65F)));
      quads.add(new DeadheadTargetRenderer.Quad3D(v[5], v[1], v[2], v[6], darken(color, 0.88F)));
      quads.add(new DeadheadTargetRenderer.Quad3D(v[0], v[4], v[7], v[3], darken(color, 0.88F)));
   }

   private void render3DMesh(Minecraft mc, List<DeadheadTargetRenderer.Quad3D> quads, Vec3 cameraPos, Matrix4f viewPose) {
      if (!quads.isEmpty() && mc.level != null) {
         RenderTarget target = mc.gameRenderer.mainRenderTarget();
         GpuTextureView colorView = target != null ? target.getColorTextureView() : null;
         if (colorView != null) {
            AbstractTexture bloom = mc.getTextureManager().getTexture(Textures.Shader.BLOOM);
            GpuTextureView bloomView = bloom != null ? bloom.getTextureView() : null;
            if (bloomView != null) {
               BufferBuilder builder = new BufferBuilder(
                  ByteBufferBuilder.exactlySized(quads.size() * 6 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize()),
                  PrimitiveTopology.TRIANGLES,
                  DefaultVertexFormat.POSITION_TEX_COLOR
               );

               for (DeadheadTargetRenderer.Quad3D q : quads) {
                  Vector4f v0 = Render3DUtil.toViewSpace(q.p0, cameraPos, viewPose);
                  Vector4f v1 = Render3DUtil.toViewSpace(q.p1, cameraPos, viewPose);
                  Vector4f v2 = Render3DUtil.toViewSpace(q.p2, cameraPos, viewPose);
                  Vector4f v3 = Render3DUtil.toViewSpace(q.p3, cameraPos, viewPose);
                  builder.addVertex(v0.x, v0.y, v0.z).setUv(0.0F, 0.0F).setColor(q.color);
                  builder.addVertex(v1.x, v1.y, v1.z).setUv(1.0F, 0.0F).setColor(q.color);
                  builder.addVertex(v2.x, v2.y, v2.z).setUv(1.0F, 1.0F).setColor(q.color);
                  builder.addVertex(v0.x, v0.y, v0.z).setUv(0.0F, 0.0F).setColor(q.color);
                  builder.addVertex(v2.x, v2.y, v2.z).setUv(1.0F, 1.0F).setColor(q.color);
                  builder.addVertex(v3.x, v3.y, v3.z).setUv(0.0F, 1.0F).setColor(q.color);
               }

               MeshData meshData = builder.build();
               if (meshData != null) {
                  GpuDevice device = RenderSystem.getDevice();
                  GpuBuffer vertexBuffer = null;
                  GpuBuffer uniformBuffer = null;

                  try {
                     vertexBuffer = device.createBuffer(() -> "XRose 3D Deadhead Vertices", 40, meshData.vertexBuffer());
                     uniformBuffer = this.uploadUniform(1.0F);
                     GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                     RenderPass pass = device.createCommandEncoder().createRenderPass(() -> "XRose 3D Deadhead Pass", colorView, Optional.empty());

                     try {
                        pass.setPipeline(ALPHA_THROUGH_WALLS);
                        pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        pass.setUniform("WorldParticleUniforms", uniformBuffer);
                        pass.bindTexture("BloomSampler", bloomView, sampler);
                        pass.setVertexBuffer(0, vertexBuffer.slice());
                        pass.draw(quads.size() * 6, 1, 0, 0);
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
   }

   private GpuBuffer uploadUniform(float brightness) {
      GpuDevice device = RenderSystem.getDevice();
      GpuBuffer buffer = device.createBuffer(() -> "XRose 3D Deadhead UBO", 136, UNIFORM_SIZE);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE).putVec4(brightness, 0.0F, 0.0F, 0.0F).get();
         device.createCommandEncoder().writeToBuffer(buffer.slice(), data);
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }

      return buffer;
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

   private static int packARGB(int a, int r, int g, int b) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   private void updateAnimations(boolean present) {
      if (present != this.animationTarget) {
         this.animationTarget = present;
         float to = present ? 1.0F : 0.0F;
         this.appear.animate(this.appear.getValue(), to, present ? 650L : 450L, present ? Animation.Easing.EASE_OUT_EXPO : Animation.Easing.EASE_OUT_QUAD);
         this.inner.animate(this.inner.getValue(), to, present ? 850L : 500L, present ? Animation.Easing.EASE_IN_OUT_QUAD : Animation.Easing.EASE_OUT_QUAD);
      }
   }

   private static boolean valid(LivingEntity e) {
      return e != null && e.isAlive() && !e.isRemoved();
   }

   private static boolean dead(LivingEntity e) {
      return e != null && (e.isDeadOrDying() || e.deathTime > 0 || e.getHealth() <= 0.0F);
   }

   public void release() {
      this.lastTarget = null;
      this.glowSprites.clear();
   }

   private record Quad3D(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color) {
   }
}

