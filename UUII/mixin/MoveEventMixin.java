package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventMove;

@Mixin(Entity.class)
public abstract class MoveEventMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void expensive$move(MoverType type, Vec3 motion, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self != Minecraft.getInstance().player) {
            return;
        }
        Vec3 from = self.position();
        EventMove event = new EventMove(from, from.add(motion), motion);
        EventManager.call(event);
        if (event.isCancel()) {
            self.setPos(from.x + event.motion.x, from.y + event.motion.y, from.z + event.motion.z);
            ci.cancel();
        }
    }
}
