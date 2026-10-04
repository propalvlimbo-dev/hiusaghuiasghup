package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Выпадающий список: строка с подписью и «чипом» выбранного значения.
 * Список раскрывается плавно (растёт и проявляется), стрелка поворачивается.
 */
public class UiDropdown extends UiWidget {

    private static final int ITEM_H = 18;
    private static final int PAD = 5;

    private final String label;
    private final List<String> options;
    private final Consumer<Integer> onChange;
    private int index;
    private boolean open;
    private float openT;

    public UiDropdown(String label, List<String> options, int index, Consumer<Integer> onChange) {
        super(UiTheme.ROW_H);
        this.label = label;
        this.options = options;
        this.index = Math.max(0, Math.min(options.size() - 1, index));
        this.onChange = onChange;
    }

    public int index() {
        return index;
    }

    public void setIndex(int i) {
        this.index = Math.max(0, Math.min(options.size() - 1, i));
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public void replay(float delay) {
        super.replay(delay);
        openT = open ? 1f : 0f;
    }

    @Override
    public boolean hasPopup() {
        return open;
    }

    private int chipW() {
        var font = font();
        int widest = 0;
        for (String s : options) {
            widest = Math.max(widest, UiDraw.width(font, s));
        }
        return Math.max(widest, UiDraw.width(font, label)) + 2 * PAD + 16;
    }

    private int chipX() {
        return x + w - 10 - chipW();
    }

    private int listH() {
        return options.size() * ITEM_H + 8;
    }

    private int popupTop() {
        int below = y + h + 4;
        int top = below + listH() <= popupLimitBottom ? below : y - 4 - listH();
        return Math.max(popupLimitTop + 2, top);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!visible) {
            return;
        }
        beginFrame(mouseX, mouseY, dt);
        openT += ((open ? 1f : 0f) - openT) * (ANIMATIONS ? Math.min(1f, dt * 15f) : 1f);
        if (!open && openT < 0.02f) {
            openT = 0f;
        }
        if (open && openT > 0.98f) {
            openT = 1f;
        }

        int bg = UiTheme.mix(UiTheme.ROW, UiTheme.ROW_HOVER, Math.max(hoverT, open ? 1f : 0f));
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, fadeIn(bg));

        var font = font();
        UiDraw.text(graphics, font, trim(font, label, chipX() - x - 14), x + 10, y + (h - 8) / 2 + 1, fadeIn(UiTheme.TEXT));

        int cw = chipW();
        int cx = chipX();
        int chipBg = UiTheme.mix(UiTheme.TRACK, UiTheme.accentSoft(accent, 0.30f), Math.max(hoverT * 0.5f, openT));
        UiDraw.roundRect(graphics, cx, y + 4, cw, h - 8, UiTheme.R_SM, fadeIn(chipBg));
        UiDraw.text(graphics, font, trim(font, options.get(index), cw - 26), cx + PAD, y + (h - 8) / 2 + 1, fadeIn(UiTheme.TEXT_SOFT));

        // поворачивающаяся «галочка»: две линии, угол зависит от openT
        float ccx = cx + cw - 11;
        float ccy = y + h / 2f;
        float vy = 2f - 4f * openT;
        UiDraw.line(graphics, ccx - 3.5f, ccy - vy, ccx, ccy + vy, 1.6f, fadeIn(UiTheme.TEXT_DIM));
        UiDraw.line(graphics, ccx, ccy + vy, ccx + 3.5f, ccy - vy, 1.6f, fadeIn(UiTheme.TEXT_DIM));
        endFrame();
    }

    @Override
    public boolean popupContains(double mx, double my) {
        if (!open) {
            return false;
        }
        int top = popupTop();
        return mx >= chipX() && mx < chipX() + chipW() && my >= top && my < top + listH();
    }

    @Override
    public void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (openT <= 0.02f) {
            return;
        }
        int cx = chipX();
        int cw = chipW();
        int top = popupTop();
        int full = listH();
        int height = Math.max(8, Math.round(full * Math.min(1f, openT * 1.2f)));
        float a = Math.min(1f, openT * 1.4f);

        UiDraw.shadow(graphics, cx, top, cw, height, UiTheme.R_MD, 4, UiTheme.withAlpha(0xFF000000, 0.30f * a));
        UiDraw.roundRectBordered(graphics, cx, top, cw, height, UiTheme.R_MD,
                UiTheme.withAlpha(UiTheme.POPUP, a), UiTheme.withAlpha(UiTheme.BORDER, a));

        var font = font();
        for (int i = 0; i < options.size(); i++) {
            int iy = top + 4 + i * ITEM_H;
            if (iy + ITEM_H > top + height) {
                break;
            }
            boolean ih = mouseX >= cx + 2 && mouseX < cx + cw - 2 && mouseY >= iy && mouseY < iy + ITEM_H;
            if (ih) {
                UiDraw.roundRect(graphics, cx + 3, iy, cw - 6, ITEM_H, UiTheme.R_SM, UiTheme.withAlpha(UiTheme.ROW_HOVER, a));
            }
            if (i == index) {
                UiDraw.roundRect(graphics, cx + 3, iy + 2, 2, ITEM_H - 4, 1, UiTheme.withAlpha(accent, a));
            }
            UiDraw.text(graphics, font, trim(font, options.get(i), cw - 14), cx + PAD + 3, iy + (ITEM_H - 8) / 2 + 1,
                    UiTheme.withAlpha(i == index ? UiTheme.TEXT : UiTheme.TEXT_SOFT, a));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || !visible) {
            return false;
        }
        if (open) {
            if (popupContains(mx, my)) {
                int top = popupTop();
                int i = (int) ((my - top - 4) / ITEM_H);
                i = Math.max(0, Math.min(options.size() - 1, i));
                if (i != index) {
                    index = i;
                    if (onChange != null) {
                        onChange.accept(index);
                    }
                }
            }
            open = false;
            return true;
        }
        if (contains(mx, my)) {
            open = true;
            return true;
        }
        return false;
    }
}
