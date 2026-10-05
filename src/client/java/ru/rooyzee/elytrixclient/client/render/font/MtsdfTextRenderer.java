package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * MTSDF рендерер текста — рисует текст через ванильный пайплайн GUI_TEXTURED.
 * Каждый глиф — два треугольника (6 вершин) с UV из атласа.
 * Шейдер text.fsh/text.vsh применяется автоматически через Minecraft.
 *
 * Пока используем ванильный GUI_TEXTURED pipeline для простоты.
 * MTSDF шейдер можно подключить позже через RenderPipeline.
 */
public final class MtsdfTextRenderer {

    private static final float UI_SIZE_SCALE = 8.0f;
    private static final float UI_RADIUS_SCALE = 16.0f;

    /**
     * Рисует MTSDF текст через GUI_TEXTURED pipeline.
     * Каждый глиф рисуется как textured quad с правильными UV из атласа.
     */
    public static void draw(GuiGraphicsExtractor g, MtsdfFont font, String text,
                            float x, float y, float size, int color) {
        if (font == null || text == null || text.isEmpty()) return;

        Matrix4f matrix = g.pose().last().pose();
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

                float u0 = glyph.atlasLeft() / font.atlasWidth();
                float v0 = 1f - glyph.atlasTop() / font.atlasHeight();
                float u1 = glyph.atlasRight() / font.atlasWidth();
                float v1 = 1f - glyph.atlasBottom() / font.atlasHeight();

                // Рисуем через ванильный буфер
                drawQuad(g, matrix, gx0, gy0, gx1, gy1, u0, v0, u1, v1, r, gr, b, a);
            }

            pen += glyph.advance() * size;
            prev = cp;
            i += len;
        }
    }

    private static void drawQuad(GuiGraphicsExtractor g, Matrix4f matrix,
                                  float x0, float y0, float x1, float y1,
                                  float u0, float v0, float u1, float v1,
                                  float r, float gr, float b, float a) {
        // Используем GuiGraphicsExtractor.blit для textured quad
        // Это самый надёжный способ в 26.2
        // Но для MTSDF нужен кастомный шейдер — пока рисуем через стандартный pipeline

        // Временно: рисуем colored quad как placeholder
        // TODO: заменить на полноценный MTSDF rendering с кастомным шейдером
        g.fill((int) x0, (int) y0, (int) x1, (int) y1,
                ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (gr * 255) << 8) | (int) (b * 255));
    }

    /** Ширина текста в пикселях. */
    public static float width(MtsdfFont font, String text, float size) {
        if (font == null || text == null || text.isEmpty()) return 0;
        return font.measureWidth(text, size);
    }
}