package org.xrose.utils.render.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.xrose.feature.impl.visual.AtmoDawnFogFeature;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Render3DUtil;
import org.xrose.utils.render.post.PostFx;
import org.xrose.utils.render.post.PostPipelines;
import org.xrose.utils.render.post.PostTarget;
import sdk.api.optimize.optimize;

@optimize
public final class AtmoDawnFogRenderer {
   private static final int UNIFORM_SIZE = new Std140SizeCalculator()
      .putMat4f()
      .putMat4f()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .putVec4()
      .get();
   private static final int DUSK_PALETTE = 13203624;
   private final GpuBuffer uniforms = PostFx.createUniforms("XRose Dawn Fog UBO", UNIFORM_SIZE);
   private final PostTarget sceneCopy = new PostTarget("xrose-dawn-fog-scene", PostPipelines.EFFECT_FORMAT, false);
   private final Matrix4f inverseProjection = new Matrix4f();
   private final Matrix4f inverseView = new Matrix4f();
   private final Vector4f sunVector = new Vector4f();
   private final float[] palette = new float[18];

   public void render(AtmoDawnFogFeature feature, WorldEffectContext context) {
      Minecraft minecraft = Minecraft.getInstance();
      RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
      CameraRenderState cameraState = context.cameraRenderState();
      if (valid(mainTarget) && cameraState != null && cameraState.initialized) {
         Vec3 cameraPos = new Vec3(cameraState.pos.x(), cameraState.pos.y(), cameraState.pos.z());
         Matrix4f projection = Render3DUtil.levelProjectionCopy();
         if (projection != null) {
            float tickDelta = context.tickDelta();
            float sunAngle = context.levelRenderState().skyRenderState.sunAngle;
            float sinSun = -((float)Math.sin(sunAngle));
            float sunSide = sinSun >= 0.0F ? 1.0F : -1.0F;
            float sunPitch = sunPitch(feature.modeIndex());
            float sunDirY = sunSide * (float)Math.cos(sunPitch);
            float sunDirZ = (float)Math.sin(sunPitch);
            float sunScreenX = 0.5F;
            float sunScreenY = 0.5F;
            float sunScreenZ = 0.0F;
            this.sunVector.set(sunDirY, sunDirZ, 0.0F, 0.0F);
            cameraState.viewRotationMatrix.transform(this.sunVector);
            float sunForward = -this.sunVector.z;
            if (sunForward > 1.0E-4F) {
               this.sunVector.set(sunDirY * 1000.0F, sunDirZ * 1000.0F, sunForward * 1000.0F, 1.0F);
               projection.transform(this.sunVector);
               if (this.sunVector.w > 1.0E-4F) {
                  sunScreenX = this.sunVector.x / this.sunVector.w * 0.5F + 0.5F;
                  sunScreenY = this.sunVector.y / this.sunVector.w * 0.5F + 0.5F;
                  sunScreenZ = clamp01(sunForward * 4.0F);
               }
            }

            float rainbowDirX = -sunSide * (float)Math.cos(0.3F);
            float rainbowDirY = -((float)Math.sin(0.3F));
            this.fillPalette(feature);
            this.inverseProjection.set(projection).invert();
            this.inverseView.set(cameraState.viewRotationMatrix).invert();
            this.inverseView.m30((float)cameraPos.x);
            this.inverseView.m31((float)cameraPos.y);
            this.inverseView.m32((float)cameraPos.z);
            TextureTarget scene = this.sceneCopy.ensure(mainTarget.width, mainTarget.height);
            if (scene != null && scene.getColorTexture() != null) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .copyTextureToTexture(mainTarget.getColorTexture(), scene.getColorTexture(), 0, 0, 0, 0, 0, mainTarget.width, mainTarget.height);
               this.writeUniforms(
                  feature,
                  cameraPos,
                  tickDelta,
                  sunDirY,
                  sunDirZ,
                  sunScreenX,
                  sunScreenY,
                  sunScreenZ,
                  rainbowDirX,
                  rainbowDirY,
                  mainTarget.width,
                  mainTarget.height
               );
               PostFx.pass("XRose Dawn Fog", PostPipelines.WORLD_DAWN_FOG, mainTarget, pass -> {
                  pass.setUniform("DawnFogUniforms", this.uniforms);
                  pass.bindTexture("SceneSampler", scene.getColorTextureView(), PostFx.linearSampler());
                  pass.bindTexture("DepthSampler", mainTarget.getDepthTextureView(), PostFx.nearestSampler());
               });
            }
         }
      }
   }

   private void writeUniforms(
      AtmoDawnFogFeature feature,
      Vec3 cameraPos,
      float tickDelta,
      float sunDirY,
      float sunDirZ,
      float sunScreenX,
      float sunScreenY,
      float sunScreenZ,
      float rainbowDirX,
      float rainbowDirY,
      int width,
      int height
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      float time = ((float)(minecraft.level.getGameTime() % 100000L) + tickDelta) * 0.05F;
      float viewDistance = ((Integer)minecraft.options.renderDistance().get()).intValue() * 16.0F;
      float scatterHeight = feature.scatterHeight.getValue().floatValue();
      float density = clamp(feature.density.getValue().floatValue(), 0.05F, 0.8F);
      boolean nightMode = feature.modeIndex() == 3;
      float rainbow = !nightMode && feature.rainbow.getValue() ? clamp01(feature.rainbowBrightness.getValue().floatValue()) : 0.0F;
      float rainbowSize = clamp(feature.rainbowSize.getValue().floatValue(), 40.0F, 64.0F);
      float godRays = clamp01(feature.godRays.getValue().floatValue());
      float softness = clamp01(feature.softness.getValue().floatValue());
      float stars = nightMode ? clamp01(feature.stars.getValue().floatValue()) : 0.0F;
      float aurora = nightMode ? clamp01(feature.aurora.getValue().floatValue()) : 0.0F;
      float moon = nightMode ? 1.0F : 0.0F;
      float sunGlow = clamp(feature.sunGlow.getValue().floatValue(), 0.0F, 1.5F);
      float zzo = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE)
            .putMat4f(this.inverseProjection)
            .putMat4f(this.inverseView)
            .putVec4((float)cameraPos.x, (float)cameraPos.y, (float)cameraPos.z, 0.0F)
            .putVec4(time, zzo, 0.0F, 0.0F)
            .putVec4(width, height, density, 0.0F)
            .putVec4(sunDirY, sunDirZ, 0.0F, scatterHeight - 18.0F)
            .putVec4(scatterHeight, viewDistance, 0.0F, 0.0F)
            .putVec4(this.palette[0], this.palette[1], this.palette[2], 1.0F)
            .putVec4(this.palette[3], this.palette[4], this.palette[5], 1.0F)
            .putVec4(this.palette[6], this.palette[7], this.palette[8], 1.0F)
            .putVec4(this.palette[9], this.palette[10], this.palette[11], 1.0F)
            .putVec4(this.palette[12], this.palette[13], this.palette[14], 1.0F)
            .putVec4(this.palette[15], this.palette[16], this.palette[17], 1.0F)
            .putVec4(sunScreenX, sunScreenY, sunScreenZ, rainbow)
            .putVec4(rainbowDirX, rainbowDirY, 0.0F, rainbowSize)
            .putVec4(godRays, softness, 0.0F, 0.0F)
            .putVec4(stars, aurora, moon, sunGlow)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), data);
      } catch (Throwable var32) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var31) {
               var32.addSuppressed(var31);
            }
         }

         throw var32;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private void fillPalette(AtmoDawnFogFeature feature) {
      int dawnColor = feature.dawnColor.getValue();
      float dr = ColorUtil.red(dawnColor) / 255.0F;
      float dg = ColorUtil.green(dawnColor) / 255.0F;
      float db = ColorUtil.blue(dawnColor) / 255.0F;
      int mode = feature.modeIndex();
      if (mode == 1) {
         float jr = 0.7882353F;
         float jg = 0.47058824F;
         float jb = 0.65882355F;
         mixOklab(this.palette, 0, 0.085F, 0.1F, 0.2F, jr, jg, jb, 0.3F);
         mixOklab(this.palette, 3, jr, jg, jb, 1.0F, 0.93F, 0.82F, 0.35F);
         mixOklab(this.palette, 6, 0.52F, 0.58F, 0.74F, jr, jg, jb, 0.28F);
         mixOklab(this.palette, 9, jr, jg, jb, 0.97F, 0.93F, 0.88F, 0.45F);
         mixOklab(this.palette, 12, 0.58F, 0.63F, 0.76F, jr, jg, jb, 0.35F);
         mixOklab(this.palette, 15, jr, jg, jb, 1.0F, 0.96F, 0.88F, 0.3F);
      } else if (mode == 2) {
         mixOklab(this.palette, 0, 0.16F, 0.19F, 0.38F, dr, dg, db, 0.14F);
         set3(this.palette, 3, clamp01(dr * 1.12F), clamp01(dg * 0.88F), clamp01(db * 0.62F));
         set3(this.palette, 6, 0.56F, 0.62F, 0.8F);
         mixOklab(this.palette, 9, dr, dg, db, 0.95F, 0.55F, 0.63F, 0.42F);
         set3(this.palette, 12, 0.6F, 0.67F, 0.82F);
         set3(this.palette, 15, clamp01(dr * 1.08F), clamp01(dg * 0.94F), clamp01(db * 0.72F));
      } else if (mode == 3) {
         set3(this.palette, 0, 0.045F, 0.055F, 0.135F);
         set3(this.palette, 3, 0.6F, 0.68F, 0.87F);
         set3(this.palette, 6, 0.095F, 0.125F, 0.25F);
         set3(this.palette, 9, 0.155F, 0.185F, 0.32F);
         set3(this.palette, 12, 0.115F, 0.145F, 0.275F);
         set3(this.palette, 15, 0.73F, 0.79F, 0.97F);
      } else {
         set3(this.palette, 0, 0.135F, 0.125F, 0.3F);
         set3(this.palette, 3, 0.89F, 0.46F, 0.55F);
         set3(this.palette, 6, 0.38F, 0.35F, 0.56F);
         set3(this.palette, 9, 0.8F, 0.52F, 0.62F);
         set3(this.palette, 12, 0.47F, 0.44F, 0.64F);
         set3(this.palette, 15, 0.92F, 0.56F, 0.72F);
      }
   }

   private static void set3(float[] floats, int offset, float r, float g, float b) {
      floats[offset] = r;
      floats[offset + 1] = g;
      floats[offset + 2] = b;
   }

   private static void mixOklab(float[] floats, int offset, float f, float g, float h, float j, float k, float l, float amount) {
      float t = clamp01(amount);
      float cbrtR1 = (float)Math.cbrt(0.41222146F * srgbToLinear(f) + 0.53633255F * srgbToLinear(g) + 0.051445995F * srgbToLinear(h));
      float cbrtG1 = (float)Math.cbrt(0.2119035F * srgbToLinear(f) + 0.6806995F * srgbToLinear(g) + 0.10739696F * srgbToLinear(h));
      float cbrtB1 = (float)Math.cbrt(0.08830246F * srgbToLinear(f) + 0.28171885F * srgbToLinear(g) + 0.6299787F * srgbToLinear(h));
      float cbrtR2 = (float)Math.cbrt(0.41222146F * srgbToLinear(j) + 0.53633255F * srgbToLinear(k) + 0.051445995F * srgbToLinear(l));
      float cbrtG2 = (float)Math.cbrt(0.2119035F * srgbToLinear(j) + 0.6806995F * srgbToLinear(k) + 0.10739696F * srgbToLinear(l));
      float cbrtB2 = (float)Math.cbrt(0.08830246F * srgbToLinear(j) + 0.28171885F * srgbToLinear(k) + 0.6299787F * srgbToLinear(l));
      float r = cbrtR1 + (cbrtR2 - cbrtR1) * t;
      float g2 = cbrtG1 + (cbrtG2 - cbrtG1) * t;
      float b2 = cbrtB1 + (cbrtB2 - cbrtB1) * t;
      float rr = r * r * r;
      float gg = g2 * g2 * g2;
      float bb = b2 * b2 * b2;
      floats[offset] = srgbFromLinear(4.0767417F * rr - 3.3077116F * gg + 0.23096994F * bb);
      floats[offset + 1] = srgbFromLinear(-1.268438F * rr + 2.6097574F * gg - 0.34131938F * bb);
      floats[offset + 2] = srgbFromLinear(-0.0041960864F * rr - 0.7034186F * gg + 1.7076147F * bb);
   }

   private static float srgbToLinear(float value) {
      return value <= 0.04045F ? value / 12.92F : (float)Math.pow((value + 0.055F) / 1.055F, 2.4);
   }

   private static float srgbFromLinear(float value) {
      value = clamp01(value);
      return value <= 0.0031308F ? value * 12.92F : (float)(1.055 * Math.pow(value, 0.4166666666666667) - 0.055);
   }

   private static float sunPitch(int mode) {
      return mode == 1 ? -0.045F : (mode == 2 ? 0.13F : (mode == 3 ? 0.17F : 0.11F));
   }

   private static float clamp01(float value) {
      return clamp(value, 0.0F, 1.0F);
   }

   private static float clamp(float value, float min, float max) {
      return !Float.isFinite(value) ? min : Math.max(min, Math.min(max, value));
   }

   private static boolean valid(RenderTarget target) {
      return target != null
         && target.width > 0
         && target.height > 0
         && target.getColorTexture() != null
         && target.getColorTextureView() != null
         && target.getDepthTexture() != null
         && target.getDepthTextureView() != null;
   }

   public void release() {
      this.sceneCopy.release();
      this.uniforms.close();
   }
}

