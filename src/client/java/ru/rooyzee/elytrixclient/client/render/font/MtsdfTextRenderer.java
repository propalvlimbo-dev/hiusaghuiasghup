package ru.rooyzee.elytrixclient.client.render.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * MTSDF текстовый рендерер — инициализация и рендеринг через кастомный шейдер.
 * Полный порт из delta-26.2.
 */
public final class MtsdfTextRenderer {

    private static FontRenderer regularFont;
    private static FontRenderer mediumFont;
    private static boolean initialized = false;

    /** Загружает MTSDF атласы и регистрирует текстуры. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        try {
            MsdfFont regularMsdf = MsdfFont.load(
                    Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_regular.json"));
            MsdfFont mediumMsdf = MsdfFont.load(
                    Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_medium.json"));
            regularFont = new FontRenderer("Google Sans Regular", regularMsdf);
            mediumFont = new FontRenderer("Google Sans Medium", mediumMsdf);
            System.out.println("[Elytrix] MTSDF fonts loaded: regular=" + (regularFont != null)
                    + " medium=" + (mediumFont != null));
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF font load failed: " + e);
        }
    }

    public static FontRenderer regular() { return regularFont; }
    public static FontRenderer medium() { return mediumFont; }

    /** Рисует MTSDF текст через кастомный шейдер. */
    public static void draw(GuiGraphicsExtractor g, Font mcFont, String text,
                            float x, float y, float size, int color) {
        if (text == null || text.isEmpty()) return;
        if (regularFont != null) {
            regularFont.draw(g, text, x, y, size, color);
        } else {
            // Fallback если шрифт не загружен
            net.minecraft.client.gui.Font f = mcFont != null ? mcFont : Minecraft.getInstance().font;
            ru.rooyzee.elytrixclient.client.ui.kit.UiText.draw(g, f, text, (int) x, (int) y, color,
                    ru.rooyzee.elytrixclient.client.ui.kit.UiText.FACE, false);
        }
    }

    /** Ширина текста через MtsdfFont. */
    public static float width(String text, float size) {
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
        return regularFont != null ? regularFont.msdfFont().lineHeight(size) : size * 1.2f;
    }

    /** Ascender. */
    public static float ascender(float size) {
        return regularFont != null ? regularFont.msdfFont().ascender(size) : size * 0.8f;
    }

    /** Вызывать в начале каждого кадра для flush очереди. */
    public static void beginFrame() {
        ElytrixRenderUtil.beginFrame();
    }

    /** Вызывать в конце отрисовки GUI для отправки всех queued render states. */
    public static void flush(GuiGraphicsExtractor g) {
        ElytrixRenderUtil.flush(g);
    }
}