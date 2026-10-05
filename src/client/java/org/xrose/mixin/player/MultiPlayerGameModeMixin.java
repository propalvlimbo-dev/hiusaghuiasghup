package org.xrose.mixin.player;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.context.MinecraftContext;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.impl.combat.CriticalsFeature;
import org.xrose.feature.impl.visual.HitParticlesFeature;
import org.xrose.feature.impl.visual.KillEffectFeature;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
   @Inject(method = "attack", at = @At("HEAD"))
   private void onAttack(Player player, Entity target, CallbackInfo ci) {
      if (player == MinecraftContext.mc.player) {
         HitParticlesFeature hitParticles = HitParticlesFeature.getEnabled();
         if (hitParticles != null) {
            hitParticles.onAttack(target);
         }

         KillEffectFeature killEffect = KillEffectFeature.getEnabled();
         if (killEffect != null) {
            killEffect.onAttack(target);
         }

         CriticalsFeature criticals = CriticalsFeature.getEnabled();
         if (criticals != null) {
            criticals.onAttack();
         }
      }
   }

   @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
   private void onCancelShieldUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      AuraFeature aura = AuraFeature.getInstance();
      if (aura != null && aura.shouldCancelInteractItem(hand)) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }

   @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
   private void onCancelUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
      AuraFeature aura = AuraFeature.getInstance();
      if (aura != null && aura.shouldCancelInteractBlock()) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }

   @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
   private void onCancelInteract(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      AuraFeature aura = AuraFeature.getInstance();
      if (aura != null && aura.shouldCancelEntityInteraction()) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }
}

