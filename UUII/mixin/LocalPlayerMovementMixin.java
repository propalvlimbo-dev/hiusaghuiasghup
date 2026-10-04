package wtf.expensive.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMovementMixin {
    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void expensive$noSlow(CallbackInfoReturnable<Float> cir) {
        if (Managment.FUNCTION_MANAGER == null) return;
        Function function = Managment.FUNCTION_MANAGER.get("NoSlow");
        if (function != null && function.isState()) cir.setReturnValue(1.0f);
    }
}
