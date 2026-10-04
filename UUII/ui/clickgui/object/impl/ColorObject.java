package wtf.expensive.client.ui.clickgui.object.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import wtf.expensive.client.modules.settings.imp.ColorSetting;
import wtf.expensive.client.ui.clickgui.object.SettingObject;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

import java.awt.Color;

public class ColorObject extends SettingObject {
    private static final float PICKER = 70;
    private static final float SLIDER = 5;
    private static final int STEPS = 24;

    private final ColorSetting option;
    private float[] hsb;
    private float alpha;
    private boolean opened;
    private boolean dragPalette;
    private boolean dragHue;
    private boolean dragAlpha;

    public ColorObject(ColorSetting option) {
        super(option);
        this.option = option;
        sync();
    }

    private void sync() {
        Color color = option.getColor();
        hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        alpha = ((option.get() >>> 24) & 0xFF) / 255f;
    }

    @Override
    public void setPosition(float x, float y, float width) {
        super.setPosition(x, y, width);
        if (opened) {
            this.height = 16 + PICKER + 22;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        text(graphics, option.getName(), Fonts.LIGHT_13, x + 10, y + height / 2f - 3,
                ColorUtil.rgba(161, 166, 179, 255));

        if (!opened) {
            RenderUtil.roundedRect(graphics, x + width - 20, y + 4, 8, 8, 2,
                    ColorUtil.reAlpha(option.get(), 255));
            return;
        }

        RenderUtil.roundedRect(graphics, x + width - 20, y + 4, 8, 8, 2,
                ColorUtil.reAlpha(option.get(), 255));

        float px = x + 10;
        float py = y + 16;
        float pw = width - 20;

        if (dragPalette) {
            hsb[1] = Mth.clamp((mouseX - px) / pw, 0f, 1f);
            hsb[2] = 1f - Mth.clamp((mouseY - py) / PICKER, 0f, 1f);
        }
        float hueY = py + PICKER + 4;
        float alphaY = hueY + SLIDER + 4;
        if (dragHue) {
            hsb[0] = Mth.clamp((mouseX - px) / pw, 0f, 1f);
        }
        if (dragAlpha) {
            alpha = Mth.clamp((mouseX - px) / pw, 0f, 1f);
        }

        float step = pw / STEPS;
        for (int i = 0; i < STEPS; i++) {
            float saturation = (float) i / STEPS;
            RenderUtil.roundedRect(graphics, px + i * step, py, step + 0.5f, PICKER, 0, 0, 0, 0,
                    ColorUtil.reAlpha(Color.HSBtoRGB(hsb[0], saturation, 1f), 255), 0xFF000000);
        }
        RenderUtil.circle(graphics, px + hsb[1] * pw, py + (1f - hsb[2]) * PICKER, 2.5f, 0xFF000000);

        for (int i = 0; i < STEPS; i++) {
            RenderUtil.roundedRect(graphics, px + i * step, hueY, step + 0.5f, SLIDER, 0,
                    ColorUtil.reAlpha(Color.HSBtoRGB((float) i / STEPS, 1f, 1f), 255));
        }
        RenderUtil.circle(graphics, px + hsb[0] * pw, hueY + SLIDER / 2f, 3f, 0xFF000000);

        int solid = ColorUtil.reAlpha(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), 255);
        for (int i = 0; i < STEPS; i++) {
            RenderUtil.roundedRect(graphics, px + i * step, alphaY, step + 0.5f, SLIDER, 0,
                    ColorUtil.interpolate(ColorUtil.rgba(17, 18, 21, 255), solid, (float) i / STEPS));
        }
        RenderUtil.circle(graphics, px + alpha * pw, alphaY + SLIDER / 2f, 3f, 0xFF000000);

        if (dragPalette || dragHue || dragAlpha) {
            option.color = ColorUtil.reAlpha(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), (int) (alpha * 255));
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        if (mouseX >= x + width - 22 && mouseX <= x + width - 8
                && mouseY >= y + 2 && mouseY <= y + 14) {
            opened = !opened;
            if (opened) {
                sync();
            }
            return;
        }
        if (!opened) {
            return;
        }

        float px = x + 10;
        float py = y + 16;
        float pw = width - 20;
        float hueY = py + PICKER + 4;
        float alphaY = hueY + SLIDER + 4;

        if (mouseX >= px && mouseX <= px + pw && mouseY >= py && mouseY <= py + PICKER) {
            dragPalette = true;
        } else if (mouseX >= px && mouseX <= px + pw && mouseY >= hueY - 2 && mouseY <= hueY + SLIDER + 2) {
            dragHue = true;
        } else if (mouseX >= px && mouseX <= px + pw && mouseY >= alphaY - 2 && mouseY <= alphaY + SLIDER + 2) {
            dragAlpha = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragPalette = false;
        dragHue = false;
        dragAlpha = false;
    }
}
