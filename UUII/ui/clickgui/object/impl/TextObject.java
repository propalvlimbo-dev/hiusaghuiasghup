package wtf.expensive.client.ui.clickgui.object.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import wtf.expensive.client.modules.settings.imp.TextSetting;
import wtf.expensive.client.ui.clickgui.object.SettingObject;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;

public class TextObject extends SettingObject {
    private final TextSetting option;
    private boolean typing;

    public TextObject(TextSetting option) {
        super(option);
        this.option = option;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        text(graphics, option.getName(), Fonts.LIGHT_13, x + 10, y + height / 2f - 3,
                ColorUtil.rgba(161, 166, 179, 255));

        String value = option.get() + (typing && System.currentTimeMillis() % 1000 > 500 ? "_" : "");
        float boxWidth = Math.max(20, textWidth(value, Fonts.LIGHT_13) + 4);

        RenderUtil.roundedRect(graphics, x + width - boxWidth - 10, y + 2, boxWidth, 10, 2,
                ColorUtil.rgba(20, 21, 24, 255));
        centered(graphics, value, Fonts.LIGHT_13, x + width - boxWidth - 10 + boxWidth / 2f,
                y + height / 2f - 3, ColorUtil.rgba(161, 166, 179, 255));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            typing = !typing;
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (!typing) {
            return;
        }
        if (event.key() == GLFW_KEY_BACKSPACE) {
            if (!option.text.isEmpty()) {
                option.text = option.text.substring(0, option.text.length() - 1);
            }
        } else if (event.key() == GLFW_KEY_ENTER || event.isEscape()) {
            typing = false;
        }
    }

    @Override
    public void charTyped(CharacterEvent event) {
        if (typing && event.isAllowedChatCharacter()) {
            option.text += event.codepointAsString();
        }
    }

    @Override
    public boolean isCapturingInput() {
        return typing;
    }
}
