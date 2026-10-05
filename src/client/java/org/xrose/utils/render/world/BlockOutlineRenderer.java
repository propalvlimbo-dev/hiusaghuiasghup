package org.xrose.utils.render.world;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.xrose.feature.impl.visual.BlockOutlineFeature;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Render3DUtil;
import org.xrose.utils.render.post.PostFx;
import org.xrose.utils.render.post.PostPipelines;
import sdk.api.optimize.optimize;

@optimize
public final class BlockOutlineRenderer {
   private static final float BOX_EPSILON = 0.0025F;
   private static final float FADE_RATE = 7.5F;
   private static final int TRANSFORM_SIZE = new Std140SizeCalculator().putMat4f().get();
   private static final int STYLE_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().putVec4().get();
   private final GpuBuffer transformUniforms = uniformBuffer("XRose Block Outline Transform UBO", TRANSFORM_SIZE);
   private final GpuBuffer styleUniforms = uniformBuffer("XRose Block Outline Style UBO", STYLE_SIZE);
   private BlockPos selectedPos;
   private BlockState selectedState;
   private double renderX;
   private double renderY;
   private double renderZ;
   private boolean hasRenderPos;
   private long lastFrameNanos;
   private float transition;
   private float fade;
   private GpuBuffer cachedVertexBuffer;
   private int cachedVertexCount;

   public void render(BlockOutlineFeature feature, CameraRenderState cameraState) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null && minecraft.gameRenderer != null && cameraState != null && cameraState.initialized) {
         BlockPos blockPos = null;
         BlockState state = null;
         if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
            BlockPos hitPos = hit.getBlockPos();
            if (!minecraft.level.getBlockState(hitPos).isAir()) {
               blockPos = hitPos;
               state = minecraft.level.getBlockState(hitPos);
            }
         }

         float seconds = this.stepSeconds(System.nanoTime());
         if (blockPos == null) {
            if (this.selectedPos == null) {
               return;
            }

            this.fade = Math.max(0.0F, this.fade - seconds * 7.5F);
            if (this.fade <= 0.0F) {
               this.resetSelection();
               return;
            }
         } else {
            if (this.selectedPos == null) {
               this.selectedPos = blockPos.immutable();
               this.transition = 0.0F;
            } else if (!blockPos.equals(this.selectedPos)) {
               this.selectedPos = blockPos.immutable();
            }

            if (state != this.selectedState || this.cachedVertexBuffer == null) {
               this.selectedState = state;
               this.rebuildMesh(minecraft, blockPos, state);
            }

            this.fade = Math.min(1.0F, this.fade + seconds * 7.5F);
            this.transition = Math.min(1.0F, this.transition + seconds * feature.animationSpeed.getValue().floatValue() * 0.35F);
            this.updateRenderPos(feature, seconds);
         }

         if (this.selectedPos != null && this.cachedVertexBuffer != null && this.cachedVertexCount != 0) {
            RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
            if (target != null && target.getColorTextureView() != null && target.getDepthTextureView() != null) {
               float eased = 1.0F - (float)Math.pow(1.0F - this.transition, 3.0);
               float scale = 0.92F + eased * 0.08F;
               Matrix4f projection = Render3DUtil.levelProjectionCopy();
               if (projection != null) {
                  Matrix4f modelViewProjection = projection.mul(cameraState.viewRotationMatrix)
                     .translate(
                        (float)(this.renderX - cameraState.pos.x() + 0.5),
                        (float)(this.renderY - cameraState.pos.y() + 0.5),
                        (float)(this.renderZ - cameraState.pos.z() + 0.5)
                     )
                     .scale(scale)
                     .translate(-0.5F, -0.5F, -0.5F);
                  this.writeUniforms(feature, modelViewProjection, target.width, target.height, this.fade);
                  RenderPass pass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "XRose Block Outline", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty()
                     );

                  try {
                     pass.setPipeline(this.pipeline(feature));
                     RenderSystem.bindDefaultUniforms(pass);
                     pass.setUniform("BlockOutlineTransform", this.transformUniforms);
                     pass.setUniform("BlockOutlineStyle", this.styleUniforms);
                     pass.setVertexBuffer(0, this.cachedVertexBuffer.slice());
                     pass.draw(this.cachedVertexCount, 1, 0, 0);
                  } finally {
                     pass.close();
                  }
               }
            }
         }
      } else {
         this.resetSelection();
      }
   }

   private void updateRenderPos(BlockOutlineFeature feature, float seconds) {
      BlockPos target = this.selectedPos;
      if (target != null) {
         if (!this.hasRenderPos) {
            this.renderX = target.getX();
            this.renderY = target.getY();
            this.renderZ = target.getZ();
            this.hasRenderPos = true;
         } else {
            double rate = feature.transitionSpeed.getValue();
            double alpha = 1.0 - Math.exp(-rate * 3.0 * seconds);
            this.renderX = this.renderX + (target.getX() - this.renderX) * alpha;
            this.renderY = this.renderY + (target.getY() - this.renderY) * alpha;
            this.renderZ = this.renderZ + (target.getZ() - this.renderZ) * alpha;
         }
      }
   }

   private void rebuildMesh(Minecraft minecraft, BlockPos blockPos, BlockState state) {
      this.releaseMesh();
      List<AABB> boxes = state.getShape(minecraft.level, blockPos).toAabbs();
      if (boxes.isEmpty()) {
         boxes = List.of(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
      }

      MeshData mesh = this.buildMesh(boxes);
      if (mesh != null) {
         MeshData var6 = mesh;

         try {
            this.cachedVertexCount = mesh.drawState().vertexCount();
            this.cachedVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "XRose Block Outline Vertices", 32, mesh.vertexBuffer());
         } catch (Throwable var10) {
            if (var6 != null) {
               try {
                  var6.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (var6 != null) {
            var6.close();
         }
      }
   }

   private void releaseMesh() {
      if (this.cachedVertexBuffer != null) {
         this.cachedVertexBuffer.close();
         this.cachedVertexBuffer = null;
      }

      this.cachedVertexCount = 0;
   }

   public void resetSelection() {
      this.selectedPos = null;
      this.selectedState = null;
      this.hasRenderPos = false;
      this.transition = 0.0F;
      this.fade = 0.0F;
      this.lastFrameNanos = 0L;
      this.releaseMesh();
   }

   public void release() {
      this.resetSelection();
      this.transformUniforms.close();
      this.styleUniforms.close();
   }

   private float stepSeconds(long now) {
      if (this.lastFrameNanos == 0L) {
         this.lastFrameNanos = now;
         return 0.0F;
      } else {
         float seconds = Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
         this.lastFrameNanos = now;
         return seconds;
      }
   }

   private void writeUniforms(BlockOutlineFeature feature, Matrix4f modelViewProjection, int width, int height, float alpha) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer transform = Std140Builder.onStack(stack, TRANSFORM_SIZE).putMat4f(modelViewProjection).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.transformUniforms.slice(), transform);
         int tint = -1;
         ByteBuffer style = Std140Builder.onStack(stack, STYLE_SIZE)
            .putVec4(ColorUtil.red(tint) / 255.0F, ColorUtil.green(tint) / 255.0F, ColorUtil.blue(tint) / 255.0F, alpha * 0.82F)
            .putVec4(width, height, PostFx.shaderTime() * feature.shaderSpeed.getValue().floatValue(), feature.shaderIntensity.getValue().floatValue())
            .putVec4(rgb(feature.auroraColor.getValue())[0], rgb(feature.auroraColor.getValue())[1], rgb(feature.auroraColor.getValue())[2], 1.0F)
            .putVec4(rgb(feature.secondaryColor.getValue())[0], rgb(feature.secondaryColor.getValue())[1], rgb(feature.secondaryColor.getValue())[2], 1.0F)
            .putVec4(rgb(feature.starColor.getValue())[0], rgb(feature.starColor.getValue())[1], rgb(feature.starColor.getValue())[2], 1.0F)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.styleUniforms.slice(), style);
      } catch (Throwable var11) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private RenderPipeline pipeline(BlockOutlineFeature feature) {
      return feature.ignoreDepth.getValue() ? PostPipelines.BLOCK_OUTLINE_NIGHT_THROUGH : PostPipelines.BLOCK_OUTLINE_NIGHT;
   }

   private MeshData buildMesh(List<AABB> boxes) {
      int vertexCount = boxes.size() * 36;
      if (vertexCount == 0) {
         return null;
      }

      BufferBuilder builder = new BufferBuilder(
         new ByteBufferBuilder(Math.max(256, vertexCount * DefaultVertexFormat.POSITION.getVertexSize())),
         PrimitiveTopology.TRIANGLES,
         DefaultVertexFormat.POSITION
      );

      for (AABB box : boxes) {
         float minX = (float)box.minX - 0.0025F;
         float minY = (float)box.minY - 0.0025F;
         float minZ = (float)box.minZ - 0.0025F;
         float maxX = (float)box.maxX + 0.0025F;
         float maxY = (float)box.maxY + 0.0025F;
         float maxZ = (float)box.maxZ + 0.0025F;
         this.face(builder, minX, minY, minZ, maxX, maxY, minZ);
         this.face(builder, maxX, minY, maxZ, minX, maxY, maxZ);
         this.face(builder, minX, minY, maxZ, minX, maxY, minZ);
         this.face(builder, maxX, minY, minZ, maxX, maxY, maxZ);
         this.face(builder, minX, maxY, minZ, maxX, maxY, maxZ);
         this.face(builder, minX, minY, maxZ, maxX, minY, minZ);
      }

      return builder.buildOrThrow();
   }

   private void face(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2) {
      boolean constantX = x1 == x2;
      boolean constantY = y1 == y2;
      if (constantX) {
         this.vertex(builder, x1, y1, z1);
         this.vertex(builder, x1, y1, z2);
         this.vertex(builder, x1, y2, z2);
         this.vertex(builder, x1, y2, z2);
         this.vertex(builder, x1, y2, z1);
         this.vertex(builder, x1, y1, z1);
      } else if (constantY) {
         this.vertex(builder, x1, y1, z1);
         this.vertex(builder, x2, y1, z1);
         this.vertex(builder, x2, y1, z2);
         this.vertex(builder, x2, y1, z2);
         this.vertex(builder, x1, y1, z2);
         this.vertex(builder, x1, y1, z1);
      } else {
         this.vertex(builder, x1, y1, z1);
         this.vertex(builder, x2, y1, z1);
         this.vertex(builder, x2, y2, z1);
         this.vertex(builder, x2, y2, z1);
         this.vertex(builder, x1, y2, z1);
         this.vertex(builder, x1, y1, z1);
      }
   }

   private void vertex(BufferBuilder builder, float x, float y, float z) {
      builder.addVertex(x, y, z);
   }

   private static float[] rgb(int argb) {
      return new float[]{ColorUtil.red(argb) / 255.0F, ColorUtil.green(argb) / 255.0F, ColorUtil.blue(argb) / 255.0F};
   }

   private static GpuBuffer uniformBuffer(String label, int size) {
      return RenderSystem.getDevice().createBuffer(() -> label, 136, size);
   }
}

