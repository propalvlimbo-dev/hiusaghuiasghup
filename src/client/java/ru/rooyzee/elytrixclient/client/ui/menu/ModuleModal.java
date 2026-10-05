package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Компактное окно настроек render-модуля delta — по образу модалки MusicIsland.
 * Рисует ВСЕ типы настроек delta (Boolean/Slider/Mode/Color/MultiMode/Bind/
 * String/Button) и обрабатывает их прямо здесь, без выдумок.
 */
public final class ModuleModal {
    private static platform.api.module.Module mod;
    private static boolean wasPressed;
    private static float px, py, pw, ph;

    private static Object drag;          // SliderSetting или ColorDrag
    private static platform.api.module.setting.ColorSetting expColor;
    private static platform.api.module.setting.MultiModeSetting expMulti;
    private static platform.api.module.setting.BindSetting listenBind;
    private static platform.api.module.setting.StringSetting focusString;

    private static final class ColorDrag {
        final platform.api.module.setting.ColorSetting c;
        final int ch;
        ColorDrag(platform.api.module.setting.ColorSetting c, int ch) { this.c = c; this.ch = ch; }
    }

    private ModuleModal() {
    }

    public static void open(platform.api.module.Module m) {
        mod = m;
        drag = null; expColor = null; expMulti = null; listenBind = null; focusString = null;
    }

    public static void close() {
        mod = null; drag = null;
    }

    public static boolean isOpen() {
        return mod != null;
    }

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my, float dt) {
        if (mod == null) {
            return;
        }
        boolean pr = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean cl = pr && !wasPressed;
        wasPressed = pr;

        // захват клавиши для bind
        if (listenBind != null) {
            for (int k = 32; k < 340; k++) {
                if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 1) {
                    listenBind.a(k);
                    listenBind = null;
                    break;
                }
            }
        }
        // ввод строки
        if (focusString != null) {
            pollString(focusString);
        }
        if (!pr) {
            drag = null;
        }

        // высота считается в два прохода: сначала соберём структуру
        List<Object[]> rows = buildRows();
        float rowH = 14f, pad = 6f;
        pw = 176f;
        ph = pad * 2 + 16f + rows.size() * rowH + 4f;
        px = (sw - pw) / 2f;
        py = (sh - ph) / 2f;

        boolean inside = mx >= px && mx <= px + pw && my >= py && my <= py + ph;
        if (cl && !inside && drag == null) {
            close();
            return;
        }

        UiVector.roundRect(g, px, py, pw, ph, 7f, 0xF2141218);
        UiVector.outline(g, px, py, pw, ph, 7f, .5f, 0x33FFFFFF);
        MtsdfTextRenderer.draw(g, Fonts.MEDIUM, mod.j(), px + pad, py + pad, 9f, 0xFFFFFFFF);

        float cx = px + pw - 14f, cy = py + pad;
        if (in(mx, my, cx - 2, cy - 2, 12, 12)) {
            UiVector.roundRect(g, cx - 2, cy - 2, 12, 12, 4f, 0x1FFFFFFF);
        }
        MtsdfTextRenderer.draw(g, Fonts.REGULAR, "x", cx + 2f, cy, 8f, 0xB3FFFFFF);
        if (cl && in(mx, my, cx - 2, cy - 2, 12, 12)) {
            close();
            return;
        }

        float ry = py + pad + 16f;
        for (Object[] row : rows) {
            ry = drawRow(g, row, ry, rowH, mx, my, pr, cl);
        }
    }

    /** Собрать список строк: [kind, setting] с учётом раскрытий. */
    private static List<Object[]> buildRows() {
        List<Object[]> rows = new ArrayList<>();
        for (platform.api.module.setting.Setting<?> s : mod.e()) {
            if (s == null || (s.e() != null && !s.e().get())) {
                continue;
            }
            rows.add(new Object[]{kindOf(s), s});
            if (s instanceof platform.api.module.setting.ColorSetting cs && expColor == cs) {
                rows.add(new Object[]{10, cs}); rows.add(new Object[]{11, cs}); rows.add(new Object[]{12, cs});
            } else if (s instanceof platform.api.module.setting.MultiModeSetting mm && expMulti == mm) {
                for (platform.api.module.setting.BooleanSetting b : mm.c()) {
                    rows.add(new Object[]{20, b});
                }
            }
        }
        return rows;
    }

    private static int kindOf(platform.api.module.setting.Setting<?> s) {
        if (s instanceof platform.api.module.setting.BooleanSetting) return 0;
        if (s instanceof platform.api.module.setting.SliderSetting) return 1;
        if (s instanceof platform.api.module.setting.ModeSetting) return 2;
        if (s instanceof platform.api.module.setting.ColorSetting) return 3;
        if (s instanceof platform.api.module.setting.MultiModeSetting) return 4;
        if (s instanceof platform.api.module.setting.BindSetting) return 5;
        if (s instanceof platform.api.module.setting.StringSetting) return 6;
        if (s instanceof platform.api.module.setting.ButtonSetting) return 7;
        return 8;
    }

    private static float drawRow(GuiGraphicsExtractor g, Object[] row, float ry, float rowH,
                                 int mx, int my, boolean pr, boolean cl) {
        int kind = (Integer) row[0];
        Object o = row[1];
        boolean sub = kind >= 10;
        float ix = px + (sub ? 14 : 8);
        boolean hov = in(mx, my, px + 3, ry, pw - 6, rowH);
        if (hov && !sub) {
            UiVector.roundRect(g, px + 3, ry, pw - 6, rowH, 4f, 0x14FFFFFF);
        }

        switch (kind) {
            case 0, 20 -> {
                platform.api.module.setting.BooleanSetting bs = (platform.api.module.setting.BooleanSetting) o;
                label(g, bs.i(), ix, ry, sub ? 0xB3FFFFFF : 0xE6FFFFFF);
                float tx = px + pw - 20f, ty = ry + 3f;
                float t = bs.c() ? 1f : 0f;
                UiVector.roundRect(g, tx, ty, 14, 8, 4f, bs.c() ? 0xFF4FC3FF : 0x40FFFFFF);
                UiVector.roundRect(g, tx + (bs.c() ? 7 : 1), ty + 1, 6, 6, 3f, 0xFFFFFFFF);
                if (cl && hov) bs.a(!bs.c());
            }
            case 1 -> {
                platform.api.module.setting.SliderSetting ss = (platform.api.module.setting.SliderSetting) o;
                label(g, ss.i(), ix, ry, 0xE6FFFFFF);
                float tw2 = 46f, tx = px + pw - 8 - tw2, ty = ry + 6f;
                UiVector.roundRect(g, tx, ty, tw2, 2.5f, 1.2f, 0x40FFFFFF);
                float frac = (ss.c() - ss.a) / Math.max(1e-5f, ss.b - ss.a);
                UiVector.roundRect(g, tx, ty, Math.max(2.5f, tw2 * frac), 2.5f, 1.2f, 0xFF4FC3FF);
                String v = String.valueOf(ss.c());
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v, px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 6.5f), ry + 1f, 6.5f, 0xFFFF4FC3);
                if ((cl && hov) || drag == ss) {
                    drag = ss;
                    float f = clamp01((mx - tx) / tw2);
                    float val = ss.a + f * (ss.b - ss.a);
                    val = Math.round(val / ss.c) * ss.c;
                    ss.a(clamp(val, ss.a, ss.b));
                }
            }
            case 2 -> {
                platform.api.module.setting.ModeSetting ms = (platform.api.module.setting.ModeSetting) o;
                label(g, ms.i(), ix, ry, 0xE6FFFFFF);
                String v = ms.c();
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v, px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3.5f, 7f, 0xFFFF4FC3);
                if (cl && hov) {
                    List<String> modes = ms.k();
                    int i = (modes.indexOf(ms.c()) + 1) % modes.size();
                    ms.a(modes.get(i));
                }
            }
            case 3 -> {
                platform.api.module.setting.ColorSetting cs = (platform.api.module.setting.ColorSetting) o;
                label(g, cs.i(), ix, ry, 0xE6FFFFFF);
                int c = cs.c();
                UiVector.roundRect(g, px + pw - 20f, ry + 2.5f, 12, 9, 3f, c | 0xFF000000);
                UiVector.outline(g, px + pw - 20f, ry + 2.5f, 12, 9, 3f, .5f, 0x33FFFFFF);
                if (cl && hov) expColor = (expColor == cs) ? null : cs;
            }
            case 10, 11, 12 -> {
                platform.api.module.setting.ColorSetting cs = (platform.api.module.setting.ColorSetting) o;
                int ch = kind - 10;
                String nm = ch == 0 ? "R" : ch == 1 ? "G" : "B";
                label(g, nm, ix, ry, 0xB3FFFFFF);
                int c = cs.c();
                int val = (c >> (16 - ch * 8)) & 0xFF;
                float tw2 = 60f, tx = px + pw - 8 - tw2, ty = ry + 6f;
                UiVector.roundRect(g, tx, ty, tw2, 2.5f, 1.2f, 0x40FFFFFF);
                UiVector.roundRect(g, tx, ty, tw2 * (val / 255f), 2.5f, 1.2f, 0xFF4FC3FF);
                ColorDrag cd = new ColorDrag(cs, ch);
                if ((cl && hov) || (drag instanceof ColorDrag d && d.c == cs && d.ch == ch)) {
                    drag = cd;
                    int nv = (int) (clamp01((mx - tx) / tw2) * 255);
                    setC(cs, ch, nv);
                }
            }
            case 4 -> {
                platform.api.module.setting.MultiModeSetting mm = (platform.api.module.setting.MultiModeSetting) o;
                label(g, mm.i() + " (" + mm.c().size() + ")", ix, ry, 0xE6FFFFFF);
                if (cl && hov) expMulti = (expMulti == mm) ? null : mm;
            }
            case 5 -> {
                platform.api.module.setting.BindSetting bd = (platform.api.module.setting.BindSetting) o;
                label(g, bd.i(), ix, ry, 0xE6FFFFFF);
                String v = listenBind == bd ? "..." : keyName(bd.c());
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v, px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3.5f, 7f, 0xFFFF4FC3);
                if (cl && hov) listenBind = bd;
            }
            case 6 -> {
                platform.api.module.setting.StringSetting st = (platform.api.module.setting.StringSetting) o;
                label(g, st.i(), ix, ry, 0xE6FFFFFF);
                String v = st.c();
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v, px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3.5f, 7f, 0xFFFF4FC3);
                if (cl && hov) focusString = (focusString == st) ? null : st;
            }
            case 7 -> {
                platform.api.module.setting.ButtonSetting bt = (platform.api.module.setting.ButtonSetting) o;
                UiVector.roundRect(g, px + 8, ry + 1.5f, pw - 16, rowH - 3, 4f, hov ? 0x33FFFFFF : 0x1FFFFFFF);
                String nm = bt.i();
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, nm, px + 8 + (pw - 16 - MtsdfTextRenderer.width(Fonts.MEDIUM, nm, 7f)) / 2, ry + 3.5f, 7f, 0xE6FFFFFF);
                if (cl && hov) bt.k();
            }
            default -> label(g, ((platform.api.module.setting.Setting<?>) o).i(), ix, ry, 0xE6FFFFFF);
        }
        return ry + rowH;
    }

    private static void setC(platform.api.module.setting.ColorSetting cs, int ch, int nv) {
        int c = cs.c() | 0xFF000000;
        int r = (c >> 16) & 0xFF, gg = (c >> 8) & 0xFF, b = c & 0xFF;
        if (ch == 0) r = nv; else if (ch == 1) gg = nv; else b = nv;
        cs.a(0xFF000000 | (r << 16) | (gg << 8) | b);
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        return n == null ? "KEY" + key : n.toUpperCase();
    }

    private static void pollString(platform.api.module.setting.StringSetting st) {
        String cur = st.c();
        if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_BACKSPACE) == 1 && !cur.isEmpty()) {
            st.a(cur.substring(0, cur.length() - 1));
        }
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
            if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 2) {
                st.a(cur + (char) ('a' + (k - GLFW.GLFW_KEY_A)));
            }
        }
    }

    private static void label(GuiGraphicsExtractor g, String s, float x, float ry, int color) {
        MtsdfTextRenderer.draw(g, Fonts.REGULAR, s, x, ry + 3.5f, 7f, color);
    }

    private static float clamp01(float v) {
        return Math.max(0, Math.min(1, v));
    }

    private static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    private static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
