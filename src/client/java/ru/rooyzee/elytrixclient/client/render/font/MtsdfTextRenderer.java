package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MTSDF текстовый рендерер — рисует текст из MTSDF атласа.
 *
 * <p>Каждый глиф рисуется как textured quad через GUI_TEXTURED pipeline.
 * Текстура атласа загружается из ресурсов и используется как обычная текстура.
 *
 * <p>Для полноценного MTSDF рендеринга (с SDF math в шейдере) нужен
 * кастомный RenderPipeline с text.vsh/text.fsh. Это требует глубокой
 * интеграции с MC 26.2 shader system.
 */
public final class MtsdfTextRenderer {

    private static Identifier fontTextureId;
    private static DynamicTexture fontTexture;
    private static boolean initialized = false;
    private static int texWidth = 1, texHeight = 1;

    /** Регистрирует MTSDF текстуру в Minecraft. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        try {
            Identifier pngId = Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_regular.png");
            try (InputStream is = mc.getResourceManager().open(pngId)) {
                NativeImage image = NativeImage.read(is);
                texWidth = image.getWidth();
                texHeight = image.getHeight();
                fontTextureId = Identifier.fromNamespaceAndPath("elytrixclient", "font/mtsdf_atlas");
                fontTexture = new DynamicTexture(() -> "elytrix-mtsdf", image);
                mc.getTextureManager().register(fontTextureId, fontTexture);
                System.out.println("[Elytrix] MTSDF texture registered: " + texWidth + "x" + texHeight);
            }
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF texture load failed: " + e);
        }
    }

    /**
     * Рисует MTSDF текст через GUI_TEXTURED pipeline.
     * Каждый глиф = textured quad с UV из MTSDF атласа.
     */
    public static void draw(GuiGraphicsExtractor g, MtsdfFont font, String text,
                            float x, float y, float size, int color) {
        if (font == null || text == null || text.isEmpty()) return;
        if (fontTextureId == null) {
            // Fallback на UiText если текстура не загружена
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                ru.rooyzee.elytrixclient.client.ui.kit.UiText.draw(g, mc.font, text, (int) x, (int) y, color,
                        ru.rooyzee.elytrixclient.client.ui.kit.UiText.FACE, false);
            }
            return;
        }

        float r = ((color >> 16) & 0xFF) / 255f;
        float gr = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float pen = x;
        int prev = -1;
        float baseline = y + font.ascender(size);

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);

            MtsdfFont.Glyph glyph = font.glyph(cp);
            if (prev != -1) pen += font.kerning(prev, cp) * size;

            if (glyph.atlasRight() > glyph.atlasLeft()) {
                float gx0 = pen + glyph.planeLeft() * size;
                float gy0 = baseline - glyph.planeTop() * size;
                float gx1 = pen + glyph.planeRight() * size;
                float gy1 = baseline - glyph.planeBottom() * size;

                // UV в атласе (нормализованные)
                float u0 = glyph.atlasLeft() / (float) font.atlasWidth();
                float v0 = glyph.atlasBottom() / (float) font.atlasHeight();
                float u1 = glyph.atlasRight() / (float) font.atlasWidth();
                float v1 = glyph.atlasTop() / (float) font.atlasHeight();

                // Рисуем через blit
                int ix0 = (int) gx0, iy0 = (int) gy0;
                int ix1 = (int) gx1, iy1 = (int) gy1;
                int dw = ix1 - ix0, dh = iy1 - iy0;
                if (dw > 0 && dh > 0) {
                    g.blit(RenderPipelines.GUI_TEXTURED, fontTextureId,
                            ix0, iy0,
                            u0, v0,
                            dw, dh,
                            (int) ((u1 - u0) * texWidth), (int) ((v1 - v0) * texHeight),
                            texWidth, texHeight,
                            color);
                }
            }

            pen += glyph.advance() * size;
            prev = cp;
            i += len;
        }
    }

    /** Ширина текста через MtsdfFont. */
    public static float width(MtsdfFont font, String text, float size) {
        if (font == null || text == null || text.isEmpty()) return 0;
        return font.measureWidth(text, size);
    }

    /** Fallback ширина через mc.font. */
    public static float widthFallback(net.minecraft.client.gui.Font mcFont, String text) {
        if (text == null || text.isEmpty()) return 0;
        return mcFont.width(text);
    }
}