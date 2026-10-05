package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;

/**
 * MTSDF текстовый рендерер.
 *
 * <p>Рисует текст через GuiElementRenderState с GUI_TEXTURED pipeline.
 * Каждый глиф — textured quad с UV из MTSDF атласа.
 *
 * <p>Для SDF math (median-of-3 + smoothstep) нужен кастомный шейдер.
 * Сейчас GUI_TEXTURED рендерит RAW атлас — будет видно цветные пиксели.
 * Когда будет зарегистрирован кастомный snippet для text.fsh — заменить pipeline().
 */
public final class MtsdfTextRenderer {

    private static MtsdfFont regularFont;
    private static MtsdfFont mediumFont;
    private static boolean initialized = false;
    private static Identifier fontTextureId;

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
            fontTextureId = Identifier.fromNamespaceAndPath("elytrixclient", "textures/font/google_sans_regular.png");
            System.out.println("[Elytrix] MTSDF fonts loaded: regular=" + (regularFont != null)
                    + " medium=" + (mediumFont != null));
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF font load failed: " + e);
        }
    }

    public static MtsdfFont regular() { return regularFont; }
    public static MtsdfFont medium() { return mediumFont; }

    /**
     * Рисует текст через UiText (Inter TTF). MtsdfFont — только для измерения.
     * Когда кастомный snippet с text.fsh будет зарегистрирован —
     * заменить на MtsdfTextState + кастомный pipeline.
     */
    public static void draw(GuiGraphicsExtractor g, Font mcFont, String text,
                            float x, float y, float size, int color) {
        if (text == null || text.isEmpty()) return;
        UiText.draw(g, mcFont, text, (int) x, (int) y, color, UiText.FACE, false);
    }

    /** Ширина текста через MtsdfFont. */
    public static float width(String text, float size) {
        if (regularFont == null || text == null || text.isEmpty()) return 0;
        return regularFont.measureWidth(text, size);
    }

    /** Fallback ширина через mc.font. */
    public static float widthFallback(Font mcFont, String text) {
        if (text == null || text.isEmpty()) return 0;
        return mcFont.width(text);
    }

    public static float lineHeight(float size) {
        return regularFont != null ? regularFont.lineHeight(size) : size * 1.2f;
    }

    public static float ascender(float size) {
        return regularFont != null ? regularFont.ascender(size) : size * 0.8f;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GuiElementRenderState — textured quads для MTSDF глифов
    // ─────────────────────────────────────────────────────────────────────

    static final class MtsdfTextState implements GuiElementRenderState {
        private final Matrix3x2fc pose;
        private final MtsdfFont font;
        private final String text;
        private final float x, y, size;
        private final int color;
        private final ScreenRectangle bounds;

        MtsdfTextState(Matrix3x2fc pose, MtsdfFont font, String text,
                       float x, float y, float size, int color,
                       ScreenRectangle bounds) {
            this.pose = pose;
            this.font = font;
            this.text = text;
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.bounds = bounds;
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
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

                    float u0 = glyph.atlasLeft() / (float) font.atlasWidth();
                    float v0 = 1f - glyph.atlasTop() / (float) font.atlasHeight();
                    float u1 = glyph.atlasRight() / (float) font.atlasWidth();
                    float v1 = 1f - glyph.atlasBottom() / (float) font.atlasHeight();

                    // TL, BL, BR, TR — ванильный обход (CW для GUI)
                    vert(consumer, pose, gx0, gy0, u0, v0, r, g, b, a);
                    vert(consumer, pose, gx0, gy1, u0, v1, r, g, b, a);
                    vert(consumer, pose, gx1, gy1, u1, v1, r, g, b, a);
                    vert(consumer, pose, gx1, gy0, u1, v0, r, g, b, a);
                }

                pen += glyph.advance() * size;
                prev = cp;
                i += len;
            }
        }

        private static void vert(VertexConsumer c, Matrix3x2fc pose,
                                  float x, float y, float u, float v,
                                  float r, float g, float b, float a) {
            c.addVertexWith2DPose(pose, x, y)
                    .setUv(u, v)
                    .setColor(r, g, b, a);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.singleTexture(fontTextureId);
        }

        @Override
        public ScreenRectangle scissorArea() {
            return null;
        }

        @Override
        public ScreenRectangle bounds() {
            return bounds;
        }
    }
}