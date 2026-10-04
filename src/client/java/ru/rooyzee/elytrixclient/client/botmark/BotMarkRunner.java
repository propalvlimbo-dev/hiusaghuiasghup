package ru.rooyzee.elytrixclient.client.botmark;

import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Обёртка над BotMark (https://github.com/Pumpkin-MC/BotMark).
 *
 * BotMark — Rust-бинарник, который заливает сервер «сырыми» ботами.
 * Важно: он работает только по offline-mode (online-mode=false в server.properties),
 * то есть это нагрузочный слой, а не полноценные игроки. Умные боты (капча/регистрация) — SoulFire.
 *
 * Запуск: botmark --ip host:port --count N --delay MS [флаги поведения]
 */
public class BotMarkRunner {
    private final LogBuffer log;
    private Process process;
    private Thread pump;
    private String status = "остановлен";

    public BotMarkRunner(LogBuffer log) {
        this.log = log;
    }

    public synchronized boolean isRunning() {
        return process != null && process.isAlive();
    }

    public synchronized String status() {
        if (isRunning()) {
            return "работает (pid " + process.pid() + ")";
        }
        return status;
    }

    public synchronized void start(ElytrixConfig cfg) {
        if (isRunning()) {
            log.add("[BotMark] уже запущен");
            return;
        }
        List<String> cmd = buildCommand(cfg);
        log.add("[BotMark] $ " + String.join(" ", cmd));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            process = pb.start();
            status = "запущен";
            pump = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.add("[BotMark] " + line);
                    }
                } catch (IOException e) {
                    log.add("[BotMark] поток вывода оборван: " + e.getMessage());
                }
                log.add("[BotMark] процесс завершился (код " + (process.isAlive() ? "?" : process.exitValue()) + ")");
            }, "elytrix-botmark-pump");
            pump.setDaemon(true);
            pump.start();
        } catch (IOException e) {
            status = "ошибка: " + e.getMessage();
            log.add("[BotMark] не удалось запустить: " + e.getMessage());
            log.add("[BotMark] проверь путь к бинарнику в Настройках");
        }
    }

    public synchronized void stop() {
        if (process != null) {
            process.destroy();
            status = "остановлен";
            log.add("[BotMark] отправлен stop");
            process = null;
        }
    }

    private List<String> buildCommand(ElytrixConfig cfg) {
        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.botmarkPath);
        cmd.add("--ip");
        cmd.add(cfg.target());
        cmd.add("--count");
        cmd.add(String.valueOf(cfg.botmarkCount));
        cmd.add("--delay");
        cmd.add(String.valueOf(cfg.botmarkDelay));
        cmd.add("--timeout");
        cmd.add(String.valueOf(cfg.botmarkTimeout));
        cmd.add("--enable_spam_message");
        cmd.add(String.valueOf(cfg.bmSpam));
        cmd.add("--spam_message");
        cmd.add(cfg.bmSpamMessage);
        cmd.add("--enable_rotation");
        cmd.add(String.valueOf(cfg.bmRotation));
        cmd.add("--enable_swing");
        cmd.add(String.valueOf(cfg.bmSwing));
        cmd.add("--enable_movement");
        cmd.add(String.valueOf(cfg.bmMovement));
        cmd.add("--enable_jumping");
        cmd.add(String.valueOf(cfg.bmJumping));
        cmd.add("--enable_physics");
        cmd.add(String.valueOf(cfg.bmPhysics));
        return cmd;
    }
}
