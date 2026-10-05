package org.xrose.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.context.RotationContext;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.visual.SwingAnimationFeature;
import org.xrose.feature.impl.visual.ViewModelFeature;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = Shift.AFTER))
   private void applyViewModelOffset(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equippedProgress,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      ViewModelFeature viewModel = FeatureManager.INSTANCE.getEnabled(ViewModelFeature.class);
      if (viewModel != null && !stack.isEmpty() && (!player.isUsingItem() || player.getUsedItemHand() != hand)) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         poseStack.translate(viewModel.offsetX(arm), viewModel.offsetY(arm), viewModel.offsetZ(arm));
      }
   }

   @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
   private void customSwingArm(float swingProgress, PoseStack poseStack, int direction, HumanoidArm arm, CallbackInfo ci) {
      SwingAnimationFeature swing = FeatureManager.INSTANCE.getEnabled(SwingAnimationFeature.class);
      if (swing != null && arm == HumanoidArm.RIGHT) {
         poseStack.translate(0.56F, -0.52F, -0.72F);
         switch (swing.style()) {
            case SLICE: {
               float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               poseStack.mulPose(Axis.XP.rotationDegrees(wave * -80.0F));
               poseStack.mulPose(Axis.YP.rotationDegrees(wave * -30.0F));
               poseStack.mulPose(Axis.ZP.rotationDegrees(wave * 10.0F));
               break;
            }
            case SPIRAL:
               float angle = swingProgress * (float) Math.PI * 2.0F;
               poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(angle) * 60.0F));
               poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(angle * 0.5F) * -70.0F));
               break;
            case THRUST: {
               float wave = Mth.sin(swingProgress * (float) Math.PI);
               float shrink = 1.0F - wave * 0.15F;
               poseStack.translate(0.0F, 0.0F, -wave * 0.4F);
               poseStack.scale(shrink, shrink, 1.0F);
               poseStack.mulPose(Axis.XP.rotationDegrees(wave * -15.0F));
               break;
            }
            case SPEAR: {
               float wave = Mth.sin(swingProgress * (float) Math.PI);
               poseStack.mulPose(Axis.XP.rotationDegrees(-95.0F));
               poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F));
               poseStack.translate(0.0F, wave * 0.4F, 0.0F);
            }
         }

         ci.cancel();
      }
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot(F)F"))
   private float useFreeLookPitch(LocalPlayer player, float partialTick) {
      return RotationContext.isActive() ? RotationContext.getFreePitch() : player.getXRot(partialTick);
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"))
   private float useCameraPitch(LocalPlayer player, float partialTick) {
      return this.minecraft.gameRenderer.mainCamera().xRot();
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"))
   private float useCameraYaw(LocalPlayer player, float partialTick) {
      return this.minecraft.gameRenderer.mainCamera().yRot();
   }
}

