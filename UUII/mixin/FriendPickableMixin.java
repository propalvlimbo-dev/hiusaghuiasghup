package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;

@Mixin(Player.class)
public abstract class FriendPickableMixin {

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void expensive$skipFriends(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (player == Minecraft.getInstance().player
                || Managment.FUNCTION_MANAGER == null || Managment.FRIEND_MANAGER == null) {
            return;
        }
        Function function = Managment.FUNCTION_MANAGER.get("No Friend Damage");
        if (function != null && function.isState()
                && Managment.FRIEND_MANAGER.isFriend(player.getName().getString())) {
            cir.setReturnValue(false);
        }
    }
}
