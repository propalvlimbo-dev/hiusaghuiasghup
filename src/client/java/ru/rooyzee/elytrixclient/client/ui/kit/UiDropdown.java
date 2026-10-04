package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.Consumer;

/** Выпадающий список: строка с подписью и «чипом» выбранного значения. */
public class UiDropdown extends UiWidget {

    private static final int ITEM_H = 18;
    private static final int PAD = 5;

    private final String label;
    private final List<String> options;
    private final Consumer<Integer> onChange;
    private int index;
    private boolean open;

    /** Границы, за которые попап не должен вылезать (проставляет экран). */
    public int popupLimitTop = 0;
    public int popupLimitBottom = Integer.MAX_VALUE;

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
    public boolean hasPopup() {
        return open;
    }

    private int chipW() {
        var font = font();
        int widest = 0;
        for (String s : options) {
            widest = Math.max(widest, font.width(s));
        }
        return Math.max(widest, font.width(label)) + 2 * PAD + 14;
    }

    private int chipX() {
        return x + w - 10 - chipW();
    }

    private int popupTop() {
        int listH = options.size() * ITEM_H + 8;
        int below = y + h + 4;
        int top = below + listH <= popupLimitBottom ? below : y - 4 - listH;
        return Math.max(popupLimitTop + 2, top);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!visible) {
            return;
        }
        hovered = enabled && contains(mouseX, mouseY);
        UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, hovered || open ? UiTheme.ROW_HOVER : UiTheme.ROW);

        var font = font();
        UiDraw.text(graphics, font, label, x + 10, y + (h - 8) / 2 + 1, UiTheme.TEXT);

        int cw = chipW();
        int cx = chipX();
        UiDraw.roundRect(graphics, cx, y + 4, cw, h - 8, UiTheme.R_SM, open ? UiTheme.mix(UiTheme.TRACK, accent, 0.25f) : UiTheme.TRACK);
        UiDraw.text(graphics, font, options.get(index), cx + PAD, y + (h - 8) / 2 + 1, UiTheme.TEXT_SOFT);
        UiDraw.icon(graphics, UiDraw.Icon.CHEVRON, cx + cw - 16, y + (h - 10) / 2, 10, UiTheme.TEXT_DIM);
    }

    @Override
    public boolean popupContains(double mx, double my) {
        if (!open) {
            return false;
        }
        int top = popupTop();
        int height = options.size() * ITEM_H + 8;
        return mx >= chipX() && mx < chipX() + chipW() && my >= top && my < top + height;
    }

    @Override
    public void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!open) {
            return;
        }
        int cx = chipX();
        int cw = chipW();
        int top = popupTop();
        int height = options.size() * ITEM_H + 8;
        UiDraw.shadow(graphics, cx, top, cw, height, UiTheme.R_MD, 4, UiTheme.withAlpha(0xFF000000, 0.35f));
        UiDraw.roundRectBordered(graphics, cx, top, cw, height, UiTheme.R_MD, UiTheme.POPUP, UiTheme.BORDER);
        var font = font();
        for (int i = 0; i < options.size(); i++) {
            int iy = top + 4 + i * ITEM_H;
            boolean ih = mouseX >= cx + 2 && mouseX < cx + cw - 2 && mouseY >= iy && mouseY < iy + ITEM_H;
            if (ih) {
                UiDraw.roundRect(graphics, cx + 3, iy, cw - 6, ITEM_H, UiTheme.R_SM, UiTheme.ROW_HOVER);
            }
            if (i == index) {
                UiDraw.roundRect(graphics, cx + 3, iy, 2, ITEM_H, 1, accent);
            }
            UiDraw.text(graphics, font, options.get(i), cx + PAD + 3, iy + (ITEM_H - 8) / 2 + 1,
                    i == index ? UiTheme.TEXT : UiTheme.TEXT_SOFT);
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
