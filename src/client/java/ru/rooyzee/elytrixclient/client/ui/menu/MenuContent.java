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
        var ob = ElytrixclientClient.OWN_BOTS;
        var rd = ElytrixclientClient.RUST_BOTS;

        list.add(new MenuCard("Боты")
                .badge(() -> ru.rooyzee.elytrixclient.client.bots.rust.RustBotDaemon.available(cfg) || rd.isRunning()
                        ? "rust: " + rd.statusLine()
                        : "встроенные: " + ob.status(), 0)
                .add(new MenuRow.Header("Подключение"))
                .add(new MenuRow.Text("Путь к rust-ботам", 200, () -> cfg.rustBotsPath, v -> {
                    cfg.rustBotsPath = v;
                    dirty.run();
                }))
                .add(new MenuRow.Text("Адрес сервера", 200, () -> cfg.botAddress, v -> {
                    cfg.botAddress = v;
                    dirty.run();
                }))
                .add(new MenuRow.Slider("Ботов", 1, 500, 1, "", () -> cfg.botmarkCount, v -> {
                    cfg.botmarkCount = v;
                    dirty.run();
                }))
                .add(new MenuRow.Slider("Задержка входа", 0, 2000, 50, " мс", () -> cfg.botmarkDelay, v -> {
                    cfg.botmarkDelay = v;
                    dirty.run();
                }))
                .add(new MenuRow.Slider("Таймаут", 1000, 30000, 500, " мс", () -> cfg.botmarkTimeout, v -> {
                    cfg.botmarkTimeout = v;
                    dirty.run();
                }))
                .add(new MenuRow.Text("Префикс ников", 160, () -> cfg.ownBotPrefix, v -> {
                    cfg.ownBotPrefix = v;
                    dirty.run();
                }))
                .add(new MenuRow.Header("Поведение"))
                .add(new MenuRow.Mode("Режим", new String[]{"стоит", "за мной", "гулять"}, () -> cfg.botMode, v -> {
                    cfg.botMode = v;
                    dirty.run();
                }).describe("стоит — бот на месте; за мной — следует за тобой, как follow в SoulFire; гулять — случайные прогулки"))
                .add(toggle("Повороты головы", () -> cfg.bmRotation, v -> cfg.bmRotation = v)
                        .when(() -> cfg.botMode != 1))
                .add(toggle("Взмахи рукой", () -> cfg.bmSwing, v -> cfg.bmSwing = v))
                .add(new MenuRow.Header("Чат и авторизация"))
                .add(toggle("Авторегистрация", () -> cfg.botAutoReg, v -> cfg.botAutoReg = v))
                .add(toggle("Автовход", () -> cfg.botAutoLogin, v -> cfg.botAutoLogin = v))
                .add(new MenuRow.Text("Пароль", 160, () -> cfg.botPassword, v -> {
                    cfg.botPassword = v;
                    dirty.run();
                }).when(() -> cfg.botAutoReg || cfg.botAutoLogin))
                .add(toggle("Сообщения в чат", () -> cfg.bmSpam, v -> cfg.bmSpam = v))
                .add(new MenuRow.Text("Текст сообщения", 140, () -> cfg.bmSpamMessage, v -> {
                    cfg.bmSpamMessage = v;
                    dirty.run();
                }).when(() -> cfg.bmSpam))
                .add(new MenuRow.Slider("Интервал спама от", 500, 30000, 500, " мс", () -> cfg.botSpamMin, v -> {
                    cfg.botSpamMin = v;
                    dirty.run();
                }).when(() -> cfg.bmSpam))
                .add(new MenuRow.Slider("до", 500, 60000, 500, " мс", () -> cfg.botSpamMax, v -> {
                    cfg.botSpamMax = v;
                    dirty.run();
                }).when(() -> cfg.bmSpam))
                .add(new MenuRow.Button(() -> (rd.isRunning() || ob.isRunning()) ? "Остановить" : "Запустить " + cfg.botmarkCount + " ботов",
                        MenuRow.Button.Kind.PRIMARY, () -> {
                    if (rd.isRunning() || ob.isRunning()) {
                        rd.stop();
                        ob.stop();
                    } else if (ru.rooyzee.elytrixclient.client.bots.rust.RustBotDaemon.available(cfg)) {
                        rd.start(cfg);
                    } else {
                        ru.rooyzee.elytrixclient.client.bots.own.OwnBotSettings st =
                                new ru.rooyzee.elytrixclient.client.bots.own.OwnBotSettings();
                        st.count = cfg.botmarkCount;
                        st.delayMs = cfg.botmarkDelay;
                        st.timeoutMs = cfg.botmarkTimeout;
                        st.prefix = cfg.ownBotPrefix;
                        st.autoReg = cfg.botAutoReg;
                        st.autoLogin = cfg.botAutoLogin;
                        st.mode = cfg.botMode;
                        st.password = cfg.botPassword;
                        st.spam = cfg.bmSpam;
                        st.spamMessage = cfg.bmSpamMessage;
                        st.spamDelayMin = cfg.botSpamMin;
                        st.spamDelayMax = cfg.botSpamMax;
                        st.rotation = cfg.bmRotation;
                        st.swing = cfg.bmSwing;
                        st.movement = cfg.bmMovement;
                        String addr = cfg.botAddress.trim();
                        String[] hp = addr.split(":");
                        ob.start(hp[0], hp.length > 1 ? Integer.parseInt(hp[1]) : 25565, 776, st);
                    }
                })));
        return list;
    }

    public List<MenuCard> proxy() {
        List<MenuCard> list = new ArrayList<>();
        list.add(new MenuCard("Прокси")
                .badge(() -> "0", 0)
                .add(new MenuRow.Info("В списке", () -> "пусто", 0))
                .add(new MenuRow.Info("Где задаются", () -> "SoulFire", 0)));
        list.add(new MenuCard("Подсказка")
                .add(new MenuRow.Info("Импорт", () -> "scripts/proxy-parser.py", 0))
                .add(new MenuRow.Info("Проверка", () -> "в SoulFire", 0)));
        return list;
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
                        || !VISUAL_DELTA.contains(mod.j())) {
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
