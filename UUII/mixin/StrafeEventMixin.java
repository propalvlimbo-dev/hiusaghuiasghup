package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventStrafe;
import wtf.expensive.client.events.impl.player.EventTravel;

@Mixin(Entity.class)
public abstract class StrafeEventMixin {
    @ModifyVariable(method = "moveRelative", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float expensive$travel(float speed) {
        if ((Object) this != Minecraft.getInstance().player) {
            return speed;
        }
        EventTravel event = new EventTravel(speed);
        EventManager.call(event);
        return event.speed;
    }

    @Redirect(method = "moveRelative",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float expensive$strafe(Entity self) {
        float yaw = self.getYRot();
        if (self != Minecraft.getInstance().player) {
            return yaw;
        }
        EventStrafe event = new EventStrafe(yaw);
        EventManager.call(event);
        return event.yaw;
    }
}
