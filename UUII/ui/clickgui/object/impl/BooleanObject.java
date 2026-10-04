package wtf.expensive.client.ui.clickgui.object.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import wtf.expensive.client.modules.settings.imp.BooleanOption;
import wtf.expensive.client.ui.clickgui.object.SettingObject;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

public class BooleanObject extends SettingObject {
    private static final float TRAVEL = 6.5f;

    private final BooleanOption option;
    private float animation;

    public BooleanObject(BooleanOption option) {
        super(option);
        this.option = option;
    }

    @Override
    public void setPosition(float x, float y, float width) {
        super.setPosition(x, y, width);
        this.height = 16;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        animation = AnimationMath.fast(animation, option.get() ? TRAVEL : 0f, 10f);

        text(graphics, option.getName(), Fonts.LIGHT_13, x + 10, y + 3,
                ColorUtil.rgba(161, 166, 179, 255));

        RenderUtil.roundedRect(graphics, x + width - 23.5f, y + 4, 13.5f, 6, 3,
                ColorUtil.rgba(20, 21, 24, 255));

        int color = ColorUtil.interpolate(
                ColorUtil.rgba(42, 56, 73, 255),
                ColorUtil.rgba(127, 134, 154, 255),
                animation / TRAVEL);
        RenderUtil.circle(graphics, x + width - 20 + animation, y + 7, 2.5f, color);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            option.toggle();
        }
    }
}
