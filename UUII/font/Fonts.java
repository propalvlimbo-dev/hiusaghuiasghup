package wtf.expensive.client.util.font;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import wtf.expensive.client.Expensive;

public final class Fonts {
    public static final Style SORA_18 = of("sora18");
    public static final Style SORA_15 = of("sora15");

    public static final Style SEMIBOLD_20 = of("mssemibold20");
    public static final Style SEMIBOLD_18 = of("mssemibold18");
    public static final Style SEMIBOLD_16 = of("mssemibold16");
    public static final Style SEMIBOLD_15 = of("mssemibold15");
    public static final Style SEMIBOLD_14 = of("mssemibold14");
    public static final Style SEMIBOLD_13 = of("mssemibold13");
    public static final Style SEMIBOLD_11 = of("mssemibold11");

    public static final Style LIGHT_10 = of("mslight10");
    public static final Style LIGHT_13 = of("mslight13");
    public static final Style LIGHT_12 = of("mslight12");
    public static final Style LIGHT_11 = of("mslight11");

    public static final Style REGULAR_23 = of("msregular23");
    public static final Style REGULAR_18 = of("msregular18");
    public static final Style REGULAR_14 = of("msregular14");

    public static final Style MEDIUM_16 = of("msmedium16");
    public static final Style MEDIUM_15 = of("msmedium15");
    public static final Style MEDIUM_14 = of("msmedium14");
    public static final Style MEDIUM_12 = of("msmedium12");
    public static final Style MEDIUM_20 = of("msmedium20");

    public static final Style BOLD_13 = of("msbold13");

    public static final Style GILROY_14 = of("gilroy14");
    public static final Style GILROY_13 = of("gilroy13");
    public static final Style GILROY_12 = of("gilroy12");
    public static final Style GILROY_BOLD_15 = of("gilroybold15");
    public static final Style GILROY_BOLD_14 = of("gilroybold14");

    public static final Style ICONS_15 = of("icons15");
    public static final Style ICONS_20 = of("icons20");
    public static final Style ICONS_12 = of("icons12");
    public static final Style ICONS_130 = of("icons130");
    public static final Style CONFIG_ICONS_16 = of("configicons16");
    public static final Style CONFIG_ICONS_22 = of("configicons22");

    private Fonts() {
    }

    private static Style of(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Expensive.MOD_ID, name);
        return Style.EMPTY.withFont(new FontDescription.Resource(id));
    }
}
