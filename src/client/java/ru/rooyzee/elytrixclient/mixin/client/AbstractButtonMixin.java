package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ui.ElytrixMenuButtons;

/**
 * Кнопки главного меню — в стиле клиента ({@link ElytrixMenuButtons}).
 * SpriteIconButton (язык, доступность и т.д.) — полностью пропускаются.
 */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {

    @Unique
    private static GuiGraphicsExtractor elytrix$graphics;

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$background(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!ElytrixMenuButtons.active()) {
            elytrix$graphics = null;
            return;
        }
        AbstractButton self = (AbstractButton) (Object) this;
        // Полностью пропусаем маленькие иконки (язык, доступность и т.д.)
        if (self instanceof SpriteIconButton) {
            elytrix$graphics = null;
            ci.cancel();
            return;
        }
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
        AbstractButton self = (AbstractButton) (Object) this;
        // Пропусаем подпись для иконок if (self instanceof SpriteIconButton) {
            ci.cancel();
            return;
        }
        ElytrixMenuButtons.label(g, self);
        ci.cancel();
    }
}