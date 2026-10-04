package wtf.expensive.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventMotion;
import wtf.expensive.client.events.impl.player.EventUpdate;

@Mixin(LocalPlayer.class)
public abstract class MovementEventMixin {
    private float expensive$yaw;
    private float expensive$pitch;
    private boolean expensive$spoof;

    @Inject(method = "tick", at = @At("HEAD"))
    private void expensive$update(CallbackInfo ci) {
        if (((LocalPlayer) (Object) this).level() != null) {
            EventManager.call(new EventUpdate());
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void expensive$motion(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        EventMotion event = new EventMotion(player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.onGround());
        EventManager.call(event);
        expensive$yaw = event.getYaw();
        expensive$pitch = event.getPitch();
        expensive$spoof = event.getYaw() != player.getYRot() || event.getPitch() != player.getXRot();
    }

    @Redirect(method = "sendPosition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float expensive$redirectYaw(LocalPlayer player) {
        return expensive$spoof ? expensive$yaw : player.getYRot();
    }

    @Redirect(method = "sendPosition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float expensive$redirectPitch(LocalPlayer player) {
        return expensive$spoof ? expensive$pitch : player.getXRot();
    }
}
