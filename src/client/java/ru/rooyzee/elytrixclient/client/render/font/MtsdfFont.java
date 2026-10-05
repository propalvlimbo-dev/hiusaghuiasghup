package ru.rooyzee.elytrixclient.client.render.font;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * MTSDF шрифт — загружает JSON атлас + PNG текстуру.
 * Формат: msdf-atlas-gen JSON (atlas, metrics, glyphs, kerning).
 */
public final class MtsdfFont {

    public record Glyph(float advance, float planeLeft, float planeBottom, float planeRight, float planeTop,
                        float atlasLeft, float atlasBottom, float atlasRight, float atlasTop) {}

    private final Identifier textureId;
    private final float emSize;
    private final float lineHeight;
    private final float ascender;
    private final float descender;
    private final float distanceRange;
    private final int atlasWidth, atlasHeight;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Long, Float> kerning = new HashMap<>();
    private final Glyph spaceGlyph;

    public MtsdfFont(Identifier textureId, Identifier jsonId) {
        this.textureId = textureId;
        float em = 1, lh = 1.2f, asc = 0.8f, desc = -0.2f, dr = 20;
        int aw = 1, ah = 1;
        Glyph space = null;

        try (InputStream is = Minecraft.getInstance().getResourceManager().open(jsonId);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);

            JsonObject atlas = root.getAsJsonObject("atlas");
            if (atlas != null) {
                dr = atlas.has("distanceRange") ? atlas.get("distanceRange").getAsFloat() : 20;
                aw = atlas.has("width") ? atlas.get("width").getAsInt() : 1;
                ah = atlas.has("height") ? atlas.get("height").getAsInt() : 1;
            }

            JsonObject metrics = root.getAsJsonObject("metrics");
            if (metrics != null) {
                em = metrics.has("emSize") ? metrics.get("emSize").getAsFloat() : 1;
                lh = metrics.has("lineHeight") ? metrics.get("lineHeight").getAsFloat() : 1.2f;
                asc = metrics.has("ascender") ? metrics.get("ascender").getAsFloat() : 0.8f;
                desc = metrics.has("descender") ? metrics.get("descender").getAsFloat() : -0.2f;
            }

            JsonArray glyphArr = root.getAsJsonArray("glyphs");
            if (glyphArr != null) {
                for (JsonElement el : glyphArr) {
                    JsonObject g = el.getAsJsonObject();
                    int unicode = g.get("unicode").getAsInt();
                    float advance = g.get("advance").getAsFloat();
                    float pL = 0, pB = 0, pR = 0, pT = 0;
                    float aL = 0, aB = 0, aR = 0, aT = 0;
                    if (g.has("planeBounds")) {
                        JsonObject pb = g.getAsJsonObject("planeBounds");
                        pL = pb.get("left").getAsFloat();
                        pB = pb.get("bottom").getAsFloat();
                        pR = pb.get("right").getAsFloat();
                        pT = pb.get("top").getAsFloat();
                    }
                    if (g.has("atlasBounds")) {
                        JsonObject ab = g.getAsJsonObject("atlasBounds");
                        aL = ab.get("left").getAsFloat();
                        aB = ab.get("bottom").getAsFloat();
                        aR = ab.get("right").getAsFloat();
                        aT = ab.get("top").getAsFloat();
                    }
                    Glyph glyph = new Glyph(advance, pL, pB, pR, pT, aL, aB, aR, aT);
                    glyphs.put(unicode, glyph);
                    if (unicode == 32) space = glyph;
                }
            }

            JsonArray kernArr = root.getAsJsonArray("kerning");
            if (kernArr != null) {
                for (JsonElement el : kernArr) {
                    JsonObject k = el.getAsJsonObject();
                    int u1 = k.get("unicode1").getAsInt();
                    int u2 = k.get("unicode2").getAsInt();
                    float adv = k.get("advance").getAsFloat();
                    kerning.put(((long) u1 << 32) | u2, adv);
                }
            }
        } catch (Exception e) {
            System.err.println("[Elytrix] Failed to load MTSDF font " + jsonId + ": " + e);
        }

        this.emSize = em;
        this.lineHeight = lh;
        this.ascender = asc;
        this.descender = desc;
        this.distanceRange = dr;
        this.atlasWidth = aw;
        this.atlasHeight = ah;
        this.spaceGlyph = space != null ? space : new Glyph(0.25f, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public Identifier textureId() { return textureId; }
    public float emSize() { return emSize; }
    public float lineHeight() { return lineHeight; }
    public float ascender() { return ascender; }
    public float descender() { return descender; }
    public float distanceRange() { return distanceRange; }
    public int atlasWidth() { return atlasWidth; }
    public int atlasHeight() { return atlasHeight; }

    public Glyph glyph(int codePoint) {
        Glyph g = glyphs.get(codePoint);
        return g != null ? g : spaceGlyph;
    }

    public float kerning(int prev, int curr) {
        Float k = kerning.get(((long) prev << 32) | curr);
        return k != null ? k : 0;
    }

    /** Ширина текста в em-единицах. */
    public float measureWidth(String text, float size) {
        if (text == null || text.isEmpty()) return 0;
        float pen = 0;
        int prev = -1;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            Glyph g = glyph(cp);
            if (prev != -1) pen += kerning(prev, cp);
            pen += g.advance();
            prev = cp;
            i += Character.charCount(cp);
        }
        return pen * size;
    }

    /** Высота строки. */
    public float lineHeight(float size) {
        return lineHeight * size;
    }

    /** Ascender в пикселях. */
    public float ascender(float size) {
        return ascender * size;
    }

    /** localPxPerSdfUnit для шейдера. */
    public float localPxPerSdfUnit(float size) {
        return size * emSize / distanceRange;
    }
}