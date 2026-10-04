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
    USER("user"), LIST("list"), FOLDER("folder"), CHART("chart");

    private static final String DIR = "elytrixclient:textures/gui/icons/";
    /** Нетекстовый (белый) цвет: blit с ним не перекрашивает текстуру. */
    private static final int INK_WHITE = 0xFFFFFFFF;

    private final String id;
    private Identifier cache;

    UiIcon(String id) {
        this.id = id;
    }

    public Identifier texture() {
        if (cache == null) {
            cache = Identifier.fromNamespaceAndPath("elytrixclient", "textures/gui/icons/" + id + ".png");
        }
        return cache;
    }

    /** Натуральный размер PNG (все иконки 16×16). */
    public int size() {
        return 16;
    }

    /** Иконка 16×16 с левым верхним углом в (x, y); {@code argb} — цвет тонировки. */
    public void draw(GuiGraphicsExtractor g, int x, int y, int argb) {
        UiDraw.icon(g, texture(), x, y, 16, argb);
    }

    /** Иконка, растянутая в квадрат {@code size}×{@code size}. */
    public void draw(GuiGraphicsExtractor g, int x, int y, int size, int argb) {
        UiDraw.icon(g, texture(), x, y, size, argb);
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
