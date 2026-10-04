package wtf.expensive.client.ui.clickgui.object.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import wtf.expensive.client.modules.settings.imp.ModeSetting;
import wtf.expensive.client.ui.clickgui.object.SettingObject;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

public class ModeObject extends SettingObject {
    private static final int ACTIVE = ColorUtil.rgba(119, 121, 134, 255);
    private static final int INACTIVE = ColorUtil.rgba(26, 30, 41, 255);
    private static final float LINE = 11;

    private final ModeSetting option;

    public ModeObject(ModeSetting option) {
        super(option);
        this.option = option;
    }

    private float rowWidth() {
        float size = 0;
        for (String mode : option.modes) {
            float next = size + textWidth(mode, Fonts.SEMIBOLD_11) + 3;
            if (next > width - 20) {
                break;
            }
            size = next;
        }
        return size;
    }

    private int lineCount() {
        float rowWidth = rowWidth();
        int lines = 1;
        float offset = 0;
        for (String mode : option.modes) {
            float next = offset + textWidth(mode, Fonts.SEMIBOLD_11) + 3;
            if (next > rowWidth) {
                lines++;
                offset = 0;
            }
            offset += textWidth(mode, Fonts.SEMIBOLD_11) + 3;
        }
        return lines;
    }

    @Override
    public void setPosition(float x, float y, float width) {
        super.setPosition(x, y, width);
        this.height = 18 + LINE * lineCount();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float rowWidth = rowWidth();
        int lines = lineCount();

        text(graphics, option.getName(), Fonts.LIGHT_12, x + 10, y + 2,
                ColorUtil.rgba(161, 164, 177, 255));

        RenderUtil.roundedRect(graphics, x + 10, y + 14, rowWidth + 7, LINE * lines + 2, 3,
                ColorUtil.rgba(11, 12, 15, 255));

        float offset = 0;
        float offsetY = 0;
        for (int i = 0; i < option.modes.length; i++) {
            String mode = option.modes[i];
            float wordWidth = textWidth(mode, Fonts.SEMIBOLD_11) + 3;
            if (offset + wordWidth > rowWidth) {
                offset = 0;
                offsetY += LINE;
            }
            text(graphics, mode, Fonts.SEMIBOLD_11, x + 13 + offset, y + 16 + offsetY,
                    option.getIndex() == i ? ACTIVE : INACTIVE);
            offset += wordWidth;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        float rowWidth = rowWidth();
        float offset = 0;
        float offsetY = 0;
        for (int i = 0; i < option.modes.length; i++) {
            String mode = option.modes[i];
            float wordWidth = textWidth(mode, Fonts.SEMIBOLD_11) + 3;
            if (offset + wordWidth > rowWidth) {
                offset = 0;
                offsetY += LINE;
            }
            if (mouseX >= x + 10 + offset && mouseX <= x + 13 + offset + wordWidth
                    && mouseY >= y + 14 + offsetY && mouseY <= y + 14 + offsetY + LINE) {
                option.set(i);
                return;
            }
            offset += wordWidth;
        }
    }
}
