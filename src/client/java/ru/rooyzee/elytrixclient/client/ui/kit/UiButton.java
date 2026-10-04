package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Кнопка кастомного GUI: скруглённая, с состояниями hover/press и акцентным градиентом. */
public class UiButton extends UiWidget {

    public enum Style {
        /** Акцентная (главное действие). */
        PRIMARY,
        /** Обычная: тёмная подложка с рамкой. */
        SECONDARY,
        /** Без фона, только текст (мелкие действия). */
        GHOST,
        /** Опасное действие. */
        DANGER
    }

    private final String label;
    private final UiDraw.Icon icon;
    private final Style style;
    private final Runnable action;
    private float press;

    public UiButton(String label, Runnable action) {
        this(label, null, Style.SECONDARY, action);
    }

    public UiButton(String label, UiDraw.Icon icon, Style style, Runnable action) {
        super(UiTheme.ROW_H);
        this.label = label;
        this.icon = icon;
        this.style = style;
        this.action = action;
    }

    public UiButton height(int h) {
        this.h = h;
        return this;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!visible) {
            return;
        }
        hovered = enabled && contains(mouseX, mouseY);
        press = Math.max(0f, press - dt * 6f);
        int oy = press > 0.35f ? 1 : 0;

        int top;
        int bottom;
        int fg;
        switch (style) {
            case PRIMARY -> {
                top = UiTheme.accentLight(accent);
                bottom = UiTheme.accentDark(accent);
                fg = 0xFFFFFFFF;
            }
            case DANGER -> {
                top = UiTheme.mix(UiTheme.ERROR, 0xFFFFFFFF, 0.15f);
                bottom = UiTheme.mix(UiTheme.ERROR, 0xFF2A0B0B, 0.45f);
                fg = 0xFFFFFFFF;
            }
            case GHOST -> {
                top = bottom = 0;
                fg = hovered ? UiTheme.TEXT : UiTheme.TEXT_SOFT;
            }
            default -> {
                top = hovered ? UiTheme.ROW_HOVER : UiTheme.ROW;
                bottom = top;
                fg = hovered ? UiTheme.TEXT : UiTheme.TEXT_SOFT;
            }
        }
        if (style == Style.GHOST) {
            if (hovered) {
                UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, UiTheme.withAlpha(0xFFFFFFFF, 0.07f));
            }
        } else if (style == Style.SECONDARY) {
            UiDraw.roundRect(graphics, x, y + oy, w, h, UiTheme.R_MD, hovered ? UiTheme.BORDER : UiTheme.BORDER_SOFT);
            UiDraw.roundRect(graphics, x + 1, y + oy + 1, w - 2, h - 2, UiTheme.R_MD - 1, top);
        } else {
            UiDraw.roundRectGradient(graphics, x, y + oy, w, h, UiTheme.R_MD, top, bottom);
            UiDraw.roundRect(graphics, x + 1, y + oy + 1, w - 2, Math.max(1, h / 2 - 1), UiTheme.R_MD - 1,
                    UiTheme.withAlpha(0xFFFFFFFF, 0.10f));
        }

        var font = font();
        int ty = y + oy + (h - 8) / 2 + 1;
        if (icon == null) {
            UiDraw.textCenter(graphics, font, label, x + w / 2, ty, fg);
        } else {
            int iconSize = 11;
            int total = font.width(label) + iconSize + 4;
            int startX = x + (w - total) / 2;
            UiDraw.icon(graphics, icon, startX, y + oy + (h - iconSize) / 2, iconSize, fg);
            UiDraw.text(graphics, font, label, startX + iconSize + 4, ty, fg);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || !visible || !contains(mx, my)) {
            return false;
        }
        press = 1f;
        if (action != null) {
            action.run();
        }
        return true;
    }
}
