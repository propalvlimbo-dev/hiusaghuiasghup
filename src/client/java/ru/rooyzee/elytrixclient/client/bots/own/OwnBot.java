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
            SP_DISCONNECT = 32, SP_CHUNK_START = 12, SP_COMBAT_KILL = 68, SP_PING = 61;

    public final String name;
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
    private boolean havePos;
    private int entityId = -1;

    private long regAt = -1, loginAt = -1;
    private boolean regSent, loginSent;
    private long nextChatAt;
    private long nextSwingAt;
    private long nextTurnAt;
    private double walkDirX, walkDirZ;
    private long nextWalkChangeAt;
    private boolean walking;

    public OwnBot(String name, String host, int port, int protocol, OwnBotSettings settings, LogBuffer log) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.settings = settings;
        this.log = log;
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
            socket = new Socket(host, port);
            socket.setSoTimeout(30000);
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
                        return;
                    }
                }
                long now = System.currentTimeMillis();
                if (!any && now - lastRead > settings.timeoutMs) {
                    status = "таймаут";
                    log.add("[Бот " + name + "] таймаут соединения");
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
        } catch (Exception e) {
            if (alive) {
                status = "ошибка: " + e.getMessage();
                log.add("[Бот " + name + "] " + e);
            }
        } finally {
            alive = false;
            close();
        }
    }

    // ---------- логика ----------

    private boolean handle(Frame f) throws IOException {
        switch (state) {
            case "handshake":
            case "login": {
                if (f.id == SL_COMPRESSION) {
                    threshold = f.v();
                    compression = true;
                } else if (f.id == SL_ENC) {
                    status = "кик: сервер в online-mode";
                    log.add("[Бот " + name + "] сервер требует online-mode — встроенные боты работают только на offline-серверах");
                    return false;
                } else if (f.id == SL_DISCONNECT) {
                    status = "кик: " + f.component();
                    log.add("[Бот " + name + "] кик при входе: " + status);
                    return false;
                } else if (f.id == SL_SUCCESS) {
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
                    });
                    send(CFG_KNOWN, w -> w.varInt(0));
                }
                return true;
            }
            case "config": {
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
        if (settings.movement) {
            if (now >= nextWalkChangeAt) {
                nextWalkChangeAt = now + 2000 + rnd.nextInt(4000);
                walking = rnd.nextBoolean();
                double a = rnd.nextDouble() * Math.PI * 2;
                walkDirX = Math.sin(a);
                walkDirZ = -Math.cos(a);
            }
            if (walking) {
                x += walkDirX * 0.09;
                z += walkDirZ * 0.09;
                yaw = (float) Math.toDegrees(Math.atan2(walkDirX, -walkDirZ));
            }
        } else if (settings.rotation && now >= nextTurnAt) {
            nextTurnAt = now + 1500 + rnd.nextInt(3000);
            yaw = rnd.nextFloat() * 360f;
            pitch = -20f + rnd.nextFloat() * 60f;
        }
        if (havePos) {
            sendPosRot();
        }
        if (settings.swing && now >= nextSwingAt) {
            nextSwingAt = now + 1500 + rnd.nextInt(2500);
            send(P_SWING, w -> w.varInt(0));
        }
        if (settings.spam && now >= nextChatAt) {
            nextChatAt = now + settings.spamDelayMin + rnd.nextInt(Math.max(1, settings.spamDelayMax - settings.spamDelayMin));
            sendChat(settings.spamMessage, now);
        }
    }

    private void sendPosRot() throws IOException {
        send(P_POS_ROT, w -> {
            w.f64(x);
            w.f64(y);
            w.f64(z);
            w.f32(yaw);
            w.f32(pitch);
            w.u8(1); // on ground
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
        int[] holder = new int[1];
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
                int t = data[off++] & 0xFF;
                if (t == 8) {
                    return str();
                }
                return "(компонент)";
            } catch (Exception e) {
                return "(?)";
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
