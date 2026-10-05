package ru.rooyzee.elytrixclient.client.bots.rust;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Управление Rust-демоном elytrix-bots: клиент поднимает exe и рулит им по HTTP JSON
 * (127.0.0.1:25570) — как у SoulFire, только локально и быстро.
 */
public class RustBotDaemon {
    private final LogBuffer log;
    private Process proc;
    private Thread pump;

    public RustBotDaemon(LogBuffer log) {
        this.log = log;
    }

    public static boolean available(ElytrixConfig cfg) {
        try {
            return Files.exists(Paths.get(cfg.rustBotsPath.trim()));
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean isRunning() {
        return proc != null && proc.isAlive();
    }

    public String statusLine() {
        JsonObject st = status();
        if (st == null) {
            return isRunning() ? "демон жив, ждём" : "стоп";
        }
        return "в игре " + st.get("alive").getAsInt() + "/" + st.get("total").getAsInt();
    }

    public synchronized void start(ElytrixConfig cfg) {
        if (isRunning()) {
            post("/start", startJson(cfg));
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(cfg.rustBotsPath.trim(), "--port", "25570");
            pb.redirectErrorStream(true);
            proc = pb.start();
            pump = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        log.add("[rust-боты] " + line);
                    }
                } catch (Exception ignored) {
                }
            }, "elytrix-rustbots-pump");
            pump.setDaemon(true);
            pump.start();
        } catch (Exception e) {
            log.add("[rust-боты] не удалось запустить " + cfg.rustBotsPath + ": " + e.getMessage());
            proc = null;
            return;
        }
        if (!waitPort()) {
            log.add("[rust-боты] демон не поднял порт 25570 за 5 секунд");
        }
        post("/start", startJson(cfg));
        log.add("[rust-боты] команда start отправлена демону");
    }

    public synchronized void stop() {
        post("/stop", "{}");
        if (proc != null) {
            proc.destroy();
            proc = null;
        }
        log.add("[rust-боты] остановлены");
    }

    private boolean waitPort() {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket("127.0.0.1", 25570)) {
                return true;
            } catch (Exception e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }
        return false;
    }

    private String startJson(ElytrixConfig cfg) {
        String addr = cfg.botAddress.trim();
        String[] hp = addr.split(":");
        String host = hp[0];
        int port = hp.length > 1 ? Integer.parseInt(hp[1]) : 25565;
        JsonObject o = new JsonObject();
        o.addProperty("host", host);
        o.addProperty("port", port);
        o.addProperty("count", cfg.botmarkCount);
        o.addProperty("delay_ms", cfg.botmarkDelay);
        o.addProperty("timeout_ms", cfg.botmarkTimeout);
        o.addProperty("prefix", cfg.ownBotPrefix);
        o.addProperty("spam", cfg.bmSpam);
        o.addProperty("spam_message", cfg.bmSpamMessage);
        o.addProperty("spam_delay_min", cfg.botSpamMin);
        o.addProperty("spam_delay_max", cfg.botSpamMax);
        o.addProperty("rotation", cfg.bmRotation);
        o.addProperty("swing", cfg.bmSwing);
        o.addProperty("movement", cfg.bmMovement);
        o.addProperty("auto_reg", cfg.botAutoReg);
        o.addProperty("auto_login", cfg.botAutoLogin);
        o.addProperty("password", cfg.botPassword);
        return o.toString();
    }

    private void post(String path, String body) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:25570" + path).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(2000);
            c.setReadTimeout(2000);
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            c.getResponseCode();
            c.disconnect();
        } catch (Exception e) {
            log.add("[rust-боты] API недоступно: " + e.getMessage());
        }
    }

    public JsonObject status() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:25570/status").openConnection();
            c.setConnectTimeout(1000);
            c.setReadTimeout(1000);
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
                return JsonParser.parseString(sb.toString()).getAsJsonObject();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
