package ru.rooyzee.elytrixclient.client.render.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * MTSDF текстовый рендерер — lazy-loading как в delta-26.2.
 * Шрифты загружаются при первом вызове draw(), а не в onInitializeClient.
 */
public final class MtsdfTextRenderer {

    private static FontRenderer regularFont;
    private static boolean fontsLoaded = false;
    private static boolean fontFailed = false;

    private static void ensureFonts() {
        if (fontsLoaded || fontFailed) return;
        fontsLoaded = true;
        try {
            MsdfFont regularMsdf = MsdfFont.load(
                    Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_regular.json"));
            regularFont = new FontRenderer("Google Sans Regular", regularMsdf);
            System.out.println("[Elytrix] MTSDF font loaded OK");
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF font load failed: " + e);
            fontFailed = true;
        }
    }

    public static FontRenderer regular() { ensureFonts(); return regularFont; }

    /** Рисует MTSDF текст через кастомный шейдер. */
    public static void draw(GuiGraphicsExtractor g, Font mcFont, String text,
                            float x, float y, float size, int color) {
        if (text == null || text.isEmpty()) return;
        ensureFonts();
        if (regularFont != null) {
            try {
                regularFont.draw(g, text, x, y, size, color);
                return;
            } catch (Exception e) {
                System.err.println("[Elytrix] MTSDF draw failed: " + e);
                fontFailed = true;
            }
        }
        // Fallback если шрифт не загружен
        ru.rooyzee.elytrixclient.client.ui.kit.UiText.draw(g, mcFont, text, (int) x, (int) y, color,
                ru.rooyzee.elytrixclient.client.ui.kit.UiText.FACE, false);
    }

    /** Ширина текста через MtsdfFont. */
    public static float width(String text, float size) {
        ensureFonts();
        if (regularFont == null || text == null || text.isEmpty()) return 0;
        return regularFont.width(text, size);
    }

    /** Fallback ширина через mc.font. */
    public static float widthFallback(Font mcFont, String text) {
        if (text == null || text.isEmpty()) return 0;
        return mcFont.width(text);
    }

    /** Высота строки. */
    public static float lineHeight(float size) {
        ensureFonts();
        return regularFont != null ? regularFont.msdfFont().lineHeight(size) : size * 1.2f;
    }

    /** Ascender. */
    public static float ascender(float size) {
        ensureFonts();
        return regularFont != null ? regularFont.msdfFont().ascender(size) : size * 0.8f;
    }

    /** Вызывать в начале каждого кадра. */
    public static void beginFrame() {
        ElytrixRenderUtil.beginFrame();
    }

    /** Вызывать в конце отрисовки GUI для flush очереди. */
    public static void flush(GuiGraphicsExtractor g) {
        ElytrixRenderUtil.flush(g);
    }
}