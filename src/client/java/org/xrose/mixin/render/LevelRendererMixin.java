package org.xrose.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.feature.impl.visual.BlockOutlineFeature;
import org.xrose.feature.impl.visual.ChamsFeature;
import org.xrose.utils.render.EntityEspStateCache;
import org.xrose.utils.render.chams.ChamsTargetMatcher;
import org.xrose.utils.render.world.WorldEffectContext;
import org.xrose.utils.render.world.WorldEffects;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
   @Shadow
   @Final
   private LevelRenderState levelRenderState;

   @Inject(method = "submitEntities", at = @At("HEAD"))
   private void captureEntityEspStates(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
      EntityEspStateCache.capture(levelRenderState.entityRenderStates);
      ChamsFeature chams = ChamsFeature.getEnabled();
      if (chams != null && !chams.keepsOriginalModel() && !levelRenderState.entityRenderStates.isEmpty()) {
         List<Entity> targets = ChamsTargetMatcher.collectTargets(Minecraft.getInstance(), chams);
         if (!targets.isEmpty()) {
            levelRenderState.entityRenderStates.removeIf(state -> ChamsTargetMatcher.matchingTarget(state, targets) != null);
         }
      }
   }

   @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
   private void replaceVanillaBlockOutline(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState, CallbackInfo ci) {
      BlockOutlineFeature feature = BlockOutlineFeature.getEnabled();
      if (feature != null && feature.usesShader()) {
         ci.cancel();
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void renderWorldEffects(
      GraphicsResourceAllocator graphicsResourceAllocator,
      DeltaTracker deltaTracker,
      boolean renderBlockOutline,
      CameraRenderState cameraRenderState,
      Matrix4fc projectionMatrix,
      GpuBufferSlice fogParameters,
      Vector4f skyColor,
      boolean hasCapturedFrustum,
      CallbackInfo ci
   ) {
      WorldEffects.render(new WorldEffectContext(this.levelRenderState, cameraRenderState, deltaTracker.getGameTimeDeltaPartialTick(false)));
   }
}

