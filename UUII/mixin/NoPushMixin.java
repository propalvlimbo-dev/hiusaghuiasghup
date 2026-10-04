package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.player.NoPushFunction;

@Mixin(Entity.class)
public abstract class NoPushMixin {
    private NoPushFunction expensive$module() {
        if (Managment.FUNCTION_MANAGER == null || (Object) this != Minecraft.getInstance().player) return null;
        Function function = Managment.FUNCTION_MANAGER.get("NoPush");
        return function instanceof NoPushFunction noPush && noPush.isState() ? noPush : null;
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void expensive$players(Entity entity, CallbackInfo ci) {
        NoPushFunction module = expensive$module();
        if (module != null && module.types.get(0)) ci.cancel();
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void expensive$blocks(double x, double y, double z, CallbackInfo ci) {
        NoPushFunction module = expensive$module();
        if (module != null && module.types.get(1)) ci.cancel();
    }

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void expensive$fluid(CallbackInfoReturnable<Boolean> cir) {
        NoPushFunction module = expensive$module();
        if (module != null && module.types.get(2)) cir.setReturnValue(false);
    }
}
