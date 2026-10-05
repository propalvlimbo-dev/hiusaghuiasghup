package ru.rooyzee.elytrixclient.client.render.font;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Высокоуровневый API для рендеринга MTSDF текста.
 * Полный порт из delta-26.2 Font.java.
 */
public class FontRenderer {
    private final String name;
    private final MsdfFont msdfFont;

    public FontRenderer(String name, MsdfFont msdfFont) {
        this.name = name;
        this.msdfFont = msdfFont;
    }

    public MsdfFont msdfFont() { return this.msdfFont; }

    private static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); ) {
            char c = raw.charAt(i);
            if (c == 9889) {
                sb.append((char) 349);
                i++;
            } else if (c == 9733) {
                sb.append((char) 350);
                i++;
            } else if (c == 3618) {
                i++;
            } else if (c == 167 && i + 1 < raw.length() && "0123456789abcdefklor".indexOf(raw.charAt(i + 1)) >= 0) {
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    public float width(String text, float size) {
        if (text == null || text.isEmpty()) return 0.0f;
        return this.msdfFont.measureWidth(normalize(text), size);
    }

    public void draw(GuiGraphicsExtractor context, String text, float x, float y, float size, int color) {
        draw(context, text, x, y, size, color, 0.0f);
    }

    public void draw(GuiGraphicsExtractor context, String text, float x, float y, float size, int color, float letterSpacing) {
        if (text == null || text.isEmpty() || context == null) return;
        text = normalize(text);
        if (text.isEmpty()) return;

        Identifier fontTex = this.msdfFont.textureId();
        float baseline = y + this.msdfFont.ascender(size);
        List<MtsdfTextRenderState.GlyphData> gd = new ArrayList<>();
        float pen = x;
        int prev = -1;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            MsdfFont.Glyph glyph = this.msdfFont.glyph(cp);
            if (glyph.planeBounds() != null) {
                MsdfFont.Bounds plane = glyph.planeBounds();
                float gx0 = pen + plane.left() * size;
                float gy0 = baseline - plane.top() * size;
                float gx1 = pen + plane.right() * size;
                float gy1 = baseline - plane.bottom() * size;
                gd.add(new MtsdfTextRenderState.GlyphData(
                    gx0, gy0, gx1, gy1,
                    glyph.u0(), glyph.v0(), glyph.u1(), glyph.v1(),
                    this.msdfFont.localPxPerSdfUnit(size)
                ));
            }
            if (prev != -1) pen += this.msdfFont.kerning(prev, cp) * size;
            pen += glyph.advanceEm() * size + letterSpacing;
            prev = cp;
            i += len;
        }
        if (!gd.isEmpty()) {
            float totalW = pen - x;
            float totalH = this.msdfFont.lineHeight(size);
            ElytrixRenderUtil.queueText(context, x, y, size, color, -1, 0.0f, 0.0f,
                    this.msdfFont.distanceRange(), fontTex, gd, totalW, totalH, null);
        }
    }

    public void drawCentered(GuiGraphicsExtractor context, String text, float x, float y, float size, int color) {
        float textWidth = width(text, size);
        draw(context, text, x - (textWidth / 2.0f), y, size, color, 0.0f);
    }

    public void drawClamped(GuiGraphicsExtractor context, String text, float x, float y, float size, int color, float maxWidth) {
        if (text == null || text.isEmpty() || context == null || maxWidth <= 0.0f) return;
        float textWidth = width(text, size);
        if (textWidth <= maxWidth) {
            draw(context, text, x, y, size, color, 0.0f);
            return;
        }
        context.enableScissor((int) x - 1, (int) (y - size * 0.5f), (int) (x + maxWidth + 1), (int) (y + size * 1.5f + 0.5f));
        draw(context, text, x, y, size, color, 0.0f);
        context.disableScissor();
    }
}