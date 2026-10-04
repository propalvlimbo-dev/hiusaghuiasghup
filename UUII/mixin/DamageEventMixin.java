package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventDamage;

@Mixin(LivingEntity.class)
public abstract class DamageEventMixin {
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void expensive$damage(DamageSource source, CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        EventDamage.DamageType type;
        if (source.is(DamageTypes.FALL)) {
            type = EventDamage.DamageType.FALL;
        } else if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            type = EventDamage.DamageType.EXPLOSION;
        } else {
            type = EventDamage.DamageType.OTHER;
        }
        EventManager.call(new EventDamage(type));
    }
}
