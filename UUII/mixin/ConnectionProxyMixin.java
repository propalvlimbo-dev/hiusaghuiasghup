package wtf.expensive.client.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.proxy.ProxyHandler;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wtf.expensive.client.util.proxy.ProxyManager;

@Mixin(Connection.class)
public abstract class ConnectionProxyMixin {
    @ModifyArg(
            method = "connect",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/AbstractBootstrap;"
            ),
            index = 0
    )
    private static ChannelHandler expensive$installProxy(ChannelHandler minecraftInitializer) {
        ProxyHandler proxy = ProxyManager.createHandler();
        if (proxy == null) return minecraftInitializer;

        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast("expensive_proxy", proxy);
                channel.pipeline().addLast("expensive_minecraft", minecraftInitializer);
            }
        };
    }
}
