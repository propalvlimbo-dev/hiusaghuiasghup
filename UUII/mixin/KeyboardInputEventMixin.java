package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventInput;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputEventMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void expensive$input(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || player.input != (Object) this) {
            return;
        }

        ClientInput self = (ClientInput) (Object) this;
        Input keys = self.keyPresses;
        Vec2 move = self.getMoveVector();

        EventInput event = new EventInput(move.y, move.x, keys.jump(), keys.shift(),
                player.getAttributeValue(Attributes.SNEAKING_SPEED));
        EventManager.call(event);

        if (event.forward != move.y || event.strafe != move.x) {
            ((ClientInputAccessor) self).expensive$setMoveVector(new Vec2(event.strafe, event.forward));
        }
        if (event.jump != keys.jump() || event.sneak != keys.shift()) {
            self.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(),
                    keys.right(), event.jump, event.sneak, keys.sprint());
        }
    }
}
