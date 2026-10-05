package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Окно настроек xrose-фичи — полный аналог {@link ModuleModal}, но для
 * настроек xrose (Boolean/Number/Mode/Color/MultiSelect/Bind/Text/Button).
 * Родное меню xrose убрано по решению пользователя — настройки живут здесь.
 */
public final class XroseModal {
    private static org.xrose.feature.Feature feature;
    private static boolean wasPressed;
    private static float px, py, pw, ph;

    private static Object drag;
    private static org.xrose.feature.setting.ColorSetting expColor;
    private static org.xrose.feature.setting.MultiSelectSetting expMulti;
    private static org.xrose.feature.setting.BindSetting listenBind;
    private static org.xrose.feature.setting.InputBindSetting listenInput;
    private static org.xrose.feature.setting.TextSetting focusText;

    private static final class ColorDrag {
        final org.xrose.feature.setting.ColorSetting c;
        final int ch;
        ColorDrag(org.xrose.feature.setting.ColorSetting c, int ch) { this.c = c; this.ch = ch; }
    }

    private XroseModal() {
    }

    public static void open(org.xrose.feature.Feature f) {
        feature = f;
        drag = null; expColor = null; expMulti = null; listenBind = null; listenInput = null; focusText = null;
    }

    public static void close() {
        feature = null; drag = null;
    }

    public static boolean isOpen() {
        return feature != null;
    }

    public static void render(GuiGraphicsExtractor g, int sw, int sh, int mx, int my, float dt) {
        if (feature == null) {
            return;
        }
        boolean pr = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), 0) == 1;
        boolean cl = pr && !wasPressed;
        wasPressed = pr;

        if (listenBind != null) {
            for (int k = 32; k < 340; k++) {
                if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 1) {
                    listenBind.setValue(List.of(org.xrose.feature.setting.BindSetting.key(k)));
                    listenBind = null;
                    break;
                }
            }
            if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_ESCAPE) == 1) {
                listenBind.setValue(List.of());
                listenBind = null;
            }
        }
        if (listenInput != null) {
            for (int k = 32; k < 340; k++) {
                if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 1) {
                    listenInput.setKey(k);
                    listenInput = null;
                    break;
                }
            }
            if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_ESCAPE) == 1) {
                listenInput.setKey(org.xrose.feature.setting.InputBindSetting.UNBOUND);
                listenInput = null;
            }
        }
        if (focusText != null) {
            pollText(focusText);
        }
        if (!pr) {
            drag = null;
        }

        List<Object[]> rows = buildRows();
        float pad = 6f;
        pw = 190f;
        float content = 0;
        for (Object[] r : rows) content += (Float) r[2];
        ph = pad * 2 + 16f + content + 4f;
        px = (sw - pw) / 2f;
        py = (sh - ph) / 2f;

        boolean inside = mx >= px && mx <= px + pw && my >= py && my <= py + ph;
        if (cl && !inside && drag == null) {
            close();
            return;
        }

        // Размытие фона — как в меню xrose, панель наша.
        try {
            org.xrose.utils.render.gui.Render2DUtil.rect(0, 0, sw, sh)
                    .color(0x66000000).blur(20f).draw();
        } catch (Throwable ignored) {
        }

        UiVector.roundRect(g, px, py, pw, ph, 10f, 0xF2141218);
        UiVector.outline(g, px, py, pw, ph, 10f, .5f, 0x33FFFFFF);
        UiVector.rect(g, px + 8, py + 15f, pw - 16, 1f, (MenuKit.accent & 0xFFFFFF) | 0x66000000);
        MtsdfTextRenderer.draw(g, Fonts.MEDIUM, feature.getName(), px + pad, py + pad, 9f, 0xFFFFFFFF);

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
            ry = drawRow(g, row, ry, mx, my, pr, cl);
        }
    }

    private static List<Object[]> buildRows() {
        List<Object[]> rows = new ArrayList<>();
        for (org.xrose.feature.setting.Setting<?> s : feature.getSettings()) {
            if (s == null || !s.isVisible()) {
                continue;
            }
            rows.add(new Object[]{kindOf(s), s, heightOf(kindOf(s))});
            if (s instanceof org.xrose.feature.setting.ColorSetting cs && expColor == cs) {
                rows.add(new Object[]{10, cs, 14f});
                rows.add(new Object[]{11, cs, 14f});
                rows.add(new Object[]{12, cs, 14f});
            } else if (s instanceof org.xrose.feature.setting.MultiSelectSetting mm && expMulti == mm) {
                for (String opt : mm.getOptions()) {
                    rows.add(new Object[]{20, mm, 14f, opt});
                }
            }
        }
        return rows;
    }

    private static float heightOf(int kind) {
        return kind == 1 ? 20f : 14f;
    }

    private static int kindOf(org.xrose.feature.setting.Setting<?> s) {
        if (s instanceof org.xrose.feature.setting.BooleanSetting) return 0;
        if (s instanceof org.xrose.feature.setting.NumberSetting) return 1;
        if (s instanceof org.xrose.feature.setting.ModeSetting) return 2;
        if (s instanceof org.xrose.feature.setting.ColorSetting) return 3;
        if (s instanceof org.xrose.feature.setting.MultiSelectSetting) return 4;
        if (s instanceof org.xrose.feature.setting.BindSetting) return 5;
        if (s instanceof org.xrose.feature.setting.TextSetting) return 6;
        if (s instanceof org.xrose.feature.setting.ButtonSetting) return 7;
        if (s instanceof org.xrose.feature.setting.InputBindSetting) return 8;
        return 9;
    }

    @SuppressWarnings("unchecked")
    private static float drawRow(GuiGraphicsExtractor g, Object[] row, float ry,
                                 int mx, int my, boolean pr, boolean cl) {
        int kind = (Integer) row[0];
        float rh = (Float) row[2];
        boolean sub = kind >= 10 && kind <= 20 && kind != 8;
        float ix = px + (sub ? 16 : 8);
        boolean hov = in(mx, my, px + 3, ry, pw - 6, rh);
        if (hov && !sub) {
            UiVector.roundRect(g, px + 3, ry, pw - 6, rh, 4f, 0x14FFFFFF);
        }

        switch (kind) {
            case 0 -> {
                org.xrose.feature.setting.BooleanSetting bs = (org.xrose.feature.setting.BooleanSetting) row[1];
                label(g, bs.getName(), ix, ry + 3.5f, 0xE6FFFFFF);
                float tx = px + pw - 22f, ty = ry + 3f;
                UiVector.roundRect(g, tx, ty, 14, 8, 4f, bs.getValue() ? 0xFF4FC3FF : 0x40FFFFFF);
                UiVector.roundRect(g, tx + (bs.getValue() ? 7 : 1), ty + 1, 6, 6, 3f, 0xFFFFFFFF);
                if (cl && hov) bs.setValue(!bs.getValue());
            }
            case 1 -> {
                org.xrose.feature.setting.NumberSetting ns = (org.xrose.feature.setting.NumberSetting) row[1];
                label(g, ns.getName(), ix, ry + 3f, 0xE6FFFFFF);
                String v = ns.getDisplayValue();
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v,
                        px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3f, 7f, 0xFFFF4FC3);
                float tx = px + 8, tw2 = pw - 16, ty = ry + 13f;
                UiVector.roundRect(g, tx, ty, tw2, 3f, 1.5f, 0x40FFFFFF);
                float min = (float) ns.getMin(), max = (float) ns.getMax(), step = (float) ns.getStep();
                float frac = clamp01((ns.getFloat() - min) / Math.max(1e-5f, max - min));
                UiVector.roundRect(g, tx, ty, Math.max(3f, tw2 * frac), 3f, 1.5f, 0xFF4FC3FF);
                UiVector.roundRect(g, tx + tw2 * frac - 1.5f, ty - 1.5f, 6, 6, 3f, 0xFFFFFFFF);
                if ((cl && hov) || drag == ns) {
                    drag = ns;
                    double val = min + clamp01((mx - tx) / tw2) * (max - min);
                    if (step > 0) val = Math.round(val / step) * step;
                    ns.setValue(clamp(val, min, max));
                }
            }
            case 2 -> {
                org.xrose.feature.setting.ModeSetting ms = (org.xrose.feature.setting.ModeSetting) row[1];
                label(g, ms.getName(), ix, ry + 3.5f, 0xE6FFFFFF);
                String v = ms.getValue();
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v,
                        px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3.5f, 7f, 0xFFFF4FC3);
                if (cl && hov) {
                    List<String> modes = ms.getModes();
                    ms.setValue(modes.get((modes.indexOf(ms.getValue()) + 1) % modes.size()));
                }
            }
            case 3 -> {
                org.xrose.feature.setting.ColorSetting cs = (org.xrose.feature.setting.ColorSetting) row[1];
                label(g, cs.getName(), ix, ry + 3.5f, 0xE6FFFFFF);
                UiVector.roundRect(g, px + pw - 22f, ry + 2.5f, 14, 9, 3f, cs.getValue() | 0xFF000000);
                UiVector.outline(g, px + pw - 22f, ry + 2.5f, 14, 9, 3f, .5f, 0x33FFFFFF);
                if (cl && hov) expColor = (expColor == cs) ? null : cs;
            }
            case 10, 11, 12 -> {
                org.xrose.feature.setting.ColorSetting cs = (org.xrose.feature.setting.ColorSetting) row[1];
                int ch = kind - 10;
                label(g, ch == 0 ? "R" : ch == 1 ? "G" : "B", ix, ry + 3.5f, 0xB3FFFFFF);
                int val = (cs.getValue() >> (16 - ch * 8)) & 0xFF;
                float tx = px + 30, tw2 = pw - 30 - 34, ty = ry + 6f;
                UiVector.roundRect(g, tx, ty, tw2, 2.5f, 1.2f, 0x40FFFFFF);
                UiVector.roundRect(g, tx, ty, tw2 * (val / 255f), 2.5f, 1.2f, 0xFF4FC3FF);
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, String.valueOf(val),
                        px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, String.valueOf(val), 6.5f), ry + 3f, 6.5f, 0xB3FFFFFF);
                if ((cl && hov) || (drag instanceof ColorDrag d && d.c == cs && d.ch == ch)) {
                    drag = new ColorDrag(cs, ch);
                    setC(cs, ch, (int) (clamp01((mx - tx) / tw2) * 255));
                }
            }
            case 4 -> {
                org.xrose.feature.setting.MultiSelectSetting mm = (org.xrose.feature.setting.MultiSelectSetting) row[1];
                label(g, mm.getName() + "  ·  " + mm.getValue().size(), ix, ry + 3.5f, 0xE6FFFFFF);
                if (cl && hov) expMulti = (expMulti == mm) ? null : mm;
            }
            case 20 -> {
                org.xrose.feature.setting.MultiSelectSetting mm = (org.xrose.feature.setting.MultiSelectSetting) row[1];
                String opt = (String) row[3];
                boolean sel = mm.isSelected(opt);
                label(g, opt, ix, ry + 3.5f, sel ? 0xE6FFFFFF : 0xB3FFFFFF);
                float tx = px + pw - 22f, ty = ry + 3f;
                UiVector.roundRect(g, tx, ty, 14, 8, 4f, sel ? 0xFF4FC3FF : 0x40FFFFFF);
                UiVector.roundRect(g, tx + (sel ? 7 : 1), ty + 1, 6, 6, 3f, 0xFFFFFFFF);
                if (cl && hov) mm.toggle(opt);
            }
            case 5 -> {
                org.xrose.feature.setting.BindSetting bd = (org.xrose.feature.setting.BindSetting) row[1];
                label(g, bd.getName(), ix, ry + 3.5f, 0xE6FFFFFF);
                String v;
                if (listenBind == bd) {
                    v = "...";
                } else if (bd.isBound() && org.xrose.feature.setting.BindSetting.isMouse(bd.get(0))) {
                    v = "MOUSE" + (org.xrose.feature.setting.BindSetting.rawButton(bd.get(0)) + 1);
                } else if (bd.isBound()) {
                    v = keyName(bd.get(0));
                } else {
                    v = "нет";
                }
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v,
                        px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3.5f, 7f, 0xFFFF4FC3);
                if (cl && hov) listenBind = bd;
            }
            case 6 -> {
                org.xrose.feature.setting.TextSetting ts = (org.xrose.feature.setting.TextSetting) row[1];
                label(g, ts.getName(), ix, ry + 3.5f, 0xE6FFFFFF);
                String v = ts.getValue();
                if (ts.isSecret()) v = "•".repeat(v.length());
                if (v.isEmpty()) v = "...";
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v,
                        px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3.5f, 7f, 0xFFFF4FC3);
                if (cl && hov) focusText = (focusText == ts) ? null : ts;
            }
            case 7 -> {
                org.xrose.feature.setting.ButtonSetting bt = (org.xrose.feature.setting.ButtonSetting) row[1];
                UiVector.roundRect(g, px + 8, ry + 1.5f, pw - 16, rh - 3, 4f, hov ? 0x33FFFFFF : 0x1FFFFFFF);
                String nm = bt.getButtonLabel();
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, nm,
                        px + 8 + (pw - 16 - MtsdfTextRenderer.width(Fonts.MEDIUM, nm, 7f)) / 2, ry + 3.5f, 7f, 0xE6FFFFFF);
                if (cl && hov) bt.press();
            }
            case 8 -> {
                org.xrose.feature.setting.InputBindSetting ib = (org.xrose.feature.setting.InputBindSetting) row[1];
                label(g, ib.getName(), ix, ry + 3.5f, 0xE6FFFFFF);
                String v = listenInput == ib ? "..."
                        : (ib.isBound() ? keyName(ib.getValue()) : "нет");
                MtsdfTextRenderer.draw(g, Fonts.MEDIUM, v,
                        px + pw - 8 - MtsdfTextRenderer.width(Fonts.MEDIUM, v, 7f), ry + 3.5f, 7f, 0xFFFF4FC3);
                if (cl && hov) listenInput = ib;
            }
            default -> label(g, ((org.xrose.feature.setting.Setting<?>) row[1]).getName(), ix, ry + 3.5f, 0xE6FFFFFF);
        }
        return ry + rh;
    }

    private static void setC(org.xrose.feature.setting.ColorSetting cs, int ch, int nv) {
        int c = cs.getValue() | 0xFF000000;
        int r = (c >> 16) & 0xFF, gg = (c >> 8) & 0xFF, b = c & 0xFF;
        if (ch == 0) r = nv; else if (ch == 1) gg = nv; else b = nv;
        cs.setValue(0xFF000000 | (r << 16) | (gg << 8) | b);
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        return n == null ? "KEY" + key : n.toUpperCase();
    }

    private static void pollText(org.xrose.feature.setting.TextSetting ts) {
        String cur = ts.getValue();
        if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_BACKSPACE) == 1 && !cur.isEmpty()) {
            ts.setValue(cur.substring(0, cur.length() - 1));
        }
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
            if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 2 && cur.length() < ts.getMaxLength()) {
                ts.setValue(cur + (char) ('a' + (k - GLFW.GLFW_KEY_A)));
            }
        }
        for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) {
            if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 2 && cur.length() < ts.getMaxLength()) {
                ts.setValue(cur + (char) ('0' + (k - GLFW.GLFW_KEY_0)));
            }
        }
    }

    private static void label(GuiGraphicsExtractor g, String s, float x, float y, int color) {
        MtsdfTextRenderer.draw(g, Fonts.REGULAR, s, x, y, 7f, color);
    }

    private static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    private static float clamp01(float v) {
        return Math.max(0, Math.min(1, v));
    }

    private static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
