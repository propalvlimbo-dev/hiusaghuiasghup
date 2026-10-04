package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.player.ItemScroller;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;

@Mixin(AbstractContainerScreen.class)
public abstract class ItemScrollerMixin {
    @Shadow protected Slot hoveredSlot;

    @Unique private static long expensive$nextClick;

    @Inject(method = "tick", at = @At("TAIL"))
    private void expensive$scroll(CallbackInfo ci) {
        if (Managment.FUNCTION_MANAGER == null) return;
        Function function = Managment.FUNCTION_MANAGER.get("ItemScroller");
        if (!(function instanceof ItemScroller scroller) || !scroller.isState()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null) return;
        if (hoveredSlot == null || !hoveredSlot.isActive() || !hoveredSlot.hasItem()) return;

        long window = minecraft.getWindow().handle();
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) != GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) != GLFW_PRESS) return;

        long now = System.currentTimeMillis();
        if (now < expensive$nextClick) return;
        expensive$nextClick = now + scroller.delay.getValue().longValue();

        minecraft.gameMode.handleContainerInput(minecraft.player.containerMenu.containerId,
                hoveredSlot.index, 1, ContainerInput.QUICK_MOVE, minecraft.player);
    }
}
