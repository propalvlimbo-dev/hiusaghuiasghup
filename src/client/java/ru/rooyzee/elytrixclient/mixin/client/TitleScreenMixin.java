package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.ElytrixBrand;

/**
 * Главное меню: убираем ванильный логотип «MINECRAFT» и сплэш, вместо логотипа рисуем
 * свою «шапку» клиента ({@link ElytrixBrand}), а внизу — свой бейдж версии
 * вместо строки «Minecraft 26.2».
 *
 * <p>{@code require = 0}: если Mojang поменяет способ вызова — миксин просто ничего
 * не сделает (останется ваниль), игра не упадёт.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void elytrix$versionChip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!ElytrixclientClient.CONFIG.hackerBackground) {
            return;
        }
        ElytrixBrand.drawVersionChip(graphics, graphics.guiHeight(), 1.0F);
    }

    @Redirect(method = "extractRenderState", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"))
    private void elytrix$brandInsteadOfLogo(LogoRenderer renderer, GuiGraphicsExtractor graphics, int screenWidth,
                                            float alpha) {
        if (!ElytrixclientClient.CONFIG.hackerBackground) {
            renderer.extractRenderState(graphics, screenWidth, alpha);
            return;
        }
        ElytrixBrand.draw(graphics, screenWidth, alpha);
    }

    @Redirect(method = "extractRenderState", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"))
    private void elytrix$noSplash(SplashRenderer splash, GuiGraphicsExtractor graphics, int screenWidth, Font font,
                                  float alpha) {
        // сплэш «Also try Minecraft!» не показываем
    }
}
