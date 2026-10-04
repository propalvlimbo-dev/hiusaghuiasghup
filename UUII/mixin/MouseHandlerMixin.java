package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.util.drag.DragManager;
import wtf.expensive.client.util.drag.Dragging;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.game.EventKey;
import wtf.expensive.client.events.impl.game.EventMouseTick;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void expensive$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (action == GLFW_PRESS) {
            EventManager.call(new EventMouseTick(info.button()));

            if (info.button() > 1 && Minecraft.getInstance().gui.screen() == null) {
                int bind = -100 + info.button();
                EventManager.call(new EventKey(bind));
                Expensive.onKeyPressed(bind);
            }
        }
        if (action != GLFW_RELEASE) {
            return;
        }
        for (Dragging drag : DragManager.draggables.values()) {
            drag.onRelease(info.button());
        }
        if (DragManager.dirty) {
            DragManager.dirty = false;
            DragManager.save();
        }
    }
}
