package wtf.expensive.client.ui.clickgui.object;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.settings.Setting;
import wtf.expensive.client.modules.settings.imp.*;
import wtf.expensive.client.ui.clickgui.object.impl.*;
import wtf.expensive.client.util.KeyUtil;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

import static wtf.expensive.client.ui.clickgui.ClickGui.LIGHT;

public class ModuleObject {
    private static final float HEADER = 22;

    public final Function function;
    private final List<SettingObject> objects = new ArrayList<>();

    private float x;
    private float y;
    private float width = 160;
    private float animation;
    private boolean binding;

    public ModuleObject(Function function) {
        this.function = function;
        for (Setting setting : function.getSettingList()) {
            if (setting instanceof BooleanOption option) {
                objects.add(new BooleanObject(option));
            } else if (setting instanceof SliderSetting option) {
                objects.add(new SliderObject(option));
            } else if (setting instanceof ModeSetting option) {
                objects.add(new ModeObject(option));
            } else if (setting instanceof MultiBoxSetting option) {
                objects.add(new MultiObject(option));
            } else if (setting instanceof BindSetting option) {
                objects.add(new BindObject(option));
            } else if (setting instanceof ColorSetting option) {
                objects.add(new ColorObject(option));
            } else if (setting instanceof TextSetting option) {
                objects.add(new TextObject(option));
            }
        }
    }

    public void setPosition(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;

        float offset = 3;
        for (SettingObject object : objects) {
            if (object.getSetting().visible()) {
                object.setPosition(x, y + HEADER + offset, width);
                offset += object.height;
            }
        }
    }

    public float getHeight() {
        float height = HEADER;
        for (SettingObject object : objects) {
            if (object.getSetting().visible()) {
                height += object.height;
            }
        }
        return height + 3;
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float height = getHeight();
        animation = AnimationMath.fast(animation, function.isState() ? 1f : 0f, 5f);

        RenderUtil.roundedRect(graphics, x, y, width, height, 4,
                ColorUtil.rgba(42, 47, 67, 235));
        RenderUtil.roundedRect(graphics, x + 1, y + 1, width - 2, height - 2, 3,
                ColorUtil.rgba(18, 19, 25, 242));

        StyledFontRenderer.draw(graphics, SettingObject.font(),
                net.minecraft.network.chat.Component.literal(function.name).withStyle(Fonts.SEMIBOLD_15),
                x + 10, y + 7,
                ColorUtil.interpolate(LIGHT, -1, animation));

        renderBind(graphics);

        if (!objects.isEmpty()) {
            RenderUtil.roundedRect(graphics, x + 10, y + HEADER, width - 20, 0.5f, 0,
                    ColorUtil.rgba(32, 35, 57, 255));
        }

        for (SettingObject object : objects) {
            if (object.getSetting().visible()) {
                object.render(graphics, mouseX, mouseY);
            }
        }
    }

    private void renderBind(GuiGraphicsExtractor graphics) {
        String label = binding ? "..." : function.bind == 0 ? "None"
                : KeyUtil.getKeyName(function.bind).toUpperCase(java.util.Locale.ROOT);
        if (!binding && function.bind == 0) {
            return;
        }
        var text = net.minecraft.network.chat.Component.literal(label).withStyle(Fonts.LIGHT_13);
        float textWidth = StyledFontRenderer.width(SettingObject.font(), text);
        float boxWidth = Math.max(14, textWidth + 8);
        RenderUtil.roundedRect(graphics, x + width - boxWidth - 8, y + 5, boxWidth, 12, 3,
                binding ? ColorUtil.rgba(40, 45, 60, 255) : ColorUtil.rgba(20, 21, 24, 255));
        StyledFontRenderer.drawCentered(graphics, SettingObject.font(), text,
                x + width - boxWidth - 8 + boxWidth / 2f, y + 7,
                binding ? ColorUtil.rgba(255, 200, 0, 255) : ColorUtil.rgba(161, 166, 179, 255));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + getHeight()) {
            return false;
        }
        if (mouseY < y + HEADER) {
            if (binding && button > 1) {
                function.bind = -100 + button;
                binding = false;
                return true;
            }
            if (button == 0) {
                function.toggle();
            } else if (button == 2) {
                binding = !binding;
            }
            return true;
        }
        for (SettingObject object : objects) {
            if (object.getSetting().visible()) {
                object.mouseClicked(mouseX, mouseY, button);
            }
        }
        return true;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (SettingObject object : objects) {
            object.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void keyPressed(KeyEvent event) {
        if (binding) {
            function.bind = event.isEscape() ? 0 : event.key();
            binding = false;
            return;
        }
        for (SettingObject object : objects) {
            object.keyPressed(event);
        }
    }

    public void charTyped(CharacterEvent event) {
        for (SettingObject object : objects) {
            object.charTyped(event);
        }
    }

    public boolean isCapturingInput() {
        if (binding) {
            return true;
        }
        for (SettingObject object : objects) {
            if (object.isCapturingInput()) {
                return true;
            }
        }
        return false;
    }
}
