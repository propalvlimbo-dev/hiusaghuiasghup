package ru.rooyzee.elytrixclient.client.render.font;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр MTSDF-шрифтов клиента — порт {@code platform.client.utils.render.Fonts}
 * из delta-26.2 (там это {@code FontBuilder().setFont("sf_regular").build()}).
 *
 * <p>Атласы лежат в {@code assets/elytrixclient/textures/font/} и взяты из набора
 * {@code xrose_1/fonts}: три семейства (Google Sans, Google Sans Flex, SF Pro)
 * по четыре начертания (Regular, Medium, Semibold, Bold). Имя файла строится как
 * {@code <семейство>_<начертание>.json} + одноимённый {@code .png}.
 *
 * <p>Загрузка ленивая: {@link MsdfFont#load} читает ResourceManager, которого на
 * момент инициализации класса ещё нет. Поэтому в отличие от delta-26.2 здесь не
 * {@code static final Font}, а кэш с отложенным построением.
 */
public final class Fonts {

    // ── семейства ───────────────────────────────────────────────────────
    public static final int GOOGLE_SANS = 0;
    public static final int GOOGLE_SANS_FLEX = 1;
    public static final int SF_PRO = 2;

    /** Названия для настройки «Шрифт» в панели. */
    public static final String[] FAMILY_NAMES = {"Google Sans", "Google Sans Flex", "SF Pro"};
    private static final String[] FAMILY_PREFIX = {"google_sans", "google_sans_flex", "sf_pro"};

    // ── начертания ──────────────────────────────────────────────────────
    public static final int REGULAR = 0;
    public static final int MEDIUM = 1;
    public static final int SEMIBOLD = 2;
    public static final int BOLD = 3;

    public static final String[] WEIGHT_NAMES = {"Regular", "Medium", "Semibold", "Bold"};
    private static final String[] WEIGHT_SUFFIX = {"regular", "medium", "semibold", "bold"};

    /** Каталог атласов внутри неймспейса мода. */
    public static final String ATLAS_DIR = "textures/font/";

    private static final Map<String, FontRenderer> CACHE = new HashMap<>();
    private static final Map<String, FontRenderer> BROKEN = new HashMap<>();

    private static int family = GOOGLE_SANS;

    private Fonts() {
    }

    /** Текущее семейство (0 — Google Sans, 1 — Google Sans Flex, 2 — SF Pro). */
    public static int family() {
        return family;
    }

    public static void setFamily(int index) {
        family = index < 0 || index >= FAMILY_PREFIX.length ? GOOGLE_SANS : index;
    }

    /** Шрифт конкретного начертания текущего семейства. */
    public static FontRenderer get(int weight) {
        return of(name(weight));
    }

    /** Имя атласа: {@code google_sans_bold}, {@code sf_pro_medium}, … */
    public static String name(int weight) {
        int w = weight < 0 || weight >= WEIGHT_SUFFIX.length ? REGULAR : weight;
        return FAMILY_PREFIX[family] + "_" + WEIGHT_SUFFIX[w];
    }

    public static FontRenderer regular() {
        return get(REGULAR);
    }

    public static FontRenderer medium() {
        return get(MEDIUM);
    }

    public static FontRenderer semibold() {
        return get(SEMIBOLD);
    }

    public static FontRenderer bold() {
        return get(BOLD);
    }

    /**
     * Шрифт по точному имени атласа (без расширения). Кэшируется; при ошибке
     * чтения запоминается в {@link #BROKEN}, чтобы не долбиться в ресурсы
     * каждый кадр и не спамить в лог.
     */
    public static synchronized FontRenderer of(String atlasName) {
        FontRenderer cached = CACHE.get(atlasName);
        if (cached != null) {
            return cached;
        }
        if (BROKEN.containsKey(atlasName)) {
            return BROKEN.get(atlasName);
        }
        FontRenderer font = null;
        try {
            Identifier id = Identifier.fromNamespaceAndPath("elytrixclient", ATLAS_DIR + atlasName + ".json");
            font = new FontRenderer(atlasName, MsdfFont.load(id));
            CACHE.put(atlasName, font);
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF font '" + atlasName + "' load failed: " + e);
        }
        BROKEN.put(atlasName, font);
        return font;
    }

    /** Загрузить атлас текущего семейства в память (вызывать после загрузки ресурсов). */
    public static void preload() {
        for (int w = REGULAR; w <= BOLD; w++) {
            FontRenderer font = get(w);
            if (font != null) {
                try {
                    font.msdfFont().textureSetup();
                } catch (Exception ignored) {
                    // текстура зарегистрируется при первой отрисовке
                }
            }
        }
    }

    /** Загружен ли хотя бы один шрифт (иначе UI остаётся на ванильном рендерере). */
    public static boolean available() {
        return regular() != null;
    }

    /** Сброс кэша — на случай перезагрузки пакетов ресурсов. */
    public static synchronized void invalidate() {
        CACHE.clear();
        BROKEN.clear();
    }
}
