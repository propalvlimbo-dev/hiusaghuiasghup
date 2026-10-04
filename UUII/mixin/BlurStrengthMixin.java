package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.impl.render.ClickGuiFunction;
import wtf.expensive.client.ui.clickgui.ClickGui;

@Mixin(Options.class)
public abstract class BlurStrengthMixin {

    @Inject(method = "getMenuBackgroundBlurriness", at = @At("HEAD"), cancellable = true)
    private void expensive$clickGuiBlur(CallbackInfoReturnable<Integer> cir) {
        if (!(Minecraft.getInstance().gui.screen() instanceof ClickGui)
                || Managment.FUNCTION_MANAGER == null) {
            return;
        }
        if (Managment.FUNCTION_MANAGER.get("Click Gui") instanceof ClickGuiFunction settings
                && settings.blur.get()) {
            cir.setReturnValue(settings.blurValue.getValue().intValue());
        }
    }
}
