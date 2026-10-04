package wtf.expensive.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.NoRenderFunction;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    private static boolean hidden(int option) {
        if (Managment.FUNCTION_MANAGER == null) return false;
        Function function = Managment.FUNCTION_MANAGER.get("No Render");
        return function instanceof NoRenderFunction noRender && noRender.isState() && noRender.elements.get(option);
    }

    @Redirect(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"))
    private boolean expensive$fireOverlay(LocalPlayer player) {
        return !hidden(0) && player.isOnFire();
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    private void expensive$itemActivation(PoseStack pose, float partialTick,
                                           SubmitNodeCollector collector, CallbackInfo ci) {
        if (hidden(5)) ci.cancel();
    }
}
