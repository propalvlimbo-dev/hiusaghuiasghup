package platform.client.ui.element;

import platform.api.system.interfaces.NativeMethodLookup;
import platform.client.Xivivide;
import platform.client.utils.render.Fonts;
import platform.client.utils.render.ColorUtil;
import platform.client.utils.math.MathUtil;
import platform.api.system.configs.ThemeInfo;
import platform.api.system.configs.ThemeProcessor;
import platform.client.utils.render.Draw2DProcessor;
import platform.api.module.setting.SliderSetting;
import platform.api.annotation.Compile;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector4f;

public class SliderElement extends Element_2<SliderSetting> {
    private boolean d;

    @Override
    @Compile
    public boolean a(double mouseX, double mouseY, int button) {
        Vector4f vector4f = this.a;
        var setting = this.b;
        if (!MathUtil.a(mouseX, mouseY, vector4f.x, vector4f.y + Fonts.c.a(6.5f), vector4f.z, 11.0f)) {
            return false;
        }
        if (button == 0) {
            this.d = true;
            a(mouseX);
            return true;
        }
        if (button != 2) return false;
        ((SliderSetting) setting).b();
        return true;
    }

    @Override
    @Compile
    public boolean b(double mouseX, double mouseY, int button) {
        this.d = false;
        return false;
    }

    @Override
    @Compile
    public boolean a(double mouseX, double mouseY, double amount) {
        SliderSetting sliderSetting = (SliderSetting) this.b;
        Vector4f vector4f = this.a;
        if (!sliderSetting.e || !MathUtil.a(mouseX, mouseY, vector4f.x, vector4f.y, vector4f.z, ((vector4f.y + Fonts.c.a(6.5f)) + 9.5f) - vector4f.y)) {
            return false;
        }
        Float fC = sliderSetting.c();
        sliderSetting.a(Float.valueOf(MathUtil.b(Math.round((fC.floatValue() + (((float) Math.signum(amount)) * sliderSetting.c)) / sliderSetting.c) * sliderSetting.c, sliderSetting.a, sliderSetting.b)));
        return true;
    }

    static {
        NativeMethodLookup.lookup(SliderElement.class, 13);
    }

    /**
     * The compact row a widget's own settings panel uses: the name, the value, and a thin track along the
     * bottom of the row. Without this the row was laid out and left blank, which is where the empty gaps in
     * those panels came from.
     */
    @Override
    public void a(platform.api.event.events.render.DrawEvent event, float x, float y, float width, float animation) {
        GuiGraphicsExtractor context = event.i();
        Draw2DProcessor draw = event.d();
        ThemeProcessor theme = Xivivide.h().d().o();
        SliderSetting setting = (SliderSetting) this.b;
        int accent = theme.a(ThemeInfo.PRIMARY).a();
        float value = setting.c().floatValue();
        float progress = MathUtil.b((value - setting.a) / Math.max(0.0001f, setting.b - setting.a), 0.0f, 1.0f);
        String shown = setting.c % 1.0f == 0.0f ? String.valueOf(Math.round(value))
                : String.valueOf(Math.round(value * 100.0f) / 100.0f);

        Fonts.settingsIcon.a(context, "A", x + 4.5f, y + 2.0f, 8.0f, ColorUtil.a(accent, animation));
        int separator = ColorUtil.a(theme.a(ThemeInfo.TEXT).a(), 0.3f * animation);
        draw.a(context, x + 15.5f, y + 3.0f, 0.5f, 6.0f, separator);
        Fonts.e.a(context, setting.i(), x + 19.5f, y + 1.5f, 6.5f, ColorUtil.a(theme.a(ThemeInfo.TEXT).a(), animation));
        float valueWidth = Fonts.e.a(shown, 6.0f);
        draw.a(context, x + width - valueWidth - 9.5f, y + 2.0f, 0.5f, 5.5f, separator);
        Fonts.e.a(context, shown, (x + width) - valueWidth - 5.0f, y + 1.75f, 6.0f,
                ColorUtil.a(theme.a(ThemeInfo.TEXT).a(), 0.6f * animation));

        float trackX = x + 19.5f;
        float trackWidth = Math.max(6.0f, (x + width) - 5.0f - valueWidth - 4.0f - trackX);
        float trackY = y + 9.5f;
        draw.a(context, trackX, trackY, trackWidth, 1.5f, 0.75f,
                ColorUtil.a(theme.a(ThemeInfo.TEXT).a(), 0.18f * animation));
        draw.a(context, trackX, trackY, trackWidth * progress, 1.5f, 0.75f,
                ColorUtil.a(theme.a(ThemeInfo.SLIDER).a(), animation));
    }

    public SliderElement(SliderSetting setting) {
        super(setting);
        this.a.w = 22.0f;
    }

    @Override
    public void a(GuiGraphicsExtractor context, double mouseX, double mouseY, float delta, float extend) {
        Draw2DProcessor draw = Xivivide.h().d().i();
        ThemeProcessor theme = Xivivide.h().d().o();
        this.a.w = 18.0f;
        if (this.d) {
            a(mouseX);
        }
        b().c(MathUtil.c(b().a(), (((SliderSetting) this.b).c().floatValue() - ((SliderSetting) this.b).a) / (((SliderSetting) this.b).b - ((SliderSetting) this.b).a), 1.0f));
        float progress = b().a();
        boolean hovered = MathUtil.a(mouseX, mouseY, this.a.x, this.a.y, this.a.z, this.a.w) && extend >= 1.0f;
        float current = ((SliderSetting) this.b).a + ((((SliderSetting) this.b).b - ((SliderSetting) this.b).a) * progress);
        String value = ((SliderSetting) this.b).c % 1.0f == 0.0f ? String.valueOf(Math.round(current)) : String.valueOf(Math.round(current * 100.0f) / 100.0f);
        float boxWidth = Fonts.c.a(value, 6.25f) + 6.0f;
        float boxHeight = Fonts.c.a(6.25f) + 2.0f;
        float boxX = (this.a.x + this.a.z) - boxWidth;
        a(context, Fonts.c, ((SliderSetting) this.b).i(), this.a.x, this.a.y + 0.5f, Fonts.c.a(6.5f), 6.5f, theme.a(ThemeInfo.TEXT).a(), (boxX - this.a.x) - 4.0f, hovered, extend, delta);
        draw.a(context, boxX, this.a.y, boxWidth, boxHeight, 2.0f, ColorUtil.a(theme.a(ThemeInfo.PRIMARY).a(), 0.03137255f * extend));
        draw.a(context, boxX, this.a.y, boxWidth, boxHeight, 2.0f, 0.5f, ColorUtil.a(theme.a(ThemeInfo.OUTLINE_SMALL).a(), theme.a(ThemeInfo.OUTLINE_SMALL).b() * extend));
        Fonts.c.b(context, value, boxX + (boxWidth / 2.0f), (this.a.y + ((boxHeight - Fonts.c.a(6.25f)) / 2.0f)) - 0.5f, 6.25f, ColorUtil.a(theme.a(ThemeInfo.TEXT).a(), extend));
        float trackY = this.a.y + Fonts.c.a(6.5f) + 6.5f;
        draw.a(context, this.a.x, trackY, this.a.z, 3.0f, 0.75f, ColorUtil.a(ColorUtil.a(50, 52, 60, 255), extend * 0.35f));
        // The filled part of the track and the knob riding on it are their own colours in the theme
        draw.a(context, this.a.x, trackY, this.a.z * progress, 3.0f, 0.75f, ColorUtil.a(theme.a(ThemeInfo.SLIDER).a(), extend));
        draw.a(context, this.a.x + ((this.a.z - 6.0f) * progress), (trackY + 1.5f) - 3.0f, 6.0f, 6.0f, 2.0f, ColorUtil.a(theme.a(ThemeInfo.SLIDER_KNOB).a(), extend));
    }

    private void a(double mouseX) {
        float progress = MathUtil.b(((float) (mouseX - ((double) this.a.x))) / this.a.z, 0.0f, 1.0f);
        float value = ((SliderSetting) this.b).a + ((((SliderSetting) this.b).b - ((SliderSetting) this.b).a) * progress);
        ((SliderSetting) this.b).a(Float.valueOf(MathUtil.b(Math.round(value / ((SliderSetting) this.b).c) * ((SliderSetting) this.b).c, ((SliderSetting) this.b).a, ((SliderSetting) this.b).b)));
    }
}



