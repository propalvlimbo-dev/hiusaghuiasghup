package wtf.expensive.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventAction;

@Mixin(LocalPlayer.class)
public abstract class ActionEventMixin {
    @Redirect(method = "sendIsSprintingIfNeeded",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z"))
    private boolean expensive$action(LocalPlayer self) {
        EventAction event = new EventAction(self.isSprinting());
        EventManager.call(event);
        return event.isSprintState();
    }
}
