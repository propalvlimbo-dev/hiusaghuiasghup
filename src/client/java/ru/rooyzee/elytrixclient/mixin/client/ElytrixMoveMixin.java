package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ui.ElytrixScreen;

/** Ходьба (WASD/прыжок/красться) при открытом нашем меню. */
@Mixin(KeyboardInput.class)
public abstract class ElytrixMoveMixin {

    @Inject(method = {"tick"}, at = {@At("RETURN")})
    private void elytrix$moveInMenu(CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.options == null || !(mc.gui.screen() instanceof ElytrixScreen)) {
                return;
            }
            KeyboardInput input = (KeyboardInput) (Object) this;
            boolean f = mc.options.keyUp.isDown();
            boolean b = mc.options.keyDown.isDown();
            boolean l = mc.options.keyLeft.isDown();
            boolean r = mc.options.keyRight.isDown();
            boolean jump = mc.options.keyJump.isDown();
            boolean shift = mc.options.keyShift.isDown();
            boolean sprint = input.keyPresses.sprint();
            input.keyPresses = new Input(f, b, l, r, jump, shift, sprint);
            ((platform.inject.accessors.ClientInputAccessor) input)
                    .setMoveVector(new Vec2((l ? 1f : 0f) - (r ? 1f : 0f), (f ? 1f : 0f) - (b ? 1f : 0f)).normalized());
        } catch (Throwable ignored) {
        }
    }
}
