package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.music.CustomMusic;

/** Пока играет своя музыка из папки — ванильная не запускается. */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$customMusic(CallbackInfo ci) {
        if (CustomMusic.active()) {
            ci.cancel();
        }
    }
}
