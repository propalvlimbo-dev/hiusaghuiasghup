package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Карточка-раздел панели: заголовок, описание, «бейдж» справа и строки-виджеты внутри. */
public class UiSection extends UiWidget {

    private final String title;
    private final String subtitle;
    private String badge;
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

    public UiSection add(UiWidget widget) {
        children.add(widget);
        return this;
    }

    public List<UiWidget> children() {
        return children;
    }

    private int headerH() {
        int hh = 8;
        if (title != null) {
            hh += 12;
        }
        if (subtitle != null) {
            hh += 11;
        }
        return hh;
    }

    public int contentHeight() {
        int hh = headerH();
        for (int i = 0; i < children.size(); i++) {
            hh += children.get(i).h + (i < children.size() - 1 ? 6 : 0);
        }
        return hh + 8;
    }

    /** Раскладывает карточку и её строки. */
    public void layoutAt(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = contentHeight();
        int cy = y + headerH();
        for (UiWidget c : children) {
            c.place(x + 8, cy, w - 16);
            cy += c.h + 6;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
        UiDraw.roundRectBordered(graphics, x, y, w, h, UiTheme.R_LG, UiTheme.CARD, UiTheme.DIVIDER);

        var font = font();
        int ty = y + 8;
        if (title != null) {
            UiDraw.text(graphics, font, title, x + 10, ty, UiTheme.TEXT);
            if (badge != null) {
                int bw = font.width(badge) + 12;
                int bx = x + w - 10 - bw;
                UiDraw.roundRect(graphics, bx, ty - 3, bw, 14, UiTheme.R_SM, UiTheme.accentSoft(accent, 0.20f));
                UiDraw.textCenter(graphics, font, badge, bx + bw / 2, ty, UiTheme.mix(accent, 0xFFFFFFFF, 0.40f));
            }
            ty += 12;
        }
        if (subtitle != null) {
            UiDraw.text(graphics, font, subtitle, x + 10, ty, UiTheme.TEXT_DIM);
        }

        for (UiWidget c : children) {
            c.accent = accent;
            c.render(graphics, mouseX, mouseY, dt);
        }
    }
}
