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
    /** Физика конкретного сервера (настраивается на странице папки). */
    public volatile boolean pAntiAfk = true;
    public volatile boolean pRotation = true;
    public volatile boolean pSwing = true;
    public volatile boolean pAutoJump = true;

    public static volatile int liveMode;
    public static volatile boolean liveRotation = true;
    public static volatile boolean liveSwing = true;
    public static volatile boolean liveAutoJump = true;
    public static volatile boolean liveAntiAfk = true;
    public static volatile boolean liveFfServer;

    public static void apply(ru.rooyzee.elytrixclient.client.config.ElytrixConfig c) {
        liveMode = c.botMode;
        liveRotation = c.bmRotation;
        liveSwing = c.bmSwing;
        liveAutoJump = c.botAutoJump;
        liveAntiAfk = c.botAntiAfk;

    }

    // ── Heightmap чанков (из пакетов 45): чтобы боты не парили, а стояли на земле ──
    /** Секции блоков: ключ (cx, cz, secY) -> state-id[4096] — для 3D-вида глазами бота. */
    private final java.util.concurrent.ConcurrentHashMap<Long, short[]> blocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static long blockKey(int cx, int cz, int sy) {
        return ((long) (cx & 0x7FFFF) << 41) | ((long) (cz & 0x7FFFF) << 20) | (sy & 0xFFFFF);
    }

    public void storeSection(int cx, int cz, int sy, short[] states) {
        blocks.put(blockKey(cx, cz, sy), states);
        if (blocks.size() > 3000) {
            blocks.clear();
        }
    }

    /** Глобальный state-id блока; -1, если секция не загружена. */
    public int blockAt(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        if (by < -64 || by > 320) {
            return -1;
        }
        short[] sec = blocks.get(blockKey(Math.floorDiv(bx, 16), Math.floorDiv(bz, 16),
                Math.floorDiv(by, 16)));
        if (sec == null) {
            return -1;
        }
        return sec[(by & 15) * 256 + (bz & 15) * 16 + (bx & 15)] & 0xFFFF;
    }

    /** Карта высот — своя на движок (на сервер): боты разных папок не путают чанки. */
    private final java.util.concurrent.ConcurrentHashMap<Long, int[]> chunkH =
            new java.util.concurrent.ConcurrentHashMap<>();

    public void putHeights(int cx, int cz, int[] hs) {
        chunkH.put(((long) cx << 32) | (cz & 0xFFFFFFFFL), hs);
        if (chunkH.size() > 4096) {
            chunkH.clear();
        }
    }

    /** Высота верха блока под точкой; MIN_VALUE, если чанк ещё не получен. */
    public int heightAt(double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int[] hs = chunkH.get(((long) Math.floorDiv(bx, 16) << 32)
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

    public java.util.List<OwnBot> botList() {
        return new java.util.ArrayList<>(bots);
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

    /** В причине кика бан — бот меняет аккаунт из пула папки. */
    static boolean isBanReason(String status) {
        String t = status.toLowerCase(java.util.Locale.ROOT);
        return t.contains("бан") || t.contains("ban") || t.contains("блокиров");
    }

    public synchronized void start(String host, int port, int protocol, OwnBotSettings s) {
        if (running) {
            log.add("[Боты] уже запущены");
            return;
        }
        running = true;
        if (s.folder == null) {
            pAntiAfk = s.antiAfk;
            pRotation = s.rotation;
            pSwing = s.swing;
            pAutoJump = s.autoJump;
        }
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
            final java.util.List<String> pool = java.util.Collections.synchronizedList(
                    new java.util.ArrayList<>(s.folder != null ? s.folder.freeAccounts() : java.util.List.of()));
            for (int i = 1; i <= s.count && running; i++) {
                final String initial = !pool.isEmpty() ? pool.remove(0)
                        : (s.randomNames ? randomName() : s.prefix + i);
                Thread t = new Thread(() -> {
                    String nick = initial;
                    int proxyBanTries = 0;
                    while (running) {
                        OwnBot bot = new OwnBot(OwnBotEngine.this, nick, fHost, fPort, proto, s, log);
                        bots.add(bot);
                        bot.run();
                        bots.remove(bot);
                        if (!running || !s.rejoin) {
                            break;
                        }
                        String why = bot.status();
                        if (s.folder != null && why != null && isBanReason(why)) {
                            BotProxy badIp = bot.proxy();
                            if (badIp != null && proxyBanTries < 3) {
                                // Забанен IP прокси, а не ник: ник остаётся, прокси в блэклист
                                proxyBanTries++;
                                markBadProxy(badIp);
                                log.add("[Бот " + nick + "] бан по IP прокси " + badIp.host
                                        + " — ник чистый, меняю прокси (попытка " + proxyBanTries + "/3)");
                                continue;
                            }
                            if (badIp != null) {
                                markBadProxy(badIp);
                            }
                            s.folder.banned.add(nick);
                            BotManager.save();
                            log.add("[Бот " + nick + "] забанен на " + fHost + " — меняю аккаунт");
                            String next = s.folder.nextAccount(nick);
                            if (next == null) {
                                log.add("[Боты] свободные аккаунты в папке кончились — добавьте ники");
                                break;
                            }
                            nick = next;
                            proxyBanTries = 0;
                        }
                        log.add("[Бот " + nick + "] кик (" + why + ") — реждойн через "
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
                    // Вход волнами: реалистичная нагрузка (антибот-плагины реагируют на шквал)
                    if (s.waveSize > 0 && i % s.waveSize == 0 && i < s.count) {
                        log.add("[Боты] волна " + (i / s.waveSize) + " (" + s.waveSize
                                + " ботов) запущена — пауза " + (s.wavePauseMs / 1000) + " с");
                        Thread.sleep(s.wavePauseMs);
                    }
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
