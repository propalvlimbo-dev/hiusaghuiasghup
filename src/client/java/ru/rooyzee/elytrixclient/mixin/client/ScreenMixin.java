package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.ElytrixBackground;
import ru.rooyzee.elytrixclient.client.ui.ElytrixScreen;
import ru.rooyzee.elytrixclient.client.ui.MusicIsland;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * «Хакерский» фон главного меню + «Динамический островок» на всех экранах.
 * Клик по островку обрабатывается глобально в ElytrixclientClient (ClientTickEvents).
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow protected int width;
    @Shadow protected int height;

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void elytrix$hackerBackground(GuiGraphicsExtractor graphics, float a, CallbackInfo ci) {
        if (!((Object) this instanceof TitleScreen)) return;
        if (!ElytrixclientClient.CONFIG.hackerBackground) return;
        ru.rooyzee.elytrixclient.client.ui.MenuBackgrounds.render(graphics, graphics.guiWidth(), graphics.guiHeight(),
                ElytrixBackground.time(), UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex), 1.0F);
        ci.cancel();
    }

    /** Динамический островок — на всех экранах кроме панели Elytrix. */
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void elytrix$musicIsland(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!((Object) this instanceof ElytrixScreen)) {
            MusicIsland.render(graphics, this.width, this.height, mouseX, mouseY);
        }
    }
}