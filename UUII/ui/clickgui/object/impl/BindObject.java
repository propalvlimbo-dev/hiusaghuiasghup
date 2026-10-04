package wtf.expensive.client.ui.clickgui.object.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import wtf.expensive.client.modules.settings.imp.BindSetting;
import wtf.expensive.client.ui.clickgui.object.SettingObject;
import wtf.expensive.client.util.KeyUtil;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

public class BindObject extends SettingObject {
    private final BindSetting option;
    private boolean binding;
    private float hoverAnim;

    public BindObject(BindSetting option) {
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
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnim = (float) AnimationMath.lerp(hoverAnim, hovered ? 1f : 0f, 10f);

        text(graphics, option.getName(), Fonts.LIGHT_13, x + 10, y + 3,
                ColorUtil.rgba(161, 166, 179, 255));

        String key = binding ? "..." : (option.getKey() == -1 ? "None" : KeyUtil.getKeyName(option.getKey()));
        float boxWidth = Math.max(14, textWidth(key, Fonts.LIGHT_13) + 8);

        int bgColor = ColorUtil.interpolate(
                ColorUtil.rgba(20, 21, 24, 255),
                ColorUtil.rgba(40, 45, 60, 255),
                hoverAnim
        );
        int textColor = ColorUtil.interpolate(
                ColorUtil.rgba(161, 166, 179, 255),
                -1,
                hoverAnim
        );

        RenderUtil.roundedRect(graphics, x + width - boxWidth - 10, y + 2, boxWidth, 12, 3, bgColor);
        centered(graphics, key, Fonts.LIGHT_13, x + width - boxWidth - 10 + boxWidth / 2f,
                y + 3, binding ? ColorUtil.rgba(255, 200, 0, 255) : textColor);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (binding && button > 1) {
            option.setKey(-100 + button);
            binding = false;
            return;
        }
        if (button == 0 && isHovered(mouseX, mouseY)) {
            binding = !binding;
            if (!binding && option.getKey() == -1) {
                option.setKey(-1);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (!binding) return;
        if (event.isEscape()) {
            option.setKey(-1);
        } else {
            option.setKey(event.key());
        }
        binding = false;
    }

    @Override
    public boolean isCapturingInput() {
        return binding;
    }
}
