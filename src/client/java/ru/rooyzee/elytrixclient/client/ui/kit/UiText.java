package ru.rooyzee.elytrixclient.client.ui.kit;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

import java.util.Map.Entry;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;

/**
 * Шрифты интерфейса ElytrixClient.
 *
 * <p>Раньше текст рисовала сама игра через TTF-провайдеры (Inter + JetBrains Mono
 * из {@code assets/elytrixclient/font/*.json}). Теперь весь интерфейс рисуется
 * MTSDF-атласами из набора {@code xrose_1} (Google Sans / Google Sans Flex /
 * SF Pro) тем же конвейером, что и в delta-26.2 — см. {@link MtsdfTextRenderer}
 * и {@link Fonts}. Семейство переключается настройкой «Шрифт» в панели.
 *
 * <p>Преимущества MTSDF перед TTF-провайдерами игры: один атлас на все размеры
 * (не нужно по варианту шрифта на каждый масштаб интерфейса — {@code _x1…_x6}),
 * настоящий жирный/полужирный вместо одного начертания и ровный край на любом
 * масштабе.
 *
 * <p>Логические шрифты остались те же идентификаторы ({@link #UI}, {@link #TITLE},
 * {@link #MONO} и {@code MenuKit.BODY/SMALL/TITLE/MONO}) — они теперь задают
 * начертание и кегль (таблица {@link #STYLES}), а не файл TTF. Ванильный
 * TTF-путь сохранён как запасной: если атлас не читается, текст рисуется игрой.
 */
public final class UiText {
    /** Основной шрифт интерфейса. */
    public static final Identifier UI = Identifier.fromNamespaceAndPath("elytrixclient", "ui");
    /** Крупные заголовки. */
    public static final Identifier TITLE = Identifier.fromNamespaceAndPath("elytrixclient", "ui_title");
    /** Моноширинный вид: консоль, версии, адреса, числа. */
    public static final Identifier MONO = Identifier.fromNamespaceAndPath("elytrixclient", "mono");

    /** Текущий шрифт по умолчанию для {@link UiDraw#text}. */
    public static Identifier FACE = UI;

    /**
     * Начертание и кегль логического шрифта.
     *
     * @param weight        начертание из {@link Fonts} (REGULAR/MEDIUM/SEMIBOLD/BOLD)
     * @param size          кегль в единицах GUI (до поправки на высоту прописных)
     * @param shift         вертикальный сдвиг базовой линии из старого TTF-json
     * @param letterSpacing разрядка, em-независимая (в единицах GUI на символ)
     */
    public record Face(int weight, float size, float shift, float letterSpacing) {
    }

    /**
     * Таблица соответствия «старый TTF-шрифт → начертание и кегль».
     * Кегли взяты из {@code assets/elytrixclient/font/*.json} (поле {@code size}),
     * сдвиги — из {@code shift[1]} того же файла: базовая линия ванильного TTF
     * лежит на {@code y + 7 + shift} (см. {@code MenuKit.ty}).
     */
    private static final Map<String, Face> STYLES = new HashMap<>();
    private static final Face DEFAULT_FACE = new Face(Fonts.REGULAR, 8.0f, 0.0f, 0.0f);

    static {
        STYLES.put("ui", new Face(Fonts.REGULAR, 10.0f, 1.5f, 0.0f));
        STYLES.put("ui_title", new Face(Fonts.SEMIBOLD, 20.0f, 2.0f, 0.0f));
        STYLES.put("mono", new Face(Fonts.REGULAR, 10.0f, 1.5f, 0.15f));
        STYLES.put("menu", new Face(Fonts.REGULAR, 8.0f, 0.0f, 0.0f));
        STYLES.put("menu_s", new Face(Fonts.REGULAR, 7.0f, 0.0f, 0.0f));
        STYLES.put("menu_t", new Face(Fonts.SEMIBOLD, 10.0f, 0.0f, 0.0f));
        STYLES.put("mono_s", new Face(Fonts.REGULAR, 7.0f, 0.0f, 0.15f));
    }

    /**
     * Поправка кегля: у Inter высота прописных 0.727 em, у атласов xrose_1 —
     * 0.707 em (измерено по глифу «H» в json атласа). Без поправки весь текст
     * стал бы на 3 % мельче и «поплыл» бы относительно старой вёрстки.
     */
    private static final float CAP_SCALE = 0.727f / 0.707f;

    /** Базовая линия ванильного TTF-шрифта игры: {@code y + 7 + shift}. */
    private static final float VANILLA_ASCENT = 7.0f;

    private static final Map<String, Component> CACHE = new HashMap<>();
    /** Идентификатор шрифта → описание для {@link Style#withFont(FontDescription)} (26.2). */
    private static final Map<Identifier, FontDescription> DESCRIPTIONS = new HashMap<>();
    private static final int CACHE_LIMIT = 1024;

    private UiText() {
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Логический шрифт → начертание и кегль
    // ─────────────────────────────────────────────────────────────────────

    /** Начертание и кегль логического шрифта (с учётом суффикса {@code _x<k>}). */
    public static Face face(Identifier logicalFace) {
        if (logicalFace == null) {
            return DEFAULT_FACE;
        }
        String path = logicalFace.getPath();
        Face face = STYLES.get(path);
        if (face == null) {
            int cut = path.lastIndexOf('_');
            if (cut > 0 && cut + 2 == path.length() && path.charAt(cut + 1) == 'x') {
                face = STYLES.get(path.substring(0, cut));
            }
        }
        return face != null ? face : DEFAULT_FACE;
    }

    /** Кегль MTSDF под логический шрифт (уже с поправкой на высоту прописных). */
    public static float size(Identifier logicalFace) {
        return face(logicalFace).size() * CAP_SCALE;
    }

    public static int weight(Identifier logicalFace) {
        return face(logicalFace).weight();
    }

    public static float letterSpacing(Identifier logicalFace) {
        return face(logicalFace).letterSpacing();
    }

    /**
     * Y для MTSDF, при котором базовая линия совпадает с базовой линией
     * ванильного TTF на том же {@code y}. Без этого весь текст уехал бы вверх
     * на высоту выносных элементов.
     */
    public static float alignY(Identifier logicalFace, float y) {
        Face face = face(logicalFace);
        float size = face.size() * CAP_SCALE;
        return y + VANILLA_ASCENT + face.shift() - MtsdfTextRenderer.ascender(face.weight(), size);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Отрисовка и измерение
    // ─────────────────────────────────────────────────────────────────────

    public static void draw(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color,
                            Identifier face, boolean shadow) {
        draw(g, font, text, (float) x, (float) y, color, face, shadow);
    }

    /**
     * Текст логическим шрифтом. Сначала пробуем MTSDF-атласы xrose_1; если они
     * не загрузились — рисуем ванильным TTF-шрифтом (старое поведение).
     */
    public static void draw(GuiGraphicsExtractor g, Font font, String text, float x, float y, int color,
                            Identifier face, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Face style = face(face);
        float size = style.size() * CAP_SCALE;
        if (MtsdfTextRenderer.available()) {
            float baselineY = alignY(face, y);
            if (shadow) {
                MtsdfTextRenderer.draw(g, style.weight(), text, x + 0.5f, baselineY + 0.5f, size,
                        0x60000000, style.letterSpacing());
            }
            MtsdfTextRenderer.draw(g, style.weight(), text, x, baselineY, size, color, style.letterSpacing());
            return;
        }
        g.text(font, of(text, face), Math.round(x), Math.round(y), color, shadow);
    }

    public static int width(Font font, String text, Identifier face) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Face style = face(face);
        if (MtsdfTextRenderer.available()) {
            return Math.round(MtsdfTextRenderer.width(style.weight(), text, style.size() * CAP_SCALE,
                    style.letterSpacing()));
        }
        return font.width(of(text, face));
    }

    /**
     * Текст с дополнительной разрядкой (заголовки «вразрядку»). MTSDF делает это
     * одним прогоном — кернинг сохраняется, а буквы не распадаются на отдельные
     * элементы кадра, как при отрисовке посимвольно.
     */
    public static void drawSpaced(GuiGraphicsExtractor g, Font font, String text, float x, float y, int color,
                                  Identifier face, float spacing) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Face style = face(face);
        float size = style.size() * CAP_SCALE;
        float gap = style.letterSpacing() + spacing;
        if (MtsdfTextRenderer.available()) {
            MtsdfTextRenderer.draw(g, style.weight(), text, x, alignY(face, y), size, color, gap);
            return;
        }
        int cx = Math.round(x);
        int step = Math.round(spacing);
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int cw = font.width(of(ch, face));
            if (!ch.equals(" ")) {
                g.text(font, of(ch, face), cx, Math.round(y), color, false);
            }
            cx += (ch.equals(" ") ? Math.max(3, cw) : cw) + step;
        }
    }

    /** Ширина строки с разрядкой (без завершающего интервала). */
    public static int widthSpaced(Font font, String text, Identifier face, float spacing) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Face style = face(face);
        float gap = style.letterSpacing() + spacing;
        if (MtsdfTextRenderer.available()) {
            float w = MtsdfTextRenderer.width(style.weight(), text, style.size() * CAP_SCALE, gap);
            return Math.max(0, Math.round(w - gap));
        }
        int w = 0;
        int step = Math.round(spacing);
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int cw = font.width(of(ch, face));
            w += (ch.equals(" ") ? Math.max(3, cw) : cw) + step;
        }
        return Math.max(0, w - step);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Запасной путь: ванильный TTF-шрифт игры
    // ─────────────────────────────────────────────────────────────────────

    /** В 26.2 стиль принимает {@code FontDescription}, а не {@code Identifier}. */
    private static FontDescription description(Identifier face) {
        FontDescription cached = DESCRIPTIONS.get(face);
        if (cached == null) {
            cached = new FontDescription.Resource(face);
            DESCRIPTIONS.put(face, cached);
        }
        return cached;
    }

    private static final Map<String, Identifier> RESOLVED = new HashMap<>();
    private static final int SCALE_MAX = 6;

    /**
     * Вариант TTF-шрифта под текущий масштаб интерфейса: {@code <face>_x<k>}, где
     * у провайдера {@code oversample = k}. Нужно только запасному пути — MTSDF
     * от масштаба не зависит.
     */
    public static Identifier resolve(Identifier face) {
        if (!"elytrixclient".equals(face.getNamespace())) {
            return face;
        }
        int k = 1;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            k = Math.max(1, Math.min(SCALE_MAX, UiDraw.scaleOverride > 0 ? UiDraw.scaleOverride
                    : mc.getWindow().getGuiScale()));
        }
        String key = face.getPath() + "_x" + k;
        Identifier id = RESOLVED.get(key);
        if (id == null) {
            id = Identifier.fromNamespaceAndPath(face.getNamespace(), key);
            RESOLVED.put(key, id);
        }
        return id;
    }

    public static Component of(String text, Identifier logicalFace) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        Identifier face = resolve(logicalFace);
        String key = face + "\u0000" + text;
        Component component = CACHE.get(key);
        if (component == null) {
            component = Component.literal(text).withStyle(net.minecraft.network.chat.Style.EMPTY
                    .withFont(description(face)));
            if (CACHE.size() > CACHE_LIMIT) {
                CACHE.clear();
            }
            CACHE.put(key, component);
        }
        return component;
    }

    /** Для справки/отладки: какие стили известны. */
    public static Iterable<Entry<String, Face>> styles() {
        return STYLES.entrySet();
    }
}
