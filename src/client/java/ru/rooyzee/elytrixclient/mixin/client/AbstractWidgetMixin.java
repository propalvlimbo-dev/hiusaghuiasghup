package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ui.ElytrixMenuButtons;
import ru.rooyzee.elytrixclient.client.ui.UiSound;

/** Щелчок кнопок главного меню — звуком клиента (набор и громкость — из настроек). */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {

    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$clickSound(SoundManager soundManager, CallbackInfo ci) {
        if ((Object) this instanceof AbstractButton && ElytrixMenuButtons.active()) {
            UiSound.play(UiSound.Event.CLICK);
            ci.cancel();
        }
    }
}
