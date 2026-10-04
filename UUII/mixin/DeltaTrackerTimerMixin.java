package wtf.expensive.client.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.expensive.client.util.movement.ClientTimerController;

@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {
    @Redirect(method = "advanceGameTime", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
    private float expensive$timerSpeed(FloatUnaryOperator provider, float baseMspt) {
        return provider.apply(baseMspt) / ClientTimerController.speed();
    }
}
