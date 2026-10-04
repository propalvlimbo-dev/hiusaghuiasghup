package wtf.expensive.client.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class MainMenuOverlay {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/backmenu.png");
    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/expensive.png");

    private static final float BUTTON_WIDTH = 176;
    private static final float BUTTON_HEIGHT = 34;
    private static final float BUTTON_STEP = 39;

    private static final int IDLE_OUTLINE = ColorUtil.rgba(53, 56, 81, 255);
    private static final int HOVER_OUTLINE = ColorUtil.rgba(129, 147, 196, 255);
    private static final int IDLE_TEXT = ColorUtil.rgba(132, 135, 147, 255);

    private final List<Button> buttons = new ArrayList<>();

    public MainMenuOverlay() {
        buttons.add(new Button("singleplayer"));
        buttons.add(new Button("multiplayer"));
        buttons.add(new Button("alt manager"));
        buttons.add(new Button("options"));
        buttons.add(new Button("exit"));
    }

    public void render(GuiGraphicsExtractor graphics, Screen screen, int mouseX, int mouseY) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        RenderUtil.texture(graphics, BACKGROUND, 0, 0, width, height, -1);

        float widthPerc = width / 960f;
        float heightPerc = height / 505f;
        float logoWidth = 317.5f * widthPerc;
        float logoHeight = 50.5f * heightPerc;
        RenderUtil.texture(graphics, LOGO,
                width / 2f - logoWidth / 2f, height / 2f - 125, logoWidth, logoHeight, -1);

        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            float x = width / 2f - BUTTON_WIDTH / 2f;
            float y = buttonY(height, i);

            boolean hovered = RenderUtil.isHovered(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
            button.animation = AnimationMath.lerp(button.animation, hovered ? 1f : 0f, 10f);

            int outline = ColorUtil.interpolate(IDLE_OUTLINE, HOVER_OUTLINE, button.animation);

            RenderUtil.roundedRect(graphics, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 5,
                    ColorUtil.rgba(0x13, 0x15, 0x1b, Math.round(90 + button.animation * 60)));
            if (button.animation > 0.01f) {
                RenderUtil.shadow(graphics, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 5,
                        RenderUtil.withAlpha(HOVER_OUTLINE, Math.round(70 * button.animation)));
            }
            RenderUtil.roundedOutline(graphics, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 5, 1, outline);

            Component label = Component.literal(button.label).withStyle(Fonts.REGULAR_23);
            float labelWidth = StyledFontRenderer.width(Minecraft.getInstance().font, label);
            StyledFontRenderer.draw(graphics, Minecraft.getInstance().font, label,
                    Math.round(x + BUTTON_WIDTH / 2f - labelWidth / 2f),
                    Math.round(y + BUTTON_HEIGHT / 2f - 11.5f / 2f + 2),
                    ColorUtil.interpolate(IDLE_TEXT, -1, button.animation));
        }
    }

    private float buttonY(int height, int index) {
        if (index == 4) {
            return height / 2f - 15 + 160;
        }
        return height / 2f - 15 + BUTTON_STEP * index;
    }

    public boolean click(Screen screen, double mouseX, double mouseY, int guiWidth, int guiHeight) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < buttons.size(); i++) {
            float x = guiWidth / 2f - BUTTON_WIDTH / 2f;
            float y = buttonY(guiHeight, i);
            if (!RenderUtil.isHovered(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                continue;
            }
            switch (i) {
                case 0 -> mc.gui.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(screen));
                case 1 -> mc.gui.setScreen(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(screen));
                case 2 -> mc.gui.setScreen(new wtf.expensive.client.ui.alt.AltManagerScreen(screen));
                case 3 -> mc.gui.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(screen, mc.options, false));
                case 4 -> mc.stop();
                default -> {
                }
            }
            return true;
        }
        return false;
    }

    private static final class Button {
        private final String label;
        private float animation;

        private Button(String label) {
            this.label = label;
        }
    }
}
