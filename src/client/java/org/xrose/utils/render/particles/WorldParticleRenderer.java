package org.xrose.utils.render.particles;

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
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.xrose.utils.render.Render3DUtil;
import org.xrose.utils.render.Textures;
import sdk.api.optimize.optimize;

@optimize
public final class WorldParticleRenderer {
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
      .withSampler("BloomSampler")
      .withUniform("WorldParticleUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.parse("xrose:pipeline/world/world_particle"))
      .withVertexShader(Identifier.parse("xrose:core/world_particle"))
      .withFragmentShader(Identifier.parse("xrose:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final RenderPipeline THROUGH_WALLS_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.parse("xrose:pipeline/world/world_particle_through_walls"))
      .withVertexShader(Identifier.parse("xrose:core/world_particle"))
      .withFragmentShader(Identifier.parse("xrose:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE)))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final RenderPipeline ALPHA_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.parse("xrose:pipeline/world/world_particle_alpha"))
      .withVertexShader(Identifier.parse("xrose:core/world_particle"))
      .withFragmentShader(Identifier.parse("xrose:core/world_particle"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final RenderPipeline ALPHA_THROUGH_WALLS_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.parse("xrose:pipeline/world/world_particle_alpha_through_walls"))
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

   public void render(List<WorldParticleRenderer.Sprite> sprites, float brightness) {
      this.render(sprites, brightness, false, true);
   }

   public void render(List<WorldParticleRenderer.Sprite> sprites, float brightness, boolean throughWalls) {
      this.render(sprites, brightness, throughWalls, true);
   }

   public void render(List<WorldParticleRenderer.Sprite> sprites, float brightness, boolean throughWalls, boolean additive) {
      this.render(sprites, brightness, throughWalls, additive, Textures.Shader.BLOOM);
   }

   public void render(List<WorldParticleRenderer.Sprite> sprites, float brightness, boolean throughWalls, boolean additive, Identifier texture) {
      Minecraft mc = Minecraft.getInstance();
      if (!sprites.isEmpty() && mc.level != null) {
         RenderTarget target = mc.gameRenderer.mainRenderTarget();
         GpuTextureView colorView = target != null ? target.getColorTextureView() : null;
         if (colorView != null) {
            AbstractTexture bloom = mc.getTextureManager().getTexture(texture);
            GpuTextureView bloomView = bloom != null ? bloom.getTextureView() : null;
            if (bloomView != null) {
               MeshData meshData = this.buildMesh(mc, sprites);
               if (meshData != null) {
                  GpuDevice device = RenderSystem.getDevice();
                  GpuBuffer vertexBuffer = null;
                  GpuBuffer uniformBuffer = null;

                  try {
                     vertexBuffer = device.createBuffer(() -> "XRose World Particles Vertices", 40, meshData.vertexBuffer());
                     uniformBuffer = this.uploadUniform(brightness);
                     GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                     GpuTextureView depthView = target.getDepthTextureView();
                     RenderPass pass = !throughWalls && depthView != null
                        ? device.createCommandEncoder()
                           .createRenderPass(() -> "XRose World Particles Pass", colorView, Optional.empty(), depthView, OptionalDouble.empty())
                        : device.createCommandEncoder().createRenderPass(() -> "XRose World Particles Pass", colorView, Optional.empty());

                     try {
                        RenderPipeline pipeline = additive
                           ? (throughWalls ? THROUGH_WALLS_PIPELINE : PIPELINE)
                           : (throughWalls ? ALPHA_THROUGH_WALLS_PIPELINE : ALPHA_PIPELINE);
                        pass.setPipeline(pipeline);
                        pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        pass.setUniform("WorldParticleUniforms", uniformBuffer);
                        pass.bindTexture("BloomSampler", bloomView, sampler);
                        pass.setVertexBuffer(0, vertexBuffer.slice());
                        pass.draw(sprites.size() * 6, 1, 0, 0);
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
      GpuBuffer buffer = device.createBuffer(() -> "XRose World Particles UBO", 136, UNIFORM_SIZE);
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

   private MeshData buildMesh(Minecraft mc, List<WorldParticleRenderer.Sprite> sprites) {
      Camera camera = mc.gameRenderer.mainCamera();
      Vec3 cameraPos = camera.position();
      Matrix4f pose = Render3DUtil.cameraViewPose(camera);
      BufferBuilder builder = new BufferBuilder(
         ByteBufferBuilder.exactlySized(sprites.size() * 6 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize()),
         PrimitiveTopology.TRIANGLES,
         DefaultVertexFormat.POSITION_TEX_COLOR
      );

      for (WorldParticleRenderer.Sprite sprite : sprites) {
         Vector4f center = Render3DUtil.toViewSpace(sprite.position(), cameraPos, pose);
         float half = sprite.halfSize();
         int color = sprite.color();
         float rot = sprite.rotationRadians();
         float cosRot = Mth.cos(rot);
         float sinRot = Mth.sin(rot);
         this.addVertex(builder, center, -half, -half, cosRot, sinRot, 0.0F, 0.0F, color);
         this.addVertex(builder, center, -half, half, cosRot, sinRot, 0.0F, 1.0F, color);
         this.addVertex(builder, center, half, half, cosRot, sinRot, 1.0F, 1.0F, color);
         this.addVertex(builder, center, -half, -half, cosRot, sinRot, 0.0F, 0.0F, color);
         this.addVertex(builder, center, half, half, cosRot, sinRot, 1.0F, 1.0F, color);
         this.addVertex(builder, center, half, -half, cosRot, sinRot, 1.0F, 0.0F, color);
      }

      return builder.build();
   }

   private void addVertex(BufferBuilder builder, Vector4f center, float dx, float dy, float cosRot, float sinRot, float u, float v, int color) {
      float rx = dx * cosRot - dy * sinRot;
      float ry = dx * sinRot + dy * cosRot;
      builder.addVertex(center.x + rx, center.y + ry, center.z).setUv(u, v).setColor(color);
   }

   public record Sprite(Vec3 position, float halfSize, int color, float rotationRadians) {
      public Sprite(Vec3 position, float halfSize, int color) {
         this(position, halfSize, color, 0.0F);
      }
   }
}

