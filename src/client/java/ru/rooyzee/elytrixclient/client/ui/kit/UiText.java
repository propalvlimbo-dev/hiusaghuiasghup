package ru.rooyzee.elytrixclient.client.ui.kit;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Шрифты интерфейса ElytrixClient.
 *
 * <p>Ванильный шрифт Minecraft — bitmap 5×7, он «пиксельный» по рисунку и не
 * годится для современного вида. Здесь свои TTF-шрифты (Inter для текста,
 * JetBrains Mono для консоли и чисел), которые рендерит сама игра через
 * font-провайдеры {@code type: ttf} — то есть со сглаживанием и под любым
 * масштабом интерфейса.
 *
 * <p>Файлы: {@code assets/elytrixclient/font/*.json} + {@code .ttf}.
 * Стиль со своим шрифтом применяется к {@link Component}, поэтому измерение
 * ширины тоже делается через {@code UiDraw.width(font, component)} — иначе текст
 * «не влезал» бы в отведённые рамки.
 */
public final class UiText {
    /** Основной шрифт интерфейса (Inter). */
    public static final Identifier UI = Identifier.fromNamespaceAndPath("elytrixclient", "ui");
    /** Крупные заголовки (Inter, размер ~20). */
    public static final Identifier TITLE = Identifier.fromNamespaceAndPath("elytrixclient", "ui_title");
    /** Моноширинный: консоль, версии, адреса, числа (JetBrains Mono). */
    public static final Identifier MONO = Identifier.fromNamespaceAndPath("elytrixclient", "mono");

    /** Текущий шрифт по умолчанию для {@link UiDraw#text}. */
    public static Identifier FACE = UI;

    private static final Map<String, Component> CACHE = new HashMap<>();
    /** Идентификатор шрифта → описание для {@link Style#withFont(FontDescription)} (26.2). */
    private static final Map<Identifier, FontDescription> DESCRIPTIONS = new HashMap<>();
    private static final int CACHE_LIMIT = 1024;

    private UiText() {
    }

    /** В 26.2 стиль принимает {@code FontDescription}, а не {@code Identifier}. */
    private static FontDescription description(Identifier face) {
        FontDescription cached = DESCRIPTIONS.get(face);
        if (cached == null) {
            cached = new FontDescription.Resource(face);
            DESCRIPTIONS.put(face, cached);
        }
        return cached;
    }

    public static Component of(String text, Identifier face) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        String key = face + "\u0000" + text;
        Component component = CACHE.get(key);
        if (component == null) {
            component = Component.literal(text).withStyle(Style.EMPTY.withFont(description(face)));
            if (CACHE.size() > CACHE_LIMIT) {
                CACHE.clear();
            }
            CACHE.put(key, component);
        }
        return component;
    }

    public static int width(Font font, String text, Identifier face) {
        return font.width(of(text, face));
    }

    public static void draw(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color,
                            Identifier face, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        g.text(font, of(text, face), x, y, color, shadow);
    }
}
