package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.media.CoverArt;
import ru.rooyzee.elytrixclient.client.media.CoverTexture;
import ru.rooyzee.elytrixclient.client.media.Lyrics;
import ru.rooyzee.elytrixclient.client.media.MediaSession;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.List;
import java.util.Locale;

/**
 * MusicWidget — портирован из Xivivide MusicWidget.java.
 * Текст рисуется MTSDF-атласами xrose_1 (см. {@link MtsdfTextRenderer});
 * кегли и начертания — как в оригинальном виджете: заголовок 9, исполнитель 7,
 * субтитры 8.5, тайминги 6.
 * PANEL_WIDTH=164, SCALE=0.85, COVER=33 — как в сурцах.
 */
public final class MusicIsland {
    private MusicIsland() {}

    private static final float PANEL_W = 164.0f;
    private static final float SC = 0.85f;
    private static final float PAD = 6.0f;
    private static final float COV = 33.0f;
    private static final float LSIZE = 8.5f;
    private static final float LROW = 11.0f;

    /** Кегли текста (в оригинальном виджете: 9 / 7 / 8.5 / 6). */
    private static final float TITLE_SIZE = 9.0f;
    private static final float ARTIST_SIZE = 7.0f;
    private static final float TIME_SIZE = 6.0f;

    /** Начертания: заголовок и субтитры плотнее, остальное — обычное. */
    private static final int W_TITLE = Fonts.MEDIUM;
    private static final int W_SUB = Fonts.REGULAR;
    private static final int W_LYRIC = Fonts.MEDIUM;
    private static final int W_TIME = Fonts.REGULAR;

    private static float sX = 6f, sY = 80f;
    private static float openT, trackFade, coverFade, lineFade;
    private static float lyricsRoom, lyricRows, sungWidth, progress, beat;
    private static long lastNanos;
    private static String lastTrack = "", lastLine = "", prevLine = "";
    private static boolean wasPressed, dragging, expanded = true;
    private static float dragOX, dragOY;
    private static LyricLayout curLayout;
    private static final Object tKey = new Object(), aKey = new Object();

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        MediaSession.keepAlive();
        boolean want = MediaSession.present();
        long now = Util.getMillis();
        float dt = lastNanos == 0 ? .016f : Math.min(.05f, (now - lastNanos) / 1000f);
        lastNanos = now;
        openT += ((want ? 1f : 0f) - openT) * (1f - (float) Math.exp(-5f * dt));
        if (openT < .005f && !want) { openT = 0; return; }

        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);

        String title = MediaSession.title(), artist = MediaSession.artist(), source = MediaSession.source();
        long pos = MediaSession.positionMs(), dur = MediaSession.durationMs();
        Lyrics.ensure(artist, title);
        String lyric = expanded ? lyricLine(pos) : "";
        boolean lk = expanded && Lyrics.has();
        LyricLayout lay = layout(lyric, PANEL_W - PAD * 2);
        float wr = lay.lines().isEmpty() ? (lk ? 1f : 0f) : lay.lines().size();
        lyricRows = lerp(lyricRows, wr, dt, 8f);
        lyricsRoom = lerp(lyricsRoom, expanded && lk ? 1f : 0f, dt, 8f);
        boolean lr = lyricsRoom > .01f;
        float lh = (LROW * lyricRows + 3f) * lyricsRoom;
        float pH = PAD * 2 + COV + lh + 13f;

        float x = sX, y = sY;
        boolean over = mx >= x && mx <= x + PANEL_W && my >= y && my <= y + pH;
        boolean pr = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean cl = pr && !wasPressed; wasPressed = pr;
        if (pr && over && !dragging) { dragging = true; dragOX = mx - x; dragOY = my - y; }
        if (pr && dragging) {
            sX = Math.max(0, Math.min(sw - PANEL_W, mx - dragOX));
            sY = Math.max(0, Math.min(sh - pH, my - dragOY));
            x = sX; y = sY;
        }
        if (!pr) dragging = false;
        if (cl && over && !dragging) expanded = !expanded;

        int accent2 = withAlpha(accent, .7f);

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(SC, SC);
        g.pose().translate(-x, -y);
        try {
            draw(g, x, y, PANEL_W, pH, accent, accent2, title, artist, source, pos, dur, dt, lh, lr);
        } finally {
            g.pose().popMatrix();
        }
    }

    private static void draw(GuiGraphicsExtractor g, float x, float y, float w, float h,
            int ac, int ac2, String title, String artist, String source,
            long pos, long dur, float dt, float lh, boolean lyricsRow) {
        int txt = 0xFFFFFFFF;
        bg(g, x, y, w, h, openT);
        String stamp = title + "|" + artist;
        if (!stamp.equals(lastTrack)) { lastTrack = stamp; trackFade = 0; }
        trackFade = lerp(trackFade, 1f, dt, 6f);
        float ta = openT * (.25f + .75f * trackFade);
        float sl = (1f - trackFade) * 2.5f;
        float cx = x + PAD;

        cover(g, cx, y + PAD, ac, ac2, dt);
        cx += COV + 7f;
        eq(g, x + w - PAD, y + PAD + 1, ac, dt);
        float dr = x + w - PAD;
        float tw = Math.max(10f, dr - cx - 16f);
        String st = title.isEmpty() ? "Ничего не играет" : title;
        scroll(g, W_TITLE, tKey, st, cx + sl, y + PAD + 1.5f, TITLE_SIZE, withAlpha(txt, ta), tw - sl, 18f, dt);
        String un = artist;
        if (!source.isEmpty()) un = un.isEmpty() ? source : un + " - " + source;
        if (!un.isEmpty()) {
            scroll(g, W_SUB, aKey, un, cx + sl, y + PAD + 14f, ARTIST_SIZE, withAlpha(txt, .5f * ta),
                    dr - cx - sl, 14f, dt);
        }

        float ry = y + PAD + COV;
        if (lyricsRow) { lyrics(g, curLayout, x + PAD, ry + 2, pos, txt, openT * lyricsRoom); ry += lh; }
        bar(g, x + PAD, ry + 3, w - PAD * 2, pos, dur, ac, ac2, txt, dt);
    }

    private static void bg(GuiGraphicsExtractor g, float x, float y, float w, float h, float a) {
        if (a <= .01f) return;
        UiVector.roundRect(g, x, y, w, h, 5f, withAlpha(0xFF0B0B16, .88f * a));
        UiVector.outline(g, x, y, w, h, 5f, .5f, withAlpha(0xFFFFFFFF, .12f * a));
    }

    private static void cover(GuiGraphicsExtractor g, float x, float y, int ac, int ac2, float dt) {
        Identifier c = CoverTexture.get();
        coverFade = lerp(coverFade, c != null ? 1f : 0f, dt, 6f);
        if (c != null && coverFade > .99f) {
            g.blit(RenderPipelines.GUI_TEXTURED, c, (int)x, (int)y, 0, 0, (int)COV, (int)COV, (int)COV, (int)COV, (int)COV, (int)COV, -1);
            UiVector.outline(g, x, y, COV, COV, 5f, .5f, withAlpha(0xFFFFFFFF, .16f * openT));
            return;
        }
        float w = (float)(.5 + .5 * Math.sin(System.currentTimeMillis() / 1600.0));
        int c1 = mix(withAlpha(ac, 1f), withAlpha(ac2, 1f), w);
        int c2 = mix(withAlpha(ac2, 1f), withAlpha(ac, 1f), w);
        UiVector.roundRectBilinear(g, x, y, COV, COV, 6f, 6f, 6f, 6f, withAlpha(c1, openT), withAlpha(c2, openT), withAlpha(c2, openT), withAlpha(c1, openT));
        UiVector.outline(g, x, y, COV, COV, 6f, .5f, withAlpha(0xFFFFFFFF, .16f * openT));
        if (c != null) g.blit(RenderPipelines.GUI_TEXTURED, c, (int)x, (int)y, 0, 0, (int)COV, (int)COV, (int)COV, (int)COV, withAlpha(-1, openT * coverFade));
        CoverArt.ensure(MediaSession.artist(), MediaSession.title());
        float pulse = MediaSession.playing() ? 1f + .08f * (float)Math.sin(System.currentTimeMillis() / 320.0) : 1f;
        int ink = withAlpha(0xFFFFFFFF, .92f * openT);
        float mx = x + COV / 2, my = y + COV / 2, hd = 5.5f * pulse;
        UiVector.roundRect(g, mx - 7, my + 2, hd, hd * .8f, hd / 2, ink);
        UiVector.roundRect(g, mx + 1.5f, my + .5f, hd, hd * .8f, hd / 2, ink);
        UiVector.roundRect(g, mx - 3, my - 6, 1.2f, 9, .6f, ink);
        UiVector.roundRect(g, mx + 5.5f, my - 7.5f, 1.2f, 9, .6f, ink);
        UiVector.roundRect(g, mx - 3, my - 7.5f, 9.7f, 1.6f, .8f, ink);
    }

    private static void eq(GuiGraphicsExtractor g, float right, float top, int ac, float dt) {
        float bw = 2.1f, gap = 1.8f, mh = 9f, bot = top + mh;
        double s = System.currentTimeMillis() / 1000.0;
        beat = lerp(beat, MediaSession.playing() ? 1f : 0f, dt, 5f);
        for (int i = 0; i < 4; i++) {
            double ph = s * (2.6 + i * .47) + i * 1.7;
            double sl = Math.sin(s * (1.1 + i * .19) + i);
            float lv = (float)Math.max(.08, Math.min(1, .5 + .35 * Math.sin(ph) + .15 * sl));
            float bh = Math.max(2.1f, mh * (.18f + .82f * lv * beat));
            float bx = right - bw - i * (bw + gap);
            int col = withAlpha(mix(withAlpha(0xFFFFFFFF, 1f), withAlpha(ac, 1f), lv), (.45f + .45f * lv) * openT);
            UiVector.roundRect(g, bx, bot - bh, bw, bh, bw / 2, col);
        }
    }

    private static void lyrics(GuiGraphicsExtractor g, LyricLayout lay, float x, float y, long pos, int txt, float a) {
        if (lay.lines().isEmpty()) return;
        String key = String.join("\n", lay.lines());
        if (!key.equals(lastLine)) { prevLine = lastLine; lastLine = key; lineFade = 0; sungWidth = cl01(Lyrics.progress(pos)) * tw(W_LYRIC, lay); }
        lineFade = lerp(lineFade, 1f, 1f / 60f, 6f);
        float e = lineFade * lineFade * (3f - 2f * lineFade);
        if (!prevLine.isEmpty() && e < .999f) {
            txt(g, W_LYRIC, prevLine.split("\n")[0], x, y - 2f * e, lay.size, withAlpha(txt, .3f * a * (1f - e)));
        }
        float total = tw(W_LYRIC, lay), sung = cl01(Lyrics.progress(pos)) * total;
        sungWidth = lerp(sungWidth, sung, 1f / 60f, 25f);
        float lift = (1f - e) * 2.5f, fade = a * e, consumed = 0;
        for (int r = 0; r < lay.lines().size(); r++) {
            String ln = lay.lines().get(r);
            float lw = twS(W_LYRIC, ln, lay.size), lit = cl01(sungWidth - consumed); lit = Math.min(lit, lw);
            int cut = 0;
            while (cut < ln.length() && twS(W_LYRIC, ln.substring(0, cut + 1), lay.size) <= lit) cut++;
            String done = ln.substring(0, cut), left = ln.substring(cut);
            float ry = y + r * LROW + lift;
            txt(g, W_LYRIC, done, x, ry, lay.size, withAlpha(txt, fade));
            txt(g, W_LYRIC, left, x + twS(W_LYRIC, done, lay.size), ry, lay.size, withAlpha(txt, .35f * fade));
            consumed += lw;
        }
    }

    private static void bar(GuiGraphicsExtractor g, float x, float y, float w,
            long pos, long dur, int ac, int ac2, int txt, float dt) {
        float tgt = dur > 0 ? cl01(pos / (float)dur) : 0;
        progress = Math.abs(tgt - progress) < .0015f ? tgt : lerp(progress, tgt, dt, 15f);
        String el = time(pos), lt = dur > 0 ? "-" + time(Math.max(0, dur - pos)) : "--:--";
        float ew = twS(W_TIME, el, TIME_SIZE), lw = twS(W_TIME, lt, TIME_SIZE);
        float bx = x + ew + 8, bw = Math.max(10f, w - ew - lw - 16);
        txt(g, W_TIME, el, x, y - 1, TIME_SIZE, withAlpha(txt, .45f * openT));
        txt(g, W_TIME, lt, x + w - lw, y - 1, TIME_SIZE, withAlpha(txt, .45f * openT));
        UiVector.roundRect(g, bx, y + 1.5f, bw, 3, 1.5f, withAlpha(txt, .16f * openT));
        float fl = bw * progress;
        if (fl > .5f) {
            float br = .88f + .12f * (float)Math.sin(System.currentTimeMillis() / 900.0);
            UiVector.roundRectBilinear(g, bx, y + 1.5f, fl, 3, 1.5f, 1.5f, 1.5f, 1.5f,
                    withAlpha(ac, openT * br), withAlpha(ac2, openT), withAlpha(ac, openT * br), withAlpha(ac2, openT));
            UiVector.roundRect(g, bx + fl - 2.25f, y + .5f, 4.5f, 4.5f, 2.25f, withAlpha(0xFFFFFFFF, openT));
        }
    }

    /* ══ ТЕКСТ: MTSDF-атласы xrose_1, ванильный шрифт — только запасной путь ══ */

    private static void txt(GuiGraphicsExtractor g, int weight, String t, float x, float y, float sz, int col) {
        if (t == null || t.isEmpty()) return;
        if (MtsdfTextRenderer.draw(g, weight, t, x, y, sz, col)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) UiText.draw(g, mc.font, t, x, y, col, UiText.FACE, false);
    }

    private static void scroll(GuiGraphicsExtractor g, int weight, Object key, String text, float x, float y,
            float size, int color, float maxW, float speed, float delta) {
        if (text == null || text.isEmpty() || maxW <= 0) return;
        if (MtsdfTextRenderer.scroll(g, weight, key, text, x, y, size, color, maxW, true, speed, delta)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) UiText.draw(g, mc.font, text, x, y, color, UiText.FACE, false);
    }

    /** Ширина строки тем же шрифтом и кеглем, каким она рисуется. */
    private static float twS(int weight, String t, float sz) {
        return t == null || t.isEmpty() ? 0f : MtsdfTextRenderer.width(weight, t, sz);
    }

    private static float tw(int weight, LyricLayout l) {
        float t = 0;
        for (String s : l.lines()) t += twS(weight, s, l.size);
        return Math.max(1f, t);
    }

    /* ══ LyricLayout ══ */
    private record LyricLayout(float size, List<String> lines) {}
    private static LyricLayout layout(String raw, float width) {
        if (raw.isEmpty() || width <= 0) { curLayout = new LyricLayout(LSIZE, List.of()); return curLayout; }
        float size = LSIZE;
        if (twS(W_LYRIC, raw, size) <= width) { curLayout = new LyricLayout(size, List.of(raw)); return curLayout; }
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (String w : raw.split(" ")) {
            StringBuilder t = b.isEmpty() ? a : b;
            String c = t.isEmpty() ? w : t + " " + w;
            if (t == a && twS(W_LYRIC, c, size) > width) { b.append(w); continue; }
            t.setLength(0); t.append(c);
        }
        if (b.isEmpty()) { curLayout = new LyricLayout(size, List.of(a.toString())); return curLayout; }
        curLayout = new LyricLayout(size, List.of(a.toString(), b.toString()));
        return curLayout;
    }

    private static String lyricLine(long p) { Lyrics.Line l = Lyrics.current(p); return l == null ? "" : l.text(); }
    private static String time(long ms) { long s = Math.max(0, ms / 1000); return String.format(Locale.US, "%d:%02d", s / 60, s % 60); }
    private static float lerp(float c, float t, float dt, float sp) { return c + (t - c) * (1f - (float)Math.exp(-sp * dt)); }
    private static float cl01(float v) { return Math.max(0, Math.min(1, v)); }
    private static int withAlpha(int c, float a) { int al = (int)Math.max(0, Math.min(255, ((c >> 24) & 0xFF) * a)); return (al << 24) | (c & 0x00FFFFFF); }
    private static int mix(int a, int b, float t) {
        t = cl01(t);
        int ra = (a >> 24) & 0xFF, ga = (a >> 8) & 0xFF, ba = a & 0xFF, aa = (a >> 24) & 0xFF;
        int rb = (b >> 24) & 0xFF, gb = (b >> 8) & 0xFF, bb = b & 0xFF, ab = (b >> 24) & 0xFF;
        return ((int)(aa + (ab - aa) * t) << 24) | ((int)(ra + (rb - ra) * t) << 16)
                | ((int)(ga + (gb - ga) * t) << 8) | (int)(ba + (bb - ba) * t);
    }
    public static void onClick(int mx, int my) {}
}
