package platform.client.utils.media;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Album art looked up by name. Not every player hands its picture to Windows - a browser rarely does - so
 * when none comes with the track, the cover is fetched from Apple's open search instead, which answers for
 * almost anything that was ever released.
 */
public final class CoverArt {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile String loadedFor = "";
    private static volatile byte[] bytes;
    private static volatile boolean loading;

    private CoverArt() {
    }

    /** Starts a lookup for this track if one is not already on hand. */
    public static void ensure(String artist, String title) {
        String key = artist + " - " + title;
        if (title == null || title.isBlank() || key.equals(loadedFor) || loading) {
            return;
        }
        loading = true;
        Thread thread = new Thread(() -> {
            byte[] fetched = fetch(artist, title);
            bytes = fetched;
            loadedFor = key;
            loading = false;
        }, "Xivivide-Cover");
        thread.setDaemon(true);
        thread.start();
    }

    /** The picture found for the track on hand, or null. */
    public static byte[] bytes() {
        return bytes;
    }

    public static String loadedFor() {
        return loadedFor;
    }

    private static byte[] fetch(String artist, String title) {
        try {
            String term = (artist == null ? "" : artist + " ") + title;
            String url = "https://itunes.apple.com/search?media=music&limit=1&term="
                    + URLEncoder.encode(term, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Xivivide (Minecraft client)")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            int start = body.indexOf("\"artworkUrl100\":\"");
            if (start < 0) {
                return null;
            }
            start += "\"artworkUrl100\":\"".length();
            int end = body.indexOf('"', start);
            if (end < 0) {
                return null;
            }
            // The answer points at a small copy; the same address serves a large one
            String art = body.substring(start, end).replace("\\/", "/").replace("100x100bb", "512x512bb");
            HttpRequest image = HttpRequest.newBuilder(URI.create(art))
                    .header("User-Agent", "Xivivide (Minecraft client)")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = CLIENT.send(image, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() == 200 && response.body().length > 0 ? response.body() : null;
        } catch (Exception exception) {
            return null;
        }
    }
}
