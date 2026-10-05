package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.List;
import java.util.Locale;

/**
 * MusicWidget — точная копия MusicWidget из Xivivide, адаптированная под ElytrixClient.
 * Источник: MusicWidget/src/main/java/platform/client/ui/widget/MusicWidget.java
 *
 * Читает трек из Windows Media Session (Spotify, YouTube и т.д.).
 * Показывает обложку, название, исполнителя, эквалайзер, субтитры, прогресс-бар.
 * Клик по виджету — раскрытие/сворачивание. Перетаскивание.
 */
public final class MusicIsland {
    private MusicIsland() {}

    /* ══ константы (из сурцов) ══ */
    private static final float PANEL_WIDTH = 164.0f;
    private static final float SCALE = 0.85f;
    private static final float PADDING = 6.0f;
    private static final float COVER = 33.0f;
    private static final float LYRIC_SIZE = 8.5f;
    private static final float LYRIC_ROW = 11.0f;

    /* ══ состояние ══ */
    private static float sX = 6f, sY = 80f;
    private static float openT, trackFade, coverFade, lineFade;
    private static float lyricsRoom, lyricRows;
    private static float sungWidth, progress, beat;
    private static long lastFrameNanos;
    private static String lastTrack = "", lastLineShown = "", previousLine = "";
    private static boolean wasPressed, dragging;
    private static float dragOffX, dragOffY;
    private static LyricLayout currentLayout;
    private static boolean expanded = true; // показываем по умолчанию
    private static float expandT = 1f;
    private static float hoverT;

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        MediaSession.keepAlive();
        boolean wantOpen = MediaSession.present();
        long now = Util.getMillis();
        float dt = lastFrameNanos == 0 ? 0.016f : Math.min(0.05f, (now - lastFrameNanos) / 1000f);
        lastFrameNanos = now;

        openT += ((wantOpen ? 1f : 0f) - openT) * (1f - (float) Math.exp(-5f * dt));
        if (openT < 0.005f && !wantOpen) { openT = 0; return; }

        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        int accent = UiTheme.accent(cfg != null ? cfg.accentIndex : 0);
        int accentTwo = UiTheme.withAlpha(accent, 0.7f);

        String title = MediaSession.title();
        String artist = MediaSession.artist();
        String source = MediaSession.source();
        long position = MediaSession.positionMs();
        long duration = MediaSession.durationMs();

        Lyrics.ensure(artist, title);

        /* ══ размеры карточки (из сурцов) ══ */
        String lyric = expanded ? lyricLine(position) : "";
        boolean lyricsKnown = expanded && Lyrics.has();
        LyricLayout layout = lyricLayout(lyric, PANEL_WIDTH - PADDING * 2.0f);
        float wantedRows = layout.lines().isEmpty() ? (lyricsKnown ? 1.0f : 0.0f) : layout.rows();
        lyricRows = lerp(lyricRows, wantedRows, dt, 8f);
        lyricsRoom = lerp(lyricsRoom, expanded && lyricsKnown ? 1.0f : 0.0f, dt, 8f);
        boolean lyricsRow = lyricsRoom > 0.01f;
        float lyricsHeight = (LYRIC_ROW * lyricRows + 3.0f) * lyricsRoom;
        float panelHeight = PADDING * 2.0f + COVER + lyricsHeight + 13.0f;

        /* ══ drag ══ */
        float x = sX, y = sY;
        boolean over = mx >= x && mx <= x + PANEL_WIDTH && my >= y && my <= y + panelHeight;
        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (pressed && over && !dragging) {
            dragging = true;
            dragOffX = mx - x;
            dragOffY = my - y;
        }
        if (pressed && dragging) {
            sX = Math.max(0, Math.min(sw - PANEL_WIDTH, mx - dragOffX));
            sY = Math.max(0, Math.min(sh - panelHeight, my - dragOffY));
            x = sX; y = sY;
        }
        if (!pressed) dragging = false;

        /* клик — свернуть/развернуть субтитры */
        if (clicked && over && !dragging) {
            expanded = !expanded;
        }

        hoverT = lerp(hoverT, over ? 1f : 0f, dt, 12f);

        /* ══ рендер с масштабом (из сурцов) ══ */
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(SCALE, SCALE);
        g.pose().translate(-x, -y);
        try {
            drawCard(g, mc.font, x, y, PANEL_WIDTH, panelHeight, accent, accentTwo,
                    title, artist, source, position, duration, dt, lyricsHeight);
        } finally {
            g.pose().popMatrix();
        }
    }

    /* ════════════════════════════════════════════════════ */
    private static void drawCard(GuiGraphicsExtractor g, Font font, float x, float y,
            float panelWidth, float panelHeight, int accent, int accentTwo,
            String title, String artist, String source, long position, long duration, float dt,
            float lyricsHeight) {

        /* ── фон карточки (из сурцов) ── */
        drawBackground(g, x, y, panelWidth, panelHeight, true, openT);

        /* ── fade при смене трека ── */
        String stamp = title + "|" + artist;
        if (!stamp.equals(lastTrack)) {
            lastTrack = stamp;
            trackFade = 0.0f;
        }
        trackFade = lerp(trackFade, 1.0f, dt, 6f);
        int text = 0xFFFFFFFF;
        float trackAlpha = openT * (0.25f + 0.75f * trackFade);
        float trackSlide = (1.0f - trackFade) * 2.5f;

        float contentX = x + PADDING;

        /* ── обложка ── */
        drawCover(g, contentX, y + PADDING, accent, accentTwo, dt);
        contentX += COVER + 7.0f;

        /* ── эквалайзер ── */
        float dotsRight = x + panelWidth - PADDING;
        drawEqualizer(g, dotsRight, y + PADDING + 1.0f, accent, text, dt);

        /* ── название (прокручиваемое) ── */
        float titleWidth = Math.max(10.0f, dotsRight - contentX - 16.0f);
        float delta = dt;
        String shownTitle = title.isEmpty() ? "Ничего не играет" : title;
        drawScrollText(g, font, shownTitle, contentX + trackSlide, y + PADDING + 1.5f,
                9.0f, withAlpha(text, trackAlpha), titleWidth - trackSlide, true, 18.0f, delta);

        /* ── исполнитель + источник ── */
        String under = artist;
        if (!source.isEmpty()) {
            under = under.isEmpty() ? source : under + " - " + source;
        }
        if (!under.isEmpty()) {
            drawScrollText(g, font, under, contentX + trackSlide, y + PADDING + 14.0f,
                    7.0f, withAlpha(text, 0.5f * trackAlpha), dotsRight - contentX - trackSlide,
                    true, 14.0f, delta);
        }

        /* ── субтитры ── */
        float rowY = y + PADDING + COVER;
        if (lyricsRoom > 0.01f && currentLayout != null) {
            drawLyrics(g, font, currentLayout, x + PADDING, rowY + 2.0f, LYRIC_ROW, position, text,
                    openT * lyricsRoom);
            rowY += lyricsHeight;
        }

        /* ── прогресс-бар ── */
        drawProgress(g, font, x + PADDING, rowY + 3.0f, panelWidth - PADDING * 2.0f,
                position, duration, accent, accentTwo, text, dt);
    }

    /* ══ фон карточки (из сурцов Widget.drawBackground) ══ */
    private static void drawBackground(GuiGraphicsExtractor g, float x, float y,
            float width, float height, boolean glow, float animation) {
        if (animation <= 0.01f) return;
        int bg = withAlpha(0xFF0B0B16, 0.88f * animation);
        UiVector.roundRect(g, x, y, width, height, 5.0f, bg);
        UiVector.outline(g, x, y, width, height, 5.0f, 0.5f,
                withAlpha(0xFFFFFFFF, 0.12f * animation));
    }

    /* ══ обложка (из сурцов MusicWidget.drawCover) ══ */
    private static void drawCover(GuiGraphicsExtractor g, float x, float y,
            int accent, int accentTwo, float dt) {
        Identifier cover = CoverTexture.get();
        coverFade = lerp(coverFade, cover != null ? 1.0f : 0.0f, dt, 6f);

        if (cover != null && coverFade > 0.99f) {
            g.blit(RenderPipelines.GUI_TEXTURED, cover,
                    (int) x, (int) y, 0f, 0f,
                    (int) COVER, (int) COVER,
                    (int) COVER, (int) COVER,
                    (int) COVER, (int) COVER, -1);
            UiVector.outline(g, x, y, COVER, COVER, 5.0f, 0.5f,
                    withAlpha(0xFFFFFFFF, 0.16f * openT));
            return;
        }

        /* градиентный фон с волной (из сурцов) */
        float wave = (float) (0.5d + 0.5d * Math.sin(System.currentTimeMillis() / 1600.0d));
        int first = mix(withAlpha(accent, 1.0f), withAlpha(accentTwo, 1.0f), wave);
        int second = mix(withAlpha(accentTwo, 1.0f), withAlpha(accent, 1.0f), wave);
        UiVector.roundRectBilinear(g, x, y, COVER, COVER, 6.0f, 6.0f, 6.0f, 6.0f,
                withAlpha(first, openT), withAlpha(second, openT),
                withAlpha(second, openT), withAlpha(first, openT));
        UiVector.outline(g, x, y, COVER, COVER, 6.0f, 0.5f,
                withAlpha(0xFFFFFFFF, 0.16f * openT));

        if (cover != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, cover,
                    (int) x, (int) y, 0f, 0f,
                    (int) COVER, (int) COVER,
                    (int) COVER, (int) COVER,
                    (int) COVER, (int) COVER,
                    withAlpha(-1, openT * coverFade));
        }

        /* нота (из сурцов) */
        float pulse = MediaSession.playing()
                ? 1.0f + 0.08f * (float) Math.sin(System.currentTimeMillis() / 320.0d) : 1.0f;
        int ink = withAlpha(0xFFFFFFFF, 0.92f * openT);
        float cx = x + COVER / 2.0f;
        float cy = y + COVER / 2.0f;
        float head = 5.5f * pulse;
        UiVector.roundRect(g, cx - 7.0f, cy + 2.0f, head, head * 0.8f, head / 2.0f, ink);
        UiVector.roundRect(g, cx + 1.5f, cy + 0.5f, head, head * 0.8f, head / 2.0f, ink);
        UiVector.roundRect(g, cx - 3.0f, cy - 6.0f, 1.2f, 9.0f, 0.6f, ink);
        UiVector.roundRect(g, cx + 5.5f, cy - 7.5f, 1.2f, 9.0f, 0.6f, ink);
        UiVector.roundRect(g, cx - 3.0f, cy - 7.5f, 9.7f, 1.6f, 0.8f, ink);
    }

    /* ══ эквалайзер (из сурцов MusicWidget.drawEqualizer) ══ */
    private static void drawEqualizer(GuiGraphicsExtractor g, float right, float top,
            int accent, int text, float dt) {
        float barWidth = 2.1f;
        float gap = 1.8f;
        float maxHeight = 9.0f;
        float bottom = top + maxHeight;
        double seconds = System.currentTimeMillis() / 1000.0d;
        beat = lerp(beat, MediaSession.playing() ? 1.0f : 0.0f, dt, 5f);
        for (int index = 0; index < 4; index++) {
            double phase = seconds * (2.6d + index * 0.47d) + index * 1.7d;
            double slow = Math.sin(seconds * (1.1d + index * 0.19d) + index);
            float level = (float) Math.max(0.08, Math.min(1.0,
                    0.5d + 0.35d * Math.sin(phase) + 0.15d * slow));
            float height = Math.max(2.1f, maxHeight * (0.18f + 0.82f * level * beat));
            float barX = right - barWidth - index * (barWidth + gap);
            int color = withAlpha(mix(withAlpha(text, 1.0f), withAlpha(accent, 1.0f), level),
                    (0.45f + 0.45f * level) * openT);
            UiVector.roundRect(g, barX, bottom - height, barWidth, height, barWidth / 2.0f, color);
        }
    }

    /* ══ субтитры (из сурцов MusicWidget.drawLyrics) ══ */
    private static void drawLyrics(GuiGraphicsExtractor g, Font font, LyricLayout layout,
            float x, float y, float rowHeight, long position, int text, float alpha) {
        if (layout.lines().isEmpty()) return;
        String key = String.join("\n", layout.lines());
        if (!key.equals(lastLineShown)) {
            previousLine = lastLineShown;
            lastLineShown = key;
            lineFade = 0.0f;
            sungWidth = clamp01(Lyrics.progress(position)) * totalWidth(font, layout);
        }
        lineFade = lerp(lineFade, 1.0f, 1f / 60f, 6f);
        float ease = lineFade * lineFade * (3.0f - 2.0f * lineFade);

        if (!previousLine.isEmpty() && ease < 0.999f) {
            String leaving = previousLine.split("\n")[0];
            drawText(g, font, leaving, x, y - 2.0f * ease, layout.size,
                    withAlpha(text, 0.3f * alpha * (1.0f - ease)));
        }

        float total = totalWidth(font, layout);
        float sung = clamp01(Lyrics.progress(position)) * total;
        sungWidth = lerp(sungWidth, sung, 1f / 60f, 25f);
        float lift = (1.0f - ease) * 2.5f;
        float fade = alpha * ease;

        float consumed = 0.0f;
        for (int row = 0; row < layout.lines().size(); row++) {
            String line = layout.lines().get(row);
            float lineWidth = textWidth(font, line, layout.size);
            float lit = clamp01(sungWidth - consumed);
            lit = Math.min(lit, lineWidth);
            int cut = 0;
            while (cut < line.length() && textWidth(font, line.substring(0, cut + 1), layout.size) <= lit) {
                cut++;
            }
            String done = line.substring(0, cut);
            String left = line.substring(cut);
            float rowY = y + row * rowHeight + lift;
            drawText(g, font, done, x, rowY, layout.size, withAlpha(text, fade));
            drawText(g, font, left, x + textWidth(font, done, layout.size), rowY, layout.size,
                    withAlpha(text, 0.35f * fade));
            consumed += lineWidth;
        }
    }

    /* ══ прогресс-бар (из сурцов MusicWidget.drawProgress) ══ */
    private static void drawProgress(GuiGraphicsExtractor g, Font font, float x, float y, float width,
            long position, long duration, int accent, int accentTwo, int text, float dt) {
        float target = duration > 0 ? clamp01(position / (float) duration) : 0.0f;
        progress = Math.abs(target - progress) < 0.0015f ? target : lerp(progress, target, dt, 15f);

        String elapsed = time(position);
        String left = duration > 0 ? "-" + time(Math.max(0, duration - position)) : "--:--";
        float timeSize = 6.0f;
        float elapsedWidth = textWidth(font, elapsed, timeSize);
        float leftWidth = textWidth(font, left, timeSize);
        float barX = x + elapsedWidth + 8.0f;
        float barWidth = Math.max(10.0f, width - elapsedWidth - leftWidth - 16.0f);

        drawText(g, font, elapsed, x, y - 1.0f, timeSize, withAlpha(text, 0.45f * openT));
        drawText(g, font, left, x + width - leftWidth, y - 1.0f, timeSize, withAlpha(text, 0.45f * openT));
        UiVector.roundRect(g, barX, y + 1.5f, barWidth, 3.0f, 1.5f, withAlpha(text, 0.16f * openT));

        float filled = barWidth * progress;
        if (filled > 0.5f) {
            float breath = 0.88f + 0.12f * (float) Math.sin(System.currentTimeMillis() / 900.0d);
            int start = withAlpha(accent, openT * breath);
            int end = withAlpha(accentTwo, openT);
            UiVector.roundRectBilinear(g, barX, y + 1.5f, filled, 3.0f, 1.5f, 1.5f, 1.5f, 1.5f,
                    start, end, start, end);
            UiVector.roundRect(g, barX + filled - 2.25f, y + 0.5f, 4.5f, 4.5f, 2.25f,
                    withAlpha(0xFFFFFFFF, openT));
        }
    }

    /* ══ прокручиваемый текст (из сурцов Font.a с scroll) ══ */
    private static final java.util.Map<Object, float[]> SCROLL_STATES = new java.util.WeakHashMap<>();
    private static Object titleScrollKey = new Object();
    private static Object artistScrollKey = new Object();

    private static void drawScrollText(GuiGraphicsExtractor g, Font font, String text,
            float x, float y, float size, int color, float maxWidth,
            boolean isHovered, float speed, float delta) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return;
        float textW = textWidth(font, text, size);
        Object key = y < 100 ? titleScrollKey : artistScrollKey; // простая дедукция
        if (textW <= maxWidth) {
            SCROLL_STATES.remove(key);
            drawText(g, font, text, x, y, size, color);
            return;
        }
        float[] s = SCROLL_STATES.computeIfAbsent(key, k -> new float[]{0.0f, 1.0f});
        float maxOffset = textW - maxWidth;
        float dt = Math.max(0, Math.min(delta, 0.05f));
        if (isHovered) {
            s[0] += s[1] * speed * dt;
            if (s[0] >= maxOffset) { s[0] = maxOffset; s[1] = -1.0f; }
            else if (s[0] <= 0) { s[0] = 0; s[1] = 1.0f; }
        } else {
            s[0] += (0 - s[0]) * Math.min(1, dt * 12f);
            if (s[0] < 0.3f) { s[0] = 0; s[1] = 1.0f; }
        }
        /* clip */
        int sw = g.guiWidth(), sh = g.guiHeight();
        int x0 = Math.max(0, (int) (x - 1));
        int y0 = Math.max(0, (int) (y - size * 0.5f));
        int x1 = Math.min(sw, (int) Math.ceil(x + maxWidth + 1));
        int y1 = Math.min(sh, (int) Math.ceil(y + size * 1.5f + 0.5f));
        if (x1 > x0 && y1 > y0) {
            g.enableScissor(x0, y0, x1, y1);
            drawText(g, font, text, x - s[0], y, size, color);
            g.disableScissor();
        }
    }

    /* ══ вспомогательные рисование ══ */

    private static void drawText(GuiGraphicsExtractor g, Font font, String text,
            float x, float y, float size, int color) {
        if (text == null || text.isEmpty()) return;
        UiText.draw(g, font, text, (int) x, (int) y, color, UiText.FACE, false);
    }

    private static float textWidth(Font font, String text, float size) {
        if (text == null || text.isEmpty()) return 0;
        return font.width(text) * (size / 9.0f);
    }

    /* ══ LyricLayout (из сурцов) ══ */
    private record LyricLayout(float size, List<String> lines) {
        float rows() { return lines.size(); }
    }

    private static LyricLayout lyricLayout(String raw, float width) {
        if (raw.isEmpty() || width <= 0) {
            currentLayout = new LyricLayout(LYRIC_SIZE, List.of());
            return currentLayout;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            currentLayout = new LyricLayout(LYRIC_SIZE, List.of());
            return currentLayout;
        }
        Font font = mc.font;
        for (float size = LYRIC_SIZE; size >= LYRIC_SIZE - 0.5f; size -= 0.5f) {
            if (textWidth(font, raw, size) <= width) {
                currentLayout = new LyricLayout(size, List.of(raw));
                return currentLayout;
            }
        }
        float size = LYRIC_SIZE - 0.5f;
        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        for (String word : raw.split(" ")) {
            StringBuilder target = second.isEmpty() ? first : second;
            String candidate = target.isEmpty() ? word : target + " " + word;
            if (target == first && textWidth(font, candidate, size) > width) {
                second.append(word);
                continue;
            }
            target.setLength(0);
            target.append(candidate);
        }
        if (second.isEmpty()) {
            currentLayout = new LyricLayout(size, List.of(first.toString()));
            return currentLayout;
        }
        while (size > 4.0f && textWidth(font, second.toString(), size) > width) {
            size -= 0.25f;
        }
        currentLayout = new LyricLayout(size, List.of(first.toString(), second.toString()));
        return currentLayout;
    }

    private static float totalWidth(Font font, LyricLayout layout) {
        float total = 0;
        for (String line : layout.lines()) total += textWidth(font, line, layout.size);
        return Math.max(1.0f, total);
    }

    private static String lyricLine(long position) {
        Lyrics.Line line = Lyrics.current(position);
        return line == null ? "" : line.text();
    }

    private static String time(long millis) {
        long seconds = Math.max(0, millis / 1000L);
        return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    /* ══ математика ══ */
    private static float lerp(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }
    private static float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    private static int withAlpha(int color, float a) {
        int alpha = (int) Math.max(0, Math.min(255, ((color >> 24) & 0xFF) * a));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
    private static int mix(int a, int b, float t) {
        t = clamp01(t);
        int ra = (a >> 16) & 0xFF, ga = (a >> 8) & 0xFF, ba = a & 0xFF, aa = (a >> 24) & 0xFF;
        int rb = (b >> 16) & 0xFF, gb = (b >> 8) & 0xFF, bb = b & 0xFF, ab = (b >> 24) & 0xFF;
        int r = (int) (ra + (rb - ra) * t);
        int gr = (int) (ga + (gb - ga) * t);
        int bl = (int) (ba + (bb - ba) * t);
        int al = (int) (aa + (ab - aa) * t);
        return (al << 24) | (r << 16) | (gr << 8) | bl;
    }

    public static void onClick(int mx, int my) {}
}