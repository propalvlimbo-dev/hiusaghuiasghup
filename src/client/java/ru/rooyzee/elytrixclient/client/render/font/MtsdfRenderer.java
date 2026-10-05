package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;

/**
 * MTSDF рендерер текста — рисует текст через ванильный pipeline с TTF шрифтами.
 * Использует MtsdfFont для точного измерения, UiText для отрисовки.
 * В будущем можно заменить на полноценный MTSDF шейдер.
 */
public final class MtsdfRenderer {

    private static MtsdfFont regular;
    private static MtsdfFont medium;

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        try {
            regular = new MtsdfFont(
                    Identifier.fromNamespaceAndPath("elytrixclient", "font/google_sans_regular"),
                    Identifier.fromNamespaceAndPath("elytrixclient", "font/google_sans_regular.json"));
            medium = new MtsdfFont(
                    Identifier.fromNamespaceAndPath("elytrixclient", "font/google_sans_medium"),
                    Identifier.fromNamespaceAndPath("elytrixclient", "font/google_sans_medium.json"));
            System.out.println("[Elytrix] MTSDF fonts loaded: regular=" + (regular != null) + " medium=" + (medium != null));
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF font load failed: " + e);
        }
    }

    public static MtsdfFont regular() { return regular; }
    public static MtsdfFont medium() { return medium; }

    /** Рисует текст через UiText (Inter TTF) с точным позиционированием. */
    public static void draw(GuiGraphicsExtractor g, Font mcFont, String text, float x, float y, float size, int color) {
        if (text == null || text.isEmpty()) return;
        UiText.draw(g, mcFont, text, (int) x, (int) y, color, UiText.FACE, false);
    }

    /** Ширина текста через MtsdfFont (точнее чем mc.font.width). */
    public static float width(String text, float size) {
        if (regular == null || text == null || text.isEmpty()) return 0;
        return regular.measureWidth(text, size);
    }

    /** Ширина текста через mc.font (fallback). */
    public static float widthFallback(Font mcFont, String text) {
        if (text == null || text.isEmpty()) return 0;
        return mcFont.width(text);
    }

    /** Высота строки. */
    public static float lineHeight(float size) {
        return regular != null ? regular.lineHeight(size) : size * 1.2f;
    }

    /** Ascender. */
    public static float ascender(float size) {
        return regular != null ? regular.ascender(size) : size * 0.8f;
    }
}