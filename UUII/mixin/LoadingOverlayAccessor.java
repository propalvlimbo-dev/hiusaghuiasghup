package wtf.expensive.client.mixin;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LoadingOverlay.class)
public interface LoadingOverlayAccessor {
    @Accessor("fadeOutStart")
    long expensive$fadeOutStart();

    @Accessor("currentProgress")
    float expensive$currentProgress();

    @Accessor("fadeInStart")
    long expensive$fadeInStart();

    @Accessor("fadeInStart")
    void expensive$setFadeInStart(long value);
}
