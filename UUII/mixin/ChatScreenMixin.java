package wtf.expensive.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.util.drag.DragManager;
import wtf.expensive.client.util.drag.Dragging;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void expensive$onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                    CallbackInfo ci) {
        boolean released = true;
        for (Dragging drag : DragManager.draggables.values()) {
            drag.onDraw(mouseX, mouseY, graphics.guiWidth(), graphics.guiHeight());
            if (drag.isDragging()) {
                released = false;
            }
        }
        if (released && DragManager.dirty) {
            DragManager.dirty = false;
            DragManager.save();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void expensive$onClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        for (Dragging drag : DragManager.draggables.values()) {
            drag.onClick(event.x(), event.y(), event.button());
            if (drag.isDragging()) {
                DragManager.dirty = true;
            }
        }
    }
}
