package ru.rooyzee.elytrixclient.client.soulfire;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Минимальный клиент к HTTP API SoulFire (MCP, JSON-RPC 2.0).
 * SoulFire отдаёт свои функции как MCP-инструменты: get_bot_list, set_bots_desired_state,
 * restart_bots, set_bot_movement и т.д. Токен берётся в GUI SoulFire (профиль -> API token).
 */
public class McpClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final AtomicInteger ids = new AtomicInteger(1);

    public CompletableFuture<String> callTool(String url, String token, String tool, JsonObject args) {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", ids.getAndIncrement());
        request.addProperty("method", "tools/call");

        JsonObject params = new JsonObject();
        params.addProperty("name", tool);
        params.add("arguments", args == null ? new JsonObject() : args);
        request.add("params", params);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(request.toString()));
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(response.statusCode(), response.body()))
                .exceptionally(err -> "ошибка запроса: " + err.getMessage());
    }

    /** Ответ бывает и чистым JSON, и SSE-потоком ("data: {...}"). Разбираем оба случая. */
    private String parse(int code, String body) {
        if (body == null) return "пустой ответ (HTTP " + code + ")";
        String json = body.trim();
        if (json.contains("data:")) {
            StringBuilder sb = new StringBuilder();
            for (String line : json.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("data:")) {
                    sb.append(trimmed.substring(5).trim());
                }
            }
            json = sb.toString();
        }
        try {
            var root = JsonParser.parseString(json);
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("result")) {
                    return obj.get("result").toString();
                }
                if (obj.has("error")) {
                    return "ошибка: " + obj.get("error");
                }
            }
            return json;
        } catch (Exception e) {
            return "HTTP " + code + ": " + (json.length() > 400 ? json.substring(0, 400) + "..." : json);
        }
    }
}
