package platform.inject.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URL;

@Mixin(targets = "net.fabricmc.loader.impl.launch.knot.KnotClassLoader")
public abstract class KnotClassLoaderMixin {

    @Inject(method = "defineClassFwd", at = @At("HEAD"))
    private void fixUniformInterface(String name, byte[] b, int off, int len, URL cs, CallbackInfoReturnable<?> cir) {
        if (b != null && b.length > 9) {
            int flags = ((b[8] & 0xFF) << 8) | (b[9] & 0xFF);
            if (name != null && name.startsWith("com.mojang.blaze3d.opengl.Uniform")) {
                if ((flags & 0x0200) == 0) {
                    flags |= 0x0200;
                    b[8] = (byte) (flags >> 8);
                    b[9] = (byte) (flags & 0xFF);
                }
            }
        }
    }
}

