package ru.rooyzee.elytrixclient.client.render.font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.Identifier;

/**
 * Высокоуровневый API для рендеринга MTSDF текста.
 * Полный порт из delta-26.2 Font.java.
 */
public class FontRenderer {
    private final String name;
    private final MsdfFont msdfFont;

    /** Состояния прокрутки: {смещение, направление}. Ключи — слабые, как в delta. */
    private static final Map<Object, float[]> SCROLL_STATES = new WeakHashMap<>();

    /**
     * Подстановка символов, которых нет в атласах xrose_1.
     *
     * <p>В наборах только базовая латиница (U+0020…U+007E) и кириллица
     * (U+0400…U+045F) — 191 глиф. Типографские тире, кавычки-ёлочки, многоточие
     * и стрелки, которых полно в строках интерфейса, без подстановки
     * превратились бы в «?». Таблица применяется и в {@link #width}, и в
     * {@link #draw}, поэтому ширина текста остаётся честной.
     */
    private static final Map<Integer, String> SUBSTITUTIONS = new HashMap<>();

    static {
        SUBSTITUTIONS.put(0x00A0, " ");     // неразрывный пробел
        SUBSTITUTIONS.put(0x2026, "...");   // …
        SUBSTITUTIONS.put(0x2014, "-");     // —
        SUBSTITUTIONS.put(0x2013, "-");     // –
        SUBSTITUTIONS.put(0x2010, "-");     // ‐
        SUBSTITUTIONS.put(0x2011, "-");     // ‑
        SUBSTITUTIONS.put(0x2212, "-");     // −
        SUBSTITUTIONS.put(0x00AB, "\"");    // «
        SUBSTITUTIONS.put(0x00BB, "\"");    // »
        SUBSTITUTIONS.put(0x2039, "'");     // ‹
        SUBSTITUTIONS.put(0x203A, ">");     // ›
        SUBSTITUTIONS.put(0x2018, "'");     // ‘
        SUBSTITUTIONS.put(0x2019, "'");     // ’
        SUBSTITUTIONS.put(0x201C, "\"");    // “
        SUBSTITUTIONS.put(0x201D, "\"");    // ”
        SUBSTITUTIONS.put(0x00D7, "x");     // ×
        SUBSTITUTIONS.put(0x00B7, ".");     // ·
        SUBSTITUTIONS.put(0x2248, "~");     // ≈
        SUBSTITUTIONS.put(0x2192, "->");    // →
        SUBSTITUTIONS.put(0x2190, "<-");    // ←
        SUBSTITUTIONS.put(0x2500, "-");     // ─
        SUBSTITUTIONS.put(0x2550, "=");     // ═
        SUBSTITUTIONS.put(0x2022, "*");     // •
        SUBSTITUTIONS.put(0x2605, "*");     // ★
        SUBSTITUTIONS.put(0x2606, "*");     // ☆
        SUBSTITUTIONS.put(0x2116, "No");    // №
        // Иконочные коды delta-26.2: в её атласе по этим кодам лежат глифы-иконки,
        // в наших атласах их нет — показываем звёздочку вместо пропавшей иконки.
        SUBSTITUTIONS.put(9889, "*");       // ⚡
        SUBSTITUTIONS.put(3618, "");
    }

    public FontRenderer(String name, MsdfFont msdfFont) {
        this.name = name;
        this.msdfFont = msdfFont;
    }

    public String name() { return this.name; }

    public MsdfFont msdfFont() { return this.msdfFont; }

    /** Есть ли в атласе глиф для символа. */
    public boolean supports(int codePoint) {
        return this.msdfFont.supports(codePoint);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            int len = Character.charCount(cp);
            if (cp == 167 && i + 1 < raw.length() && "0123456789abcdefklor".indexOf(raw.charAt(i + 1)) >= 0) {
                i += 2;                     // коды цвета Minecraft
                continue;
            }
            String sub = SUBSTITUTIONS.get(cp);
            if (sub != null) {
                sb.append(sub);
                i += len;
                continue;
            }
            sb.appendCodePoint(cp);
            i += len;
        }
        return sb.toString();
    }

    public float width(String text, float size) {
        return width(text, size, 0.0F);
    }

    public float width(String text, float size, float letterSpacing) {
        if (text == null || text.isEmpty()) return 0.0f;
        return this.msdfFont.measureWidth(normalize(text), size, letterSpacing);
    }

    public float lineHeight(float size) { return this.msdfFont.lineHeight(size); }

    public float ascender(float size) { return this.msdfFont.ascender(size); }

    public float textHeight(float size) { return this.msdfFont.textHeight(size); }

    public void draw(GuiGraphicsExtractor context, String text, float x, float y, float size, int color) {
        draw(context, text, x, y, size, color, 0.0f);
    }

    public void draw(GuiGraphicsExtractor context, String text, float x, float y, float size, int color, float letterSpacing) {
        render(context, text, x, y, size, color, letterSpacing, null);
    }

    /** Текст с явной обрезкой (для строк, которые не должны вылезать за панель). */
    public void drawScissored(GuiGraphicsExtractor context, String text, float x, float y, float size, int color,
                              ScreenRectangle scissor) {
        render(context, text, x, y, size, color, 0.0f, scissor);
    }

    private void render(GuiGraphicsExtractor context, String text, float x, float y, float size, int color,
                        float letterSpacing, ScreenRectangle scissor) {
        if (text == null || text.isEmpty() || context == null || size <= 0.0f) return;
        text = normalize(text);
        if (text.isEmpty()) return;

        // Прямоугольник обрезки задан в тех же координатах, что и текст
        // (локальных для текущей pose), а GuiElementRenderState.scissorArea()
        // живёт в пикселях кадра — как и bounds(), поэтому переводим его pose-ом.
        ScreenRectangle screenScissor = scissor == null ? null : scissor.transformMaxBounds(context.pose());

        // ВАЖНО: вызываем textureSetup() чтобы зарегистрировать текстуру в TextureManager
        this.msdfFont.textureSetup();
        Identifier fontTex = this.msdfFont.textureId();
        float baseline = y + this.msdfFont.ascender(size);
        List<MtsdfTextRenderState.GlyphData> gd = new ArrayList<>();
        float pen = x;
        int prev = -1;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            MsdfFont.Glyph glyph = this.msdfFont.glyph(cp);
            if (glyph != null && glyph.planeBounds() != null) {
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
            pen += (glyph != null ? glyph.advanceEm() : 0.0f) * size + letterSpacing;
            prev = cp;
            i += len;
        }
        if (!gd.isEmpty()) {
            float totalW = Math.max(0.0f, pen - x - letterSpacing);
            float totalH = this.msdfFont.lineHeight(size);
            ElytrixRenderUtil.queueTextNow(context, x, y, size, color, -1, 0.0f, 0.0f,
                    this.msdfFont.distanceRange(), fontTex, gd, totalW, totalH, screenScissor);
        }
    }

    public void drawCentered(GuiGraphicsExtractor context, String text, float x, float y, float size, int color) {
        float textWidth = width(text, size);
        render(context, text, x - (textWidth / 2.0f), y, size, color, 0.0f, null);
    }

    /** Текст, прижатый правым краем к {@code right}. */
    public void drawRight(GuiGraphicsExtractor context, String text, float right, float y, float size, int color) {
        float textWidth = width(text, size);
        render(context, text, right - textWidth, y, size, color, 0.0f, null);
    }

    public void drawClamped(GuiGraphicsExtractor context, String text, float x, float y, float size, int color, float maxWidth) {
        if (text == null || text.isEmpty() || context == null || maxWidth <= 0.0f) return;
        float textWidth = width(text, size);
        if (textWidth <= maxWidth) {
            render(context, text, x, y, size, color, 0.0f, null);
            return;
        }
        ScreenRectangle scissor = new ScreenRectangle(
                (int) x - 1, (int) (y - size * 0.5f),
                Math.max(1, (int) (maxWidth + 2.0f)), Math.max(1, (int) (size * 2.0f + 1.0f)));
        render(context, text, x, y, size, color, 0.0f, scissor);
    }

    /**
     * Бегущая строка: если текст шире {@code maxWidth}, он ездит туда-обратно
     * (наведение — едет, без наведения — возвращается в начало).
     * Порт {@code Font.a(context, key, text, …)} из delta-26.2.
     */
    public void scroll(GuiGraphicsExtractor context, Object key, String text, float x, float y, float size,
                       int color, float maxWidth, boolean hovered, float speed, float delta) {
        if (text == null || text.isEmpty() || context == null || maxWidth <= 0.0f) return;
        float textWidth = width(text, size);
        if (textWidth <= maxWidth) {
            SCROLL_STATES.remove(key);
            render(context, text, x, y, size, color, 0.0f, null);
            return;
        }
        float[] state = SCROLL_STATES.computeIfAbsent(key, k -> new float[]{0.0f, 1.0f});
        float maxOffset = textWidth - maxWidth;
        float dt = Math.max(0.0f, Math.min(delta, 0.05f));
        if (hovered) {
            state[0] += state[1] * speed * dt;
            if (state[0] >= maxOffset) {
                state[0] = maxOffset;
                state[1] = -1.0f;
            } else if (state[0] <= 0.0f) {
                state[0] = 0.0f;
                state[1] = 1.0f;
            }
        } else {
            state[0] += (0.0f - state[0]) * Math.min(1.0f, dt * 12.0f);
            if (state[0] < 0.3f) {
                state[0] = 0.0f;
                state[1] = 1.0f;
            }
        }
        ScreenRectangle scissor = new ScreenRectangle(
                (int) x - 1, (int) (y - size * 0.5f),
                Math.max(1, (int) (maxWidth + 2.0f)), Math.max(1, (int) (size * 2.0f + 1.0f)));
        render(context, text, x - state[0], y, size, color, 0.0f, scissor);
    }

    /** Обрезать строку с «...», чтобы влезла в {@code maxWidth}. */
    public String trim(String text, float size, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) return "";
        if (width(text, size) <= maxWidth) return text;
        String dots = "...";
        float dotsWidth = width(dots, size);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = sb.toString() + text.charAt(i);
            if (width(candidate, size) + dotsWidth > maxWidth) break;
            sb.append(text.charAt(i));
        }
        return sb + dots;
    }
}
