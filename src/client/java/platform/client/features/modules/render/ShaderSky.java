package platform.client.features.modules.render;

import platform.api.module.Category;
import platform.client.Delta;
import platform.api.module.Module;
import platform.api.module.ModuleRegister;
import platform.client.utils.render.SkyShaderRenderer;
import platform.api.module.setting.BooleanSetting;
import platform.api.module.setting.ColorSetting;
import platform.api.module.setting.ModeSetting;
import platform.api.module.setting.SliderSetting;
import lombok.Getter;

@ModuleRegister(a = "ShaderSky", b = "Шейдерное небо как в cursed (полный перенос mixin)", c = Category.Render)
public class ShaderSky extends Module {
    @Getter
    private static ShaderSky instance;

    public final ModeSetting rezhim = new ModeSetting("Режим", "Darkness", "Darkness", "Caustics", "Clouds", "Matrix", "Starfield", "Aurora", "Grid");
    public final SliderSetting speed = new SliderSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    public final SliderSetting scale = new SliderSetting("Размер", 5.0f, 1.0f, 20.0f, 0.5f);
    public final SliderSetting intensity = new SliderSetting("Интенсивность", 0.01f, 0.001f, 0.05f, 0.001f);
    public final SliderSetting alpha = new SliderSetting("Прозрачность", 1.0f, 0.3f, 1.0f, 0.05f);
    public final BooleanSetting ownColor = new BooleanSetting("Свой цвет неба", false);
    public final ColorSetting color = new ColorSetting("Цвет неба", 0xFF4FC3FF);

    public ShaderSky() {
        instance = this;
        a(rezhim, speed, scale, intensity, alpha, ownColor, color);
    }

    public static boolean check() {
        ShaderSky i = instance;
        return i != null && i.m();
    }


    public void renderSky() {
        if (!m()) return;
        SkyShaderRenderer.getInstance().render(
                rezhim.c(),
                speed.c().floatValue(),
                scale.c().floatValue(),
                intensity.c().floatValue(),
                alpha.c().floatValue(),
                ownColor.c().booleanValue() ? color.c().intValue() : 0
        );
    }

    @Override
    public void c() {
        super.c();
        SkyShaderRenderer.getInstance().resetTime();
    }
}
