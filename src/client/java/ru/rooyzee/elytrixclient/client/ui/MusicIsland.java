package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.pip.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.media.CoverTexture;
import ru.rooyzee.elytrixclient.client.media.Lyrics;
import ru.rooyzee.elytrixclient.client.media.MediaSession;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.List;
import java.util.Locale;

/**
 * Music Widget — компактный виджет, читающий из Windows Media Session.
 * Показывает обложку, название, исполнителя, эквалайзер, субтитры, прогресс-бар.
 * Можно перетаскивать мышью.
 */
public final class MusicIsland {
    private MusicIsland() {}

    private static final float PANEL_W = 164f;
    private static final float PADDING = 6f;
    private static final float COVER_SIZE = 33f;
    private static final float LYRIC_SIZE = 8.5f;
    private static final float LYRIC_ROW_H = 11f;

    private static float openT, trackFade, coverFade, lyricsRoom, lyricRows, lineFade, beat;
    private static long lastNanos;
    private static String lastTrack = "", lastLineShown = "", previousLine = "";
    private static float sungWidth, progress;
    private static float sX = 6f, sY = 80f;
    private static boolean wasPressed, dragging;
    private static float dragOffX, dragOffY;
    private static LyricLayout currentLayout;

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        MediaSession.keepAlive();
        boolean wantOpen = MediaSession.present();

        long now = Util.getMillis();
        float dt = lastNanos == 0 ? 0.016f : Math.min(0.05f, (now - lastNanos) / 1000f);
        lastNanos = now;
        openT += ((wantOpen ? 1f : 0f) - openT) * (1f - (float) Math.exp(-5f * dt));
        if (openT < 0.005f && !wantOpen) { openT = 0; return; }

        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);

        String title = MediaSession.title();
        String artist = MediaSession.artist();
        String source = MediaSession.source();
        long position = MediaSession.positionMs();
        long duration = MediaSession.durationMs();

        Lyrics.ensure(artist, title);
        String lyric = lyricLine(position);
        boolean lyricsKnown = Lyrics.has();
        LyricLayout layout = lyricLayout(lyric, PANEL_W - PADDING * 2f);
        float wantedRows = layout.lines().isEmpty() ? (lyricsKnown ? 1f : 0f) : layout.rows();
        lyricRows = lerp(lyricRows, wantedRows, dt, 8f);
        lyricsRoom = lerp(lyricsRoom, lyricsKnown ? 1f : 0f, dt, 8f);
        float lyricsH = (LYRIC_ROW_H * lyricRows + 3f) * lyricsRoom;
        float panelH = PADDING * 2f + COVER_SIZE + lyricsH + 13f;

        float x = sX, y = sY;
        boolean over = mx >= x && mx <= x + PANEL_W && my >= y && my <= y + panelH;
        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (pressed && over && !dragging) {
            dragging = true; dragOffX = mx - x; dragOffY = my - y;
        }
        if (pressed && dragging) {
            sX = Math.max(0, Math.min(sw - PANEL_W, mx - dragOffX));
            sY = Math.max(0, Math.min(sh - panelH, my - dragOffY));
            x = sX; y = sY;
        }
        if (!pressed) dragging = false;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.85f, 0.85f);
        g.pose().translate(-x, -y);
        try {
            drawCard(g, mc.font, x, y, PANEL_W, panelH, accent,
                    title, artist, source, position, duration, dt, lyricsH);
        } finally {
            g.pose().popMatrix();
        }
    }

    private static void drawCard(GuiGraphicsExtractor g, Font font, float x, float y,
            float w, float h, int accent, String title, String artist, String source,
            long position, long duration, float dt, float lyricsH) {
        int text = 0xFFFFFFFF;

        /* фон */
        UiVector.shadow(g, x, y, w, h, 5f, 4f, 0x28000000, 5);
        UiVector.roundRect(g, x, y, w, h, 5f, UiTheme.mix(0xFF0D0D0D, UiTheme.withAlpha(accent, 0.06f), 0.1f));
        UiVector.outline(g, x, y, w, h, 5f, 0.4f, UiTheme.withAlpha(0xFFFFFFFF, 0.08f));

        /* track fade */
        String stamp = title + "|" + artist;
        if (!stamp.equals(lastTrack)) { lastTrack = stamp; trackFade = 0; }
        trackFade = lerp(trackFade, 1f, dt, 6f);
        float tAlpha = (0.25f + 0.75f * trackFade) * openT;

        float contentX = x + PADDING;

        /* обложка */
        drawCover(g, contentX, y + PADDING, accent, dt);
        contentX += COVER_SIZE + 7f;

        /* эквалайзер */
        drawEqualizer(g, x + w - PADDING, y + PADDING + 1f, accent, dt);

        /* название */
        float dotsRight = x + w - PADDING;
        float titleW = Math.max(10f, dotsRight - contentX - 16f);
        float slide = (1f - trackFade) * 2.5f;
        String shownTitle = title.isEmpty() ? "Ничего не играет" : title;
        drawTextClipped(g, font, shownTitle, contentX + slide, y + PADDING + 1.5f,
                UiTheme.withAlpha(text, tAlpha), titleW - slide);

        /* исполнитель + источник */
        String under = artist;
        if (!source.isEmpty()) under = under.isEmpty() ? source : under + " — " + source;
        if (!under.isEmpty()) {
            drawTextClipped(g, font, under, contentX + slide, y + PADDING + 14f,
                    UiTheme.withAlpha(text, 0.5f * tAlpha), dotsRight - contentX - slide);
        }

        /* субтитры */
        float rowY = y + PADDING + COVER_SIZE;
        if (lyricsRoom > 0.01f) {
            drawLyrics(g, font, x + PADDING, rowY + 2f, position, text, openT * lyricsRoom);
            rowY += lyricsH;
        }

        /* прогресс */
        drawProgress(g, font, x + PADDING, rowY + 3f, w - PADDING * 2f, position, duration, accent, text, dt);
    }

    private static void drawCover(GuiGraphicsExtractor g, float x, float y, int accent, float dt) {
        Identifier cover = CoverTexture.get();
        coverFade = lerp(coverFade, cover != null ? 1f : 0f, dt, 6f);
        if (cover != null && coverFade > 0.99f) {
            g.blit(RenderPipelines.GUI_TEXTURED, cover, (int) x, (int) y, 0f, 0f,
                    (int) COVER_SIZE, (int) COVER_SIZE, (int) COVER_SIZE, (int) COVER_SIZE,
                    (int) COVER_SIZE, (int) COVER_SIZE, -1);
            return;
        }
        float wave = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1600.0));
        int c1 = UiTheme.mix(accent, UiTheme.withAlpha(accent, 0.7f), wave);
        int c2 = UiTheme.mix(UiTheme.withAlpha(accent, 0.7f), accent, wave);
        UiVector.roundRect(g, x, y, COVER_SIZE, COVER_SIZE, 6f, UiTheme.withAlpha(c1, openT));
        UiVector.outline(g, x, y, COVER_SIZE, COVER_SIZE, 6f, 0.5f, UiTheme.withAlpha(0xFFFFFFFF, 0.16f * openT));
        if (cover != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, cover, (int) x, (int) y, 0f, 0f,
                    (int) COVER_SIZE, (int) COVER_SIZE, (int) COVER_SIZE, (int) COVER_SIZE,
                    (int) COVER_SIZE, (int) COVER_SIZE, UiTheme.withAlpha(-1, openT * coverFade));
        }
        /* нота */
        float pulse = MediaSession.playing() ? 1f + 0.08f * (float) Math.sin(System.currentTimeMillis() / 320.0) : 1f;
        int ink = UiTheme.withAlpha(0xFFFFFFFF, 0.92f * openT);
        float cx = x + COVER_SIZE / 2f, cy = y + COVER_SIZE / 2f;
        float head = 5.5f * pulse;
        UiVector.roundRect(g, cx - 7f, cy + 2f, head, head * 0.8f, head / 2f, ink);
        UiVector.roundRect(g, cx + 1.5f, cy + 0.5f, head, head * 0.8f, head / 2f, ink);
        UiVector.roundRect(g, cx - 3f, cy - 6f, 1.2f, 9f, 0.6f, ink);
        UiVector.roundRect(g, cx + 5.5f, cy - 7.5f, 1.2f, 9f, 0.6f, ink);
        UiVector.roundRect(g, cx - 3f, cy - 7.5f, 9.7f, 1.6f, 0.8f, ink);
    }

    private static void drawEqualizer(GuiGraphicsExtractor g, float right, float top, int accent, float dt) {
        float barW = 2.1f, gap = 1.8f, maxH = 9f;
        float bottom = top + maxH;
        double sec = System.currentTimeMillis() / 1000.0;
        beat = lerp(beat, MediaSession.playing() ? 1f : 0f, dt, 5f);
        for (int i = 0; i < 4; i++) {
            double phase = sec * (2.6 + i * 0.47) + i * 1.7;
            double slow = Math.sin(sec * (1.1 + i * 0.19) + i);
            float level = (float) Math.max(0.08, Math.min(1, 0.5 + 0.35 * Math.sin(phase) + 0.15 * slow));
            float bh = Math.max(2.1f, maxH * (0.18f + 0.82f * level * beat));
            float bx = right - barW - i * (barW + gap);
            int col = UiTheme.withAlpha(UiTheme.mix(0xFFFFFFFF, accent, level), (0.45f + 0.45f * level) * openT);
            UiVector.roundRect(g, bx, bottom - bh, barW, bh, barW / 2f, col);
        }
    }

    private static void drawLyrics(GuiGraphicsExtractor g, Font font, float x, float y,
            long position, int text, float alpha) {
        LyricLayout layout = currentLayout;
        if (layout == null || layout.lines().isEmpty()) return;
        String key = String.join("\n", layout.lines());
        if (!key.equals(lastLineShown)) {
            previousLine = lastLineShown;
            lastLineShown = key;
            lineFade = 0;
            sungWidth = Lyrics.progress(position) * totalWidth(font, layout);
        }
        lineFade = lerp(lineFade, 1f, 1f / 60f, 6f);
        float ease = lineFade * lineFade * (3f - 2f * lineFade);

        float total = totalWidth(font, layout);
        float sung = Lyrics.progress(position) * total;
        sungWidth = lerp(sungWidth, sung, 1f / 60f, 25f);
        float lift = (1f - ease) * 2.5f;
        float fade = alpha * ease;

        float consumed = 0;
        for (int row = 0; row < layout.lines().size(); row++) {
            String line = layout.lines().get(row);
            float lw = font.width(line) * (layout.size() / 9f);
            float lit = Math.max(0, Math.min(lw, sungWidth - consumed));
            int cut = 0;
            while (cut < line.length() && font.width(line.substring(0, cut + 1)) * (layout.size() / 9f) <= lit) cut++;
            String done = line.substring(0, cut);
            String left = line.substring(cut);
            float rowY2 = y + row * LYRIC_ROW_H + lift;
            g.text(font, done, (int) x, (int) rowY2, UiTheme.withAlpha(text, fade), false);
            float doneW = font.width(done) * (layout.size() / 9f);
            g.text(font, left, (int) (x + doneW), (int) rowY2, UiTheme.withAlpha(text, 0.35f * fade), false);
            consumed += lw;
        }
    }

    private static void drawProgress(GuiGraphicsExtractor g, Font font, float x, float y, float width,
            long position, long duration, int accent, int text, float dt) {
        float target = duration > 0 ? Math.max(0, Math.min(1, position / (float) duration)) : 0;
        progress = Math.abs(target - progress) < 0.0015f ? target : lerp(progress, target, dt, 15f);

        String elapsed = time(position);
        String left = duration > 0 ? "-" + time(Math.max(0, duration - position)) : "--:--";
        float elapsedW = font.width(elapsed);
        float leftW = font.width(left);
        float barX = x + elapsedW + 8f;
        float barW = Math.max(10f, width - elapsedW - leftW - 16f);

        g.text(font, elapsed, (int) x, (int) (y - 1f), UiTheme.withAlpha(text, 0.45f * openT), false);
        g.text(font, left, (int) (x + width - leftW), (int) (y - 1f), UiTheme.withAlpha(text, 0.45f * openT), false);
        UiVector.roundRect(g, barX, y + 1.5f, barW, 3f, 1.5f, UiTheme.withAlpha(text, 0.16f * openT));

        float filled = barW * progress;
        if (filled > 0.5f) {
            float breath = 0.88f + 0.12f * (float) Math.sin(System.currentTimeMillis() / 900.0);
            UiVector.roundRect(g, barX, y + 1.5f, filled, 3f, 1.5f, UiTheme.withAlpha(accent, openT * breath));
            UiVector.roundRect(g, barX + filled - 2.25f, y + 0.5f, 4.5f, 4.5f, 2.25f, UiTheme.withAlpha(0xFFFFFFFF, openT));
        }
    }

    /* ── вспомогательные ── */
    private record LyricLayout(float size, List<String> lines) {
        float rows() { return lines.size(); }
    }

    private static LyricLayout lyricLayout(String raw, float width) {
        if (raw.isEmpty() || width <= 0) { currentLayout = new LyricLayout(LYRIC_SIZE, List.of()); return currentLayout; }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) { currentLayout = new LyricLayout(LYRIC_SIZE, List.of()); return currentLayout; }
        Font font = mc.font;
        for (float size = LYRIC_SIZE; size >= LYRIC_SIZE - 0.5f; size -= 0.5f) {
            if (font.width(raw) * (size / 9f) <= width) { currentLayout = new LyricLayout(size, List.of(raw)); return currentLayout; }
        }
        float size = LYRIC_SIZE - 0.5f;
        StringBuilder first = new StringBuilder(), second = new StringBuilder();
        for (String word : raw.split(" ")) {
            StringBuilder target = second.isEmpty() ? first : second;
            String candidate = target.isEmpty() ? word : target + " " + word;
            if (target == first && font.width(candidate) * (size / 9f) > width) { second.append(word); continue; }
            target.setLength(0); target.append(candidate);
        }
        if (second.isEmpty()) { currentLayout = new LyricLayout(size, List.of(first.toString())); return currentLayout; }
        while (size > 4f && font.width(second.toString()) * (size / 9f) > width) size -= 0.25f;
        currentLayout = new LyricLayout(size, List.of(first.toString(), second.toString()));
        return currentLayout;
    }

    private static float totalWidth(Font font, LyricLayout layout) {
        float t = 0;
        for (String l : layout.lines()) t += font.width(l) * (layout.size() / 9f);
        return Math.max(1f, t);
    }

    private static String lyricLine(long position) {
        Lyrics.Line line = Lyrics.current(position);
        return line == null ? "" : line.text();
    }

    private static void drawTextClipped(GuiGraphicsExtractor g, Font font, String text,
            float x, float y, int color, float maxW) {
        if (text == null || text.isEmpty()) return;
        String shown = text;
        if (font.width(shown) > maxW) {
            int end = shown.length();
            while (end > 0 && font.width(shown.substring(0, end) + "...") > maxW) end--;
            shown = end > 0 ? shown.substring(0, end) + "..." : "...";
        }
        g.text(font, shown, (int) x, (int) y, color, false);
    }

    private static String time(long millis) {
        long s = Math.max(0, millis / 1000);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private static float lerp(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }

    public static void onClick(int mx, int my) {}
}