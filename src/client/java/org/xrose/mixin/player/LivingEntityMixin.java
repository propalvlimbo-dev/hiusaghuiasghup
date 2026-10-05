package org.xrose.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventManager;
import org.xrose.event.Events;
import org.xrose.event.events.game.PlayerJumpEvent;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.visual.RemovalsFeature;
import org.xrose.feature.impl.visual.SwingAnimationFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConnection;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
   @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"), require = 0)
   private float xrose$jumpYaw(float original) {
      if ((Object)this == MinecraftContext.mc.player && AngleConnection.INSTANCE.isMoveCorrectionActive()) {
         Angle angle = AngleConnection.INSTANCE.getMoveRotation();
         if (angle != null) {
            return angle.getYaw();
         }
      }

      return original;
   }

   @Inject(method = "jumpFromGround", at = @At("HEAD"))
   private void onPlayerJump(CallbackInfo ci) {
      LocalPlayer player = MinecraftContext.mc.player;
      if (player != null && (Object)this == player && EventManager.hasListeners(PlayerJumpEvent.class)) {
         EventManager.call(Events.PLAYER_JUMP.set(player, player.position()));
      }
   }

   @Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
   private void onGetEffectBlendFactor(Holder<MobEffect> effect, float partialTick, CallbackInfoReturnable<Float> cir) {
      if ((Object)this == MinecraftContext.mc.player && RemovalsFeature.shouldRemoveBadEffectsVisuals()) {
         cir.setReturnValue(0.0F);
      }
   }

   @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
   private void customSwingDuration(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this == MinecraftContext.mc.player) {
         SwingAnimationFeature swing = FeatureManager.INSTANCE.getEnabled(SwingAnimationFeature.class);
         if (swing != null) {
            cir.setReturnValue(swing.swingDurationTicks());
         }
      }
   }
}

