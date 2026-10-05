package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Компактное окно настроек render-модуля delta — по образу модалки MusicIsland.
 * Открывается для конкретного модуля, рисует его настройки (Boolean/Slider/Mode/
 * Color) ровно как в delta, без выдумок. Закрывается кликом вне окна или по «×».
 */
public final class ModuleModal {
    private static platform.api.module.Module mod;
    private static boolean wasPressed;
    private static float px, py, pw, ph;

    private ModuleModal() {
    }

    public static void open(platform.api.module.Module m) {
        mod = m;
    }

    public static void close() {
        mod = null;
    }

    public static boolean isOpen() {
        return mod != null;
    }

    private record Row(String label, String value, int kind, Object ref) {
    }

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my, float dt) {
        if (mod == null) {
            return;
        }
        List<Row> rows = new ArrayList<>();
        for (platform.api.module.setting.Setting<?> s : mod.e()) {
            if (s == null || (s.e() != null && !s.e().get())) {
                continue;
            }
            if (s instanceof platform.api.module.setting.BooleanSetting bs) {
                rows.add(new Row(bs.i(), bs.c() ? "вкл" : "выкл", 0, bs));
            } else if (s instanceof platform.api.module.setting.SliderSetting ss) {
                rows.add(new Row(ss.i(), String.valueOf(ss.c()), 1, ss));
            } else if (s instanceof platform.api.module.setting.ModeSetting ms) {
                rows.add(new Row(ms.i(), ms.c(), 2, ms));
            } else {
                rows.add(new Row(s.i(), String.valueOf(s.c()), 3, s));
            }
        }

        float rowH = 14f, pad = 6f;
        pw = 168f;
        ph = pad * 2 + 16f + rows.size() * rowH + 4f;
        px = (sw - pw) / 2f;
        py = (sh - ph) / 2f;

        boolean pr = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean cl = pr && !wasPressed;
        wasPressed = pr;

        boolean inside = mx >= px && mx <= px + pw && my >= py && my <= py + ph;
        if (cl && !inside) {
            close();
            return;
        }

        UiVector.roundRect(g, px, py, pw, ph, 7f, 0xF2141218);
        UiVector.outline(g, px, py, pw, ph, 7f, .5f, 0x33FFFFFF);

        MtsdfTextRenderer.draw(g, Fonts.MEDIUM, mod.j(), px + pad, py + pad, 9f, 0xFFFFFFFF);
        // крестик
        float cx = px + pw - 12f, cy = py + pad;
        if (insideRect(mx, my, cx - 3, cy - 3, 12, 12) && pr) {
            UiVector.roundRect(g, cx - 3, cy - 3, 12, 12, 4f, 0x1FFFFFFF);
        }
        MtsdfTextRenderer.draw(g, Fonts.REGULAR, "x", cx + 1f, cy, 8f, 0xB3FFFFFF);
        if (cl && insideRect(mx, my, cx - 3, cy - 3, 12, 12)) {
            close();
            return;
        }

        float ry = py + pad + 16f;
        for (Row r : rows) {
            boolean hov = insideRect(mx, my, px + 3, ry, pw - 6, rowH);
            if (hov) {
                UiVector.roundRect(g, px + 3, ry, pw - 6, rowH, 4f, 0x1FFFFFFF);
            }
            MtsdfTextRenderer.draw(g, Fonts.REGULAR, r.label(), px + 8, ry + 3.5f, 7f, 0xE6FFFFFF);
            float vw = MtsdfTextRenderer.width(Fonts.MEDIUM, r.value(), 7f);
            MtsdfTextRenderer.draw(g, Fonts.MEDIUM, r.value(), px + pw - 8 - vw, ry + 3.5f, 7f, 0xFFFF4FC3);
            if (cl && hov) {
                act(r, mx);
            }
            ry += rowH;
        }
    }

    private static void act(Row r, int mx) {
        boolean rightHalf = mx > px + pw / 2f;
        switch (r.kind()) {
            case 0 -> {
                platform.api.module.setting.BooleanSetting bs = (platform.api.module.setting.BooleanSetting) r.ref();
                bs.a(!bs.c());
            }
            case 1 -> {
                platform.api.module.setting.SliderSetting ss = (platform.api.module.setting.SliderSetting) r.ref();
                float step = Math.max(ss.c, 0.1f);
                float v = ss.c() + (rightHalf ? step : -step);
                v = Math.max(ss.a, Math.min(ss.b, v));
                ss.a(v);
            }
            case 2 -> {
                platform.api.module.setting.ModeSetting ms = (platform.api.module.setting.ModeSetting) r.ref();
                List<String> modes = ms.k();
                int i = modes.indexOf(ms.c());
                i = (i + 1) % modes.size();
                ms.a(modes.get(i));
            }
            default -> {
            }
        }
    }

    private static boolean insideRect(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
