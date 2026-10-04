package wtf.expensive.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void expensive$onSendChat(String message, CallbackInfo ci) {
        if (Managment.COMMAND_MANAGER == null) {
            return;
        }
        Managment.COMMAND_MANAGER.runCommands(message);
        if (Managment.COMMAND_MANAGER.isMessage) {
            ci.cancel();
        }
    }
}
