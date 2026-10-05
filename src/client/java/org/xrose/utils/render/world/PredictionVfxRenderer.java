package org.xrose.utils.render.world;

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
import com.mojang.blaze3d.platform.BlendFactor;
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
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.xrose.utils.render.Render3DUtil;
import sdk.api.optimize.optimize;

@optimize
public final class PredictionVfxRenderer {
   private static final long TIMESTAMP = System.nanoTime();
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().get();
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder().withUniform("PredictionVfxUniforms", UniformType.UNIFORM_BUFFER).build();
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.parse("xrose:pipeline/world/prediction_vfx"))
      .withVertexShader(Identifier.parse("xrose:core/prediction_vfx"))
      .withFragmentShader(Identifier.parse("xrose:core/prediction_vfx"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final int RING_BANDS = 8;
   private static final int RING_RADIAL = 72;
   private static final int DOME_BANDS = 6;
   private static final int DOME_RADIAL = 72;
   private static final int DISC_RADIAL = 72;
   private static final int VERTICES_PER_FIELD = 10368;

   public void render(List<PredictionVfxRenderer.RadiusField> fields, float tickDelta) {
      Minecraft mc = Minecraft.getInstance();
      if (fields != null && !fields.isEmpty() && mc.level != null) {
         RenderTarget target = mc.gameRenderer.mainRenderTarget();
         GpuTextureView colorView = target != null ? target.getColorTextureView() : null;
         if (colorView != null) {
            Camera camera = mc.gameRenderer.mainCamera();
            Vec3 cameraPos = camera.position();
            Matrix4f pose = Render3DUtil.cameraViewPose(camera);
            MeshData meshData = this.buildMesh(fields, cameraPos, pose);
            if (meshData != null) {
               float gameTime = ((float)(mc.level.getGameTime() % 24000L) + tickDelta) / 24000.0F;
               GpuDevice device = RenderSystem.getDevice();
               GpuBuffer vertexBuffer = null;
               GpuBuffer uniformBuffer = null;

               try {
                  vertexBuffer = device.createBuffer(() -> "XRose Prediction Vfx Vertices", 40, meshData.vertexBuffer());
                  uniformBuffer = this.uploadUniform(gameTime);
                  RenderPass pass = device.createCommandEncoder().createRenderPass(() -> "XRose Prediction Vfx Pass", colorView, Optional.empty());

                  try {
                     pass.setPipeline(PIPELINE);
                     pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     pass.setUniform("PredictionVfxUniforms", uniformBuffer);
                     pass.setVertexBuffer(0, vertexBuffer.slice());
                     pass.draw(fields.size() * 10368, 1, 0, 0);
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

   private GpuBuffer uploadUniform(float gameTime) {
      GpuDevice device = RenderSystem.getDevice();
      GpuBuffer buffer = device.createBuffer(() -> "XRose Prediction Vfx UBO", 136, UNIFORM_SIZE);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE).putVec4(gameTime, 0.0F, 0.0F, 0.0F).get();
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

   private MeshData buildMesh(List<PredictionVfxRenderer.RadiusField> fields, Vec3 cameraPos, Matrix4f pose) {
      BufferBuilder builder = new BufferBuilder(
         ByteBufferBuilder.exactlySized(fields.size() * 10368 * DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL.getVertexSize()),
         PrimitiveTopology.TRIANGLES,
         DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
      );

      for (PredictionVfxRenderer.RadiusField field : fields) {
         this.addField(builder, field, cameraPos, pose);
      }

      return builder.buildOrThrow();
   }

   private void addField(BufferBuilder builder, PredictionVfxRenderer.RadiusField field, Vec3 cameraPos, Matrix4f pose) {
      Direction dir = field.direction();
      float fnx = dir.getStepX();
      float fny = dir.getStepY();
      float fnz = dir.getStepZ();
      Vector3f u;
      Vector3f v;
      switch (dir.getAxis()) {
         case X:
            u = new Vector3f(0.0F, 0.0F, dir == Direction.WEST ? -1.0F : 1.0F);
            v = new Vector3f(0.0F, 1.0F, 0.0F);
            break;
         case Z:
            u = new Vector3f(dir == Direction.NORTH ? -1.0F : 1.0F, 0.0F, 0.0F);
            v = new Vector3f(0.0F, 1.0F, 0.0F);
            break;
         default:
            u = new Vector3f(1.0F, 0.0F, 0.0F);
            v = new Vector3f(0.0F, 0.0F, dir == Direction.DOWN ? -1.0F : 1.0F);
      }

      Vector4f center = Render3DUtil.toViewSpace(field.landingPos().add(fnx * 0.01, fny * 0.01, fnz * 0.01), cameraPos, pose);
      Vector3f uView = rotateDir(pose, u);
      Vector3f vView = rotateDir(pose, v);
      Vector3f nView = rotateDir(pose, fnx, fny, fnz);
      float phase = (float)((System.nanoTime() - TIMESTAMP) * 1.0E-9) * 0.454545F;
      float scale = 0.92F + 0.08F * (float)Math.sin(phase * Math.PI * 2.0);
      int r = field.color() >> 16 & 0xFF;
      int g = field.color() >> 8 & 0xFF;
      int b = field.color() & 0xFF;
      this.addRing(builder, center, uView, vView, nView, 1.2F * scale, r, g, b, 56, phase, 4.0F);
      this.addRing(
         builder,
         center,
         uView,
         vView,
         nView,
         0.74F * scale,
         compute2(r, 255, 0.18F),
         compute2(g, 255, 0.14F),
         compute2(b, 255, 0.16F),
         100,
         phase + 0.27F,
         4.0F
      );
      this.addDome(
         builder,
         center,
         uView,
         vView,
         nView,
         0.54F * scale,
         0.3F * scale,
         compute2(r, 255, 0.32F),
         compute2(g, 255, 0.26F),
         compute2(b, 255, 0.28F),
         130,
         phase,
         4.0F
      );
      this.addDisc(builder, center, uView, vView, 0.62F * scale, 0.09F, r, g, b, 110, phase + 0.21F, 0.78F, 2.0F);
      this.addDisc(
         builder,
         center,
         uView,
         vView,
         0.33F * scale,
         0.038F,
         compute2(r, 255, 0.36F),
         compute2(g, 255, 0.3F),
         compute2(b, 255, 0.32F),
         200,
         phase + 0.46F,
         0.95F,
         2.0F
      );
   }

   private void addRing(
      BufferBuilder builder, Vector4f center, Vector3f u, Vector3f v, Vector3f n, float radius, int r, int g, int b, int alpha, float phase, float layer
   ) {
      float cx = center.x;
      float cy = center.y;
      float cz = center.z;

      for (int band = 0; band < 8; band++) {
         float h1 = band / 8.0F;
         float h2 = (band + 1) / 8.0F;
         double rad1 = radius * h1;
         double rad2 = radius * h2;
         int a1 = clamp255(Math.round(alpha * (1.0F - h1) * (1.0F - h1)));
         int a2 = clamp255(Math.round(alpha * (1.0F - h2) * (1.0F - h2)));

         for (int seg = 0; seg < 72; seg++) {
            float s1 = seg / 72.0F;
            float s2 = (seg + 1) / 72.0F;
            double th1 = s1 * Math.PI * 2.0;
            double th2 = s2 * Math.PI * 2.0;
            double c1 = Math.cos(th1);
            double sn1 = Math.sin(th1);
            double c2 = Math.cos(th2);
            double sn2 = Math.sin(th2);
            float uu1 = s1 + phase * 0.18F;
            float uu2 = s2 + phase * 0.18F;
            float vv1 = layer + h1;
            float vv2 = layer + (band == 7 ? 0.999F : h2);
            vert(builder, cx, cy, cz, u, v, c1, sn1, rad2, n.x, n.y, n.z, uu1, vv2, r, g, b, a2);
            vert(builder, cx, cy, cz, u, v, c1, sn1, rad1, n.x, n.y, n.z, uu1, vv1, r, g, b, a1);
            vert(builder, cx, cy, cz, u, v, c2, sn2, rad1, n.x, n.y, n.z, uu2, vv1, r, g, b, a1);
            vert(builder, cx, cy, cz, u, v, c1, sn1, rad2, n.x, n.y, n.z, uu1, vv2, r, g, b, a2);
            vert(builder, cx, cy, cz, u, v, c2, sn2, rad1, n.x, n.y, n.z, uu2, vv1, r, g, b, a1);
            vert(builder, cx, cy, cz, u, v, c2, sn2, rad2, n.x, n.y, n.z, uu2, vv2, r, g, b, a2);
         }
      }
   }

   private void addDome(
      BufferBuilder builder,
      Vector4f center,
      Vector3f u,
      Vector3f v,
      Vector3f n,
      float radius,
      float height,
      int r,
      int g,
      int b,
      int alpha,
      float phase,
      float layer
   ) {
      float cx = center.x;
      float cy = center.y;
      float cz = center.z;

      for (int band = 0; band < 6; band++) {
         float h1 = band / 6.0F;
         float h2 = (band + 1) / 6.0F;
         double rad1 = radius * h1;
         double rad2 = radius * h2;
         double up1 = height * (1.0 - h1 * h1);
         double up2 = height * (1.0 - h2 * h2);
         int a1 = clamp255(Math.round(alpha * (1.0F - h1 * 0.62F)));
         int a2 = clamp255(Math.round(alpha * (1.0F - h2 * 0.62F)));

         for (int seg = 0; seg < 72; seg++) {
            float s1 = seg / 72.0F;
            float s2 = (seg + 1) / 72.0F;
            double th1 = s1 * Math.PI * 2.0;
            double th2 = s2 * Math.PI * 2.0;
            double c1 = Math.cos(th1);
            double sn1 = Math.sin(th1);
            double c2 = Math.cos(th2);
            double sn2 = Math.sin(th2);
            float uu1 = s1 + phase * 0.26F;
            float uu2 = s2 + phase * 0.26F;
            float vv1 = layer + h1;
            float vv2 = layer + (band == 5 ? 0.999F : h2);
            Vector3f radial1 = radial(u, v, c1, sn1);
            Vector3f radial2 = radial(u, v, c2, sn2);
            Vector3f n1 = blendNormal(n, radial1, h1);
            Vector3f n2 = blendNormal(n, radial2, h2);
            domeVert(builder, cx, cy, cz, radial2, rad2, n.x, n.y, n.z, up2, n2, uu1, vv2, r, g, b, a2);
            domeVert(builder, cx, cy, cz, radial1, rad1, n.x, n.y, n.z, up1, n1, uu1, vv1, r, g, b, a1);
            domeVert(builder, cx, cy, cz, radial1, rad1, n.x, n.y, n.z, up1, n1, uu2, vv1, r, g, b, a1);
            domeVert(builder, cx, cy, cz, radial2, rad2, n.x, n.y, n.z, up2, n2, uu2, vv2, r, g, b, a2);
         }
      }
   }

   private void addDisc(
      BufferBuilder builder,
      Vector4f center,
      Vector3f u,
      Vector3f v,
      float m,
      float thickness,
      int r,
      int g,
      int b,
      int alpha,
      float phase,
      float scale2,
      float layer
   ) {
      double inner = Math.max(0.01, m - thickness * 0.5);
      double outer = m + thickness * 0.5;
      Vector3f n = new Vector3f(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
      n.normalize();
      float cx = center.x;
      float cy = center.y;
      float cz = center.z;

      for (int seg = 0; seg < 72; seg++) {
         float s1 = seg / 72.0F;
         float s2 = (seg + 1) / 72.0F;
         double th1 = s1 * Math.PI * 2.0;
         double th2 = s2 * Math.PI * 2.0;
         double c1 = Math.cos(th1);
         double sn1 = Math.sin(th1);
         double c2 = Math.cos(th2);
         double sn2 = Math.sin(th2);
         int a1 = discAlpha(alpha, s1, phase, scale2);
         int a2 = discAlpha(alpha, s2, phase, scale2);
         float uu1 = s1 + phase * 0.2F;
         float uu2 = s2 + phase * 0.2F;
         vert(builder, cx, cy, cz, u, v, c1, sn1, outer, n.x, n.y, n.z, uu1, layer + 0.92F, r, g, b, a1);
         vert(builder, cx, cy, cz, u, v, c1, sn1, inner, n.x, n.y, n.z, uu1, layer + 0.08F, r, g, b, a1);
         vert(builder, cx, cy, cz, u, v, c2, sn2, inner, n.x, n.y, n.z, uu2, layer + 0.08F, r, g, b, a2);
         vert(builder, cx, cy, cz, u, v, c1, sn1, outer, n.x, n.y, n.z, uu1, layer + 0.92F, r, g, b, a1);
         vert(builder, cx, cy, cz, u, v, c2, sn2, inner, n.x, n.y, n.z, uu2, layer + 0.08F, r, g, b, a2);
         vert(builder, cx, cy, cz, u, v, c2, sn2, outer, n.x, n.y, n.z, uu2, layer + 0.92F, r, g, b, a2);
      }
   }

   private static void vert(
      BufferBuilder builder,
      float cx,
      float cy,
      float cz,
      Vector3f u,
      Vector3f v,
      double cth,
      double sth,
      double rad,
      float nx,
      float ny,
      float nz,
      float uu,
      float vv,
      int r,
      int g,
      int b,
      int a
   ) {
      builder.addVertex(
            cx + (float)(u.x * cth + v.x * sth) * (float)rad,
            cy + (float)(u.y * cth + v.y * sth) * (float)rad,
            cz + (float)(u.z * cth + v.z * sth) * (float)rad
         )
         .setUv(uu, vv)
         .setColor(r, g, b, a)
         .setNormal(nx, ny, nz);
   }

   private static void domeVert(
      BufferBuilder builder,
      float cx,
      float cy,
      float cz,
      Vector3f radial,
      double rad,
      float nx,
      float ny,
      float nz,
      double up,
      Vector3f normal,
      float uu,
      float vv,
      int r,
      int g,
      int b,
      int a
   ) {
      builder.addVertex(cx + radial.x * (float)rad + nx * (float)up, cy + radial.y * (float)rad + ny * (float)up, cz + radial.z * (float)rad + nz * (float)up)
         .setUv(uu, vv)
         .setColor(r, g, b, a)
         .setNormal(normal.x, normal.y, normal.z);
   }

   private static Vector3f radial(Vector3f u, Vector3f v, double cth, double sth) {
      return new Vector3f((float)(u.x * cth + v.x * sth), (float)(u.y * cth + v.y * sth), (float)(u.z * cth + v.z * sth));
   }

   private static Vector3f blendNormal(Vector3f n, Vector3f radial, float t) {
      float wx = n.x * (1.0F - t * 0.32F) + radial.x * t * 0.68F;
      float wy = n.y * (1.0F - t * 0.32F) + radial.y * t * 0.68F;
      float wz = n.z * (1.0F - t * 0.32F) + radial.z * t * 0.68F;
      float len = (float)Math.sqrt(wx * wx + wy * wy + wz * wz);
      return len <= 1.0E-5F ? new Vector3f(n) : new Vector3f(wx / len, wy / len, wz / len);
   }

   private static Vector3f rotateDir(Matrix4f pose, Vector3f dir) {
      Vector3f result = new Vector3f(dir);
      pose.transformDirection(result);
      result.normalize();
      return result;
   }

   private static Vector3f rotateDir(Matrix4f pose, float x, float y, float z) {
      Vector3f result = new Vector3f(x, y, z);
      pose.transformDirection(result);
      result.normalize();
      return result;
   }

   private static int discAlpha(int alpha, float frac, float phase, float scale2) {
      float wave = 0.5F + 0.5F * (float)Math.sin((frac * 3.0F - phase * 2.0F) * Math.PI * 2.0);
      return clamp255(Math.round(alpha * scale2 * (0.48F + wave * 0.52F)));
   }

   private static int compute2(int i, int j, float f) {
      float t = Mth.clamp(f, 0.0F, 1.0F);
      return clamp255(Math.round(i + (j - i) * t));
   }

   private static int clamp255(int value) {
      return Mth.clamp(value, 0, 255);
   }

   public record RadiusField(Vec3 landingPos, Direction direction, int color) {
   }
}

