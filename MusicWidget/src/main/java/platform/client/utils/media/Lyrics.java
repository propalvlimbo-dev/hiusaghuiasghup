package platform.client.utils.media;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lyrics with timings for whatever is playing, fetched once per track from the open lyrics library at
 * lrclib.net and kept until the track changes. The lines come in the LRC format - a stamp and a line - so
 * the one being sung right now can be told, and how far into it the song is.
 */
public final class Lyrics {

    private static final Pattern STAMP = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** One line of the song and the moment it starts. */
    public record Line(long startMs, String text) {
    }

    private static volatile String loadedFor = "";
    private static volatile List<Line> lines = List.of();
    private static volatile boolean loading;

    private Lyrics() {
    }

    /** Makes sure the lyrics on hand belong to this track; fetches them in the background if they do not. */
    public static void ensure(String artist, String title) {
        String key = artist + " - " + title;
        if (key.equals(loadedFor) || loading || title == null || title.isBlank()) {
            return;
        }
        loading = true;
        Thread thread = new Thread(() -> {
            List<Line> fetched = fetch(artist, title);
            lines = fetched;
            loadedFor = key;
            loading = false;
        }, "Xivivide-Lyrics");
        thread.setDaemon(true);
        thread.start();
    }

    public static boolean has() {
        return !lines.isEmpty();
    }

    /** The line being sung at {@code positionMs}, or null before the first one. */
    public static Line current(long positionMs) {
        List<Line> snapshot = lines;
        Line found = null;
        for (Line line : snapshot) {
            if (line.startMs() > positionMs) {
                break;
            }
            found = line;
        }
        return found;
    }

    /** How far through the current line the song is, 0..1 - what tells the sung part from the rest. */
    public static float progress(long positionMs) {
        List<Line> snapshot = lines;
        for (int index = 0; index < snapshot.size(); index++) {
            Line line = snapshot.get(index);
            if (line.startMs() > positionMs) {
                continue;
            }
            long end = index + 1 < snapshot.size() ? snapshot.get(index + 1).startMs() : line.startMs() + 4000L;
            if (positionMs >= end) {
                continue;
            }
            long span = Math.max(1L, end - line.startMs());
            return Math.min(1.0f, (positionMs - line.startMs()) / (float) span);
        }
        return 0.0f;
    }

    /**
     * Asks the library by exact artist and title first. Players often dress a title up - a remaster note, a
     * featured artist, a label in brackets - and an exact match then finds nothing, so a plain search on the
     * cleaned up name is tried after that, and the first result that has timings is taken.
     */
    private static List<Line> fetch(String artist, String title) {
        String clean = clean(title);
        List<Line> exact = request("https://lrclib.net/api/get?artist_name=" + encode(artist)
                + "&track_name=" + encode(clean));
        if (!exact.isEmpty()) {
            return exact;
        }
        List<Line> searched = search("https://lrclib.net/api/search?q=" + encode(artist + " " + clean), artist, clean);
        if (!searched.isEmpty()) {
            return searched;
        }
        return search("https://lrclib.net/api/search?q=" + encode(clean), artist, clean);
    }

    /**
     * A search gives back a list, and its first entry is often some other song of a similar name. Only an
     * entry whose own title matches the one playing is taken - wrong lyrics are worse than none.
     */
    private static List<Line> search(String url, String artist, String title) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Xivivide (Minecraft client)")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            for (String entry : body.split("\\},\\s*\\{")) {
                String synced = readField(entry, "syncedLyrics");
                if (synced == null || synced.isBlank()) {
                    continue;
                }
                String foundTitle = readField(entry, "trackName");
                String foundArtist = readField(entry, "artistName");
                if (!matches(foundTitle, title)) {
                    continue;
                }
                if (!artist.isBlank() && foundArtist != null && !matches(foundArtist, artist)
                        && !foundArtist.toLowerCase().contains(artist.toLowerCase())) {
                    continue;
                }
                List<Line> parsed = parse(synced);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
            return List.of();
        } catch (Exception exception) {
            return List.of();
        }
    }

    /** Two names count as the same when what is left of them, letters and digits only, is the same. */
    private static boolean matches(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        String left = first.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
        String right = second.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
        return !left.isEmpty() && !right.isEmpty() && (left.equals(right) || left.contains(right) || right.contains(left));
    }

    private static List<Line> request(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Xivivide (Minecraft client)")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return List.of();
            }
            String synced = readField(response.body(), "syncedLyrics");
            return synced == null || synced.isBlank() ? List.of() : parse(synced);
        } catch (Exception exception) {
            return List.of();
        }
    }

    /** Drops what players add to a title and the library does not know about. */
    private static String clean(String title) {
        if (title == null) {
            return "";
        }
        String cleaned = title
                .replaceAll("(?i)\\s*[-–]\\s*(remaster(ed)?|radio edit|single version|album version)\\b.*$", "")
                .replaceAll("(?i)\\s*[(\\[][^)\\]]*(remaster|feat\\.?|ft\\.?|prod\\.?|official|video|audio|lyrics)[^)\\]]*[)\\]]", "")
                .trim();
        return cleaned.isEmpty() ? title.trim() : cleaned;
    }

    /** Pulls one string field out of the answer; a whole json reader is more than this needs. */
    private static String readField(String json, String field) {
        String needle = "\"" + field + "\":";
        int start = json.indexOf(needle);
        if (start < 0) {
            return null;
        }
        start += needle.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length() || json.charAt(start) != '"') {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (int index = start + 1; index < json.length(); index++) {
            char character = json.charAt(index);
            if (character == '\\' && index + 1 < json.length()) {
                char next = json.charAt(++index);
                switch (next) {
                    case 'n' -> value.append('\n');
                    case 'r' -> { }
                    case 't' -> value.append(' ');
                    case 'u' -> {
                        if (index + 4 < json.length()) {
                            value.append((char) Integer.parseInt(json.substring(index + 1, index + 5), 16));
                            index += 4;
                        }
                    }
                    default -> value.append(next);
                }
                continue;
            }
            if (character == '"') {
                break;
            }
            value.append(character);
        }
        return value.toString();
    }

    private static List<Line> parse(String lrc) {
        List<Line> parsed = new ArrayList<>();
        for (String raw : lrc.split("\n")) {
            Matcher matcher = STAMP.matcher(raw);
            long start = -1L;
            int end = 0;
            while (matcher.find()) {
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                long fraction = matcher.group(3) == null ? 0L : Long.parseLong(matcher.group(3));
                long millis = matcher.group(3) != null && matcher.group(3).length() == 2 ? fraction * 10L : fraction;
                start = minutes * 60_000L + seconds * 1000L + millis;
                end = matcher.end();
            }
            if (start < 0L) {
                continue;
            }
            String text = raw.substring(end).trim();
            if (!text.isEmpty()) {
                parsed.add(new Line(start, text));
            }
        }
        parsed.sort((first, second) -> Long.compare(first.startMs(), second.startMs()));
        return parsed;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
