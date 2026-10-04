package wtf.expensive.client.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.packet.EventPacket;
import wtf.expensive.client.managment.Managment;

@Mixin(Connection.class)
public abstract class ConnectionPacketMixin {
    @Unique
    private boolean expensive$isClientConnection() {
        if (Managment.FUNCTION_MANAGER == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null
                && minecraft.getConnection().getConnection() == (Object) this;
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void expensive$send(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (!expensive$isClientConnection()) return;
        EventPacket event = new EventPacket(packet, EventPacket.PacketType.SEND);
        EventManager.call(event);
        if (event.isCancel()) ci.cancel();
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void expensive$receive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (!expensive$isClientConnection()) return;
        EventPacket event = new EventPacket(packet, EventPacket.PacketType.RECEIVE);
        EventManager.call(event);
        if (event.isCancel()) ci.cancel();
    }
}
