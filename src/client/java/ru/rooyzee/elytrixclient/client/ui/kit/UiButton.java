package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Кнопка кастомного GUI: скруглённая, с градиентом, «прожатием» и плавной подсветкой.
 * Иконка — из {@link UiIcon} (PNG 16×16), тонируется под цвет текста.
 */
public class UiButton extends UiWidget {

    public enum Style {
        /** Акцентная (главное действие). */
        PRIMARY,
        /** Обычная: подложка с рамкой. */
        SECONDARY,
        /** Без фона, только текст (мелкие действия). */
        GHOST,
        /** Опасное действие. */
        DANGER
    }

    private final String label;
    private UiIcon icon;
    private final Style style;
    private Runnable action;

    public UiButton(String label, Runnable action) {
        this(label, null, Style.SECONDARY, action);
    }

    public UiButton(String label, UiIcon icon, Style style, Runnable action) {
        super(UiTheme.ROW_H);
        this.label = label == null ? "" : label;
        this.icon = icon;
        this.style = style;
        this.action = action;
    }

    public UiButton height(int h) {
        this.h = h;
        return this;
    }

    public UiButton icon(UiIcon i) {
        this.icon = i;
        return this;
    }

    public UiButton onClick(Runnable r) {
        this.action = r;
        return this;
    }

    public String label() {
        return label;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        if (!visible) {
            return;
        }
        beginFrame(mouseX, mouseY, dt);
        int oy = pressT > 0.35f ? 1 : 0;

        int top;
        int bottom;
        int fg;
        switch (style) {
            case PRIMARY -> {
                float lift = hoverT;
                top = UiTheme.mix(UiTheme.accentLight(accent), 0xFFFFFFFF, 0.10f * lift);
                bottom = UiTheme.mix(UiTheme.accentDark(accent), accent, 0.35f * lift);
                fg = 0xFFFFFFFF;
            }
            case DANGER -> {
                top = UiTheme.mix(UiTheme.ERROR, 0xFFFFFFFF, 0.15f);
                bottom = UiTheme.mix(UiTheme.ERROR, 0xFF2A0B0B, 0.45f);
                fg = 0xFFFFFFFF;
            }
            case GHOST -> {
                top = bottom = 0;
                fg = UiTheme.mix(UiTheme.TEXT_DIM, UiTheme.TEXT, hoverT);
            }
            default -> {
                top = UiTheme.mix(UiTheme.ROW, UiTheme.ROW_HOVER, hoverT);
                bottom = top;
                fg = UiTheme.mix(UiTheme.TEXT_DIM, UiTheme.TEXT, hoverT);
            }
        }

        if (style == Style.GHOST) {
            if (hoverT > 0.01f) {
                UiDraw.roundRect(graphics, x, y, w, h, UiTheme.R_MD, UiTheme.withAlpha(0xFFFFFFFF, 0.08f * hoverT));
            }
        } else if (style == Style.SECONDARY) {
            int brd = UiTheme.mix(UiTheme.BORDER_SOFT, accent, hoverT * 0.6f);
            UiDraw.roundRect(graphics, x, y + oy, w, h, UiTheme.R_MD, fadeIn(brd));
            UiDraw.roundRect(graphics, x + 1, y + oy + 1, w - 2, h - 2, UiTheme.R_MD - 1, fadeIn(top));
        } else {
            if (style == Style.PRIMARY && hoverT > 0.01f) {
                UiDraw.glow(graphics, x, y + oy, w, h, UiTheme.R_MD, accent, 0.55f * hoverT);
            }
            UiDraw.roundRectGradient(graphics, x, y + oy, w, h, UiTheme.R_MD, fadeIn(top), fadeIn(bottom));
            UiDraw.roundRect(graphics, x + 1, y + oy + 1, w - 2, Math.max(1, h / 2 - 1), UiTheme.R_MD - 1,
                    UiTheme.withAlpha(0xFFFFFFFF, (0.10f + 0.06f * hoverT) * Math.max(0.05f, appear)));
        }

        var font = font();
        int ty = y + oy + (h - 8) / 2 + 1;
        int inner = w - 12;
        if (icon == null) {
            UiDraw.textCenter(graphics, font, trim(font, label, inner), x + w / 2, ty, fadeIn(fg));
        } else {
            int iconSize = 12;
            int textW = font.width(trim(font, label, inner - iconSize - 5));
            int total = textW + iconSize + 5;
            int startX = x + (w - total) / 2;
            icon.draw(graphics, startX, y + oy + (h - iconSize) / 2, iconSize, fadeIn(fg));
            UiDraw.text(graphics, font, trim(font, label, inner - iconSize - 5), startX + iconSize + 5, ty, fadeIn(fg));
        }
        endFrame();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || !visible || !contains(mx, my)) {
            return false;
        }
        pressT = 1f;
        if (action != null) {
            action.run();
        }
        return true;
    }
}
