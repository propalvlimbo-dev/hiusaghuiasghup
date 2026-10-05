package org.xrose.mixin.gui;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.context.RenderContext;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private ReloadInstance reload;
   @Shadow
   @Final
   private Consumer<Optional<Throwable>> onFinish;
   @Shadow
   @Final
   private boolean fadeIn;
   @Shadow
   private float currentProgress;
   @Shadow
   private long fadeOutStart;
   @Shadow
   private long fadeInStart;
   @Unique
   private long lastTime = -1L;
   @Unique
   private float animTime = 0.0F;

   @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
   private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ci.cancel();
      if (Thread.currentThread().getPriority() != 10) {
         try {
            Thread.currentThread().setPriority(10);
         } catch (Throwable var40) {
         }
      }

      int width = this.minecraft.getWindow().getGuiScaledWidth();
      int height = this.minecraft.getWindow().getGuiScaledHeight();
      long currentTime = Util.getMillis();
      if (this.fadeIn && this.fadeInStart == -1L) {
         this.fadeInStart = currentTime;
      }

      if (RenderContext.overlayStartTime == -1L) {
         RenderContext.overlayStartTime = currentTime;
      }

      long elapsed = currentTime - RenderContext.overlayStartTime;
      boolean isStartup = this.minecraft.gui.screen() == null;
      long animStartDelay = isStartup ? 1200L : 0L;
      if (this.lastTime == -1L) {
         this.lastTime = currentTime;
      }

      float deltaTime = (float)(currentTime - this.lastTime) / 1000.0F;
      this.lastTime = currentTime;
      float progressDelta = Math.min(deltaTime, 0.03F);
      if (elapsed >= animStartDelay) {
         this.animTime += progressDelta;
      }

      float targetProgress;
      if (elapsed < animStartDelay) {
         targetProgress = 0.0F;
      } else if (!this.reload.isDone()) {
         float progressTime = Math.max(0.0F, (float)(elapsed - animStartDelay) / 1000.0F);
         float simulated = Mth.clamp(progressTime * 0.51F, 0.0F, 0.92F);
         targetProgress = Math.max(simulated, this.reload.getActualProgress() * 0.92F);
      } else {
         targetProgress = 1.0F;
      }

      float catchUpSpeed = !this.reload.isDone() ? 0.2F : 0.5F;
      if (this.currentProgress < targetProgress) {
         this.currentProgress = Math.min(this.currentProgress + catchUpSpeed * progressDelta, targetProgress);
      } else {
         this.currentProgress = Mth.clamp(this.currentProgress * 0.98F + targetProgress * 0.02F, 0.0F, 1.0F);
      }

      float fadeOutProgress = this.fadeOutStart > -1L ? (float)(currentTime - this.fadeOutStart) / 1500.0F : 0.0F;
      if (fadeOutProgress >= 1.0F) {
         this.minecraft.gui.setOverlay(null);
         RenderContext.overlayStartTime = -1L;
      } else {
         if (this.reload.isDone() && this.minecraft.gui.screen() != null) {
            this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(guiGraphicsExtractor, mouseX, mouseY, partialTick);
         }

         float alpha = 1.0F;
         if (this.fadeOutStart > -1L) {
            alpha = Mth.clamp(1.0F - fadeOutProgress, 0.0F, 1.0F);
         } else if (this.fadeIn && this.fadeInStart > -1L) {
            float fadeInProgress = (float)(currentTime - this.fadeInStart) / 1000.0F;
            alpha = Mth.clamp(fadeInProgress, 0.0F, 1.0F);
         }

         int bgAlpha = Math.round(255.0F * alpha);
         int bgCol = ColorUtil.rgba(21, 21, 22, bgAlpha);
         RenderContext.enter2D(null, guiGraphicsExtractor, null);

         try {
            Render2DUtil.beginFrame();
            float centerX = width / 2.0F;
            float centerY = height / 2.0F;
            Render2DUtil.rect(0.0F, 0.0F, width, height).color(bgCol).draw();
            int accentColor = Theme.getAccent();
            float barWidth = 240.0F;
            float barHeight = 5.0F;
            float barX = (width - barWidth) / 2.0F;
            float barY = centerY - barHeight / 2.0F;
            if (this.fadeOutStart > -1L) {
               barY += 18.0F * fadeOutProgress;
            }

            int trackColor = ColorUtil.rgba(38, 38, 45, Math.round(255.0F * alpha));
            int borderColor = ColorUtil.rgba(255, 255, 255, Math.round(12.75F * alpha));
            Render2DUtil.rect(barX, barY, barWidth, barHeight).color(trackColor).radius(barHeight).border(0.5F, borderColor).draw();
            int accentFadeColor = ColorUtil.withAlpha(accentColor, Math.round(255.0F * alpha));
            float fillWidth = barWidth * this.currentProgress;
            if (fillWidth > 0.0F) {
               Render2DUtil.rect(barX, barY, fillWidth, barHeight).color(accentFadeColor).radius(barHeight).draw();
            }

            String statusText;
            if (this.reload.isDone()) {
               statusText = "Almost done...";
            } else {
               statusText = "Loading";
               int dots = (int)(currentTime / 400L) % 4;

               for (int i = 0; i < dots; i++) {
                  statusText = statusText + ".";
               }
            }

            int statusColor = ColorUtil.rgba(180, 182, 190, Math.round(255.0F * alpha));
            Render2DUtil.text(barX, barY + barHeight + 18.0F, 12.0F, statusText).color(statusColor).draw();
            String percentText = Math.round(this.currentProgress * 100.0F) + "%";
            Render2DUtil.text(barX + barWidth + 14.0F, barY - 2.0F, 12.0F, percentText).color(Theme.Colors.SECONDARY).draw();
            Render2DUtil.flush();
         } finally {
            RenderContext.exit2D();
         }
      }
   }
}

