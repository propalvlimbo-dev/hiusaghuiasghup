package ru.rooyzee.elytrixclient.client.bots.own;

import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Загрузчик свежих бесплатных прокси с живой проверкой (как proxy-checker в SoulFire):
 * качает публичные списки, затем каждую прокси реально проверяет на целевом сервере —
 * через прокси делается handshake + login start, и прокси считается живой только если
 * сервер ответил хоть байтом. Выжившие пишутся в proxies.txt.
 */
public final class ProxyFetcher {

    private static final String[] SOURCES = {
            "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=socks5&timeout=8000&country=all",
            "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=8000&country=all",
            "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5.txt",
            "https://raw.githubusercontent.com/jetkai/proxy-list/main/online-proxies/txt/proxies-socks5.txt",
            "https://raw.githubusercontent.com/jetkai/proxy-list/main/online-proxies/txt/proxies-http.txt",
    };

    public static volatile boolean busy;
    public static volatile String progress = "";

    private ProxyFetcher() {
    }

    /** Асинхронно: скачать списки -> проверить на targetHost:targetPort -> сохранить живые. */
    public static void fetchValidateSave(String targetHostIn, int targetPortIn, String proxyFilePath, LogBuffer log) {
        if (busy) {
            return;
        }
        busy = true;
        Thread t = new Thread(() -> {
            try {
                String targetHost = targetHostIn;
                int targetPort = targetPortIn;
                if (targetPort == 25565) {
                    String[] srv = OwnBotEngine.resolveSrv(targetHost);
                    if (srv != null) {
                        try {
                            targetPort = Integer.parseInt(srv[1]);
                            targetHost = srv[0];
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                progress = "скачиваю списки...";
                LinkedHashSet<String> lines = new LinkedHashSet<>();
                for (String url : SOURCES) {
                    try {
                        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                        c.setConnectTimeout(8000);
                        c.setReadTimeout(12000);
                        c.setRequestProperty("User-Agent", "Mozilla/5.0");
                        int n = 0;
                        try (BufferedReader r = new BufferedReader(
                                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                            String ln;
                            while ((ln = r.readLine()) != null && lines.size() < 1500) {
                                lines.add(ln.trim());
                                n++;
                            }
                        }
                        log.add("[Прокси] " + shortName(url) + ": +" + n + " строк");
                    } catch (Exception e) {
                        log.add("[Прокси] источник недоступен: " + shortName(url));
                    }
                }
                List<BotProxy> parsed = new ArrayList<>();
                LinkedHashSet<String> seen = new LinkedHashSet<>();
                for (String ln : lines) {
                    BotProxy p = BotProxy.parse(ln);
                    if (p != null && seen.add(p.host + ":" + p.port)) {
                        parsed.add(p);
                    }
                }
                if (parsed.isEmpty()) {
                    log.add("[Прокси] не удалось скачать ни одной прокси");
                    return;
                }
                log.add("[Прокси] кандидатов: " + parsed.size() + ", проверяю на " + targetHost + ":" + targetPort + "...");
                final String fHost = targetHost;
                final int fPort = targetPort;
                List<BotProxy> alive = Collections.synchronizedList(new ArrayList<>());
                ExecutorService pool = Executors.newFixedThreadPool(48);
                AtomicInteger done = new AtomicInteger();
                for (BotProxy p : parsed) {
                    pool.submit(() -> {
                        if (check(p, fHost, fPort)) {
                            alive.add(p);
                        }
                        int d = done.incrementAndGet();
                        if (d % 50 == 0) {
                            progress = d + "/" + parsed.size() + ", живо " + alive.size();
                        }
                    });
                }
                pool.shutdown();
                pool.awaitTermination(10, TimeUnit.MINUTES);
                Path f = OwnBotEngine.proxyPath(proxyFilePath);
                if (f.getParent() != null) {
                    Files.createDirectories(f.getParent());
                }
                StringBuilder sb = new StringBuilder(
                        "# свежие прокси Elytrix, проверены на " + targetHost + ":" + targetPort + "\n");
                for (BotProxy p : alive) {
                    sb.append(p.host).append(':').append(p.port);
                    if (p.hasAuth()) {
                        sb.append(':').append(p.user).append(':').append(p.pass);
                    }
                    sb.append('\n');
                }
                Files.writeString(f, sb.toString(), StandardCharsets.UTF_8);
                OwnBotEngine.loadProxies(proxyFilePath, log);
                log.add("[Прокси] готово: живых " + alive.size() + " из " + parsed.size() + " — список сохранён и загружен");
            } catch (Exception e) {
                log.add("[Прокси] ошибка загрузки: " + e);
            } finally {
                busy = false;
                progress = "";
            }
        }, "elytrix-proxy-fetcher");
        t.setDaemon(true);
        t.start();
    }

    /** Прокси жива, если через неё сервер ответил хоть одним байтом на handshake+login. */
    private static boolean check(BotProxy p, String host, int port) {
        try (Socket s = p.connect(host, port, 5000)) {
            s.setSoTimeout(6000);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            OwnBot.Writer w = new OwnBot.Writer(b);
            w.varInt(0); // packet id: handshake
            w.varInt(776);
            w.str(host);
            w.u16(port);
            w.varInt(2);
            byte[] hs = b.toByteArray();
            ByteArrayOutputStream f1 = new ByteArrayOutputStream();
            new OwnBot.Writer(f1).varInt(hs.length);
            s.getOutputStream().write(f1.toByteArray());
            s.getOutputStream().write(hs);
            ByteArrayOutputStream b2 = new ByteArrayOutputStream();
            OwnBot.Writer w2 = new OwnBot.Writer(b2);
            w2.varInt(0); // packet id: login start
            w2.str("ElytrixProxyCheck");
            w2.uuid("ElytrixProxyCheck");
            byte[] ls = b2.toByteArray();
            ByteArrayOutputStream f2 = new ByteArrayOutputStream();
            new OwnBot.Writer(f2).varInt(ls.length);
            s.getOutputStream().write(f2.toByteArray());
            s.getOutputStream().write(ls);
            s.getOutputStream().flush();
            return s.getInputStream().read() >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String shortName(String url) {
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
