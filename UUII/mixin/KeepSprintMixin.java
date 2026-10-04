package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.impl.movement.SprintFunction;

@Mixin(Player.class)
public abstract class KeepSprintMixin {
    @Redirect(method = "causeExtraKnockback",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void expensive$keepSprint(Player player, boolean sprinting) {
        if (!sprinting && player == Minecraft.getInstance().player
                && Managment.FUNCTION_MANAGER != null) {
            var sprint = Managment.FUNCTION_MANAGER.get("Sprint");
            if (sprint instanceof SprintFunction function
                    && function.isState() && function.keepSprint.get()) {
                return;
            }
        }
        player.setSprinting(sprinting);
    }
}
