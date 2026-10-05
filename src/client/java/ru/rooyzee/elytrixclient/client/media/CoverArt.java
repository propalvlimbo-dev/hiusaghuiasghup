package ru.rooyzee.elytrixclient.client.media;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Обложка альбома из iTunes API — когда Windows не даёт обложку.
 */
public final class CoverArt {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile String loadedFor = "";
    private static volatile byte[] bytes;
    private static volatile boolean loading;

    private CoverArt() {}

    public static void ensure(String artist, String title) {
        String key = artist + " - " + title;
        if (title == null || title.isBlank() || key.equals(loadedFor) || loading) return;
        loading = true;
        Thread thread = new Thread(() -> {
            bytes = fetch(artist, title);
            loadedFor = key;
            loading = false;
        }, "Elytrix-Cover");
        thread.setDaemon(true);
        thread.start();
    }

    public static byte[] bytes() { return bytes; }
    public static String loadedFor() { return loadedFor; }

    private static byte[] fetch(String artist, String title) {
        try {
            String term = (artist == null ? "" : artist + " ") + title;
            String url = "https://itunes.apple.com/search?media=music&limit=1&term="
                    + URLEncoder.encode(term, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "ElytrixClient (Minecraft mod)")
                    .timeout(Duration.ofSeconds(6)).GET().build();
            String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            int start = body.indexOf("\"artworkUrl100\":\"");
            if (start < 0) return null;
            start += "\"artworkUrl100\":\"".length();
            int end = body.indexOf('"', start);
            if (end < 0) return null;
            String art = body.substring(start, end).replace("\\/", "/").replace("100x100bb", "512x512bb");
            HttpRequest image = HttpRequest.newBuilder(URI.create(art))
                    .header("User-Agent", "ElytrixClient (Minecraft mod)")
                    .timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<byte[]> response = CLIENT.send(image, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() == 200 && response.body().length > 0 ? response.body() : null;
        } catch (Exception e) { return null; }
    }
}