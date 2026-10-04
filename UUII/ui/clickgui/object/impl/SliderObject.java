package wtf.expensive.client.ui.clickgui.object.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import wtf.expensive.client.modules.settings.imp.SliderSetting;
import wtf.expensive.client.ui.clickgui.object.SettingObject;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

public class SliderObject extends SettingObject {
    private final SliderSetting option;
    private boolean sliding;
    private float animated;

    public SliderObject(SliderSetting option) {
        super(option);
        this.option = option;
    }

    @Override
    public void setPosition(float x, float y, float width) {
        super.setPosition(x, y, width);
        this.height = 20;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (sliding) {
            float value = (float) ((mouseX - x - 10) / (width - 20) * (option.getMax() - option.getMin()) + option.getMin());
            option.setValue(round(value, option.getIncrement()));
        }

        float track = width - 20;
        float filled = (option.getValue().floatValue() - option.getMin())
                / (option.getMax() - option.getMin()) * track;
        animated = AnimationMath.fast(animated, filled, 20f);

        text(graphics, option.getName(), Fonts.LIGHT_12, x + 10, y + 2,
                ColorUtil.rgba(161, 164, 177, 255));

        String value = formatValue();
        text(graphics, value, Fonts.LIGHT_12,
                x + width - 10 - textWidth(value, Fonts.LIGHT_12), y + 2,
                ColorUtil.rgba(161, 164, 177, 255));

        RenderUtil.roundedRect(graphics, x + 10, y + 13, track, 3, 1,
                ColorUtil.rgba(21, 22, 25, 255));
        if (animated > 0.5f) {
            RenderUtil.roundedRect(graphics, x + 10, y + 13, animated, 3, 1,
                    ColorUtil.rgba(128, 133, 152, 255));
        }

        RenderUtil.circle(graphics, x + 10 + animated, y + 14.5f, 3,
                ColorUtil.rgba(128, 133, 152, 255));
    }

    private String formatValue() {
        float value = option.getValue().floatValue();
        if (option.getIncrement() >= 1f && value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    private static float round(float value, float increment) {
        return increment <= 0 ? value : Math.round(value / increment) * increment;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            sliding = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        sliding = false;
    }
}
