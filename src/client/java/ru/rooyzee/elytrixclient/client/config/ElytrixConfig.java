package ru.rooyzee.elytrixclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.rooyzee.elytrixclient.Elytrixclient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфиг мода: %gameDir%/config/elytrixclient.json
 * Всё, что вводится в GUI, живёт здесь и сохраняется на диск.
 */
public class ElytrixConfig {
    // ---------- цель ----------
    public String host = "127.0.0.1";
    public int port = 25565;

    // ---------- BotMark ----------
    public String botmarkPath = "C:/tools/botmark.exe";
    public int botmarkCount = 50;
    public int botmarkDelay = 200;
    public int botmarkTimeout = 5000;
    public boolean bmSpam = true;
    public String bmSpamMessage = "Please do not spam!";
    public boolean bmRotation = true;
    public boolean bmSwing = true;
    public boolean bmMovement = true;
    public String ownBotPrefix = "ElytrixBot_";
    public boolean bmJumping = true;
    public boolean bmPhysics = true;
    public String botAddress = "127.0.0.1:25565";
    public boolean botAutoReg = false;
    public boolean botAutoLogin = false;
    public String botPassword = "elytrix123";
    public String rustBotsPath = "elytrix-bots.exe";

    // ---------- SoulFire ----------
    /** cli — запускать SoulFireCLI.jar как процесс и писать команды ему в stdin; mcp — дёргать HTTP API (MCP). */
    public String soulfireMode = "cli";
    public String soulfireJar = "C:/soulfire/SoulFireCLI.jar";
    public String soulfireJavaArgs = "-Xmx2G";
    public String soulfireApiUrl = "http://127.0.0.1:38765/mcp";
    public String soulfireToken = "";
    public String soulfireInstanceId = "";
    /** Последняя команда, введённая в панели (кнопка «Отправить команду»). */
    public String soulfireCommand = "";

    // ---------- UI ----------
    /** Тема интерфейса: 0 — бело-розовая, 1 — тёмная (UiTheme.PRESET_NAMES). */
    public int themeIndex = 0;
    /** Масштаб панели: 0 — как масштаб интерфейса игры, иначе 90/100/115/130 %. */
    public int uiScaleIndex = 0;
    /** Качество эффектов: 0 — авто (по размеру кадра и FPS), 1 — низкое, 2 — высокое. */
    public int effectsQuality = 0;
    /** «Хакерский» анимированный фон в главном меню вместо панорамы. */
    public boolean hackerBackground = true;
    /** Свой экран загрузки вместо ванильного красного лоадера. */
    public boolean customLoading = true;
    /** Цвет акцента строкой (для совместимости и внешних правок файла). */
    public String accent = "#FF4FC3";
    /** Индекс акцента в палитре UiTheme.ACCENTS (0 — розовый). */
    public int accentIndex = 0;
    /** Непрозрачность фона панели, % (0…100). Карточки и текст не затрагивает. */
    public int panelOpacity = 88;
    /** Звуки меню. */
    public boolean menuSounds = true;
    /** Набор звуков: 0 — «Мягкий», 1 — «Стеклянный», 2 — «Ванильный» (UiSound.SET_NAMES). */
    public int soundSet = 0;
    /** Громкость звуков меню, % (0…100). */
    public int soundVolume = 70;
    /** Фон главного меню (MenuBackgrounds.NAMES), 0 — хакерский. */
    public int menuBackground = 0;
    /** Семейство шрифтов интерфейса: 0 — Google Sans, 1 — Google Sans Flex, 2 — SF Pro (Fonts.FAMILY_NAMES). */
    public int fontFamily = 0;

    // ---------- Сеть ----------
    /** Прокси: "off" / "socks5" / "http". */
    public String proxyMode = "off";
    public String proxyHost = "";
    public int proxyPort = 1080;
    /** Желаемая версия протокола (информационно; реальную смену версий даёт ViaFabric). */
    public String protocolVersion = "";

    // ---------- Визуалы / HUD ----------
    /** Плавающий музыкальный плеер (MusicIsland). */
    public boolean musicIsland = true;
    /** Субтитры (текущая строка) в плеере. */
    public boolean islandLyrics = true;
    /** Источник (Spotify/YouTube) под исполнителем. */
    public boolean islandSource = true;
    /** Обложка трека в плеере. */
    public boolean islandCover = true;
    /** Масштаб плеера, % (80…130). */
    public int islandScale = 100;
    /** Вотермарка-инфопанель в стиле delta-26.2 (WatermarkWidget). */
    public boolean hudWatermark = true;
    /** Тихий звук при наведении на элементы. */
    public boolean hoverSounds = true;
    /** Плавные анимации интерфейса. */
    public boolean animations = true;
    /** Размывать фон за панелью. */
    public boolean blurBackground = true;
    /** Закрывать панель кликом вне её. */
    public boolean closeOnOutsideClick = false;
    /** Открывать панель правым Ctrl. */
    public boolean panelKey = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("elytrixclient.json");
    }

    public static ElytrixConfig load() {
        Path path = file();
        if (Files.exists(path)) {
            try {
                ElytrixConfig cfg = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), ElytrixConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                Elytrixclient.LOG.warn("Не удалось прочитать конфиг Elytrixclient: {}", e.getMessage());
            }
        }
        return new ElytrixConfig();
    }

    public void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Elytrixclient.LOG.warn("Не удалось сохранить конфиг Elytrixclient: {}", e.getMessage());
        }
    }

    /** "#8B5CF6" -> 0xFF8B5CF6 (ARGB), при ошибке — фиолетовый по умолчанию. */
    public int accentColor() {
        try {
            return 0xFF000000 | Integer.parseInt(accent.replace("#", ""), 16);
        } catch (Exception ignored) {
            return 0xFF8B5CF6;
        }
    }

    public String target() {
        return host + ":" + port;
    }

    public static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
