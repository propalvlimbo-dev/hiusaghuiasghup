package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.ESPFunction;

@Mixin(LivingEntityRenderer.class)
public abstract class NameTagMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void expensive$hideNameTag(LivingEntity entity, double distance,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player) || entity == Minecraft.getInstance().player
                || Managment.FUNCTION_MANAGER == null) {
            return;
        }
        Function function = Managment.FUNCTION_MANAGER.get("ESP");

        if (function instanceof ESPFunction esp && esp.isState() && esp.elements.get(3)) {
            cir.setReturnValue(false);
        }
    }
}
