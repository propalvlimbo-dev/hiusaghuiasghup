package platform.client.utils.render;

import platform.client.utils.render.Font;
import platform.api.module.Category;
import lombok.Generated;

public class Fonts {
    public static final Font a = new FontBuilder().setFont("icons").build();
    public static final Font b = new FontBuilder().setFont("sf_regular").build();
    public static final Font c = new FontBuilder().setFont("onest_regular").build();
    public static final Font d = new FontBuilder().setFont("sf_medium").build();
    public static final Font e = new FontBuilder().setFont("gt_regular").build();
    public static final Font hudIcons = new FontBuilder().setFont("nuriki").build();
    public static final Font settingsIcon = new FontBuilder().setFont("setting_module_gear").build();
    public static final Font watermarkIcon = new FontBuilder().setFont("cat_mask").build();
    public static final Font combatIcons = new FontBuilder().setFont("divine").build();
    public static final Font renderIcons = new FontBuilder().setFont("gui_icons").build();
    public static final Font playerIcons = new FontBuilder().setFont("icons2").build();
    public static final Font miscIcons = new FontBuilder().setFont("divine_icons").build();
    public static final Font alphaIcons = new FontBuilder().setFont("alphadlc").build();
    public static final Font statusIcons = new FontBuilder().setFont("icons_nurik").build();

    public static Font categoryIcon(Category category) {
        return switch (category) {
            case Combat, Movement -> combatIcons;
            case Render -> renderIcons;
            case Player -> playerIcons;
            case Misc -> miscIcons;
        };
    }

    public static void preload() {
        try {
            a.msdfFont().textureSetup();
            b.msdfFont().textureSetup();
            c.msdfFont().textureSetup();
            d.msdfFont().textureSetup();
            e.msdfFont().textureSetup();
            hudIcons.msdfFont().textureSetup();
            settingsIcon.msdfFont().textureSetup();
            watermarkIcon.msdfFont().textureSetup();
            combatIcons.msdfFont().textureSetup();
            renderIcons.msdfFont().textureSetup();
            playerIcons.msdfFont().textureSetup();
            miscIcons.msdfFont().textureSetup();
            alphaIcons.msdfFont().textureSetup();
            statusIcons.msdfFont().textureSetup();
        } catch (Exception ignored) {
        }
    }

    @Generated
    private Fonts() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
