package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Набор иконок ElytrixClient.
 *
 * <p>PNG лежат в {@code assets/elytrixclient/textures/gui/icons/<id>.png} (16×16, белые,
 * альфа-маска) и генерируются скриптом {@code scripts/generate-icons.py}. Текстура тонируется
 * цветом при отрисовке, поэтому одна и та же иконка работает и в светлой, и в тёмной теме.
 */
public enum UiIcon {
    HOME("home"), DASHBOARD("home"),
    BOTS("bots"), SHIELD("shield"), SERVER("server"),
    PROXY("proxy"), GLOBE("globe"), NETWORK("wifi"),
    CONSOLE("console"), TERMINAL("console"),
    SETTINGS("settings"), GEAR("settings"),
    CLOSE("close"), CHEVRON("chevron"), CHECK("check"),
    SEARCH("search"), PLUS("plus"), MINUS("minus"), DOTS("dots"),
    BOLT("bolt"), PLAY("play"), STOP("stop"), REFRESH("refresh"),
    TRASH("trash"), COPY("copy"), KEY("key"), CLOCK("clock"),
    USER("user"), LIST("list"), FOLDER("folder"), CHART("chart"), PALETTE("palette"), SOUND("sound"),
    USERS("users"), CLOUD("cloud"), POWER("power"), IMAGE("image"), MUSIC("music");

    private static final int K_MAX = 4;
    /** Нетекстовый (белый) цвет: blit с ним не перекрашивает текстуру. */
    private static final int INK_WHITE = 0xFFFFFFFF;

    private final String id;
    /** Варианты под масштаб интерфейса игры: _x1 (16px), _x2 (32px), _x3, _x4. */
    private final Identifier[] cache = new Identifier[K_MAX + 1];

    UiIcon(String id) {
        this.id = id;
    }

    public Identifier texture() {
        return texture(UiDraw.shapeScale());
    }

    /** Текстура под конкретный масштаб интерфейса — чтобы края не «квадратились». */
    public Identifier texture(int scale) {
        int k = Math.max(1, Math.min(K_MAX, scale));
        if (cache[k] == null) {
            cache[k] = Identifier.fromNamespaceAndPath("elytrixclient",
                    "textures/gui/icons/" + id + "_x" + k + ".png");
        }
        return cache[k];
    }

    /** Натуральный размер PNG (все иконки 16×16). */
    public int size() {
        return 16;
    }

    /** Иконка 16×16 с левым верхним углом в (x, y); {@code argb} — цвет тонировки. */
    public void draw(GuiGraphicsExtractor g, int x, int y, int argb) {
        UiDraw.icon(g, texture(), x, y, 16, argb);
    }

    private final Identifier[] cache10 = new Identifier[K_MAX + 1];

    /** Компактный набор 10×10 единиц (_10_x1 … _10_x4) — для сайдбара и поиска. */
    public Identifier texture10(int scale) {
        int k = Math.max(1, Math.min(K_MAX, scale));
        if (cache10[k] == null) {
            cache10[k] = Identifier.fromNamespaceAndPath("elytrixclient",
                    "textures/gui/icons/" + id + "_10_x" + k + ".png");
        }
        return cache10[k];
    }

    /**
     * Иконка {@code size}×{@code size}. Размеры 10 и 16 рисуются 1:1 из своего набора
     * (пиксель текстуры = пиксель экрана), остальные — растяжением ближайшего.
     */
    public void draw(GuiGraphicsExtractor g, int x, int y, int size, int argb) {
        int k = UiDraw.shapeScale();
        if (size <= 12) {
            UiDraw.icon(g, texture10(k), x, y, size, size, 10 * k, 10 * k, argb);
        } else {
            UiDraw.icon(g, texture(k), x, y, size, size, size() * k, size() * k, argb);
        }
    }

    /** Иконка вписана в квадрат {@code box}×{@code box} с центром в (cx, cy). */
    public void drawCentered(GuiGraphicsExtractor g, int cx, int cy, int box, int argb) {
        int s = Math.max(8, box);
        UiDraw.icon(g, texture(), cx - s / 2, cy - s / 2, s, argb);
    }

    public void drawCentered(GuiGraphicsExtractor g, int cx, int cy, int box) {
        drawCentered(g, cx, cy, box, INK_WHITE);
    }
}
