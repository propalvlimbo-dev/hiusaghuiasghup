package wtf.expensive.client.mixin;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.modules.impl.render.NameProtect;
import wtf.expensive.client.util.NameProtectUtil;

@Mixin(MutableComponent.class)
public abstract class ComponentMixin {
    private static final ThreadLocal<Boolean> EXPENSIVE$PATCHING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "getVisualOrderText", at = @At("HEAD"), cancellable = true)
    private void expensive$protectName(CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (EXPENSIVE$PATCHING.get()) {
            return;
        }
        NameProtect protect = NameProtectUtil.active();
        if (protect == null) {
            return;
        }
        EXPENSIVE$PATCHING.set(Boolean.TRUE);
        try {
            Component patched = NameProtectUtil.patch((Component) (Object) this, protect);
            if (patched != null) {
                cir.setReturnValue(patched.getVisualOrderText());
            }
        } finally {
            EXPENSIVE$PATCHING.set(Boolean.FALSE);
        }
    }
}
