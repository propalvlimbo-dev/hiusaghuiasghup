package wtf.expensive.client.ui.clickgui.object;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import wtf.expensive.client.modules.settings.Setting;
import wtf.expensive.client.util.font.StyledFontRenderer;

public abstract class SettingObject {
    protected final Setting setting;

    public float x;
    public float y;
    public float width;
    public float height = 16;

    protected SettingObject(Setting setting) {
        this.setting = setting;
    }

    public Setting getSetting() {
        return setting;
    }

    public void setPosition(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = 16;
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    protected static float textWidth(String text, Style style) {
        return StyledFontRenderer.width(font(), Component.literal(text).withStyle(style));
    }

    protected static void text(GuiGraphicsExtractor graphics, String value, Style style, float x, float y, int color) {
        StyledFontRenderer.draw(graphics, font(), Component.literal(value).withStyle(style), x, y, color);
    }

    protected static void centered(GuiGraphicsExtractor graphics, String value, Style style, float x, float y, int color) {
        StyledFontRenderer.drawCentered(graphics, font(), Component.literal(value).withStyle(style), x, y, color);
    }

    protected boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public abstract void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    public void mouseClicked(double mouseX, double mouseY, int button) {
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public void keyPressed(KeyEvent event) {
    }

    public void charTyped(CharacterEvent event) {
    }

    public boolean isCapturingInput() {
        return false;
    }
}
