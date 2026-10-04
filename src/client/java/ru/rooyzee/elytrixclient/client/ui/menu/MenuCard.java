package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static ru.rooyzee.elytrixclient.client.ui.menu.MenuKit.*;

/**
 * Карточка меню: шапка (название + бейдж-статус справа) и список {@link MenuRow}.
 * Карточки раскладываются экраном в две колонки по 160 единиц.
 */
public final class MenuCard {
    public static final float HEADER = 22;

    private final String title;
    private final List<MenuRow> rows = new ArrayList<>();
    private Supplier<String> badge;
    private int badgeColor;
    private float x;
    private float y;
    private float w;
    private float h;
    private float hoverT;
    /** 0…1 — анимация появления при переключении вкладки. */
    private float appear = 1f;
    private float delay;

    public MenuCard(String title) {
        this.title = title;
    }

    public MenuCard add(MenuRow row) {
        rows.add(row);
        return this;
    }

    /** Бейдж справа в шапке; {@code color = 0} — нейтральный, иначе цветная точка-статус. */
    public MenuCard badge(Supplier<String> text, int color) {
        this.badge = text;
        this.badgeColor = color;
        return this;
    }

    public String title() {
        return title;
    }

    public List<MenuRow> rows() {
        return rows;
    }

    public boolean matches(String query) {
        if (lower(title).contains(query)) {
            return true;
        }
        for (MenuRow row : rows) {
            if (row.matches(query)) {
                return true;
            }
        }
        return false;
    }

    /** Запустить анимацию появления с задержкой (каскад карточек). */
    public void replay(float delaySeconds) {
        appear = 0f;
        delay = delaySeconds;
    }

    public float layout(Font font, float x, float y, float w) {
        this.x = x;
        this.y = y;
        this.w = w;
        float off = HEADER + 2;
        for (MenuRow row : rows) {
            if (row.visible()) {
                row.layout(font, x, y + off, w);
                off += row.height();
            }
        }
        this.h = rows.isEmpty() ? HEADER : off + 4;
        return h;
    }

    public float height() {
        return h;
    }

    public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
        if (delay > 0f) {
            delay -= dt;
        } else {
            appear = approach(appear, 1f, 14f, dt);
        }
        if (appear <= 0.01f) {
            return;
        }
        float saved = MenuKit.alpha;
        MenuKit.alpha = saved * appear;
        float lift = (1f - appear) * 6f;
        float cy = y + lift;

        boolean hv = inside(mx, my, x, y, w, h);
        hoverT = approach(hoverT, hv ? 1f : 0f, 12f, dt);

        card(g, x, cy, w, h, 8, cardFill(), UiTheme.mix(cardEdge(), accent, 0.22f * hoverT));
        text(g, font, trim(font, title, BODY, w - 60), x + 10, ty(BODY, cy + HEADER / 2f + 0.5f), text(), BODY);

        if (badge != null) {
            String b = badge.get();
            if (b != null && !b.isEmpty()) {
                float bw = width(font, b, SMALL) + (badgeColor != 0 ? 14 : 8);
                float bx = x + w - 8 - bw;
                float by = cy + HEADER / 2f - 5.5f;
                fill(g, bx, by, bw, 11, 5.5f, field());
                float tx = bx + 4;
                if (badgeColor != 0) {
                    disc(g, bx + 6, by + 5.5f, 2f, badgeColor);
                    tx = bx + 10;
                }
                text(g, font, b, tx, ty(SMALL, by + 5.5f), soft(), SMALL);
            }
        }

        if (!rows.isEmpty()) {
            hline(g, x + 10, cy + HEADER, w - 20, divider());
        }

        if (lift > 0.05f) {
            g.pose().pushMatrix();
            g.pose().translate(0f, lift);
        }
        for (MenuRow row : rows) {
            if (row.visible()) {
                row.render(g, font, mx, my - lift, dt);
            }
        }
        if (lift > 0.05f) {
            g.pose().popMatrix();
        }
        MenuKit.alpha = saved;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        boolean handled = false;
        for (MenuRow row : rows) {
            if (row.visible() && row.mouseClicked(mx, my, button)) {
                handled = true;
            } else {
                row.blur();
            }
        }
        return handled || inside(mx, my, x, y, w, h);
    }

    public void mouseReleased(double mx, double my, int button) {
        for (MenuRow row : rows) {
            row.mouseReleased(mx, my, button);
        }
    }

    public boolean keyPressed(KeyEvent event) {
        for (MenuRow row : rows) {
            if (row.capturing() && row.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        for (MenuRow row : rows) {
            if (row.capturing() && row.charTyped(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean capturing() {
        for (MenuRow row : rows) {
            if (row.capturing()) {
                return true;
            }
        }
        return false;
    }

    public void blurAll() {
        for (MenuRow row : rows) {
            row.blur();
        }
    }
}
