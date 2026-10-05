package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
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

    /** Убираем надпись копирайта внизу справа и маленькие иконки (язык и т.д.). */
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void elytrix$accountButtons(GuiGraphicsExtractor g, int mx, int my, float a, CallbackInfo ci) {
        if (!ElytrixMenuButtons.active()) {
            return;
        }
        int left = ElytrixMenuButtons.left();
        int y = ElytrixMenuButtons.stackBottom() + 6;
        elytrix$drawItem(g, left, y, "Аккаунты", mx, my);
        elytrix$drawItem(g, left, y + ElytrixMenuButtons.ITEM_H + 3, "Сеть", mx, my);
    }

    private void elytrix$drawItem(GuiGraphicsExtractor g, int x, int y, String label, int mx, int my) {
        boolean hov = mx >= x && mx <= x + ElytrixMenuButtons.ITEM_W && my >= y && my <= y + ElytrixMenuButtons.ITEM_H;
        ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector.roundRect(g, x, y,
                ElytrixMenuButtons.ITEM_W, ElytrixMenuButtons.ITEM_H, 6f, hov ? 0x33FFFFFF : 0x14FFFFFF);
        ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer.draw(
                g, ru.rooyzee.elytrixclient.client.render.font.Fonts.REGULAR, label, x + 8, y + 6, 8f,
                hov ? 0xFFFFFFFF : 0xCCFFFFFF);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$accountClick(MouseButtonEvent event, boolean doubleClick,
                                      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!ElytrixMenuButtons.active()) {
            return;
        }
        int mx = (int) event.x();
        int my = (int) event.y();
        int left = ElytrixMenuButtons.left();
        int y = ElytrixMenuButtons.stackBottom() + 6;
        boolean inAcc = mx >= left && mx <= left + ElytrixMenuButtons.ITEM_W && my >= y && my <= y + ElytrixMenuButtons.ITEM_H;
        boolean inNet = mx >= left && mx <= left + ElytrixMenuButtons.ITEM_W
                && my >= y + ElytrixMenuButtons.ITEM_H + 3 && my <= y + 2 * ElytrixMenuButtons.ITEM_H + 3;
        if (inAcc) {
            cir.setReturnValue(true);
            this.minecraft.gui.setScreen(new platform.client.ui.screen.AltScreen());
        } else if (inNet) {
            cir.setReturnValue(true);
            this.minecraft.gui.setScreen(new ru.rooyzee.elytrixclient.client.ui.NetworkScreen(
                    (net.minecraft.client.gui.screens.Screen) (Object) this));
        }
    }

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
        // НЕ удаляем SpriteIconButton — layout() сам спрячет их на -9999
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
