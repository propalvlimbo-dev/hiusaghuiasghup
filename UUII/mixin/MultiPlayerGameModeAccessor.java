package wtf.expensive.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyDelay") void expensive$setDestroyDelay(int value);
    @Accessor("destroyProgress") float expensive$getDestroyProgress();
    @Accessor("destroyProgress") void expensive$setDestroyProgress(float value);
}
