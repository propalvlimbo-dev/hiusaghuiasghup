package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ui.ElytrixMenuButtons;

/**
 * Кнопки главного меню — в стиле клиента ({@link ElytrixMenuButtons}).
 * Подложка рисуется вместо ванильного спрайта, подпись — вместо ванильного текста
 * (иконки у маленьких кнопок, например «язык», остаются). Щелчок — звук клиента.
 */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {

    /** Графика текущей кнопки: подпись рисуется сразу после подложки в том же кадре. */
    @Unique
    private static GuiGraphicsExtractor elytrix$graphics;

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$background(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!ElytrixMenuButtons.active()) {
            elytrix$graphics = null;
            return;
        }
        AbstractButton self = (AbstractButton) (Object) this;
        if (!self.visible) {
            elytrix$graphics = null;
            ci.cancel();
            return;
        }
        ElytrixMenuButtons.background(graphics, self);
        elytrix$graphics = graphics;
        ci.cancel();
    }

    @Inject(method = "extractDefaultLabel", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$label(ActiveTextCollector output, CallbackInfo ci) {
        GuiGraphicsExtractor g = elytrix$graphics;
        if (g == null || !ElytrixMenuButtons.active()) {
            return;
        }
        ElytrixMenuButtons.label(g, (AbstractButton) (Object) this);
        ci.cancel();
    }
}
