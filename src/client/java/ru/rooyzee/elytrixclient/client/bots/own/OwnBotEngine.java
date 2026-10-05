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
            int proto = probeProtocol(host, port, 3000);
            if (proto < 0) {
                log.add("[Боты] протокол сервера: " + (-proto) + " (из status-ping)");
                proto = -proto;
            } else {
                log.add("[Боты] status не ответил, используем стандартный протокол " + proto);
            }
            for (int i = 1; i <= s.count && running; i++) {
                OwnBot bot = new OwnBot(s.prefix + i, host, port, proto, s, log);
                bots.add(bot);
                Thread t = new Thread(bot, "elytrix-ownbot-" + i);
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
