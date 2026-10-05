package org.xrose.mixin.accessor;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface MovePlayerPacketAccessor {
   @Accessor("onGround")
   boolean isOnGroundFlag();

   @Accessor("onGround")
   void setOnGroundFlag(boolean var1);
}
