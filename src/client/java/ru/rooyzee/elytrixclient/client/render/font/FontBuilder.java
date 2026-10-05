package ru.rooyzee.elytrixclient.client.render.font;

/**
 * Строитель шрифта — тот же API, что в delta-26.2:
 * <pre>{@code new FontBuilder().setFont("sf_pro_bold").build()}</pre>
 *
 * <p>В delta это отдельный класс, который сам ходит в {@code MsdfFont.load};
 * здесь построение и кэш живут в {@link Fonts}, а билдер оставлен как
 * совместимая обёртка — удобно в коде виджетов и для прямых имён атласов.
 */
public final class FontBuilder {

    private String fontName = "google_sans_regular";

    public FontBuilder setFont(String name) {
        if (name != null && !name.isEmpty()) {
            this.fontName = name;
        }
        return this;
    }

    /** Шрифт по начертанию текущего семейства (см. {@link Fonts#REGULAR}). */
    public FontBuilder setWeight(int weight) {
        this.fontName = Fonts.name(weight);
        return this;
    }

    /** Может вернуть {@code null}, если атлас не читается. */
    public FontRenderer build() {
        return Fonts.of(this.fontName);
    }
}
