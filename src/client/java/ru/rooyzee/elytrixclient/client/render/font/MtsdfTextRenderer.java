package ru.rooyzee.elytrixclient.client.render.font;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * MTSDF текстовый рендерер — lazy-loading как в delta-26.2.
 *
 * <p>Шрифты загружаются при первом обращении, а не в {@code onInitializeClient}:
 * ResourceManager на момент инициализации мода ещё не готов. Атласы и веса —
 * в {@link Fonts} (набор xrose_1: Google Sans / Google Sans Flex / SF Pro ×).
 *
 * <p>Важно про порядок: текст кладётся в кадр сразу
 * ({@link ElytrixRenderUtil#queueTextNow}), потому что порядок элементов в
 * {@code GuiRenderState} и есть порядок отрисовки. Отложенный flush до конца
 * кадра годится только для оверлеев вроде плеера, которые рисуются поверх
 * всего остального.
 */
public final class MtsdfTextRenderer {

    private MtsdfTextRenderer() {}

    // ── доступ к шрифтам ────────────────────────────────────────────────

    /** Шрифт начертания текущего семейства; {@code null}, если атлас не читается. */
    public static FontRenderer font(int weight) {
        return Fonts.get(weight);
    }

    public static FontRenderer regular() {
        return Fonts.get(Fonts.REGULAR);
    }

    public static FontRenderer medium() {
        return Fonts.get(Fonts.MEDIUM);
    }

    public static FontRenderer semibold() {
        return Fonts.get(Fonts.SEMIBOLD);
    }

    public static FontRenderer bold() {
        return Fonts.get(Fonts.BOLD);
    }

    /** Загружен ли шрифт текущего семейства (иначе UI остаётся на ванильном рендерере). */
    public static boolean available() {
        return Fonts.available();
    }

    /** Загрузить атласы семейства заранее (после загрузки ресурсов). */
    public static void preload() {
        Fonts.preload();
    }

    // ── отрисовка ───────────────────────────────────────────────────────

    /** Текст текущим Regular. Вернёт {@code false}, если шрифт не загружен. */
    public static boolean draw(GuiGraphicsExtractor g, String text, float x, float y, float size, int color) {
        return draw(g, Fonts.REGULAR, text, x, y, size, color);
    }

    public static boolean draw(GuiGraphicsExtractor g, int weight, String text, float x, float y, float size, int color) {
        return draw(g, weight, text, x, y, size, color, 0.0f);
    }

    /** То же с межбуквенным интервалом (разрядка в заголовках, «моноширинный» вид). */
    public static boolean draw(GuiGraphicsExtractor g, int weight, String text, float x, float y, float size,
                               int color, float letterSpacing) {
        FontRenderer font = Fonts.get(weight);
        if (font == null || text == null || text.isEmpty() || g == null) {
            return false;
        }
        font.draw(g, text, x, y, size, color, letterSpacing);
        return true;
    }

    public static boolean drawCentered(GuiGraphicsExtractor g, int weight, String text, float cx, float y,
                                       float size, int color) {
        FontRenderer font = Fonts.get(weight);
        if (font == null || text == null || text.isEmpty() || g == null) {
            return false;
        }
        font.drawCentered(g, text, cx, y, size, color);
        return true;
    }

    /** Бегущая строка (для длинного заголовка трека в плеере). */
    public static boolean scroll(GuiGraphicsExtractor g, int weight, Object key, String text, float x, float y,
                                 float size, int color, float maxWidth, boolean hovered, float speed, float delta) {
        FontRenderer font = Fonts.get(weight);
        if (font == null || text == null || text.isEmpty() || g == null) {
            return false;
        }
        font.scroll(g, key, text, x, y, size, color, maxWidth, hovered, speed, delta);
        return true;
    }

    // ── метрики ─────────────────────────────────────────────────────────

    public static float width(String text, float size) {
        return width(Fonts.REGULAR, text, size);
    }

    public static float width(int weight, String text, float size) {
        return width(weight, text, size, 0.0f);
    }

    /** Ширина с разрядкой — той же, с которой текст рисуется. */
    public static float width(int weight, String text, float size, float letterSpacing) {
        FontRenderer font = Fonts.get(weight);
        return font == null || text == null || text.isEmpty()
                ? 0.0f : font.width(text, size, letterSpacing);
    }

    public static float ascender(int weight, float size) {
        FontRenderer font = Fonts.get(weight);
        return font == null ? size * 0.8f : font.ascender(size);
    }

    public static float lineHeight(int weight, float size) {
        FontRenderer font = Fonts.get(weight);
        return font == null ? size * 1.2f : font.lineHeight(size);
    }

    /** Есть ли глиф для символа в текущем Regular (для переносов и обрезки). */
    public static boolean supports(int codePoint) {
        FontRenderer font = Fonts.regular();
        return font != null && font.supports(codePoint);
    }

    /** Обрезать строку с «...» под ширину. */
    public static String trim(int weight, String text, float size, float maxWidth) {
        FontRenderer font = Fonts.get(weight);
        return font == null ? text : font.trim(text, size, maxWidth);
    }

    // ── кадр ────────────────────────────────────────────────────────────

    /** Вызывать в начале каждого кадра (чистит отложенную очередь). */
    public static void beginFrame() {
        ElytrixRenderUtil.beginFrame();
    }

    /** Сбросить отложенную очередь в кадр. */
    public static void flush(GuiGraphicsExtractor g) {
        ElytrixRenderUtil.flush(g);
    }

    /** Каталог атласов — для справки и отладки. */
    public static Identifier atlas(int weight) {
        return Identifier.fromNamespaceAndPath("elytrixclient", Fonts.ATLAS_DIR + Fonts.name(weight) + ".json");
    }
}
