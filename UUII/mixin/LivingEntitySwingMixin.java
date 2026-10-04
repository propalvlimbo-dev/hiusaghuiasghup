package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.impl.render.SwingAnimationFunction;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {
    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void expensive$swingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != Minecraft.getInstance().player || Managment.FUNCTION_MANAGER == null) {
            return;
        }
        var function = Managment.FUNCTION_MANAGER.get("Swing Animation");
        if (function instanceof SwingAnimationFunction animation && animation.isState()) {
            cir.setReturnValue(Math.max(1, animation.smooth.getValue().intValue()));
        }
    }
}
