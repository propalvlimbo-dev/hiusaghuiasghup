package platform.client.ui.widget;

import static platform.api.module.Interface.aM_;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import platform.api.event.GlobalEvent;
import platform.api.event.events.render.DrawEvent;
import platform.api.module.setting.BooleanSetting;
import platform.api.system.configs.ThemeInfo;
import platform.client.Xivivide;
import platform.client.ui.element.DragInfo;
import platform.client.utils.math.MathUtil;
import platform.client.utils.media.Lyrics;
import platform.client.utils.media.MediaSession;
import platform.client.utils.render.ColorUtil;
import platform.client.utils.render.EasingList;
import platform.client.utils.render.Fonts;

/**
 * What you are listening to, on the hud: a cover tile, the track and who it is by, the line being sung right
 * now, and a bar that fills as the song goes on.
 *
 * The track is read from Windows' own media session, so whatever holds the play button - Spotify, a browser
 * on SoundCloud, anything - is what shows up, along with the name of that service. Lyrics arrive with their
 * timings, so the part already sung is bright and what is still to come stays dim.
 */
public class MusicWidget extends Widget {

    private static final float PANEL_WIDTH = 164.0f;
    private static final float SCALE = 0.85f;
    private Object titleScroll = new Object();
    private Object artistScroll = new Object();
    private String lastUnder = "";
    private long lastFrameNanos;

    private static final float PADDING = 6.0f;
    private static final float COVER = 33.0f;
    /** Where the lyric line starts out: it shrinks from here, or wraps, so the whole line always shows. */
    private static final float LYRIC_SIZE = 8.5f;
    private static final float LYRIC_ROW = 11.0f;

    private final BooleanSetting showLyrics = new BooleanSetting("Субтитры", true);
    private final BooleanSetting showSource = new BooleanSetting("Источник", true);
    private final BooleanSetting showCover = new BooleanSetting("Обложка", true);

    /** Smoothed play position, so the bar glides instead of stepping with every read. */
    private float progress;
    /** Fades a new line of lyrics in as the last one leaves. */
    private float lineFade = 1.0f;
    private String lastLine = "";
    /** Height of the lyric row, opening and closing with it. */
    private float lyricsRoom;
    /** How many rows the line takes, eased so the card grows smoothly into a second one. */
    private float lyricRows = 1.0f;
    /** Fades the track in when it changes, and the cover in when one is loaded. */
    private float trackFade = 1.0f;
    private float coverFade;
    private String lastTrack = "";
    /** Exact width of the sung part of the line, eased so it creeps rather than steps. */
    private float sungWidth;
    private String lastLineShown = "";
    private String previousLine = "";
    /** Settles the visualiser down when the music stops. */
    private float beat;

    public MusicWidget() {
        super(new DragInfo("Музыка", 6.0f, 80.0f, 0.0f, 0.0f));
        j().a(this);
        addSettings(this.showLyrics, this.showSource, this.showCover);
    }

    @Override
    public void a(DrawEvent event) {
        GuiGraphicsExtractor context = event.i();
        if (context == null) {
            super.a(event);
            return;
        }
        MediaSession.keepAlive();
        d().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        float alpha = a();
        if (alpha <= 0.01f) {
            super.a(event);
            return;
        }
        // Keep the card at the size selected for this layout.
        float size = SCALE;
        j().setScale(size);
        float originX = j().a();
        float originY = j().b();
        context.pose().pushMatrix();
        context.pose().translate(originX, originY);
        context.pose().scale(size, size);
        context.pose().translate(-originX, -originY);
        try {
            draw(event, context, alpha);
        } finally {
            // Whatever happens in between, the scaling this widget put on must come back off
            context.pose().popMatrix();
        }
        super.a(event);
    }

    private void draw(DrawEvent event, GuiGraphicsExtractor context, float alpha) {
        // A song name is the song's own: the interface language must not touch it, or Russian lyrics come
        // out as half-translated latin
        platform.client.utils.render.Font.beginUntranslated();
        try {
            drawCard(event, context, alpha);
        } finally {
            platform.client.utils.render.Font.endUntranslated();
        }
    }

    private void drawCard(DrawEvent event, GuiGraphicsExtractor context, float alpha) {
        String title = MediaSession.title();
        String artist = MediaSession.artist();
        String source = MediaSession.source();
        long position = MediaSession.positionMs();
        long duration = MediaSession.durationMs();
        if (this.showLyrics.c().booleanValue()) {
            Lyrics.ensure(artist, title);
        }

        int accent = Xivivide.h().d().o().a(ThemeInfo.PRIMARY).a();
        int accentTwo = Xivivide.h().d().o().a(ThemeInfo.SECONDARY).a();
        int text = hudText();
        float x = j().a();
        float y = j().b();
        float panelWidth = PANEL_WIDTH;
        long lyricPosition = position;
        String lyric = this.showLyrics.c().booleanValue() ? lyricLine(lyricPosition) : "";
        // The row stays open for as long as the track has lyrics at all: letting it close between lines is
        // what made the whole card jump up and down
        boolean lyricsKnown = this.showLyrics.c().booleanValue() && Lyrics.has();
        // The lyric row opens and closes rather than appearing at once, so the card grows into it
        LyricLayout layout = lyricLayout(lyric, panelWidth - PADDING * 2.0f);
        // Two rows of text need two rows of room, and the card grows into them rather than jumping
        // Between two lines there is nothing to show, but the room is kept so the card does not bob
        float wantedRows = layout.lines().isEmpty() ? (lyricsKnown ? 1.0f : 0.0f) : layout.rows();
        this.lyricRows = MathUtil.c(this.lyricRows, wantedRows, 0.22f);
        this.lyricsRoom = MathUtil.c(this.lyricsRoom, lyricsKnown ? 1.0f : 0.0f, 0.22f);
        boolean lyricsRow = this.lyricsRoom > 0.01f;
        float lyricsHeight = (LYRIC_ROW * this.lyricRows + 3.0f) * this.lyricsRoom;
        float panelHeight = PADDING * 2.0f + COVER + lyricsHeight + 13.0f;
        j().c(panelWidth);
        j().d(panelHeight);
        drawBackground(context, x, y, panelWidth, panelHeight, true, alpha);

        // A new track fades the old one out and the new one in, instead of swapping mid-word
        String stamp = title + "|" + artist;
        if (!stamp.equals(this.lastTrack)) {
            this.lastTrack = stamp;
            this.titleScroll = new Object();
            this.trackFade = 0.0f;
        }
        this.trackFade = MathUtil.c(this.trackFade, 1.0f, 0.2f);
        float trackAlpha = alpha * (0.25f + 0.75f * this.trackFade);
        float trackSlide = (1.0f - this.trackFade) * 2.5f;

        float contentX = x + PADDING;
        if (this.showCover.c().booleanValue()) {
            drawCover(event, context, contentX, y + PADDING, accent, accentTwo, alpha);
            contentX += COVER + 7.0f;
        }

        float dotsRight = x + panelWidth - PADDING;
        drawEqualizer(event, context, dotsRight, y + PADDING + 1.0f, accent, text, alpha);

        // Title, then who it is by with the service it comes from beside it
        float titleWidth = Math.max(10.0f, dotsRight - contentX - 16.0f);
        long now = System.nanoTime();
        float delta = this.lastFrameNanos == 0L ? 0.0f
                : Math.min(0.05f, (now - this.lastFrameNanos) / 1_000_000_000.0f);
        this.lastFrameNanos = now;
        String shownTitle = plain(title.isEmpty() ? "Ничего не играет" : title);
        Fonts.c.a(context, this.titleScroll, shownTitle, contentX + trackSlide, y + PADDING + 1.5f,
                9.0f, ColorUtil.a(text, trackAlpha), titleWidth - trackSlide, true, 18.0f, delta);

        String under = artist;
        if (this.showSource.c().booleanValue() && !source.isEmpty()) {
            under = under.isEmpty() ? source : under + " - " + source;
        }
        if (!under.equals(this.lastUnder)) {
            this.lastUnder = under;
            this.artistScroll = new Object();
        }
        if (!under.isEmpty()) {
            Fonts.e.a(context, this.artistScroll, plain(under), contentX + trackSlide, y + PADDING + 14.0f,
                    7.0f, ColorUtil.a(text, 0.5f * trackAlpha), dotsRight - contentX - trackSlide,
                    true, 14.0f, delta);
        }

        float rowY = y + PADDING + COVER;
        if (lyricsRow) {
            drawLyrics(context, layout, x + PADDING, rowY + 2.0f, LYRIC_ROW, lyricPosition, text,
                    alpha * this.lyricsRoom);
            rowY += lyricsHeight;
        }
        drawProgress(event, context, x + PADDING, rowY + 3.0f, panelWidth - PADDING * 2.0f, position, duration,
                accent, accentTwo, text, alpha);
    }

    /**
     * Four bars that dance in the corner while the music runs, the way a little visualiser does. There is no
     * sound to measure here, so each bar is carried by two waves of its own at speeds that never line up -
     * the result never repeats in an obvious way. Pausing settles them down to a resting line.
     */
    private void drawEqualizer(DrawEvent event, GuiGraphicsExtractor context, float right, float top,
                               int accent, int text, float alpha) {
        float barWidth = 2.1f;
        float gap = 1.8f;
        float maxHeight = 9.0f;
        float bottom = top + maxHeight;
        double seconds = System.currentTimeMillis() / 1000.0d;
        this.beat = MathUtil.c(this.beat, MediaSession.playing() ? 1.0f : 0.0f, 0.12f);
        for (int index = 0; index < 4; index++) {
            double phase = seconds * (2.6d + index * 0.47d) + index * 1.7d;
            double slow = Math.sin(seconds * (1.1d + index * 0.19d) + index);
            float level = (float) (0.5d + 0.35d * Math.sin(phase) + 0.15d * slow);
            level = MathUtil.b(level, 0.08f, 1.0f);
            float height = Math.max(2.1f, maxHeight * (0.18f + 0.82f * level * this.beat));
            float barX = right - barWidth - index * (barWidth + gap);
            int color = ColorUtil.a(ColorUtil.b(ColorUtil.a(text, 1.0f), ColorUtil.a(accent, 1.0f), level),
                    (0.45f + 0.45f * level) * alpha);
            event.d().a(context, barX, bottom - height, barWidth, height, barWidth / 2.0f, color);
        }
    }

    /**
     * The album art when the player gives one, and a note on a gradient tile when it does not - a track
     * without a picture should still look like something.
     */
    private void drawCover(DrawEvent event, GuiGraphicsExtractor context, float x, float y,
                           int accent, int accentTwo, float alpha) {
        net.minecraft.resources.Identifier cover = platform.client.utils.media.CoverTexture.get();
        // A cover that has just been loaded fades in over the tile underneath it
        this.coverFade = MathUtil.c(this.coverFade, cover != null ? 1.0f : 0.0f, 0.18f);
        if (cover != null && this.coverFade > 0.99f) {
            // Through the client's own textured quad: it takes a corner radius and draws the whole picture
            platform.client.utils.render.pipeline.XivivideRenderUtil.queueTexture(context, cover, x, y, COVER, COVER,
                    0.0f, 0.0f, 1.0f, 1.0f, ColorUtil.a(-1, alpha), 5.0f, null);
            event.d().a(context, x, y, COVER, COVER, 5.0f, 0.5f, ColorUtil.a(-1, 0.16f * alpha));
            return;
        }
        // The tile keeps its own solid base: a theme colour can be almost see-through, and the cover slot
        // would then be an empty hole in the card
        float wave = (float) (0.5d + 0.5d * Math.sin(System.currentTimeMillis() / 1600.0d));
        int first = ColorUtil.a(ColorUtil.b(ColorUtil.a(accent, 1.0f), ColorUtil.a(accentTwo, 1.0f), wave), alpha);
        int second = ColorUtil.a(ColorUtil.b(ColorUtil.a(accentTwo, 1.0f), ColorUtil.a(accent, 1.0f), wave), alpha);
        event.d().a(context, x, y, COVER, COVER, 6.0f, first, second, second, first);
        event.d().a(context, x, y, COVER, COVER, 6.0f, 0.5f, ColorUtil.a(-1, 0.16f * alpha));
        if (cover != null) {
            platform.client.utils.render.pipeline.XivivideRenderUtil.queueTexture(context, cover, x, y, COVER, COVER,
                    0.0f, 0.0f, 1.0f, 1.0f, ColorUtil.a(-1, alpha * this.coverFade), 5.0f, null);
        }

        // The note is drawn from plain shapes: two heads and the stems joined at the top
        float pulse = MediaSession.playing() ? 1.0f + 0.08f * (float) Math.sin(System.currentTimeMillis() / 320.0d) : 1.0f;
        int ink = ColorUtil.a(-1, 0.92f * alpha);
        float centerX = x + COVER / 2.0f;
        float centerY = y + COVER / 2.0f;
        float head = 5.5f * pulse;
        event.d().a(context, centerX - 7.0f, centerY + 2.0f, head, head * 0.8f, head / 2.0f, ink);
        event.d().a(context, centerX + 1.5f, centerY + 0.5f, head, head * 0.8f, head / 2.0f, ink);
        event.d().a(context, centerX - 3.0f, centerY - 6.0f, 1.2f, 9.0f, 0.6f, ink);
        event.d().a(context, centerX + 5.5f, centerY - 7.5f, 1.2f, 9.0f, 0.6f, ink);
        event.d().a(context, centerX - 3.0f, centerY - 7.5f, 9.7f, 1.6f, 0.8f, ink);
    }

    /**
     * The line being sung, split where the song has got to: what is already sung is bright, the rest dim.
     * The split is by letters rather than by cutting the drawing, so nothing depends on clipping.
     */
    /**
     * How the line will be laid out: the largest size at which it fits on one line, or, when nothing fits,
     * two lines broken between words. Nothing of the line is ever thrown away - a word cut in half or a
     * trailing row of dots tells the listener nothing.
     */
    private record LyricLayout(float size, java.util.List<String> lines) {
        float rows() {
            return lines.size();
        }
    }

    private LyricLayout lyricLayout(String raw, float width) {
        String text = plain(raw);
        if (text.isEmpty() || width <= 0.0f) {
            return new LyricLayout(LYRIC_SIZE, java.util.List.of());
        }
        // Only a little shrinking before wrapping: small text is harder to read than a second row
        for (float size = LYRIC_SIZE; size >= LYRIC_SIZE - 0.5f; size -= 0.5f) {
            if (Fonts.e.a(text, size) <= width) {
                return new LyricLayout(size, java.util.List.of(text));
            }
        }
        // Two rows: words are handed to the first row until it is full, the rest go below
        float size = LYRIC_SIZE - 0.5f;
        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        for (String word : text.split(" ")) {
            StringBuilder target = second.isEmpty() ? first : second;
            String candidate = target.isEmpty() ? word : target + " " + word;
            if (target == first && Fonts.e.a(candidate, size) > width) {
                second.append(word);
                continue;
            }
            target.setLength(0);
            target.append(candidate);
        }
        if (second.isEmpty()) {
            return new LyricLayout(size, java.util.List.of(first.toString()));
        }
        // A second row that is still too long is squeezed by size alone, never cut
        while (size > 4.0f && Fonts.e.a(second.toString(), size) > width) {
            size -= 0.25f;
        }
        return new LyricLayout(size, java.util.List.of(first.toString(), second.toString()));
    }

    /**
     * The line being sung, drawn in full. The part already sung is bright and the rest dim, and the split
     * between them travels by the line's own timing - a quick line lights up quickly, a long one slowly.
     */
    private void drawLyrics(GuiGraphicsExtractor context, LyricLayout layout, float x, float y, float rowHeight,
                            long position, int text, float alpha) {
        if (layout.lines().isEmpty()) {
            return;
        }
        String key = String.join("\n", layout.lines());
        if (!key.equals(this.lastLineShown)) {
            // The line that was there leaves upwards while the new one comes up from below
            this.previousLine = this.lastLineShown;
            this.lastLineShown = key;
            this.lineFade = 0.0f;
            this.sungWidth = MathUtil.b(Lyrics.progress(position), 0.0f, 1.0f) * totalWidth(layout);
        }
        this.lineFade = MathUtil.c(this.lineFade, 1.0f, 0.16f);
        float ease = this.lineFade * this.lineFade * (3.0f - 2.0f * this.lineFade);

        if (!this.previousLine.isEmpty() && ease < 0.999f) {
            String leaving = this.previousLine.split("\n")[0];
            Fonts.e.a(context, leaving, x, y - 2.0f * ease, layout.size(),
                    ColorUtil.a(text, 0.3f * alpha * (1.0f - ease)));
        }

        float total = totalWidth(layout);
        float sung = MathUtil.b(Lyrics.progress(position), 0.0f, 1.0f) * total;
        this.sungWidth = MathUtil.c(this.sungWidth, sung, 0.9f);
        float lift = (1.0f - ease) * 2.5f;
        float fade = alpha * ease;

        float consumed = 0.0f;
        for (int row = 0; row < layout.lines().size(); row++) {
            String line = layout.lines().get(row);
            float lineWidth = Fonts.e.a(line, layout.size());
            float lit = MathUtil.b(this.sungWidth - consumed, 0.0f, lineWidth);
            int cut = 0;
            while (cut < line.length() && Fonts.e.a(line.substring(0, cut + 1), layout.size()) <= lit) {
                cut++;
            }
            String done = line.substring(0, cut);
            String left = line.substring(cut);
            float rowY = y + row * rowHeight + lift;
            Fonts.e.a(context, done, x, rowY, layout.size(), ColorUtil.a(text, fade));
            Fonts.e.a(context, left, x + Fonts.e.a(done, layout.size()), rowY, layout.size(),
                    ColorUtil.a(text, 0.35f * fade));
            // The letter being sung right now comes up gradually, so the light never steps
            if (cut < line.length() && lit > 0.0f) {
                String next = String.valueOf(line.charAt(cut));
                float within = MathUtil.b((lit - Fonts.e.a(done, layout.size()))
                        / Math.max(0.5f, Fonts.e.a(next, layout.size())), 0.0f, 1.0f);
                Fonts.e.a(context, next, x + Fonts.e.a(done, layout.size()), rowY, layout.size(),
                        ColorUtil.a(text, fade * within));
            }
            consumed += lineWidth;
        }
    }

    private static float totalWidth(LyricLayout layout) {
        float total = 0.0f;
        for (String line : layout.lines()) {
            total += Fonts.e.a(line, layout.size());
        }
        return Math.max(1.0f, total);
    }

    /** The bar: the track, the part played in the theme's colours, and the times on either side. */
    private void drawProgress(DrawEvent event, GuiGraphicsExtractor context, float x, float y, float width,
                              long position, long duration, int accent, int accentTwo, int text, float alpha) {
        float target = duration > 0L ? MathUtil.b(position / (float) duration, 0.0f, 1.0f) : 0.0f;
        // Close enough to jump straight there, otherwise eased: the bar stays exact without twitching
        this.progress = Math.abs(target - this.progress) < 0.0015f ? target : MathUtil.c(this.progress, target, 0.55f);

        String elapsed = time(position);
        String left = duration > 0L ? "-" + time(Math.max(0L, duration - position)) : "--:--";
        float timeSize = 6.0f;
        float elapsedWidth = Fonts.e.a(elapsed, timeSize);
        float leftWidth = Fonts.e.a(left, timeSize);
        float barX = x + elapsedWidth + 8.0f;
        float barWidth = Math.max(10.0f, width - elapsedWidth - leftWidth - 16.0f);

        Fonts.e.a(context, elapsed, x, y - 1.0f, timeSize, ColorUtil.a(text, 0.45f * alpha));
        Fonts.e.a(context, left, x + width - leftWidth, y - 1.0f, timeSize, ColorUtil.a(text, 0.45f * alpha));
        event.d().a(context, barX, y + 1.5f, barWidth, 3.0f, 1.5f, ColorUtil.a(text, 0.16f * alpha));

        float filled = barWidth * this.progress;
        if (filled > 0.5f) {
            // The gradient runs along the bar and only breathes in brightness. Sliding the colours by a
            // counter that wraps is what made it look like one frame a second: at the wrap it jumped.
            float breath = 0.88f + 0.12f * (float) Math.sin(System.currentTimeMillis() / 900.0d);
            int start = ColorUtil.a(accent, alpha * breath);
            int end = ColorUtil.a(accentTwo, alpha);
            event.d().a(context, barX, y + 1.5f, filled, 3.0f, 1.5f, start, end, start, end);
            event.d().a(context, barX + filled - 2.25f, y + 0.5f, 4.5f, 4.5f, 2.25f, ColorUtil.a(-1, alpha));
        }
    }

    private String lyricLine(long position) {
        Lyrics.Line line = Lyrics.current(position);
        return line == null ? "" : line.text();
    }
    /**
     * Makes a line drawable. The typographic characters a track name loves - long dashes, curly quotes, an
     * ellipsis in one piece - are swapped for plain ones; letters with accents are stripped down to their
     * base letter; and anything the font still cannot draw is dropped rather than left as a question mark.
     */
    private static String plain(String text) {
        if (text == null) {
            return "";
        }
        String swapped = text
                .replace('–', '-').replace('—', '-').replace('−', '-')
                .replace('‘', '\'').replace('’', '\'').replace('ʼ', '\'')
                .replace('“', '"').replace('”', '"').replace('«', '"').replace('»', '"')
                .replace('·', '-').replace('•', '-').replace(' ', ' ')
                .replace("…", "...");
        // "ô" becomes "o", "é" becomes "e": the mark is separated from the letter and then left behind
        String flattened = java.text.Normalizer.normalize(swapped, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        StringBuilder builder = new StringBuilder(flattened.length());
        for (int index = 0; index < flattened.length(); index++) {
            char character = flattened.charAt(index);
            if (character == ' ' || Fonts.e.supports(character)) {
                builder.append(character);
            }
        }
        return builder.toString().replaceAll("\\s{2,}", " ").trim();
    }

    /** Cuts a line down to what fits, and only ever between words, with a tail of dots. */
    private static String fit(String raw, float size, float maxWidth) {
        String text = plain(raw);
        if (text.isEmpty() || maxWidth <= 0.0f || Fonts.e.a(text, size) <= maxWidth) {
            return text;
        }
        int cut = text.length();
        while (cut > 0 && Fonts.e.a(text.substring(0, cut) + "...", size) > maxWidth) {
            cut--;
        }
        // Back off to the end of the last whole word, unless that would leave almost nothing
        int space = text.lastIndexOf(' ', Math.max(0, cut - 1));
        if (space > cut / 2) {
            cut = space;
        }
        return text.substring(0, Math.max(1, cut)).trim() + "...";
    }

    private static String time(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    @Override
    public void a(GlobalEvent event) {
        MediaSession.keepAlive();
        boolean visible = MediaSession.present() || aM_.gui.screen() instanceof ChatScreen;
        d().a(visible);
        super.a(event);
    }
}
