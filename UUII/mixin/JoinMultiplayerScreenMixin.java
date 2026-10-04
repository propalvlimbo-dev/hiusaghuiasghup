package wtf.expensive.client.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.ui.proxy.ProxyScreen;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin() {
        super(Component.empty());
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void expensive$addProxyButton(CallbackInfo ci) {
        Screen current = (Screen) (Object) this;
        addRenderableWidget(Button.builder(
                Component.literal("Proxy"),
                button -> minecraft.gui.setScreen(new ProxyScreen(current))
        ).bounds(5, 5, 98, 20).build());
    }
}
