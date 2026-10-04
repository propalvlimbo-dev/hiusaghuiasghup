package ru.rooyzee.elytrixclient.client.soulfire;

import com.google.gson.JsonObject;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Два способа управлять SoulFire из мода:
 *
 *  cli — запускаем SoulFireCLI.jar процессом и пишем команды ему в stdin
 *        (bots start / bots stop / online / bot &lt;name&gt; say &lt;text&gt; ...).
 *        Плюс: работает всегда, ничего не надо настраивать. Минус: нужен локальный jar.
 *
 *  mcp — дёргаем HTTP MCP-API SoulFire (Bearer-токен). Плюс: можно управлять
 *        удалённым сервером SoulFire и получать структурированные ответы.
 *
 * В обоих режимах мод НЕ линкует код SoulFire (AGPL-3.0) — только внешний процесс/сеть.
 */
public class SoulFireController {
    private final LogBuffer log;
    private final McpClient mcp = new McpClient();

    private Process process;
    private BufferedWriter stdin;
    private Thread pump;
    private String status = "не запущен";

    public SoulFireController(LogBuffer log) {
        this.log = log;
    }

    // ---------------------------------------------------------------- CLI режим

    public synchronized boolean isRunning() {
        return process != null && process.isAlive();
    }

    public synchronized String status(ElytrixConfig cfg) {
        if ("mcp".equalsIgnoreCase(cfg.soulfireMode)) {
            return "режим MCP · " + cfg.soulfireApiUrl;
        }
        return isRunning() ? "CLI работает (pid " + process.pid() + ")" : status;
    }

    public synchronized void startSoulFire(ElytrixConfig cfg) {
        if (isRunning()) {
            log.add("[SoulFire] уже запущен");
            return;
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java");
        if (cfg.soulfireJavaArgs != null && !cfg.soulfireJavaArgs.isBlank()) {
            for (String arg : cfg.soulfireJavaArgs.split("\\s+")) {
                cmd.add(arg);
            }
        }
        cmd.add("-jar");
        cmd.add(cfg.soulfireJar);

        log.add("[SoulFire] $ " + String.join(" ", cmd));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            process = pb.start();
            stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            status = "запущен";
            pump = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.add("[SoulFire] " + line);
                    }
                } catch (IOException e) {
                    log.add("[SoulFire] поток вывода оборван: " + e.getMessage());
                }
                log.add("[SoulFire] процесс завершился");
            }, "elytrix-soulfire-pump");
            pump.setDaemon(true);
            pump.start();
        } catch (IOException e) {
            status = "ошибка: " + e.getMessage();
            log.add("[SoulFire] не удалось запустить: " + e.getMessage());
            log.add("[SoulFire] проверь путь к SoulFireCLI.jar в Настройках");
        }
    }

    public synchronized void stopSoulFire() {
        if (process != null) {
            process.destroy();
            process = null;
            stdin = null;
            status = "остановлен";
            log.add("[SoulFire] отправлен stop");
        }
    }

    /** Команда в консоль SoulFire CLI (например "bots start", "online"). */
    public synchronized void send(String command) {
        if (!isRunning() || stdin == null) {
            log.add("[SoulFire] CLI не запущен — команда не отправлена: " + command);
            return;
        }
        try {
            stdin.write(command);
            stdin.newLine();
            stdin.flush();
            log.add("[SoulFire] > " + command);
        } catch (IOException e) {
            log.add("[SoulFire] не удалось отправить команду: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- MCP режим

    /** Универсальный вызов MCP-инструмента; ответ асинхронно падает в лог. */
    public void callMcp(ElytrixConfig cfg, String tool, JsonObject args) {
        log.add("[SoulFire/MCP] " + tool + " " + (args == null ? "{}" : args));
        mcp.callTool(cfg.soulfireApiUrl, cfg.soulfireToken, tool, args)
                .thenAccept(result -> log.add("[SoulFire/MCP] <- " + result));
    }

    public void botsStart(ElytrixConfig cfg) {
        if ("mcp".equalsIgnoreCase(cfg.soulfireMode)) {
            JsonObject args = new JsonObject();
            if (!cfg.soulfireInstanceId.isBlank()) args.addProperty("instance_id", cfg.soulfireInstanceId);
            args.addProperty("desired_state", "RUNNING");
            callMcp(cfg, "set_bots_desired_state", args);
        } else {
            send("bots start");
        }
    }

    public void botsStop(ElytrixConfig cfg) {
        if ("mcp".equalsIgnoreCase(cfg.soulfireMode)) {
            JsonObject args = new JsonObject();
            if (!cfg.soulfireInstanceId.isBlank()) args.addProperty("instance_id", cfg.soulfireInstanceId);
            args.addProperty("desired_state", "STOPPED");
            callMcp(cfg, "set_bots_desired_state", args);
        } else {
            send("bots stop");
        }
    }

    public void online(ElytrixConfig cfg) {
        if ("mcp".equalsIgnoreCase(cfg.soulfireMode)) {
            JsonObject args = new JsonObject();
            if (!cfg.soulfireInstanceId.isBlank()) args.addProperty("instance_id", cfg.soulfireInstanceId);
            callMcp(cfg, "get_bot_list", args);
        } else {
            send("online");
        }
    }
}
