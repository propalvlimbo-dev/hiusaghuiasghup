package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.ElytrixConnect;

/**
 * Загрузка мира и подключение к серверу — полностью свой экран.
 *
 * <p>Раньше оверлей рисовался в конце {@code Screen.extractRenderState}, но
 * {@code LevelLoadingScreen}/{@code ConnectScreen} сначала вызывают {@code super},
 * а уже потом рисуют свою полосу, текст и карту чанков — и всё это оказывалось
 * поверх нашего экрана. Теперь ванильная отрисовка этих экранов отменяется
 * целиком (и фон, и содержимое); кнопки экрана (например, «Отмена» при
 * подключении) рисуются поверх нашего фона, чтобы ими можно было пользоваться.
 */
@Mixin({LevelLoadingScreen.class, ConnectScreen.class})
public abstract class LoadingScreensMixin extends Screen {

    protected LoadingScreensMixin() {
        super(null);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$ownLoading(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a,
                                    CallbackInfo ci) {
        if (!ElytrixclientClient.CONFIG.customLoading) {
            return;
        }
        boolean connecting = (Object) this instanceof ConnectScreen;
        ElytrixConnect.render(graphics, connecting ? 1 : 2);
        for (GuiEventListener child : this.children()) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }
        ci.cancel();
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void elytrix$noVanillaBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a,
                                             CallbackInfo ci) {
        if (ElytrixclientClient.CONFIG.customLoading) {
            ci.cancel();
        }
    }
}
