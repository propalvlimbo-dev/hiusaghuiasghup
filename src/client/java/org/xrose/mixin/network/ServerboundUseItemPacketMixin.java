package org.xrose.mixin.network;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.utils.combat.aura.AngleConnection;

@Mixin(ServerboundUseItemPacket.class)
public abstract class ServerboundUseItemPacketMixin {
   @Mutable
   @Shadow
   @Final
   private float yRot;
   @Mutable
   @Shadow
   @Final
   private float xRot;

   @Inject(method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V", at = @At("RETURN"))
   private void xrose$modifyUseItemRotation(InteractionHand hand, int sequence, float yaw, float pitch, CallbackInfo ci) {
      AngleConnection controller = AngleConnection.INSTANCE;
      if (controller.shouldApplyPacketRotation()) {
         this.yRot = controller.getPacketYaw();
         this.xRot = controller.getPacketPitch();
      }
   }
}

