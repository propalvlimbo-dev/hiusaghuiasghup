package ru.rooyzee.elytrixclient.client.bots.own;

import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Встроенный бот Elytrix: собственный клиентский протокол Minecraft (offline-сервера),
 * без внешних бинарников. Последовательность и ID пакетов — по спеке 26.2 (protocol 776).
 */
public class OwnBot implements Runnable {
    // --- C2S id (26.2) ---
    static final int HS_INTENTION = 0;
    static final int L_START = 0, L_ACK = 3;
    static final int CFG_CLIENT_INFO = 0, CFG_PAYLOAD = 2, CFG_FINISH = 3, CFG_KEEPALIVE = 4, CFG_PONG = 5, CFG_KNOWN = 7;
    static final int P_CONFIRM_TP = 0, P_CHAT = 9, P_CHUNK_BATCH = 11, P_CLIENT_CMD = 12,
            P_KEEPALIVE = 28, P_POS = 30, P_POS_ROT = 31, P_LOADED = 44, P_PONG = 45, P_SWING = 63;
    // --- S2C id (26.2) ---
    static final int SL_DISCONNECT = 0, SL_ENC = 1, SL_SUCCESS = 2, SL_COMPRESSION = 3;
    static final int SC_DISCONNECT = 2, SC_FINISH = 3, SC_KEEPALIVE = 4, SC_PING = 5;
    static final int SP_KEEPALIVE = 44, SP_LOGIN = 49, SP_PLAYER_POS = 72, SP_SET_HEALTH = 104,
            SP_DISCONNECT = 32, SP_CHUNK_START = 12, SP_COMBAT_KILL = 68, SP_PING = 61,
            SP_SYSTEM_CHAT = 121, SP_CHUNK_DATA = 45;
    static final int P_PLAYER_CMD = 42;

    public final String name;
    private final OwnBotEngine engine;
    private final String host;
    private final int port;
    private final int protocol;
    private final OwnBotSettings settings;
    private final LogBuffer log;
    private final Random rnd = new Random();

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean compression;
    private int threshold;
    private volatile String state = "handshake";
    private volatile boolean alive;
    private volatile String status = "подключение";

    private double x, y, z;
    private float yaw, pitch;
    private double groundY;
    private boolean haveGround;
    private double velY;
    private long nextJumpAt;
    private BotProxy proxy;
    private long nextAfkAt, nextTurnAt0;
    private boolean haveSpawn, afkClockwise;
    private double spawnX, spawnZ, afkAngle, afkRadius;
    private boolean chunkDiag, groundDiag;
    private int cfgLogFirst;
    private float afkTargetYaw;
    private double afkDirX, afkDirZ;
    private boolean havePos;
    private int entityId = -1;

    private int logFirst;
    private long regAt = -1, loginAt = -1;
    private boolean regSent, loginSent;
    private long nextChatAt;
    private long nextSwingAt;
    private long nextTurnAt;
    private double walkDirX, walkDirZ;
    private long nextWalkChangeAt;
    private boolean walking;

    public OwnBot(OwnBotEngine engine, String name, String host, int port, int protocol, OwnBotSettings settings, LogBuffer log) {
        this.engine = engine;
        this.name = name;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.settings = settings;
        this.log = log;
        this.proxy = settings.useProxy ? OwnBotEngine.nextProxy() : null;
    }

    public String proxyLabel() {
        return proxy == null ? "локальный IP" : proxy.host + ":" + proxy.port;
    }

    public String status() {
        return status;
    }

    public boolean isAlive() {
        return alive;
    }

    public void close() {
        alive = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        alive = true;
        try {
            int attempt = 0;
            while (alive && attempt < 3) {
                attempt++;
                if (attemptOne(attempt)) {
                    break;
                }
            }
        } finally {
            alive = false;
            close();
        }
    }

    /** Одна попытка подключения. false — прокси мёртв, пробуем следующий (как ротация в SoulFire). */
    private boolean attemptOne(int attempt) {
        try {
            state = "handshake";
            status = "подключение";
            if (proxy != null) {
                log.add("[Бот " + name + "] подключаюсь через прокси " + proxy
                        + (attempt > 1 ? " (попытка " + attempt + ")" : ""));
                socket = proxy.connect(host, port, 8000);
            } else {
                socket = new Socket(host, port);
                socket.setSoTimeout(30000);
            }
            in = socket.getInputStream();
            out = socket.getOutputStream();

            send(0, w -> { // handshake -> login
                w.varInt(protocol);
                w.str(host);
                w.u16(port);
                w.varInt(2);
            });
            send(L_START, w -> {
                w.str(name);
                w.uuid(name);
            });

            long tickAt = 0;
            long lastRead = System.currentTimeMillis();
            nextChatAt = System.currentTimeMillis() + 1500 + rnd.nextInt(2000);
            while (alive && !socket.isClosed()) {
                boolean any = false;
                while (in.available() > 0) {
                    any = true;
                    lastRead = System.currentTimeMillis();
                    if (!handle(readFrame())) {
                        return true; // кик/отключение — попытка завершена штатно
                    }
                }
                long now = System.currentTimeMillis();
                // login/config медленные сервера (Aternos и т.п.) могут отвечать долго —
                // держим 20 секунд; в play действует настроенный таймаут
                long phaseTimeout = "play".equals(state)
                        ? settings.timeoutMs
                        : Math.max(settings.timeoutMs, 20000);
                if (!any && now - lastRead > phaseTimeout) {
                    if (proxy != null && attempt < 3) {
                        OwnBotEngine.markBadProxy(proxy);
                        log.add("[Бот " + name + "] прокси " + proxy + " молчит (фаза " + state + ") — пробую следующую");
                        BotProxy next = OwnBotEngine.nextProxy();
                        if (next != null) {
                            proxy = next;
                            closeSocket();
                            return false;
                        }
                        log.add("[Бот " + name + "] живых прокси больше нет");
                    }
                    status = "таймаут";
                    log.add("[Бот " + name + "] таймаут соединения (фаза " + state + ")");
                    break;
                }
                if ("play".equals(state) && now >= tickAt) {
                    tickAt = now + 50;
                    tick(now);
                }
                if (in.available() == 0) {
                    Thread.sleep(5);
                }
            }
            return true;
        } catch (Exception e) {
            if (alive) {
                if (e instanceof java.net.UnknownHostException) {
                    status = "адрес не найден (DNS)";
                    log.add("[Бот " + name + "] адрес " + host + " не найден (DNS) — такого сервера нет или он недоступен");
                    return true;
                }
                if (proxy != null && "handshake".equals(state) && attempt < 3) {
                    OwnBotEngine.markBadProxy(proxy);
                    log.add("[Бот " + name + "] прокси " + proxy + " недоступна: " + e);
                    BotProxy next = OwnBotEngine.nextProxy();
                    if (next != null) {
                        proxy = next;
                        closeSocket();
                        return false;
                    }
                    log.add("[Бот " + name + "] живых прокси больше нет");
                }
                status = "ошибка: " + e.getMessage();
                log.add("[Бот " + name + "] " + e);
            }
            return true;
        }
    }

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        socket = null;
        in = null;
        out = null;
        compression = false;
    }

    // ---------- логика ----------

    private boolean handle(Frame f) throws IOException {
        switch (state) {
            case "handshake":
            case "login": {
                if (logFirst < 3) {
                    logFirst++;
                    StringBuilder hx = new StringBuilder();
                    for (int i = 0; i < Math.min(32, f.data.length); i++) {
                        hx.append(String.format("%02X ", f.data[i]));
                    }
                    log.add("[Бот " + name + "] login-пакет id=" + f.id + " off=" + f.off + " hex=" + hx);
                }
                if (f.id == SL_COMPRESSION) {
                    threshold = f.v();
                    compression = true;
                } else if (f.id == SL_ENC) {
                    status = "кик: сервер в online-mode";
                    log.add("[Бот " + name + "] сервер требует online-mode — встроенные боты работают только на offline-серверах");
                    return false;
                } else if (f.id == SL_SUCCESS || elytrix$looksLikeSuccess(f)) {
                    send(L_ACK, w -> {
                    });
                    state = "config";
                    status = "конфигурация";
                    send(CFG_CLIENT_INFO, w -> {
                        w.str("ru_ru");
                        w.i8(8);
                        w.varInt(0);
                        w.bool(true);
                        w.u8(127);
                        w.varInt(1);
                        w.bool(false);
                        w.bool(true);
                        w.varInt(0); // particle status (есть в 26.2)
                    });
                    send(CFG_KNOWN, w -> w.varInt(0));
                } else if (f.id == SL_DISCONNECT) {
                    String why = f.component();
                    if (why == null || why.isEmpty() || why.startsWith("(")) {
                        StringBuilder hex = new StringBuilder();
                        for (int i2 = 0; i2 < Math.min(24, f.data.length); i2++) {
                            hex.append(String.format("%02X ", f.data[i2]));
                        }
                        why = "(hex " + hex + ")";
                    }
                    status = "кик: " + why;
                    log.add("[Бот " + name + "] кик при входе: " + status);
                    return false;
                }
                return true;
            }
            case "config": {
                if (cfgLogFirst < 8) {
                    cfgLogFirst++;
                    log.add("[Бот " + name + "] config-пакет id=" + f.id);
                }
                if (f.id == SC_KEEPALIVE) {
                    long id = f.i64();
                    send(CFG_KEEPALIVE, w -> w.i64(id));
                } else if (f.id == SC_PING) {
                    int id = f.i32();
                    send(CFG_PONG, w -> w.i32(id));
                } else if (f.id == SC_DISCONNECT) {
                    status = "кик: " + f.component();
                    log.add("[Бот " + name + "] кик в конфигурации: " + status);
                    return false;
                } else if (f.id == SC_FINISH) {
                    send(CFG_FINISH, w -> {
                    });
                    state = "play";
                    status = "в игре";
                    log.add("[Бот " + name + "] в игре на " + host + ":" + port);
                    send(P_LOADED, w -> {
                    });
                    long now = System.currentTimeMillis();
                    if (settings.autoReg) {
                        regAt = now + 1000 + rnd.nextInt(500);
                    }
                    if (settings.autoLogin || settings.autoReg) {
                        loginAt = now + (settings.autoReg ? 2500 : 1000) + rnd.nextInt(500);
                    }
                }
                return true;
            }
            case "play": {
                if (f.id == SP_KEEPALIVE) {
                    long id = f.i64();
                    send(P_KEEPALIVE, w -> w.i64(id));
                } else if (f.id == SP_PING) {
                    int id = f.i32();
                    send(P_PONG, w -> w.i32(id));
                } else if (f.id == SP_PLAYER_POS) {
                    int tpId = f.v();
                    double nx = f.f64(), ny = f.f64(), nz = f.f64();
                    f.f64();
                    f.f64();
                    f.f64(); // delta
                    float nyaw = f.f32(), npitch = f.f32();
                    int rel = f.i32();
                    if ((rel & 1) != 0) nx += x;
                    if ((rel & 2) != 0) ny += y;
                    if ((rel & 4) != 0) nz += z;
                    if ((rel & 8) != 0) nyaw += yaw;
                    if ((rel & 16) != 0) npitch += pitch;
                    x = nx;
                    y = ny;
                    z = nz;
                    yaw = nyaw;
                    pitch = npitch;
                    havePos = true;
                    groundY = y;
                    haveGround = true;
                    velY = 0;
                    send(P_CONFIRM_TP, w -> w.varInt(tpId));
                    sendPosRot();
                } else if (f.id == SP_LOGIN) {
                    entityId = f.i32();
                } else if (f.id == SP_SET_HEALTH) {
                    float hp = f.f32();
                    if (hp <= 0f) {
                        send(P_CLIENT_CMD, w -> w.varInt(0)); // respawn
                    }
                } else if (f.id == SP_COMBAT_KILL) {
                    send(P_CLIENT_CMD, w -> w.varInt(0));
                } else if (f.id == SP_CHUNK_START) {
                    send(P_CHUNK_BATCH, w -> w.f32(10f));
                } else if (f.id == SP_CHUNK_DATA) {
                    try {
                        int cx = f.i32();
                        int cz = f.i32();
                        long[] hm = OwnBotEngine.extractHeightmap(f.data, off2(f));
                        if (hm == null && !chunkDiag) {
                            chunkDiag = true;
                            StringBuilder sb = new StringBuilder();
                            for (int i = 8; i < Math.min(f.data.length, 48); i++) {
                                sb.append(String.format("%02X ", f.data[i]));
                            }
                            log.add("[Бот " + name + "] чанк не распознан, байты heightmap: " + sb);
                        }
                        if (hm != null && hm.length >= 36) {
                            engine.putHeights(cx, cz, OwnBotEngine.unpackHeights(hm));
                            int h = engine.heightAt(x, z);
                            if (h != Integer.MIN_VALUE && h > 0) {
                                if (!groundDiag) {
                                    groundDiag = true;
                                    log.add("[Бот " + name + "] heightmap ок: земля=" + h + " y=" + (int) y);
                                }
                                groundY = h;
                                haveGround = true;
                                if (y < h - 0.01) {
                                    y = h;
                                }
                            }
                        }
                    } catch (Exception chunkErr) {
                        // кривой чанк — игнорируем, бот живёт дальше
                    }
                } else if (f.id == SP_SYSTEM_CHAT) {
                    if (settings.captcha) {
                        String code = captchaCode(f.component());
                        if (code != null) {
                            log.add("[Бот " + name + "] капча распознана: " + code);
                            sendChat(code, System.currentTimeMillis());
                        }
                    }
                } else if (f.id == SP_DISCONNECT) {
                    status = "кик: " + f.component();
                    log.add("[Бот " + name + "] кик: " + status);
                    return false;
                }
                return true;
            }
            default:
                return true;
        }
    }

    private void sendChat(String msg, long now) throws IOException {
        send(P_CHAT, w -> {
            w.str(msg);
            w.i64(now);
            w.i64(0);
            w.bool(false);
            w.varInt(0);
            w.bytes(new byte[3]);
            w.u8(0);
        });
    }

    private void tick(long now) throws IOException {
        if (regAt > 0 && !regSent && now >= regAt) {
            regSent = true;
            sendChat("/register " + settings.password + " " + settings.password, now);
            log.add("[Бот " + name + "] авторегистрация");
        }
        if (loginAt > 0 && !loginSent && now >= loginAt) {
            loginSent = true;
            sendChat("/login " + settings.password, now);
            log.add("[Бот " + name + "] автовход");
        }
        // Настоящий анти-афк: медленно наматывает круги вокруг спавна (~1 блок за 30 с),
        // направление и радиус меняются случайно — выглядит как живой игрок
        if (OwnBotEngine.liveAntiAfk && havePos) {
            if (!haveSpawn) {
                haveSpawn = true;
                spawnX = x;
                spawnZ = z;
                afkAngle = rnd.nextDouble() * Math.PI * 2;
                afkRadius = 1.5 + rnd.nextDouble() * 3;
                afkClockwise = rnd.nextBoolean();
            }
            afkAngle += afkClockwise ? 0.0007 : -0.0007; // полный круг ~7-8 минут
            double tx = spawnX + Math.cos(afkAngle) * afkRadius;
            double tz = spawnZ + Math.sin(afkAngle) * afkRadius;
            double dx = tx - x, dz = tz - z;
            double d = Math.hypot(dx, dz);
            if (d > 0.02) {
                double step = Math.min(d, 0.0017); // ~1 блок за 30 секунд
                moveWithCollision(dx / d * step, dz / d * step);
                yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            }
            if (now >= nextAfkAt) {
                nextAfkAt = now + 20000 + rnd.nextInt(20000);
                afkRadius = 1.5 + rnd.nextDouble() * 3;
                if (rnd.nextInt(3) == 0) {
                    afkClockwise = !afkClockwise;
                }
                pitch = -5f + rnd.nextFloat() * 15f;
            }
        } else if (OwnBotEngine.liveRotation && now >= nextTurnAt) {
            nextTurnAt = now + 1500 + rnd.nextInt(3000);
            yaw = rnd.nextFloat() * 360f;
        }
        // Земля под ногами берётся из heightmap чанка, если она есть
        int hh = engine.heightAt(x, z);
        if (hh != Integer.MIN_VALUE && hh > 0) {
            groundY = hh;
            haveGround = true;
        }
        // Физика как у живого игрока (SoulFire auto-jump): падение на землю + периодические прыжки
        if (haveGround && (velY != 0 || y > groundY + 0.001)) {
            y += velY;
            velY -= 0.08;
            if (y <= groundY) {
                y = groundY;
                velY = 0;
            }
        }
        if (havePos) {
            sendPosRot();
        }
        if (OwnBotEngine.liveSwing && now >= nextSwingAt) {
            nextSwingAt = now + 1500 + rnd.nextInt(2500);
            send(P_SWING, w -> w.varInt(0));
        }
        if (OwnBotEngine.liveSpam && now >= nextChatAt) {
            nextChatAt = now + OwnBotEngine.liveSpamMin
                    + rnd.nextInt(Math.max(1, OwnBotEngine.liveSpamMax - OwnBotEngine.liveSpamMin));
            sendChat(OwnBotEngine.liveSpamMessage, now);
        }
    }

    /** NeoProxy-стиль: авто-решение текстовых капч антибота. Ищет код 4-8 символов рядом со словом captcha/капча/код/code. */
    static String captchaCode(String t) {
        if (t == null || t.isEmpty()) {
            return null;
        }
        String low = t.toLowerCase();
        String[] keys = {"captcha", "капча", "код", "code"};
        int kw = -1, kl = 0;
        for (String k : keys) {
            int i = low.indexOf(k);
            if (i >= 0 && i > kw) {
                kw = i;
                kl = k.length();
            }
        }
        if (kw >= 0) {
            String after = capToken(t, kw + kl);
            if (after != null) {
                return after;
            }
            String before = capTokenBefore(t, kw);
            if (before != null) {
                return before;
            }
        }
        return null;
    }

    private static boolean isCapChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
    }

    private static String capToken(String t, int from) {
        int i = from;
        while (i < t.length() && !isCapChar(t.charAt(i))) {
            i++;
        }
        int st = i;
        while (i < t.length() && isCapChar(t.charAt(i))) {
            i++;
        }
        int len = i - st;
        return len >= 4 && len <= 8 ? t.substring(st, i) : null;
    }

    private static String capTokenBefore(String t, int to) {
        int i = to;
        while (i > 0 && !isCapChar(t.charAt(i - 1))) {
            i--;
        }
        int en = i;
        while (i > 0 && isCapChar(t.charAt(i - 1))) {
            i--;
        }
        int len = en - i;
        return len >= 4 && len <= 8 ? t.substring(i, en) : null;
    }

    /** Фолбэк: пакет содержит наш ник после 16-байтного uuid — это Login Success, даже если id неожиданный. */
    private boolean elytrix$looksLikeSuccess(OwnBot.Frame f) {
        byte[] nb = name.getBytes(StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i <= f.data.length - nb.length; i++) {
            for (int j = 0; j < nb.length; j++) {
                if (f.data[i + j] != nb[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /** Не даёт идти в стену: если в целевой колонке блок выше нас больше чем на 1 — стоим/прыгаем. */
    private void moveWithCollision(double mx, double mz) {
        double nx = x + mx, nz = z + mz;
        int th = engine.heightAt(nx, nz);
        if (th == Integer.MIN_VALUE || th <= y + 1.001) {
            x = nx;
            z = nz;
        } else if (OwnBotEngine.liveAutoJump && velY == 0 && th <= y + 2.001) {
            velY = 0.42; // стенка в один блок — перепрыгиваем
            x = nx;
            z = nz;
        }
    }

    private static int off2(Frame f) {
        return 8; // после i32 x и i32 z начинается NBT heightmap
    }

    private void sendPosRot() throws IOException {
        // антикик всегда: честный onGround (иначе античит видит «полёт» во время прыжка)
        boolean ground = velY == 0 && y <= groundY + 0.001;
        send(P_POS_ROT, w -> {
            w.f64(x);
            w.f64(y);
            w.f64(z);
            w.f32(yaw);
            w.f32(pitch);
            w.u8(ground ? 1 : 0);
        });
    }

    // ---------- каркас ----------

    private interface Body {
        void write(Writer w) throws IOException;
    }

    private synchronized void send(int id, Body body) throws IOException {
        if (out == null) {
            return;
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        Writer w = new Writer(raw);
        w.varInt(id);
        body.write(w);
        byte[] payload = raw.toByteArray();
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        if (compression) {
            if (payload.length >= threshold && threshold > 0) {
                ByteArrayOutputStream z = new ByteArrayOutputStream();
                Deflater d = new Deflater();
                d.setInput(payload);
                d.finish();
                byte[] buf = new byte[8192];
                while (!d.finished()) {
                    z.write(buf, 0, d.deflate(buf));
                }
                d.end();
                Writer fw = new Writer(frame);
                fw.varInt(payload.length);
                frame.write(z.toByteArray());
                writeLen(frame.toByteArray());
                return;
            }
            Writer fw = new Writer(frame);
            fw.varInt(0);
            frame.write(payload);
            writeLen(frame.toByteArray());
        } else {
            writeLen(payload);
        }
    }

    private void writeLen(byte[] body) throws IOException {
        ByteArrayOutputStream l = new ByteArrayOutputStream();
        new Writer(l).varInt(body.length);
        out.write(l.toByteArray());
        out.write(body);
        out.flush();
    }

    private Frame readFrame() throws IOException {
        DataInputStream d = new DataInputStream(in);
        int len = readVarInt(d);
        byte[] data = d.readNBytes(len);
        if (len != data.length) {
            throw new IOException("обрыв кадра");
        }
        int off = 0;
        if (compression) {
            int[] holder = new int[1];
            int dataLen = readVarInt(data, holder);
            off = holder[0];
            if (dataLen > 0) {
                Inflater inf = new Inflater();
                inf.setInput(data, off, data.length - off);
                ByteArrayOutputStream out2 = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                try {
                    while (!inf.finished()) {
                        int n = inf.inflate(buf);
                        if (n <= 0) {
                            break;
                        }
                        out2.write(buf, 0, n);
                    }
                } catch (java.util.zip.DataFormatException e) {
                    throw new IOException("битый zlib-кадр: " + e.getMessage());
                } finally {
                    inf.end();
                }
                data = out2.toByteArray();
                off = 0;
            }
        }
        int[] holder = new int[]{off};
        int id = readVarInt(data, holder);
        return new Frame(id, data, holder[0]);
    }

    static int readVarInt(InputStream in) throws IOException {
        int value = 0, pos = 0;
        while (pos < 32) {
            int b = in.read();
            if (b < 0) {
                throw new IOException("конец потока");
            }
            value |= (b & 0x7F) << pos;
            if ((b & 0x80) == 0) {
                return value;
            }
            pos += 7;
        }
        throw new IOException("VarInt слишком длинный");
    }

    static int readVarInt(byte[] data, int[] off) {
        int value = 0, pos = 0;
        while (pos < 32) {
            byte b = data[off[0]++];
            value |= (b & 0x7F) << pos;
            if ((b & 0x80) == 0) {
                return value;
            }
            pos += 7;
        }
        throw new IllegalStateException("VarInt слишком длинный");
    }

    static final class Writer {
        private final OutputStream o;

        Writer(OutputStream o) {
            this.o = o;
        }

        void varInt(int v) throws IOException {
            while ((v & ~0x7F) != 0) {
                o.write((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            o.write(v);
        }

        void str(String s) throws IOException {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            varInt(b.length);
            o.write(b);
        }

        void u16(int v) throws IOException {
            o.write(v >>> 8);
            o.write(v);
        }

        void u8(int v) throws IOException {
            o.write(v);
        }

        void i8(int v) throws IOException {
            o.write(v);
        }

        void bool(boolean b) throws IOException {
            o.write(b ? 1 : 0);
        }

        void i64(long v) throws IOException {
            for (int i = 7; i >= 0; i--) {
                o.write((int) (v >>> (i * 8)) & 0xFF);
            }
        }

        void i32(int v) throws IOException {
            for (int i = 3; i >= 0; i--) {
                o.write((v >>> (i * 8)) & 0xFF);
            }
        }

        void f32(float v) throws IOException {
            i32(Float.floatToIntBits(v));
        }

        void f64(double v) throws IOException {
            i64(Double.doubleToLongBits(v));
        }

        void bytes(byte[] b) throws IOException {
            o.write(b);
        }

        void uuid(String name) throws IOException {
            // оффлайн-uuid: как у ваніллы — от md5 имени
            try {
                java.util.UUID u = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
                i64(u.getMostSignificantBits());
                i64(u.getLeastSignificantBits());
            } catch (Exception e) {
                i64(0);
                i64(0);
            }
        }
    }

    static final class Frame {
        final int id;
        final byte[] data;
        int off;

        Frame(int id, byte[] data, int off) {
            this.id = id;
            this.data = data;
            this.off = off;
        }

        int v() {
            int[] h = {off};
            int r = readVarInt(data, h);
            off = h[0];
            return r;
        }

        long i64() {
            long r = 0;
            for (int i = 0; i < 8; i++) {
                r = (r << 8) | (data[off++] & 0xFF);
            }
            return r;
        }

        int i32() {
            int r = 0;
            for (int i = 0; i < 4; i++) {
                r = (r << 8) | (data[off++] & 0xFF);
            }
            return r;
        }

        float f32() {
            return Float.intBitsToFloat(i32());
        }

        double f64() {
            return Double.longBitsToDouble(i64());
        }

        String component() {
            try {
                int[] h = {off};
                int save = h[0];
                try {
                    int len = readVarInt(data, h);
                    if (len > 0 && h[0] + len <= data.length && data[h[0]] == '{') {
                        String json = new String(data, h[0], len, StandardCharsets.UTF_8);
                        off = h[0] + len;
                        String t = jsonText(json);
                        return t.isEmpty() ? "(нет текста)" : t;
                    }
                } catch (Exception ignored) {
                }
                h[0] = save;
                int t = data[h[0]++] & 0xFF;
                String r = nbtWalk(data, h, t, null, 0);
                off = h[0];
                return r == null || r.isEmpty() ? "(пусто)" : r;
            } catch (Exception e) {
                return "(не читается)";
            }
        }

        /** Склеивает все "text"-значения из JSON-компонента (1.16-серверы через Via шлют JSON). */
        private static String jsonText(String json) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (true) {
                int k = json.indexOf("\"text\"", i);
                if (k < 0) break;
                int q = json.indexOf(':', k + 6);
                if (q < 0) break;
                q++;
                while (q < json.length() && json.charAt(q) <= ' ') q++;
                if (q >= json.length() || json.charAt(q) != '"') { i = k + 6; continue; }
                q++;
                StringBuilder part = new StringBuilder();
                while (q < json.length() && json.charAt(q) != '"') {
                    char c = json.charAt(q);
                    if (c == '\\' && q + 1 < json.length()) {
                        char e = json.charAt(++q);
                        switch (e) {
                            case 'n' -> part.append('\n');
                            case 't' -> part.append('\t');
                            case 'u' -> {
                                if (q + 4 < json.length()) {
                                    try {
                                        part.append((char) Integer.parseInt(json.substring(q + 1, q + 5), 16));
                                        q += 4;
                                    } catch (NumberFormatException ex) { part.append(e); }
                                } else part.append(e);
                            }
                            default -> part.append(e);
                        }
                    } else if (c != '\n' && c != '\r' && c != '\t') {
                        part.append(c);
                    } else part.append(' ');
                    q++;
                }
                sb.append(part);
                i = q + 1;
            }
            return sb.toString().replace("\n\n", " ").trim();
        }

        static int u16at(byte[] d, int[] h) {
            return ((d[h[0]++] & 0xFF) << 8) | (d[h[0]++] & 0xFF);
        }

        static int i32at(byte[] d, int[] h) {
            int r = 0;
            for (int i = 0; i < 4; i++) {
                r = (r << 8) | (d[h[0]++] & 0xFF);
            }
            return r;
        }

        /** Ищет текст в NBT-компоненте: поле "text" или корневая строка. */
        static String nbtWalk(byte[] d, int[] h, int type, String key, int depth) {
            if (depth > 8) {
                return null;
            }
            switch (type) {
                case 1: h[0]++; return null;
                case 2: h[0] += 2; return null;
                case 3: h[0] += 4; return null;
                case 4: h[0] += 8; return null;
                case 5: h[0] += 4; return null;
                case 6: h[0] += 8; return null;
                case 7: h[0] += i32at(d, h); return null;
                case 8: {
                    int n = u16at(d, h);
                    String r = new String(d, h[0], n, StandardCharsets.UTF_8);
                    h[0] += n;
                    return r;
                }
                case 9: {
                    int et = d[h[0]++] & 0xFF;
                    int n = i32at(d, h);
                    for (int i = 0; i < n; i++) {
                        String r = nbtWalk(d, h, et, null, depth + 1);
                        if (r != null) {
                            return r;
                        }
                    }
                    return null;
                }
                case 10: {
                    while (true) {
                        int et = d[h[0]++] & 0xFF;
                        if (et == 0) {
                            return null;
                        }
                        int nl = u16at(d, h);
                        String name = new String(d, h[0], nl, StandardCharsets.UTF_8);
                        h[0] += nl;
                        String r = nbtWalk(d, h, et, name, depth + 1);
                        if (r != null) {
                            return r;
                        }
                    }
                }
                case 11: h[0] += 4L * i32at(d, h); return null;
                case 12: h[0] += 8L * i32at(d, h); return null;
                default: return null;
            }
        }

        String str() {
            int[] h = {off};
            int len = readVarInt(data, h);
            off = h[0];
            String s = new String(data, off, len, StandardCharsets.UTF_8);
            off += len;
            return s;
        }
    }
}
