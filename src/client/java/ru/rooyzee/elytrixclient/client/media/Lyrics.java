package ru.rooyzee.elytrixclient.client.media;

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
 * Синхронизированные субтитры из lrclib.net — загружаются раз за трек.
 */
public final class Lyrics {

    private static final Pattern STAMP = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?\]");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Line(long startMs, String text) {}

    private static volatile String loadedFor = "";
    private static volatile List<Line> lines = List.of();
    private static volatile boolean loading;

    private Lyrics() {}

    public static void ensure(String artist, String title) {
        String key = artist + " - " + title;
        if (key.equals(loadedFor) || loading || title == null || title.isBlank()) return;
        loading = true;
        Thread thread = new Thread(() -> {
            lines = fetch(artist, title);
            loadedFor = key;
            loading = false;
        }, "Elytrix-Lyrics");
        thread.setDaemon(true);
        thread.start();
    }

    public static boolean has() { return !lines.isEmpty(); }

    public static Line current(long positionMs) {
        List<Line> snapshot = lines;
        Line found = null;
        for (Line line : snapshot) {
            if (line.startMs() > positionMs) break;
            found = line;
        }
        return found;
    }

    public static float progress(long positionMs) {
        List<Line> snapshot = lines;
        for (int i = 0; i < snapshot.size(); i++) {
            Line line = snapshot.get(i);
            if (line.startMs() > positionMs) continue;
            long end = i + 1 < snapshot.size() ? snapshot.get(i + 1).startMs() : line.startMs() + 4000L;
            if (positionMs >= end) continue;
            long span = Math.max(1L, end - line.startMs());
            return Math.min(1.0f, (positionMs - line.startMs()) / (float) span);
        }
        return 0.0f;
    }

    private static List<Line> fetch(String artist, String title) {
        String clean = clean(title);
        List<Line> exact = request("https://lrclib.net/api/get?artist_name=" + encode(artist) + "&track_name=" + encode(clean));
        if (!exact.isEmpty()) return exact;
        List<Line> searched = search("https://lrclib.net/api/search?q=" + encode(artist + " " + clean), artist, clean);
        if (!searched.isEmpty()) return searched;
        return search("https://lrclib.net/api/search?q=" + encode(clean), artist, clean);
    }

    private static List<Line> search(String url, String artist, String title) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "ElytrixClient (Minecraft mod)")
                    .timeout(Duration.ofSeconds(6)).GET().build();
            String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            for (String entry : body.split("\\},\\s*\\{")) {
                String synced = readField(entry, "syncedLyrics");
                if (synced == null || synced.isBlank()) continue;
                String foundTitle = readField(entry, "trackName");
                String foundArtist = readField(entry, "artistName");
                if (!matches(foundTitle, title)) continue;
                if (!artist.isBlank() && foundArtist != null && !matches(foundArtist, artist)
                        && !foundArtist.toLowerCase().contains(artist.toLowerCase())) continue;
                List<Line> parsed = parse(synced);
                if (!parsed.isEmpty()) return parsed;
            }
            return List.of();
        } catch (Exception e) { return List.of(); }
    }

    private static boolean matches(String first, String second) {
        if (first == null || second == null) return false;
        String left = first.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
        String right = second.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
        return !left.isEmpty() && !right.isEmpty() && (left.equals(right) || left.contains(right) || right.contains(left));
    }

    private static List<Line> request(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "ElytrixClient (Minecraft mod)")
                    .timeout(Duration.ofSeconds(6)).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return List.of();
            String synced = readField(response.body(), "syncedLyrics");
            return synced == null || synced.isBlank() ? List.of() : parse(synced);
        } catch (Exception e) { return List.of(); }
    }

    private static String clean(String title) {
        if (title == null) return "";
        String cleaned = title
                .replaceAll("(?i)\\s*[-\u2013]\\s*(remaster(ed)?|radio edit|single version|album version)\\b.*$", "")
                .replaceAll("(?i)\\s*[(\\[][^)\\]]*(remaster|feat\\.?|ft\\.?|prod\\.?|official|video|audio|lyrics)[^)\\]]*[)\\]]", "")
                .trim();
        return cleaned.isEmpty() ? title.trim() : cleaned;
    }

    private static String readField(String json, String field) {
        String needle = "\"" + field + "\":";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '"') return null;
        StringBuilder value = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case 'n' -> value.append('\n');
                    case 'r' -> {}
                    case 't' -> value.append(' ');
                    case 'u' -> { if (i + 4 < json.length()) { value.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16)); i += 4; } }
                    default -> value.append(next);
                }
                continue;
            }
            if (c == '"') break;
            value.append(c);
        }
        return value.toString();
    }

    private static List<Line> parse(String lrc) {
        List<Line> parsed = new ArrayList<>();
        for (String raw : lrc.split("\n")) {
            Matcher matcher = STAMP.matcher(raw);
            long start = -1; int end = 0;
            while (matcher.find()) {
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                long fraction = matcher.group(3) == null ? 0 : Long.parseLong(matcher.group(3));
                long millis = matcher.group(3) != null && matcher.group(3).length() == 2 ? fraction * 10 : fraction;
                start = minutes * 60_000L + seconds * 1000L + millis;
                end = matcher.end();
            }
            if (start < 0) continue;
            String text = raw.substring(end).trim();
            if (!text.isEmpty()) parsed.add(new Line(start, text));
        }
        parsed.sort((a, b) -> Long.compare(a.startMs(), b.startMs()));
        return parsed;
    }

    private static String encode(String v) { return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8); }
}