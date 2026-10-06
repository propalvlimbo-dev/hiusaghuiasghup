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

    // ── Живые настройки: меняются из GUI/команд и действуют на уже бегущих ботов сразу ──
    public static volatile int liveMode;
    public static volatile boolean liveRotation = true;
    public static volatile boolean liveSwing = true;
    public static volatile boolean liveAutoJump = true;
    public static volatile boolean liveAntiAfk = true;
    public static volatile boolean liveSpam;
    public static volatile boolean liveFfServer;
    public static volatile String liveSpamMessage = "Elytrix on top!";
    public static volatile int liveSpamMin = 3000;
    public static volatile int liveSpamMax = 6000;

    public static void apply(ru.rooyzee.elytrixclient.client.config.ElytrixConfig c) {
        liveMode = c.botMode;
        liveRotation = c.bmRotation;
        liveSwing = c.bmSwing;
        liveAutoJump = c.botAutoJump;
        liveAntiAfk = c.botAntiAfk;
        liveSpam = c.bmSpam;
        liveSpamMessage = c.bmSpamMessage;
        liveSpamMin = c.botSpamMin;
        liveSpamMax = c.botSpamMax;
    }

    // ── Heightmap чанков (из пакетов 45): чтобы боты не парили, а стояли на земле ──
    private static final java.util.concurrent.ConcurrentHashMap<Long, int[]> CHUNK_H =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void putHeights(int cx, int cz, int[] hs) {
        CHUNK_H.put(((long) cx << 32) | (cz & 0xFFFFFFFFL), hs);
        if (CHUNK_H.size() > 4096) {
            CHUNK_H.clear();
        }
    }

    /** Высота верха блока под точкой; MIN_VALUE, если чанк ещё не получен. */
    public static int heightAt(double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int[] hs = CHUNK_H.get(((long) Math.floorDiv(bx, 16) << 32)
                | (Math.floorDiv(bz, 16) & 0xFFFFFFFFL));
        if (hs == null) {
            return Integer.MIN_VALUE;
        }
        return hs[Math.floorMod(bz, 16) * 16 + Math.floorMod(bx, 16)];
    }

    /** Распаковка heightmap: 256 значений по 9 бит, MSB-first, с переходом через long. */
    public static int[] unpackHeights(long[] l) {
        int[] out = new int[256];
        for (int i = 0; i < 256; i++) {
            int bit = i * 9;
            int li = bit / 64;
            int off = bit % 64;
            if (li >= l.length) {
                break;
            }
            long v;
            if (off + 9 <= 64) {
                v = (l[li] >>> (64 - off - 9)) & 0x1FF;
            } else {
                int first = 64 - off;
                int second = 9 - first;
                v = (l[li] & ((1L << first) - 1)) << second;
                if (li + 1 < l.length) {
                    v |= (l[li + 1] >>> (64 - second)) & ((1L << second) - 1);
                }
            }
            out[i] = (int) v;
        }
        return out;
    }

    /**
     * Heightmap чанка. В 1.21.5+ (и 26.2) идёт VarInt-карта: size, затем
     * (index, len, i64[len])*; index 1=WORLD_SURFACE, 4=MOTION_BLOCKING, 5=NO_LEAVES.
     * Для старых форматов — фолбэк на NBT.
     */
    public static long[] extractHeightmap(byte[] d, int from) {
        try {
            int[] h = {from};
            int size = varint(d, h);
            if (size >= 1 && size <= 16) {
                long[] best = null;
                for (int i = 0; i < size; i++) {
                    int index = varint(d, h);
                    int len = varint(d, h);
                    if (len < 0 || len > 1000 || h[0] + 8L * len > d.length) {
                        return best;
                    }
                    long[] arr = new long[len];
                    for (int j = 0; j < len; j++) {
                        arr[j] = i64(d, h);
                    }
                    if (index == 4) {
                        return arr;
                    }
                    if (best == null && (index == 1 || len >= 36)) {
                        best = arr;
                    }
                }
                return best;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static int varint(byte[] d, int[] h) {
        int value = 0, bits = 0;
        while (true) {
            if (h[0] >= d.length || bits > 35) {
                throw new IllegalArgumentException("oob varint");
            }
            byte b = d[h[0]++];
            value |= (b & 0x7F) << bits;
            if ((b & 0x80) == 0) {
                return value;
            }
            bits += 7;
        }
    }

    private static long i64(byte[] d, int[] h) {
        if (h[0] + 8 > d.length) {
            throw new IllegalArgumentException("oob i64");
        }
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (d[h[0]++] & 0xFF);
        }
        return v;
    }

    private static long[] nbtLongs(byte[] d, int[] h, int type) {
        if (h[0] < 0 || h[0] >= d.length) {
            return null;
        }
        switch (type) {
            case 1: h[0]++; return null;
            case 2: h[0] += 2; return null;
            case 3: h[0] += 4; return null;
            case 4: h[0] += 8; return null;
            case 5: h[0] += 4; return null;
            case 6: h[0] += 8; return null;
            case 7: { int n = i32(d, h); h[0] += n; return null; }
            case 8: { int n = u16(d, h); h[0] += n; return null; }
            case 9: {
                int et = d[h[0]++] & 0xFF;
                int n = i32(d, h);
                if (n < 0 || n > 100000) {
                    throw new IllegalArgumentException("bad list");
                }
                for (int i = 0; i < n; i++) {
                    long[] r = nbtLongs(d, h, et);
                    if (r != null) return r;
                }
                return null;
            }
            case 10: {
                long[] fallback = null;
                while (h[0] < d.length) {
                    int et = d[h[0]++] & 0xFF;
                    if (et == 0) return fallback;
                    int nl = u16(d, h);
                    String name = new String(d, h[0], nl, java.nio.charset.StandardCharsets.UTF_8);
                    h[0] += nl;
                    if (et == 12) {
                        int n = i32(d, h);
                        if (n < 0 || n > 100000) {
                            throw new IllegalArgumentException("bad longs");
                        }
                        long[] arr = new long[n];
                        for (int i = 0; i < n; i++) {
                            arr[i] = ((long) i32(d, h) << 32) | (i32(d, h) & 0xFFFFFFFFL);
                        }
                        if ("MOTION_BLOCKING".equals(name)) return arr;
                        if (fallback == null && n >= 36) fallback = arr;
                    } else {
                        long[] r = nbtLongs(d, h, et);
                        if (r != null) return r;
                    }
                }
                return fallback;
            }
            case 11: { int n = i32(d, h); h[0] += 4L * n; return null; }
            case 12: { int n = i32(d, h); h[0] += 8L * n; return null; }
            default: return null;
        }
    }

    private static int i32(byte[] d, int[] h) {
        if (h[0] + 4 > d.length) {
            throw new IllegalArgumentException("oob i32");
        }
        int v = ((d[h[0]] & 0xFF) << 24) | ((d[h[0] + 1] & 0xFF) << 16)
                | ((d[h[0] + 2] & 0xFF) << 8) | (d[h[0] + 3] & 0xFF);
        h[0] += 4;
        return v;
    }

    private static int u16(byte[] d, int[] h) {
        if (h[0] + 2 > d.length) {
            throw new IllegalArgumentException("oob u16");
        }
        int v = ((d[h[0]] & 0xFF) << 8) | (d[h[0] + 1] & 0xFF);
        h[0] += 2;
        return v;
    }

    /** Мёртвые прокси (как у SoulFire: не отвечающие исключаются из ротации на 10 минут). */
    private static final java.util.Map<BotProxy, Long> badProxies = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long BAD_PROXY_TTL_MS = 10 * 60 * 1000L;

    public static void markBadProxy(BotProxy p) {
        if (p != null) {
            badProxies.put(p, System.currentTimeMillis());
        }
    }

    public static int badProxyCount() {
        return badProxies.size();
    }

    /** Следующий живой прокси по кругу (round-robin на бота, мёртвые пропускаются). */
    public static BotProxy nextProxy() {
        java.util.List<BotProxy> list = proxies;
        if (list.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        for (int tries = 0; tries < list.size(); tries++) {
            BotProxy p = list.get(Math.floorMod(proxyIdx.getAndIncrement(), list.size()));
            Long badAt = badProxies.get(p);
            if (badAt == null || now - badAt > BAD_PROXY_TTL_MS) {
                return p;
            }
        }
        return null; // все прокси мёртвы — вызывающий решает, что делать
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

    /** Рандомный MC-ник: 5-11 символов, [A-Za-z0-9_], первая буква. */
    private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
    public static String randomName() {
        java.util.Random r = new java.util.Random();
        int len = 5 + r.nextInt(7);
        StringBuilder b = new StringBuilder(len);
        b.append((char) ('a' + r.nextInt(26)));
        for (int i = 1; i < len; i++) {
            b.append(NAME_CHARS.charAt(r.nextInt(NAME_CHARS.length())));
        }
        return b.toString();
    }

    /** SRV-запись _minecraft._tcp.<host> — как резолвит обычный клиент Minecraft.
     *  Возвращает {хост, порт} или null, если записи нет. */
    public static String[] resolveSrv(String host) {
        try {
            java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            javax.naming.directory.DirContext ctx = new javax.naming.directory.InitialDirContext(env);
            javax.naming.directory.Attributes attrs =
                    ctx.getAttributes("dns:/_minecraft._tcp." + host, new String[]{"SRV"});
            javax.naming.directory.Attribute a = attrs.get("SRV");
            if (a != null && a.size() > 0) {
                String[] parts = a.get(0).toString().trim().split("\\s+");
                if (parts.length >= 4) {
                    return new String[]{parts[3].replaceAll("\\.$", ""), parts[2]};
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public synchronized void start(String host, int port, int protocol, OwnBotSettings s) {
        if (running) {
            log.add("[Боты] уже запущены");
            return;
        }
        running = true;
        // Как ванильный клиент и SoulFire: при порте по умолчанию сначала SRV-запись
        // _minecraft._tcp.<домен> — хостинги (mclan и т.п.) отдают реальный адрес через неё
        String rh = host;
        int rp = port;
        if (port == 25565) {
            String[] srv = resolveSrv(host);
            if (srv != null) {
                try {
                    rp = Integer.parseInt(srv[1]);
                    rh = srv[0];
                    log.add("[Боты] SRV: " + host + " -> " + rh + ":" + rp);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        final String fHost = rh;
        final int fPort = rp;
        log.add("[Боты] запускаем " + s.count + " встроенных ботов на " + fHost + ":" + fPort
                + " (protocol " + protocol + ")");
        Thread spawner = new Thread(() -> {
            int probed = probeProtocol(fHost, fPort, 3000);
            final int proto;
            if (probed < 0) {
                log.add("[Боты] протокол сервера: " + (-probed) + " (из status-ping)");
                proto = -probed;
            } else {
                log.add("[Боты] status не ответил, используем стандартный протокол " + probed);
                proto = probed;
            }
            for (int i = 1; i <= s.count && running; i++) {
                final String name = s.randomNames ? randomName() : s.prefix + i;
                Thread t = new Thread(() -> {
                    while (running) {
                        OwnBot bot = new OwnBot(name, fHost, fPort, proto, s, log);
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
