package wtf.expensive.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.game.EventKey;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void expensive$onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != GLFW_PRESS) {
            return;
        }

        if (Minecraft.getInstance().gui.screen() == null) {
            EventManager.call(new EventKey(event.key()));
        }
        Expensive.onKeyPressed(event.key());
    }
}
