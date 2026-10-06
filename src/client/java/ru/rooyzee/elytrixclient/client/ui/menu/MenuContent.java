package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.ui.ElytrixLoader;
import ru.rooyzee.elytrixclient.client.ui.ElytrixQuality;
import ru.rooyzee.elytrixclient.client.ui.UiSound;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Карточки разделов панели. Всё, что меняется, сразу пишется в {@link ElytrixConfig}. */
public final class MenuContent {
    public static final String[] SCALE_NAMES = {"Авто", "90%", "100%", "115%", "130%"};

    private final ElytrixConfig cfg = ElytrixclientClient.CONFIG;
    private final Runnable dirty;
    private final IntConsumer open;
    private final Runnable resetUi;
    private String draftNick = "";
    private String draftChat = "";
    /** Версия структуры карточек: меняется при создании/удалении/выборе папок — экран пересобирает вкладки. */
    private int structVersion;

    public boolean structChanged(int seen) {
        return structVersion != seen;
    }

    public int structVersion() {
        return structVersion;
    }

    private void struct() {
        structVersion++;
    }

    /**
     * @param dirty   отметить конфиг изменённым (сохранится с задержкой)
     * @param open    перейти на раздел по индексу
     * @param resetUi сбросить настройки интерфейса
     */
    public MenuContent(Runnable dirty, IntConsumer open, Runnable resetUi) {
        this.dirty = dirty;
        this.open = open;
        this.resetUi = resetUi;
    }

    private static String whereAmI() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "меню";
        }
        if (mc.hasSingleplayerServer()) {
            return "одиночный мир";
        }
        var server = mc.getCurrentServer();
        return server != null ? server.ip : "сервер";
    }

    public List<MenuCard> home() {
        List<MenuCard> list = new ArrayList<>();
        list.add(new MenuCard("Клиент")
                .badge(() -> "v" + ElytrixLoader.version(), 0)
                .add(new MenuRow.Info("Minecraft", () -> "26.2 · Fabric", 0))
                .add(new MenuRow.Info("Панель", () -> cfg.panelKey ? "правый Ctrl" : "выкл", 0))
                .add(new MenuRow.Info("Конфиг", () -> String.valueOf(ElytrixConfig.file().getFileName()), 0)));

        list.add(new MenuCard("Состояние")
                .badge(() -> Minecraft.getInstance().getFps() + " fps", UiTheme.OK)
                .add(new MenuRow.Info("Эффекты", ElytrixQuality::summary, 0))
                .add(new MenuRow.Info("Где", MenuContent::whereAmI, 0))
                .add(new MenuRow.Info("Строк в логе", () -> String.valueOf(ElytrixclientClient.LOG.size()), 0))
                .add(new MenuRow.Info("Боты", () -> {
                    var rd = ElytrixclientClient.RUST_BOTS;
                    var ob = ElytrixclientClient.OWN_BOTS;
                    if (ru.rooyzee.elytrixclient.client.bots.rust.RustBotDaemon.available(cfg) || rd.isRunning()) {
                        return "rust: " + rd.statusLine();
                    }
                    return "встроенные: " + ob.status();
                }, 0)));

        list.add(new MenuCard("Цель")
                .badge(cfg::target, 0)
                .add(new MenuRow.Text("Адрес сервера", 128, () -> cfg.host, v -> {
                    cfg.host = v.trim();
                    dirty.run();
                }))
                .add(new MenuRow.Text("Порт", 5, () -> cfg.port == 0 ? "" : String.valueOf(cfg.port), v -> {
                    String digits = v.replaceAll("[^0-9]", "");
                    cfg.port = digits.isEmpty() ? 0 : Mth.clamp(ElytrixConfig.parseInt(digits, 25565), 0, 65535);
                    dirty.run();
                })));

        list.add(new MenuCard("Быстрый доступ")
                .add(new MenuRow.Button("Открыть консоль", MenuRow.Button.Kind.PRIMARY, () -> open.accept(3)))
                .add(new MenuRow.Button("Боты", MenuRow.Button.Kind.SECONDARY, () -> open.accept(1)))
                .add(new MenuRow.Button("Настройки", MenuRow.Button.Kind.SECONDARY, () -> open.accept(4))));
        return list;
    }

    public List<MenuCard> bots() {
        List<MenuCard> list = new ArrayList<>();
        list.add(foldersCard());
        return list;
    }

    public List<MenuCard> proxy() {
        List<MenuCard> list = new ArrayList<>();
        list.add(new MenuCard("Прокси для ботов")
                .badge(() -> String.valueOf(ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.proxyCount()), 0)
                .add(new MenuRow.Header("Источник"))
                .add(new MenuRow.Text("Файл со списком", 200, () -> cfg.botProxyFile, v -> {
                    cfg.botProxyFile = v;
                    dirty.run();
                }).describe("txt-файл: по одной прокси на строку. Если путь не абсолютный — ищется в .minecraft/elytrix/"))
                .add(new MenuRow.Button(() -> "Открыть файл прокси", MenuRow.Button.Kind.PRIMARY, () -> {
                    try {
                        java.nio.file.Path f = ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine
                                .proxyPath(cfg.botProxyFile);
                        if (f.getParent() != null) {
                            java.nio.file.Files.createDirectories(f.getParent());
                        }
                        if (!java.nio.file.Files.exists(f)) {
                            java.nio.file.Files.writeString(f,
                                    "# по одной прокси на строку: ip:port или ip:port:login:pass\n",
                                    java.nio.charset.StandardCharsets.UTF_8);
                        }
                        net.minecraft.util.Util.getPlatform().openFile(f.toFile());
                    } catch (Exception e) {
                        ru.rooyzee.elytrixclient.client.ElytrixclientClient.LOG.add("[Прокси] " + e.getMessage());
                    }
                }).describe("Откроет txt в системном редакторе — закинь туда прокси и сохрани"))
                .add(new MenuRow.Button(() -> "Перечитать файл", MenuRow.Button.Kind.SECONDARY, () -> {
                    ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.loadProxies(cfg.botProxyFile,
                            ru.rooyzee.elytrixclient.client.ElytrixclientClient.LOG);
                }))
                .add(new MenuRow.Button(() -> ru.rooyzee.elytrixclient.client.bots.own.ProxyFetcher.busy
                        ? "Проверка: " + ru.rooyzee.elytrixclient.client.bots.own.ProxyFetcher.progress
                        : "Скачать свежие прокси", MenuRow.Button.Kind.PRIMARY, () -> {
                    String addr = cfg.botAddress.trim();
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
                    ru.rooyzee.elytrixclient.client.bots.own.ProxyFetcher.fetchValidateSave(
                            host, port, cfg.botProxyFile,
                            ru.rooyzee.elytrixclient.client.ElytrixclientClient.LOG);
                }).describe("Качает свежие бесплатные списки, проверяет каждую на твоём сервере и ДОБАВЛЯЕТ живые к твоему списку — старые прокси не удаляются"))
                .add(new MenuRow.Info("В списке", () -> ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.proxyCount()
                        + " шт", 0))
                .add(new MenuRow.Header("Использование"))
                .add(toggle("Боты через прокси", () -> cfg.botUseProxy, v -> cfg.botUseProxy = v)
                        .describe("Выкл — боты заходят с твоего IP; вкл — каждый бот берёт следующую прокси по кругу"))
                .add(new MenuRow.Info("Режим", () -> cfg.botUseProxy
                        ? "прокси (" + ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.proxyCount() + ")"
                        : "твой IP", 0)));

        list.add(new MenuCard("Формат строк")
                .add(new MenuRow.Info("Без логина", () -> "ip:port", 0))
                .add(new MenuRow.Info("С логином", () -> "ip:port:login:pass", 0))
                .add(new MenuRow.Info("Протоколы", () -> "SOCKS5 и HTTP", 0))
                .add(new MenuRow.Info("Раздача", () -> "по кругу на бота", 0)));
        return list;
    }
    /** Список папок-серверов; клик по папке открывает её отдельную страницу. */
    private MenuCard foldersCard() {
        var bm = ru.rooyzee.elytrixclient.client.bots.own.BotManager.class;
        MenuCard c = new MenuCard("Папки-серверы")
                .badge(() -> ru.rooyzee.elytrixclient.client.bots.own.BotManager.folders.size() + " папок", 0);
        c.add(new MenuRow.Button(() -> "Создать папку", MenuRow.Button.Kind.PRIMARY, () -> {
            ru.rooyzee.elytrixclient.client.bots.own.BotManager.create(
                    "Сервер " + (ru.rooyzee.elytrixclient.client.bots.own.BotManager.folders.size() + 1),
                    cfg.botAddress);
            struct();
            dirty.run();
        }).describe("Папка = отдельная страница: сервер, аккаунты, авторизация, физика ботов этого сервера"));
        var folders = ru.rooyzee.elytrixclient.client.bots.own.BotManager.folders;
        if (folders.isEmpty()) {
            c.add(new MenuRow.Info("Пока пусто", () -> "создай папку и открой её", 0));
        }
        for (int i = 0; i < folders.size(); i++) {
            final ru.rooyzee.elytrixclient.client.bots.own.BotFolder f = folders.get(i);
            final int idx = i;
            c.add(new MenuRow.Button(
                    () -> f.name + "  ·  " + ru.rooyzee.elytrixclient.client.bots.own.BotManager.status(f),
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.running(f)
                            ? MenuRow.Button.Kind.PRIMARY : MenuRow.Button.Kind.SECONDARY, () -> {
                ru.rooyzee.elytrixclient.client.bots.own.BotManager.selected = idx;
                ru.rooyzee.elytrixclient.client.ui.ElytrixScreen.openFolderPage = idx;
                struct();
                dirty.run();
            }).describe(f.address.isEmpty() ? "адрес не задан" : f.address));
        }
        return c;
    }

    /** Полноценная страница открытой папки: всё про этот сервер, ничего лишнего. */
    public java.util.List<MenuCard> folderPage(ru.rooyzee.elytrixclient.client.bots.own.BotFolder f) {
        var list = new java.util.ArrayList<MenuCard>();
        var BM = ru.rooyzee.elytrixclient.client.bots.own.BotManager.class;

        list.add(new MenuCard(f.name).badge(() -> ru.rooyzee.elytrixclient.client.bots.own.BotManager.status(f), 0)
                .add(new MenuRow.Button(() -> "← К списку папок", MenuRow.Button.Kind.SECONDARY, () -> {
                    ru.rooyzee.elytrixclient.client.ui.ElytrixScreen.openFolderPage = -1;
                    struct();
                    dirty.run();
                }))
                .add(new MenuRow.Button(
                        () -> ru.rooyzee.elytrixclient.client.bots.own.BotManager.running(f)
                                ? "Отключить ботов" : "Подключить " + f.connectCount + " ботов",
                        ru.rooyzee.elytrixclient.client.bots.own.BotManager.running(f)
                                ? MenuRow.Button.Kind.SECONDARY : MenuRow.Button.Kind.PRIMARY, () -> {
                    if (ru.rooyzee.elytrixclient.client.bots.own.BotManager.running(f)) {
                        ru.rooyzee.elytrixclient.client.bots.own.BotManager.stop(f);
                    } else {
                        ru.rooyzee.elytrixclient.client.bots.own.BotManager.start(f);
                    }
                    dirty.run();
                }))
                .add(new MenuRow.Button(() -> "Удалить папку", MenuRow.Button.Kind.DANGER, () -> {
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.remove(f);
                    ru.rooyzee.elytrixclient.client.ui.ElytrixScreen.openFolderPage = -1;
                    struct();
                    dirty.run();
                }).describe("Отключит ботов и уберёт папку со всеми её аккаунтами")));

        list.add(new MenuCard("Сервер")
                .add(new MenuRow.Text("Имя папки", 140, () -> f.name, v -> {
                    f.name = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }))
                .add(new MenuRow.Text("Адрес (ip или домен[:порт])", 200, () -> f.address, v -> {
                    f.address = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).describe("SRV-запись поддержится автоматически (mclan и т.п.)"))
                .add(toggle("Через прокси", () -> f.useProxy, v -> {
                    f.useProxy = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).describe("Каждый бот берёт следующую прокси из общего пула; мёртвые отсеиваются"))
                .add(new MenuRow.Slider("Подключать ботов", 1, 50, 1, "", () -> f.connectCount, v -> {
                    f.connectCount = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }))
                .add(new MenuRow.Slider("Задержка входа", 50, 5000, 50, " мс", () -> f.delayMs, v -> {
                    f.delayMs = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).describe("Пауза между стартом ботов этой папки")));

        MenuCard acc = new MenuCard("Аккаунты")
                .badge(() -> f.accounts.size() + " ников", 0)
                .add(new MenuRow.Info("Готовы", () -> f.freeAccounts().size() + " · в бане: " + f.banned.size(), 0))
                .add(new MenuRow.Text("Новый ник", 110, () -> draftNick, v -> draftNick = v))
                .add(new MenuRow.Button(() -> "Добавить ник", MenuRow.Button.Kind.SECONDARY, () -> {
                    f.addAccount(draftNick);
                    draftNick = "";
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                    struct();
                    dirty.run();
                }))
                .add(new MenuRow.Button(() -> "Сгенерировать 5 ников", MenuRow.Button.Kind.SECONDARY, () -> {
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.generateAccounts(f, 5);
                    struct();
                    dirty.run();
                }))
                .add(new MenuRow.Button(() -> "Разбанить все ники", MenuRow.Button.Kind.SECONDARY, () -> {
                    f.banned.clear();
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                    struct();
                    dirty.run();
                }).describe("Снимет метки бана — ники снова пойдут в ротацию"));
        for (int ai = 0; ai < f.accounts.size(); ai++) {
            final String nick = f.accounts.get(ai);
            acc.add(new MenuRow.Button(
                    () -> nick + "  ·  " + botState(f, nick),
                    MenuRow.Button.Kind.SECONDARY, () -> {
                ru.rooyzee.elytrixclient.client.bots.own.OwnBot bot = findBot(f, nick);
                if (bot != null) {
                    ru.rooyzee.elytrixclient.client.ui.ElytrixScreen.openBotView = bot;
                    struct();
                    dirty.run();
                }
            }).describe("Клик — открыть экран бота: чат, карта, писать за него. Если бот не в игре — сначала подключи папку"));
            acc.add(new MenuRow.Button(() -> "✕ удалить " + nick, MenuRow.Button.Kind.DANGER, () -> {
                f.accounts.remove(nick);
                f.banned.remove(nick);
                ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                struct();
                dirty.run();
            }).describe("Убрать ник из пула папки"));
        }
        list.add(acc);

        list.add(new MenuCard("Авторизация")
                .add(toggle("Авторегистрация", () -> f.autoReg, v -> {
                    f.autoReg = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).describe("После входа шлёт /register пароль пароль"))
                .add(toggle("Автовход", () -> f.autoLogin, v -> {
                    f.autoLogin = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).describe("Шлёт /login пароль"))
                .add(toggle("Авторешение капч", () -> f.captcha, v -> {
                    f.captcha = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).describe("Текстовая капча из чата решается сама; иначе вбей вручную в фазе 3"))
                .add(new MenuRow.Text("Пароль", 120, () -> f.password, v -> {
                    f.password = v;
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).when(() -> f.autoReg || f.autoLogin)));

        list.add(new MenuCard("Физика ботов этого сервера")
                .add(toggle("Анти-АФК круги", () -> f.antiAfk, v -> {
                    f.antiAfk = v;
                    var e = ru.rooyzee.elytrixclient.client.bots.own.BotManager.engine(f);
                    if (e != null) {
                        e.pAntiAfk = v;
                    }
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }).describe("Медленно наматывает круги вокруг спавна (~1 блок за 30 с)"))
                .add(toggle("Повороты головы", () -> f.rotation, v -> {
                    f.rotation = v;
                    var e = ru.rooyzee.elytrixclient.client.bots.own.BotManager.engine(f);
                    if (e != null) {
                        e.pRotation = v;
                    }
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }))
                .add(toggle("Взмахи рукой", () -> f.swing, v -> {
                    f.swing = v;
                    var e = ru.rooyzee.elytrixclient.client.bots.own.BotManager.engine(f);
                    if (e != null) {
                        e.pSwing = v;
                    }
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                }))
                .add(toggle("Перепрыгивать ступеньки", () -> f.autoJump, v -> {
                    f.autoJump = v;
                    var e = ru.rooyzee.elytrixclient.client.bots.own.BotManager.engine(f);
                    if (e != null) {
                        e.pAutoJump = v;
                    }
                    ru.rooyzee.elytrixclient.client.bots.own.BotManager.save();
                })));
        return list;
    }

    private static String botState(ru.rooyzee.elytrixclient.client.bots.own.BotFolder f, String nick) {
        ru.rooyzee.elytrixclient.client.bots.own.OwnBot b = findBot(f, nick);
        if (b == null) {
            return f.banned.contains(nick) ? "забанен" : "не в игре";
        }
        return b.status();
    }

    private static ru.rooyzee.elytrixclient.client.bots.own.OwnBot findBot(
            ru.rooyzee.elytrixclient.client.bots.own.BotFolder f, String nick) {
        ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine e =
                ru.rooyzee.elytrixclient.client.bots.own.BotManager.engine(f);
        if (e == null) {
            return null;
        }
        for (ru.rooyzee.elytrixclient.client.bots.own.OwnBot b : e.botList()) {
            if (b.name.equals(nick)) {
                return b;
            }
        }
        return null;
    }

    /** Экран конкретного бота: карта окрестностей, живой чат, писать за бота, /register, /login. */
    public java.util.List<MenuCard> botPage(ru.rooyzee.elytrixclient.client.bots.own.OwnBot bot) {
        var list = new java.util.ArrayList<MenuCard>();
        String pwd = bot.folder != null ? bot.folder.password : "elytrix123";
        final String fp = pwd;
        list.add(new MenuCard(bot.name).badge(bot::status, 0)
                .add(new MenuRow.Button(() -> "← Назад", MenuRow.Button.Kind.SECONDARY, () -> {
                    ru.rooyzee.elytrixclient.client.ui.ElytrixScreen.openBotView = null;
                    struct();
                    dirty.run();
                }))
                .add(new MenuRow.Button(() -> "Отправить /register", MenuRow.Button.Kind.PRIMARY,
                        () -> bot.say("/register " + fp + " " + fp))
                        .describe("Регистрация ника паролем папки"))
                .add(new MenuRow.Button(() -> "Отправить /login", MenuRow.Button.Kind.PRIMARY,
                        () -> bot.say("/login " + fp))
                        .describe("Вход паролем папки"))
                .add(new MenuRow.Button(() -> "Отключить бота", MenuRow.Button.Kind.DANGER, () -> {
                    bot.close();
                    ru.rooyzee.elytrixclient.client.ui.ElytrixScreen.openBotView = null;
                    struct();
                    dirty.run();
                })));
        list.add(new MenuCard("Экран бота")
                .add(new MenuRow.BotMap(bot))
                .add(new MenuRow.Info("Прокси", bot::proxyLabel, 0)));
        MenuCard chat = new MenuCard("Чат бота").badge(() -> String.valueOf(bot.chatSnapshot().size()), 0);
        for (int k = 0; k < 12; k++) {
            final int idx = k;
            chat.add(new MenuRow.Line(() -> {
                java.util.List<String> snap = bot.chatSnapshot();
                int from = Math.max(0, snap.size() - 12);
                int at = from + idx;
                return at < snap.size() ? snap.get(at) : "";
            }));
        }
        chat.add(new MenuRow.Text("Сообщение / команда", 120, () -> draftChat, v -> draftChat = v));
        chat.add(new MenuRow.Button(() -> "Отправить в чат", MenuRow.Button.Kind.PRIMARY, () -> {
            bot.say(draftChat);
            draftChat = "";
        }).describe("Пишет в чат сервера от имени этого бота — капча, /msg, что угодно"));
        list.add(chat);
        return list;
    }

    private MenuCard hudCard() {
        var w = platform.client.ui.widget.WatermarkWidget.INSTANCE;
        MenuCard c = new MenuCard("Инфо-панель")
                .badge(() -> w != null ? "вкл" : "выкл", 0);
        if (w == null) {
            c.add(new MenuRow.Info("Панель", () -> "выключена в модулях", 0));
            return c;
        }
        c.add(new MenuRow.Header("Строки панели"))
                .add(toggle("FPS", () -> w.i.c(), v -> w.i.a(v)))
                .add(toggle("Пинг игрока", () -> w.j.c(), v -> w.j.a(v)))
                .add(toggle("Время", () -> w.k.c(), v -> w.k.a(v)))
                .add(toggle("Логин", () -> w.l.c(), v -> w.l.a(v)))
                .add(toggle("Координаты", () -> w.m.c(), v -> w.m.a(v)))
                .add(toggle("TPS", () -> w.n.c(), v -> w.n.a(v)))
                .add(toggle("Нагрузка MC", () -> w.o.c(), v -> w.o.a(v)))
                .add(toggle("Боты", () -> w.p.c(), v -> w.p.a(v)))
                .add(new MenuRow.Header("Вид"))
                .add(toggle("Разделять элементы", () -> w.h.c(), v -> w.h.a(v)))
                .add(toggle("Боковое отображение", () -> w.g.c(), v -> w.g.a(v)));
        return c;
    }


    public List<MenuCard> settings() {
        List<MenuCard> list = new ArrayList<>();
        list.add(new MenuCard("Внешний вид")
                .add(new MenuRow.Swatches("Акцент", UiTheme.ACCENTS, () -> cfg.accentIndex, i -> {
                    cfg.accentIndex = i;
                    MenuKit.accent = UiTheme.accent(i);
                    cfg.accent = String.format("#%06X", MenuKit.accent & 0xFFFFFF);
                    dirty.run();
                }))
                .add(new MenuRow.Mode("Размер панели", SCALE_NAMES, () -> cfg.uiScaleIndex, i -> {
                    cfg.uiScaleIndex = i;
                    dirty.run();
                }))
                .add(new MenuRow.Slider("Непрозрачность фона", 0, 100, 1, "%", () -> cfg.panelOpacity, v -> {
                    cfg.panelOpacity = v;
                    dirty.run();
                }))
                .add(new MenuRow.Mode("Качество эффектов", ElytrixQuality.NAMES, () -> cfg.effectsQuality, i -> {
                    cfg.effectsQuality = i;
                    dirty.run();
                })));

        list.add(new MenuCard("Звуки")
                .add(toggle("Звуки меню", () -> cfg.menuSounds, v -> cfg.menuSounds = v))
                .add(new MenuRow.Mode("Набор звуков", UiSound.SET_NAMES, () -> cfg.soundSet, i -> {
                    cfg.soundSet = i;
                    dirty.run();
                }).when(() -> cfg.menuSounds))
                .add(new MenuRow.Slider("Громкость", 0, 100, 5, "%", () -> cfg.soundVolume, v -> {
                    cfg.soundVolume = v;
                    dirty.run();
                }).when(() -> cfg.menuSounds))
                .add(toggle("Звук при наведении", () -> cfg.hoverSounds, v -> cfg.hoverSounds = v)
                        .when(() -> cfg.menuSounds)));
        return list;
    }

    /** Вкладка «Визуалы»: компактный плеер MusicIsland и папка визуальных модулей delta-26.2. */
    /** Кураторский список визуалов: только то, что выбрал пользователь. */
    public static final java.util.Set<String> VISUAL_DELTA = java.util.Set.of(
            "ShaderSky", "Hands Shader", "Aspect Ratio", "Interface", "Item Physic", "Jump Circles", "See Invisibles");
    public static final java.util.Set<String> VISUAL_XROSE = java.util.Set.of(
            "BlockOutline", "Removals", "Chams", "AtmoDawnFog");
    public static final java.util.Set<String> MISC_DELTA = java.util.Set.of("RP Spoofs", "Streamer Mode");

    public List<MenuCard> visuals() {
        List<MenuCard> list = new ArrayList<>();
        list.add(new MenuCard("Оптимизация ботов")
                .badge(() -> cfg.botCull ? "вкл" : "выкл", 0)
                .add(toggle("Не рендерить своих ботов", () -> cfg.botCull, v -> cfg.botCull = v)
                        .describe("Свои боты (префикс ников) не отрисовываются у тебя: FPS не проседает даже с сотнями ботов"))
                .add(new MenuRow.Info("Эффект", () -> cfg.botCull ? "боты не грузят рендер" : "рендер как обычно", 0)));
        list.add(new MenuCard("MusicIsland")
                .badge(() -> cfg.musicIsland ? "вкл" : "выкл", 0)
                .add(toggle("Показывать плеер", () -> cfg.musicIsland, v -> cfg.musicIsland = v))
                .add(new MenuRow.Button("Настройки плеера", MenuRow.Button.Kind.SECONDARY,
                        () -> ru.rooyzee.elytrixclient.client.ui.MusicIsland.settingsOpen = true)
                        .when(() -> cfg.musicIsland)));

        // FullBright — скрещенный (xrose-гамма + delta-ночное зрение).
        for (ru.rooyzee.elytrixclient.client.features.render.VisualModule vm
                : ru.rooyzee.elytrixclient.client.features.render.Visuals.all()) {
            if (vm instanceof ru.rooyzee.elytrixclient.client.features.render.modules.FullBright fb) {
                list.add(new MenuCard("FullBright")
                        .badge(() -> fb.enabled() ? "вкл" : "выкл", 0)
                        .add(toggle("Включить", fb::enabled, fb::setEnabled))
                        .add(new MenuRow.Mode("Режим", new String[]{"Гамма (xrose)", "Ночное зрение (delta)"},
                                () -> fb.mode, v -> fb.mode = v)));
            }
        }

        // delta-26.2 — только визуалы из списка пользователя.
        platform.client.Delta delta = platform.client.Delta.h();
        if (delta != null && delta.d() != null && delta.d().t() != null) {
            for (platform.api.module.Module mod : delta.d().t().d()) {
                if (mod == null || mod.l() != platform.api.module.Category.Render
                        || !VISUAL_DELTA.contains(mod.j()) || "Interface".equals(mod.j())) {
                    // Interface живёт в Misc («Инфо-панель»), карточку тут не дублируем
                    continue;
                }
                list.add(new MenuCard(mod.j())
                        .badge(() -> mod.m() ? "вкл" : "выкл", 0)
                        .add(new MenuRow.Toggle("Включить", mod::m, mod::a).describe(mod.k()))
                        .add(new MenuRow.Button("Настройки", MenuRow.Button.Kind.SECONDARY,
                                () -> ru.rooyzee.elytrixclient.client.ui.menu.ModuleModal.open(mod))
                                .describe(mod.k())));
            }
        }

        // xrose — только визуалы из списка пользователя; настройки в родном меню.
        try {
            for (org.xrose.feature.Feature fx
                    : org.xrose.feature.FeatureManager.INSTANCE.getFeatures(org.xrose.feature.FeatureCategory.VISUAL)) {
                if (fx == null || !VISUAL_XROSE.contains(fx.getName())) {
                    continue;
                }
                list.add(new MenuCard(fx.getName())
                        .badge(() -> fx.isEnabled() ? "вкл" : "выкл", 0)
                        .add(new MenuRow.Toggle("Включить", fx::isEnabled, fx::setEnabled).describe(RuText.ru(fx.getDescription())))
                        .add(new MenuRow.Button("Настройки", MenuRow.Button.Kind.SECONDARY,
                                () -> ru.rooyzee.elytrixclient.client.ui.menu.XroseModal.open(fx))
                                .describe(fx.getDescription())));
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    /** Misc: RP Spoofs / Streamer Mode (delta), NameProtect (xrose), AutoRegister (наш). */
    public List<MenuCard> misc() {
        List<MenuCard> list = new ArrayList<>();
        list.add(hudCard());
        platform.client.Delta delta = platform.client.Delta.h();
        if (delta != null && delta.d() != null && delta.d().t() != null) {
            for (platform.api.module.Module mod : delta.d().t().d()) {
                if (mod == null || mod.l() != platform.api.module.Category.Misc
                        || !MISC_DELTA.contains(mod.j())) {
                    continue;
                }
                list.add(new MenuCard(mod.j())
                        .badge(() -> mod.m() ? "вкл" : "выкл", 0)
                        .add(new MenuRow.Toggle("Включить", mod::m, mod::a).describe(mod.k()))
                        .add(new MenuRow.Button("Настройки", MenuRow.Button.Kind.SECONDARY,
                                () -> ru.rooyzee.elytrixclient.client.ui.menu.ModuleModal.open(mod))
                                .describe(mod.k())));
            }
        }
        try {
            for (org.xrose.feature.Feature fx
                    : org.xrose.feature.FeatureManager.INSTANCE.getFeatures(org.xrose.feature.FeatureCategory.MISC)) {
                if (fx == null || !"NameProtect".equals(fx.getName())) {
                    continue;
                }
                list.add(new MenuCard("NameProtect")
                        .badge(() -> fx.isEnabled() ? "вкл" : "выкл", 0)
                        .add(new MenuRow.Toggle("Включить", fx::isEnabled, fx::setEnabled).describe(RuText.ru(fx.getDescription())))
                        .add(new MenuRow.Button("Настройки", MenuRow.Button.Kind.SECONDARY,
                                () -> ru.rooyzee.elytrixclient.client.ui.menu.XroseModal.open(fx))
                                .describe("Свой ник или блюр ников (режим Blur)")));
            }
        } catch (Throwable ignored) {
        }
        for (ru.rooyzee.elytrixclient.client.features.render.VisualModule vm
                : ru.rooyzee.elytrixclient.client.features.render.Visuals.all()) {
            if (vm instanceof ru.rooyzee.elytrixclient.client.features.misc.AutoRegister ar) {
                list.add(new MenuCard("AutoRegister")
                        .badge(() -> ar.enabled() ? "вкл" : "выкл", 0)
                        .add(toggle("Включить", ar::enabled, ar::setEnabled))
                        .add(new MenuRow.Text("Пароль", 32, () -> ar.password, v -> ar.password = v)));
            }
        }
        return list;
    }

    private MenuRow toggle(String label, java.util.function.BooleanSupplier get,
                           java.util.function.Consumer<Boolean> set) {
        return new MenuRow.Toggle(label, get, v -> {
            set.accept(v);
            dirty.run();
        });
    }

    private static void openConfigFolder() {
        try {
            net.minecraft.util.Util.getPlatform().openFile(ElytrixConfig.file().getParent().toFile());
        } catch (Throwable t) {
            ElytrixclientClient.LOG.add("[Elytrix] Не удалось открыть папку конфига: " + t);
        }
    }
}
