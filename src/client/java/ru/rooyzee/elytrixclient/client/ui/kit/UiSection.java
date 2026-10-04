package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Карточка-раздел панели: заголовок, описание, «бейдж» справа и строки-виджеты внутри. */
public class UiSection extends UiWidget {

    private final String title;
    private final String subtitle;
    private String badge;
    private UiIcon icon;
    private final List<UiWidget> children = new ArrayList<>();

    public UiSection(String title, String subtitle) {
        super(0);
        this.title = title;
        this.subtitle = subtitle;
    }

    public UiSection badge(String text) {
        this.badge = text;
        return this;
    }

    public UiSection icon(UiIcon i) {
        this.icon = i;
        return this;
    }

    public UiSection add(UiWidget widget) {
        children.add(widget);
        return this;
    }

    public List<UiWidget> children() {
        return children;
    }

    @Override
    public void replay(float delay) {
        super.replay(delay);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).replay(delay + 0.05f * (i + 1));
        }
    }

    private int headerH() {
        int hh = 12;
        if (title != null) {
            hh += 16;
        }
        if (subtitle != null) {
            hh += 13;
        }
        return hh + 4;
    }

    public int contentHeight() {
        int hh = headerH();
        for (int i = 0; i < children.size(); i++) {
            hh += children.get(i).h + (i < children.size() - 1 ? 6 : 0);
        }
        return hh + 13;
    }

    /** Раскладывает карточку и её строки. */
    public void layoutAt(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = contentHeight();
        int cy = y + headerH();
        for (UiWidget c : children) {
            c.place(x + 13, cy, w - 26);
            cy += c.h + 6;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        tick(dt, false);
        UiDraw.roundRectBordered(graphics, x, y, w, h, UiTheme.R_LG, fadeIn(UiTheme.CARD),
                fadeIn(UiTheme.BORDER_SOFT));

        var font = font();
        int ty = y + 13;
        int titleX = x + 14;
        if (icon != null) {
            icon.draw(graphics, titleX, ty - 4, 16, fadeIn(UiTheme.mix(accent, UiTheme.TEXT, 0.55f)));
            titleX += 23;
        }
        if (title != null) {
            UiDraw.text(graphics, font, title, titleX, ty, fadeIn(UiTheme.TEXT));
            if (badge != null) {
                int bw = UiDraw.width(font, badge, UiText.MONO) + 14;
                int bx = x + w - 14 - bw;
                UiDraw.roundRect(graphics, bx, ty - 5, bw, 16, UiTheme.R_SM,
                        fadeIn(UiTheme.accentSoft(accent, 0.22f)));
                UiDraw.textCenter(graphics, font, badge, bx + bw / 2, ty,
                        fadeIn(UiTheme.mix(accent, 0xFFFFFFFF, 0.40f)), UiText.MONO);
            }
            ty += 16;
        }
        if (subtitle != null) {
            UiDraw.text(graphics, font, subtitle, x + 14, ty, fadeIn(UiTheme.TEXT_DIM));
        }

        // хайрлайн между шапкой и строками + между строками (стиль списков Apple)
        UiDraw.hLine(graphics, x + 14, x + w - 14, y + headerH() - 7, 1, fadeIn(UiTheme.DIVIDER));
        for (int i = 0; i < children.size(); i++) {
            UiWidget c = children.get(i);
            c.accent = accent;
            c.render(graphics, mouseX, mouseY, dt);
            if (i < children.size() - 1) {
                UiDraw.hLine(graphics, x + 14, x + w - 14, c.y + c.h + 3, 1, fadeIn(UiTheme.DIVIDER));
            }
        }
    }
}
