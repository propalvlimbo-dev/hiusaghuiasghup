package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.input.MouseButtonEvent;
import ru.rooyzee.elytrixclient.client.ui.ElytrixMenuButtons;
import ru.rooyzee.elytrixclient.client.ui.MenuBgPicker;
import java.util.ArrayList;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.ElytrixBrand;

/**
 * Главное меню: убираем ванильный логотип «MINECRAFT» и сплэш, вместо логотипа рисуем
 * свою «шапку» клиента ({@link ElytrixBrand}). Никаких бейджей версии в углах экрана нет.
 *
 * <p>{@code require = 0}: если Mojang поменяет способ вызова — миксин просто ничего
 * не сделает (останется ваниль), игра не упадёт.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin() {
        super(null);
    }

    /** Убираем надпись копирайта внизу справа (это кликабельный PlainTextButton). */
    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void elytrix$noCopyright(CallbackInfo ci) {
        if (!ElytrixclientClient.CONFIG.hackerBackground) {
            return;
        }
        for (GuiEventListener child : new ArrayList<>(this.children())) {
            if (child instanceof PlainTextButton) {
                this.removeWidget(child);
            }
        }
        // кнопки — столбцом слева, в стиле клиента
        ElytrixMenuButtons.layout(this.children(), this.width, this.height);
        MenuBgPicker.close();
    }

    /** Иконка выбора фона справа сверху и её выпадающий список. */
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void elytrix$bgPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (ElytrixclientClient.CONFIG.hackerBackground) {
            MenuBgPicker.render(graphics, this.width, this.height, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$bgPickerClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ElytrixclientClient.CONFIG.hackerBackground && event.button() == 0
                && MenuBgPicker.click(event.x(), event.y(), this.width)) {
            cir.setReturnValue(true);
        }
    }

    /** Убираем строку версии «Minecraft 26.2 (модифицировано)» внизу слева. */
    @Redirect(method = "extractRenderState", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private void elytrix$noVersion(GuiGraphicsExtractor graphics, Font font, String str, int x, int y, int color) {
        if (!ElytrixclientClient.CONFIG.hackerBackground) {
            graphics.text(font, str, x, y, color);
        }
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
