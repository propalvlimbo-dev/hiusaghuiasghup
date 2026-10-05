package platform.client.features.modules.render;

import platform.api.module.Category;
import platform.api.module.Module;
import platform.api.module.ModuleRegister;
import platform.client.utils.render.ColorUtil;
import platform.api.module.setting.ModeSetting;
import platform.api.module.setting.SliderSetting;

/**
 * Shader ESP — перенос в стиле ShaderSky: на игроков (и предметы) накладывается
 * свечение теми же шейдерными режимами, что и небо (Darkness/Caustics/Clouds/
 * Matrix/Starfield). Контур рисует ванильный outline-пасс (glowing), а цвет
 * анимируется по времени выбранной шейдерной функцией — {@link #espColor()}.
 */
@ModuleRegister(a = "Shader ESP", b = "Накладывает на игроков шейдерное свечение в стиле ShaderSky", c = Category.Render)
public class ShaderESP extends Module {
    public final ModeSetting rezhim = new ModeSetting("Режим", "Matrix", "Darkness", "Caustics", "Clouds", "Matrix", "Starfield");
    public final SliderSetting speed = new SliderSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    public final SliderSetting intensity = new SliderSetting("Интенсивность", 0.8f, 0.1f, 1.0f, 0.05f);

    public ShaderESP() {
        a(rezhim, speed, intensity);
    }

    public SliderSetting q() {
        return this.intensity;
    }

    /** Анимированный цвет свечения — та же палитра режимов, что у ShaderSky. */
    public int espColor() {
        float t = (System.currentTimeMillis() % 100000L) / 1000.0f * speed.c().floatValue();
        float k = intensity.c().floatValue();
        float w = 0.5f + 0.5f * (float) Math.sin(t * 2.0);
        switch (rezhim.c()) {
            case "Darkness":
                return ColorUtil.a((int) (80 + 60 * w * k), (int) (25 * k), (int) (140 + 90 * w * k));
            case "Caustics":
                return ColorUtil.a((int) (25 * k), (int) (150 + 90 * w * k), (int) (190 + 60 * w * k));
            case "Clouds":
                return ColorUtil.a((int) (130 + 70 * w * k), (int) (170 + 60 * w * k), 255);
            case "Starfield": {
                float flick = (float) Math.abs(Math.sin(t * 7.0) * Math.sin(t * 3.7));
                int v = (int) (160 + 95 * flick * k);
                return ColorUtil.a(v, v, 255);
            }
            default:
                return ColorUtil.a((int) (30 * k), (int) (170 + 85 * w * k), (int) (60 * k));
        }
    }
}
