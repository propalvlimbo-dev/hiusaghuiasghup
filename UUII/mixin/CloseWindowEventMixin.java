package wtf.expensive.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventCloseWindow;

@Mixin(LocalPlayer.class)
public abstract class CloseWindowEventMixin {
    @Inject(method = "closeContainer", at = @At("HEAD"))
    private void expensive$closeWindow(CallbackInfo ci) {
        EventManager.call(new EventCloseWindow());
    }
}
