package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventJump;

@Mixin(LivingEntity.class)
public abstract class JumpEventMixin {
    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void expensive$jump(CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player) {
            EventManager.call(new EventJump());
        }
    }
}
