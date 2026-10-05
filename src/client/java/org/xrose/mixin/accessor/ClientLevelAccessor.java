package org.xrose.mixin.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
   @Accessor("blockStatePredictionHandler")
   BlockStatePredictionHandler xrose$getBlockStatePredictionHandler();
}
