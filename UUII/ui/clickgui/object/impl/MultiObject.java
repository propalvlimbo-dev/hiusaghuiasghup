package wtf.expensive.client.ui.clickgui.object.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import wtf.expensive.client.modules.settings.imp.BooleanOption;
import wtf.expensive.client.modules.settings.imp.MultiBoxSetting;
import wtf.expensive.client.ui.clickgui.object.SettingObject;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

public class MultiObject extends SettingObject {
    private static final int ACTIVE = ColorUtil.rgba(119, 121, 134, 255);
    private static final int INACTIVE = ColorUtil.rgba(26, 30, 41, 255);
    private static final float LINE = 11;

    private final MultiBoxSetting option;

    public MultiObject(MultiBoxSetting option) {
        super(option);
        this.option = option;
    }

    private float rowWidth() {
        float size = 0;
        for (BooleanOption entry : option.options) {
            float next = size + textWidth(entry.getName(), Fonts.SEMIBOLD_11) + 3;
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
        for (BooleanOption entry : option.options) {
            float wordWidth = textWidth(entry.getName(), Fonts.SEMIBOLD_11) + 3;
            if (offset + wordWidth > rowWidth) {
                lines++;
                offset = 0;
            }
            offset += wordWidth;
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
        for (int i = 0; i < option.options.size(); i++) {
            BooleanOption entry = option.options.get(i);
            float wordWidth = textWidth(entry.getName(), Fonts.SEMIBOLD_11) + 3;
            if (offset + wordWidth > rowWidth) {
                offset = 0;
                offsetY += LINE;
            }
            text(graphics, entry.getName(), Fonts.SEMIBOLD_11, x + 13 + offset, y + 16 + offsetY,
                    entry.get() ? ACTIVE : INACTIVE);
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
        for (int i = 0; i < option.options.size(); i++) {
            BooleanOption entry = option.options.get(i);
            float wordWidth = textWidth(entry.getName(), Fonts.SEMIBOLD_11) + 3;
            if (offset + wordWidth > rowWidth) {
                offset = 0;
                offsetY += LINE;
            }
            if (mouseX >= x + 10 + offset && mouseX <= x + 13 + offset + wordWidth
                    && mouseY >= y + 14 + offsetY && mouseY <= y + 14 + offsetY + LINE) {
                option.set(i, !entry.get());
                return;
            }
            offset += wordWidth;
        }
    }
}
