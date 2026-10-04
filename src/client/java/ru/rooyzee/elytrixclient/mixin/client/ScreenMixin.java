package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.ElytrixBackground;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * «Хакерский» фон главного меню: вместо ванильной панорамы рисуем свой анимированный фон
 * (сетка + дождь символов), а панораму не рисуем.
 *
 * <p>{@code extractPanorama} объявлен в {@code Screen} (TitleScreen его просто вызывает),
 * поэтому инжектируемся именно в {@code Screen} и срабатываем только для {@link TitleScreen}.
 * Так не приходится угадывать дескриптор вызова внутри чужого метода.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void elytrix$hackerBackground(GuiGraphicsExtractor graphics, float a, CallbackInfo ci) {
        if (!((Object) this instanceof TitleScreen)) {
            return;
        }
        if (!ElytrixclientClient.CONFIG.hackerBackground) {
            return;
        }
        float alpha = 1.0F;
        ru.rooyzee.elytrixclient.client.ui.MenuBackgrounds.render(graphics, graphics.guiWidth(), graphics.guiHeight(),
                ElytrixBackground.time(), UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex), alpha);
        ci.cancel();
    }
}
