package wtf.expensive.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wtf.expensive.client.modules.impl.render.NameProtect;
import wtf.expensive.client.util.NameProtectUtil;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component expensive$protectName(Component message) {
        NameProtect protect = NameProtectUtil.active();
        if (message == null || protect == null) {
            return message;
        }
        Component patched = NameProtectUtil.patch(message, protect);
        return patched == null ? message : patched;
    }
}
