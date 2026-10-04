package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventWindowClick;

@Mixin(MultiPlayerGameMode.class)
public abstract class WindowClickEventMixin {
    @Inject(method = "handleContainerInput", at = @At("HEAD"))
    private void expensive$clickPre(int containerId, int slot, int button, ContainerInput input,
                                    Player player, CallbackInfo ci) {
        expensive$fire(containerId, slot, button, player, EventWindowClick.ClickStage.PRE);
    }

    @Inject(method = "handleContainerInput", at = @At("RETURN"))
    private void expensive$clickPost(int containerId, int slot, int button, ContainerInput input,
                                     Player player, CallbackInfo ci) {
        expensive$fire(containerId, slot, button, player, EventWindowClick.ClickStage.POST);
    }

    private static void expensive$fire(int containerId, int slot, int button, Player player,
                                       EventWindowClick.ClickStage stage) {
        if (player != Minecraft.getInstance().player) {
            return;
        }
        ClickAction action = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
        EventManager.call(new EventWindowClick(containerId, slot, button, action, stage));
    }
}
