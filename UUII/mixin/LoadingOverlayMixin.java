package wtf.expensive.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;
import wtf.expensive.client.util.BetterText;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
    @Unique
    private static final Identifier EXPENSIVE_LOADING_IMAGE =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/loading.png");

    @Unique
    private static boolean expensive$imageRegistered;

    @Unique
    private final BetterText expensive$text = new BetterText(List.of(
            "думаю...", "запускаю...", "готовлю...", "жарю...", "почти все готово...",
            "ваша пицца будет у вас через 20 минут", "дождитесь", "серьезно?",
            "у тебя еще не загрузило?", "ну значит скоро все будет!", "смертникс пидор",
            "ну ладно пропустим."
    ), 800);

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void expensive$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                  CallbackInfo ci) {
        LoadingOverlayAccessor accessor = (LoadingOverlayAccessor) this;
        Minecraft minecraft = Minecraft.getInstance();
        expensive$registerImage(minecraft);

        long fadeOutStart = accessor.expensive$fadeOutStart();
        long now = Util.getMillis();
        float fadeOut = fadeOutStart > -1L ? (now - fadeOutStart) / 1000f : -1f;

        if (accessor.expensive$fadeInStart() == -1L) {
            accessor.expensive$setFadeInStart(now);
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        float fadeAlpha = fadeOut >= 0f ? 1f - Math.clamp(fadeOut, 0f, 1f) : 1f;

        int alpha = Math.round(255 * fadeAlpha);
        RenderUtil.texture(graphics, EXPENSIVE_LOADING_IMAGE, 0, 0, width, height,
                ColorUtil.rgba(255, 255, 255, alpha));

        StyledFontRenderer.drawCentered(graphics, minecraft.font,
                Component.literal(expensive$text.get()).withStyle(Fonts.MEDIUM_20),
                width / 2f, height - 40, ColorUtil.rgba(255, 255, 255, alpha));

        if (fadeOut >= 1f && fadeAlpha <= 0f) {
            minecraft.gui.setOverlay(null);
        }
        ci.cancel();
    }

    @Unique
    private static void expensive$registerImage(Minecraft minecraft) {
        if (expensive$imageRegistered) {
            return;
        }
        expensive$imageRegistered = true;
        minecraft.getTextureManager().registerAndLoad(EXPENSIVE_LOADING_IMAGE,
                new ReloadableTexture(EXPENSIVE_LOADING_IMAGE) {
                    @Override
                    public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
                        try (InputStream stream = LoadingOverlayMixin.class.getClassLoader()
                                .getResourceAsStream("assets/" + Expensive.MOD_ID + "/textures/images/loading.png")) {
                            if (stream == null) {
                                return TextureContents.createMissing();
                            }
                            return new TextureContents(NativeImage.read(stream),
                                    new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0f));
                        }
                    }
                });
    }
}
