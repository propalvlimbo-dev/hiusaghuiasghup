package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.ElytrixLoader;

/**
 * Подменяет ванильный экран загрузки (красный фон и логотип Mojang) на свой —
 * {@link ElytrixLoader}. Работает всё время загрузки ресурсов, а не только
 * после показа главного меню.
 *
 * <p>Важно: ванильный {@code extractRenderState} не только рисует, но и обновляет
 * «сглаженный» прогресс и снимает оверлей по завершении ({@code gui.setOverlay(null)}) —
 * поэтому его логику мы повторяем здесь, а не просто отменяем.
 *
 * <p>Если в настройках выключить «свой экран загрузки», метод не отменяется и
 * показывается ванильный лоадер.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    private float currentProgress;

    @Shadow
    private long fadeOutStart;

    @Shadow
    private long fadeInStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void elytrix$renderCustomLoader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a,
                                            CallbackInfo ci) {
        if (!ElytrixclientClient.CONFIG.customLoading) {
            return;
        }
        ci.cancel();

        long now = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }
        float fadeOutAnim = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInAnim = this.fadeInStart > -1L ? (float) (now - this.fadeInStart) / 500.0F : -1.0F;

        float alpha = 1.0F;
        boolean showScreen = false;
        if (fadeOutAnim >= 1.0F) {
            // загрузка закончилась: показываем экран под растворением лоадера
            showScreen = true;
            alpha = 1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn && fadeInAnim < 1.0F) {
            showScreen = true;
            alpha = Mth.clamp(fadeInAnim, 0.15F, 1.0F);
        }
        if (showScreen) {
            Screen screen = this.minecraft.gui.screen();
            if (screen != null) {
                screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, a);
            }
            graphics.nextStratum();
        }

        // тот же сглаживающий фильтр, что и в ванили
        float actual = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + actual * 0.050000012F, 0.0F, 1.0F);
        ElytrixLoader.render(graphics, this.currentProgress, alpha);

        if (fadeOutAnim >= 2.0F) {
            this.minecraft.gui.setOverlay(null);
        }
    }
}
