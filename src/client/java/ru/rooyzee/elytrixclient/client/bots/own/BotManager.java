package ru.rooyzee.elytrixclient.client.bots.own;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер папок-серверов: у каждой папки свой движок ботов, свой пул ников
 * и свои настройки. Папки переживают перезапуск клиента (JSON в .minecraft/elytrix/).
 */
public final class BotManager {

    public static final List<BotFolder> folders = new ArrayList<>();
    private static final Map<BotFolder, OwnBotEngine> engines = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static volatile int selected;

    private BotManager() {
    }

    public static BotFolder selected() {
        int i = selected;
        return i >= 0 && i < folders.size() ? folders.get(i) : null;
    }

    public static synchronized BotFolder create(String name, String address) {
        BotFolder f = new BotFolder();
        f.name = name;
        f.address = address == null ? "" : address.trim();
        folders.add(f);
        selected = folders.size() - 1;
        save();
        return f;
    }

    public static synchronized void remove(BotFolder f) {
        stop(f);
        folders.remove(f);
        engines.remove(f);
        if (selected >= folders.size()) {
            selected = folders.size() - 1;
        }
        save();
    }

    public static void generateAccounts(BotFolder f, int n) {
        for (int i = 0; i < n; i++) {
            f.addAccount(OwnBotEngine.randomName());
        }
        save();
    }

    public static OwnBotEngine engine(BotFolder f) {
        return engines.get(f);
    }

    public static boolean running(BotFolder f) {
        OwnBotEngine e = engines.get(f);
        return e != null && e.isRunning();
    }

    public static String status(BotFolder f) {
        OwnBotEngine e = engines.get(f);
        if (e == null || !e.isRunning()) {
            return "стоп";
        }
        return e.aliveCount() + "/" + f.connectCount + " в игре";
    }

    /** Запуск ботов папки: адрес (SRV поддержит движок), протокол из status-пинга, пул ников. */
    public static synchronized void start(BotFolder f) {
        LogBuffer log = ElytrixclientClient.LOG;
        OwnBotEngine e = engines.get(f);
        if (e == null) {
            e = new OwnBotEngine(log);
            engines.put(f, e);
        }
        if (e.isRunning()) {
            return;
        }
        var cfg = ElytrixclientClient.CONFIG;
        OwnBotSettings st = new OwnBotSettings();
        st.folder = f;
        st.count = Math.max(1, f.connectCount);
        st.delayMs = Math.max(50, f.delayMs);
        st.timeoutMs = cfg.botmarkTimeout;
        st.randomNames = f.accounts.isEmpty();
        st.prefix = cfg.ownBotPrefix;
        st.autoReg = f.autoReg;
        st.autoLogin = f.autoLogin;
        st.password = f.password;
        st.spam = cfg.bmSpam;
        st.spamMessage = cfg.bmSpamMessage;
        st.spamDelayMin = cfg.botSpamMin;
        st.spamDelayMax = cfg.botSpamMax;
        st.rotation = cfg.bmRotation;
        st.swing = cfg.bmSwing;
        st.mode = 0; // ходьба убрана: боты стоят/анти-афк у спавна
        st.autoJump = cfg.botAutoJump;
        st.captcha = cfg.botCaptcha;
        st.antiAfk = cfg.botAntiAfk;
        st.useProxy = f.useProxy;
        st.rejoin = true; // всегда: кик -> реждойн, бан -> смена ника
        st.rejoinDelayMs = cfg.botRejoinDelay;
        String addr = f.address.trim();
        String host = addr;
        int port = 25565;
        int colon = addr.lastIndexOf(':');
        if (colon > 0) {
            host = addr.substring(0, colon);
            try {
                port = Integer.parseInt(addr.substring(colon + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        if (host.isEmpty()) {
            log.add("[Папки] у «" + f.name + "» не задан адрес");
            return;
        }
        e.start(host, port, 776, st);
    }

    public static void stop(BotFolder f) {
        OwnBotEngine e = engines.get(f);
        if (e != null) {
            e.stop();
        }
    }

    public static void stopAll() {
        for (OwnBotEngine e : engines.values()) {
            e.stop();
        }
    }

    // ── персистентность ──────────────────────────────────────────────────

    public static java.nio.file.Path path() {
        return OwnBotEngine.proxyPath("bot-folders.json");
    }

    public static synchronized void save() {
        try {
            java.nio.file.Path f = path();
            if (f.getParent() != null) {
                Files.createDirectories(f.getParent());
            }
            Files.writeString(f, GSON.toJson(folders), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static synchronized void load(LogBuffer log) {
        try {
            java.nio.file.Path f = path();
            if (!Files.exists(f)) {
                return;
            }
            List<BotFolder> loaded = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8),
                    new TypeToken<List<BotFolder>>() {
                    }.getType());
            if (loaded != null) {
                folders.clear();
                for (BotFolder b : loaded) {
                    if (b != null) {
                        if (b.accounts == null) {
                            b.accounts = new ArrayList<>();
                        }
                        if (b.banned == null) {
                            b.banned = new java.util.HashSet<>();
                        }
                        folders.add(b);
                    }
                }
                log.add("[Папки] загружено папок: " + folders.size());
            }
        } catch (Exception e) {
            log.add("[Папки] не читается bot-folders.json: " + e);
        }
    }
}
