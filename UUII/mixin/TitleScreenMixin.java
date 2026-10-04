package wtf.expensive.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.ui.menu.MainMenuOverlay;
import wtf.expensive.client.util.ClientUtil;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final MainMenuOverlay expensive$overlay = new MainMenuOverlay();

    protected TitleScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void expensive$clearVanillaWidgets(CallbackInfo ci) {
        if (ClientUtil.legitMode) {
            return;
        }
        this.clearWidgets();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void expensive$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                  CallbackInfo ci) {
        if (ClientUtil.legitMode) {
            return;
        }
        expensive$overlay.render(graphics, this, mouseX, mouseY);
        ci.cancel();
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void expensive$background(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                      CallbackInfo ci) {
        if (ClientUtil.legitMode) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void expensive$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ClientUtil.legitMode) {
            return;
        }
        if (expensive$overlay.click(this, event.x(), event.y(), width, height)) {
            cir.setReturnValue(true);
        }
    }
}
