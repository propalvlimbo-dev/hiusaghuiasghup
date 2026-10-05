package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;

import java.io.InputStream;

/**
 * MTSDF текстовый рендерер.
 *
 * <p>Сейчас использует UiText (Inter TTF) для отрисовки и MtsdfFont для
 * точного измерения ширины. MTSDF атлас загружается как текстура.
 *
 * <p>Для полноценного MTSDF рендеринга (с SDF math в шейдере) нужен
 * кастомный RenderPipeline с text.vsh/text.fsh + GuiElementRenderState.
 */
public final class MtsdfTextRenderer {

    private static MtsdfFont regularFont;
    private static MtsdfFont mediumFont;
    private static boolean initialized = false;

    /** Загружает MTSDF атласы. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        try {
            regularFont = new MtsdfFont(
                    Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_regular"),
                    Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_regular.json"));
            mediumFont = new MtsdfFont(
                    Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_medium"),
                    Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_medium.json"));
            System.out.println("[Elytrix] MTSDF fonts loaded: regular=" + (regularFont != null)
                    + " medium=" + (mediumFont != null));
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF font load failed: " + e);
        }
    }

    public static MtsdfFont regular() { return regularFont; }
    public static MtsdfFont medium() { return mediumFont; }

    /**
     * Рисует текст через UiText (Inter TTF). MtsdfFont используется для измерения.
     */
    public static void draw(GuiGraphicsExtractor g, Font mcFont, String text,
                            float x, float y, float size, int color) {
        if (text == null || text.isEmpty()) return;
        UiText.draw(g, mcFont, text, (int) x, (int) y, color, UiText.FACE, false);
    }

    /** Ширина текста через MtsdfFont (точнее чем mc.font). */
    public static float width(String text, float size) {
        if (regularFont == null || text == null || text.isEmpty()) return 0;
        return regularFont.measureWidth(text, size);
    }

    /** Fallback ширина через mc.font. */
    public static float widthFallback(Font mcFont, String text) {
        if (text == null || text.isEmpty()) return 0;
        return mcFont.width(text);
    }

    /** Высота строки. */
    public static float lineHeight(float size) {
        return regularFont != null ? regularFont.lineHeight(size) : size * 1.2f;
    }

    /** Ascender в пикселях. */
    public static float ascender(float size) {
        return regularFont != null ? regularFont.ascender(size) : size * 0.8f;
    }
}