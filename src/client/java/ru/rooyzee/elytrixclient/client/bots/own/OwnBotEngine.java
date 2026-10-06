package ru.rooyzee.elytrixclient.client.bots.own;

import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Движок встроенных ботов: поднимает N собственных протокольных клиентов
 * (как BotMark, но без внешнего exe) и держит их в игре.
 */
public class OwnBotEngine {
    private final LogBuffer log;
    private final List<OwnBot> bots = new CopyOnWriteArrayList<>();
    private volatile boolean running;

    /** Координаты игрока клиента (обновляет тик клиента) — цель для режима «за мной». */
    public static volatile double followX, followZ;
    public static volatile boolean followActive;

    // ── Пул прокси для ботов ──────────────────────────────────────────────
    private static volatile java.util.List<BotProxy> proxies = java.util.List.of();
    private static final java.util.concurrent.atomic.AtomicInteger proxyIdx =
            new java.util.concurrent.atomic.AtomicInteger();

    /** Путь к файлу прокси: абсолютный как есть, относительный — от .minecraft/elytrix/. */
    public static java.nio.file.Path proxyPath(String path) {
        java.nio.file.Path f = java.nio.file.Path.of(path);
        if (!f.isAbsolute()) {
            try {
                f = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
                        .resolve("elytrix").resolve(path);
            } catch (Throwable ignored) {
            }
        }
        return f;
    }

    /** Читает txt с прокси (ip:port или ip:port:user:pass). Возвращает число загруженных. */
    public static synchronized int loadProxies(String path, LogBuffer log) {
        java.nio.file.Path f = proxyPath(path);
        java.util.List<BotProxy> list = new java.util.ArrayList<>();
        try {
            for (String line : java.nio.file.Files.readAllLines(f, java.nio.charset.StandardCharsets.UTF_8)) {
                BotProxy pr = BotProxy.parse(line);
                if (pr != null) {
                    list.add(pr);
                }
            }
            proxies = list;
            log.add("[Прокси] загружено " + list.size() + " из " + f.getFileName());
        } catch (Exception e) {
            log.add("[Прокси] не удалось прочитать " + f + ": " + e.getMessage());
        }
        return list.size();
    }

    public static int proxyCount() {
        return proxies.size();
    }

    /** Следующий прокси по кругу (round-robin на бота). */
    public static BotProxy nextProxy() {
        java.util.List<BotProxy> list = proxies;
        if (list.isEmpty()) {
            return null;
        }
        return list.get(Math.floorMod(proxyIdx.getAndIncrement(), list.size()));
    }

    public OwnBotEngine(LogBuffer log) {
        this.log = log;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public int aliveCount() {
        int n = 0;
        for (OwnBot b : bots) {
            if (b.isAlive()) {
                n++;
            }
        }
        return n;
    }

    public String status() {
        if (!running && bots.isEmpty()) {
            return "остановлен";
        }
        return "в игре " + aliveCount() + "/" + bots.size();
    }

    /** Узнаёт номер протокола сервера через status-ping; если не вышло — 776 (26.2). */
    public static int probeProtocol(String host, int port, int timeoutMs) {
        try (Socket s = new Socket(host, port)) {
            s.setSoTimeout(timeoutMs);
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            OwnBot.Writer w = new OwnBot.Writer(raw);
            w.varInt(0);
            w.varInt(776);
            w.str(host);
            w.u16(port);
            w.varInt(1);
            flush(out, raw.toByteArray());
            raw.reset();
            new OwnBot.Writer(raw).varInt(0);
            flush(out, raw.toByteArray());
            int l = OwnBot.readVarInt(in);
            byte[] data = new DataInputStream(in).readNBytes(l);
            int[] h = {0};
            int id = OwnBot.readVarInt(data, h);
            if (id == 0) {
                int sl = OwnBot.readVarInt(data, h);
                String json = new String(data, h[0], sl, StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("\"version\"\\s*:\\s*\\{[^}]*\"protocol\"\\s*:\\s*(\\d+)").matcher(json);
                if (m.find()) {
                    return -Integer.parseInt(m.group(1)); // минус = узнали из status
                }
            }
        } catch (Exception ignored) {
        }
        return 776;
    }

    private static void flush(OutputStream out, byte[] payload) throws java.io.IOException {
        ByteArrayOutputStream len = new ByteArrayOutputStream();
        new OwnBot.Writer(len).varInt(payload.length);
        out.write(len.toByteArray());
        out.write(payload);
        out.flush();
    }

    public synchronized void start(String host, int port, int protocol, OwnBotSettings s) {
        if (running) {
            log.add("[Боты] уже запущены");
            return;
        }
        running = true;
        log.add("[Боты] запускаем " + s.count + " встроенных ботов на " + host + ":" + port
                + " (protocol " + protocol + ")");
        Thread spawner = new Thread(() -> {
            int probed = probeProtocol(host, port, 3000);
            final int proto;
            if (probed < 0) {
                log.add("[Боты] протокол сервера: " + (-probed) + " (из status-ping)");
                proto = -probed;
            } else {
                log.add("[Боты] status не ответил, используем стандартный протокол " + probed);
                proto = probed;
            }
            for (int i = 1; i <= s.count && running; i++) {
                final String name = s.prefix + i;
                Thread t = new Thread(() -> {
                    while (running) {
                        OwnBot bot = new OwnBot(name, host, port, proto, s, log);
                        bots.add(bot);
                        bot.run();
                        bots.remove(bot);
                        if (!running || !s.rejoin || !bot.isAlive()) {
                            break;
                        }
                        log.add("[Бот " + name + "] кик (" + bot.status() + ") — реждойн через "
                                + s.rejoinDelayMs + " мс");
                        try {
                            Thread.sleep(s.rejoinDelayMs);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }, "elytrix-ownbot-" + i);
                t.setDaemon(true);
                t.start();
                try {
                    Thread.sleep(Math.max(10, s.delayMs));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "elytrix-ownbot-spawn");
        spawner.setDaemon(true);
        spawner.start();
    }

    public synchronized void stop() {
        running = false;
        for (OwnBot b : bots) {
            b.close();
        }
        bots.clear();
        log.add("[Боты] остановлены");
    }
}
