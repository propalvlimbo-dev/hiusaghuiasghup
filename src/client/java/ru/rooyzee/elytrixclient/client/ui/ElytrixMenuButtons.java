package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiIcon;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;
import ru.rooyzee.elytrixclient.client.ui.menu.MenuKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Главное меню в стиле клиента: кнопки — вертикальный список слева (как боковая
 * панель macOS) с иконками. В покое — только иконка и текст; при наведении слева
 * «выезжает» стеклянная плашка с акцентной полоской, текст сдвигается и светлеет.
 * При открытии меню пункты появляются каскадом. Маленькие кнопки (язык,
 * доступность) — круглые стеклянные под списком.
 *
 * <p>Подключается миксинами {@code TitleScreenMixin} (раскладка) и
 * {@code AbstractButtonMixin} (отрисовка), только на {@link TitleScreen}.
 */
public final class ElytrixMenuButtons {
    private ElytrixMenuButtons() {
    }

    public static final int ITEM_W = 150;
    public static final int ITEM_H = 20;
    public static final int GAP = 3;
    public static final int LOGO = 40;

    private static final class State {
        float hover;
        long last;
        int index;
        UiIcon icon;
        boolean danger;
    }

    private static final Map<AbstractButton, State> STATES = new WeakHashMap<>();
    private static long openedAt;
    private static int left = 32;
    private static int stackTop = 80;

    public static boolean active() {
        if (!ElytrixclientClient.CONFIG.hackerBackground) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gui != null && mc.gui.screen() instanceof TitleScreen;
    }

    public static int left() {
        return left;
    }

    public static int logoY() {
        return stackTop - LOGO - 14;
    }

    private static String key(AbstractWidget w) {
        if (w.getMessage() != null && w.getMessage().getContents() instanceof TranslatableContents tc) {
            return tc.getKey();
        }
        return "";
    }

    private static int order(String key) {
        return switch (key) {
            case "menu.singleplayer", "menu.playdemo" -> 0;
            case "menu.multiplayer", "menu.resetdemo" -> 1;
            case "menu.online" -> 2;
            case "menu.options" -> 8;
            case "menu.quit" -> 9;
            default -> 5;
        };
    }

    private static UiIcon icon(String key) {
        return switch (key) {
            case "menu.singleplayer", "menu.playdemo" -> UiIcon.PLAY;
            case "menu.multiplayer" -> UiIcon.USERS;
            case "menu.online" -> UiIcon.CLOUD;
            case "menu.options" -> UiIcon.SETTINGS;
            case "menu.quit" -> UiIcon.POWER;
            default -> UiIcon.CHEVRON;
        };
    }

    /** Раскладка кнопок TitleScreen: столбец слева по центру высоты, иконки под ним. */
    public static void layout(List<? extends GuiEventListener> children, int width, int height) {
        List<AbstractButton> items = new ArrayList<>();
        List<AbstractButton> icons = new ArrayList<>();
        for (GuiEventListener child : children) {
            if (child instanceof SpriteIconButton sib) {
                icons.add(sib);
            } else if (child instanceof AbstractButton b) {
                items.add(b);
            }
        }
        items.sort((a, b) -> Integer.compare(order(key(a)), order(key(b))));

        int extra = 0;
        for (int i = 1; i < items.size(); i++) {
            if (order(key(items.get(i))) >= 8 && order(key(items.get(i - 1))) < 8) {
                extra = 8;
            }
        }
        int stackH = items.size() * ITEM_H + Math.max(0, items.size() - 1) * GAP + extra;
        int groupH = LOGO + 14 + stackH + 12 + ITEM_H;
        left = Math.max(20, Math.round(width * 0.075f));
        stackTop = Math.max(LOGO + 22, (height - groupH) / 2 + LOGO + 14);

        int y = stackTop;
        for (int i = 0; i < items.size(); i++) {
            AbstractButton b = items.get(i);
            String k = key(b);
            if (i > 0 && order(k) >= 8 && order(key(items.get(i - 1))) < 8) {
                y += 8;
            }
            b.setPosition(left, y);
            b.setSize(ITEM_W, ITEM_H);
            State s = STATES.computeIfAbsent(b, x -> new State());
            s.index = i;
            s.icon = icon(k);
            s.danger = "menu.quit".equals(k);
            y += ITEM_H + GAP;
        }
        int ix = left + 4;
        int iy = y - GAP + 12;
        for (int i = 0; i < icons.size(); i++) {
            AbstractButton b = icons.get(i);
            b.setPosition(ix + i * 26, iy);
            State s = STATES.computeIfAbsent(b, x -> new State());
            s.index = items.size() + i;
        }
        openedAt = Util.getMillis();
    }

    /** 0…1 — каскадное появление пункта. */
    private static float appear(State s) {
        float t = (Util.getMillis() - openedAt - s.index * 45L) / 320f;
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static State state(AbstractButton b) {
        State s = STATES.computeIfAbsent(b, k -> new State());
        long now = Util.getMillis();
        float dt = s.last == 0 ? 0f : Math.min(0.1f, (now - s.last) / 1000f);
        s.last = now;
        boolean hv = b.isActive() && b.isHoveredOrFocused();
        if (hv && s.hover < 0.02f && b.isHovered()) {
            UiSound.play(UiSound.Event.HOVER);
        }
        s.hover += ((hv ? 1f : 0f) - s.hover) * (1f - (float) Math.exp(-dt * 14f));
        return s;
    }

    public static void background(GuiGraphicsExtractor g, AbstractButton b) {
        State s = state(b);
        float t = s.hover;
        float in = appear(s);
        float alpha = b.getAlpha() * in * (b.isActive() ? 1f : 0.5f);
        int accent = s.danger ? UiTheme.ERROR : UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        float x = b.getX() - (1f - in) * 14f;
        float y = b.getY();
        float w = b.getWidth();
        float h = b.getHeight();

        if (b instanceof SpriteIconButton) {
            float r = Math.min(w, h) / 2f;
            UiVector.roundRect(g, x, y, w, h, r, UiTheme.withAlpha(UiTheme.mix(0x99100D14, UiTheme.withAlpha(accent, 0.3f), t), alpha));
            UiVector.outline(g, x, y, w, h, r, 0.6f,
                    UiTheme.withAlpha(UiTheme.mix(0x26FFFFFF, UiTheme.withAlpha(accent, 0.8f), t), alpha));
            return;
        }

        // плашка выезжает слева направо
        if (t > 0.01f) {
            float pw = w * (0.35f + 0.65f * t);
            UiVector.shadow(g, x, y, pw, h, 7f, 5f, UiTheme.withAlpha(accent, 0.18f * t * alpha), 10);
            UiVector.roundRectGradient(g, x, y, pw, h, 7f,
                    UiTheme.withAlpha(UiTheme.mix(0x33FFFFFF, accent, 0.35f), 0.75f * t * alpha),
                    UiTheme.withAlpha(UiTheme.mix(0x1AFFFFFF, accent, 0.25f), 0.75f * t * alpha));
            UiVector.outline(g, x, y, pw, h, 7f, 0.5f, UiTheme.withAlpha(0x33FFFFFF, t * alpha));
            float bh = 9f * t;
            UiVector.roundRect(g, x + 3f, y + h / 2f - bh / 2f, 1.6f, bh, 0.8f, UiTheme.withAlpha(accent, t * alpha));
        }
        if (s.icon != null) {
            int ic = UiTheme.mix(0xFFB9B3C2, s.danger ? UiTheme.ERROR : 0xFFFFFFFF, t);
            s.icon.draw(g, Math.round(x + 9 + 2f * t), Math.round(y + (h - 10) / 2f), 10,
                    UiTheme.withAlpha(ic, alpha));
        }
    }

    public static void label(GuiGraphicsExtractor g, AbstractButton b) {
        Minecraft mc = Minecraft.getInstance();
        State s = STATES.get(b);
        float t = s == null ? 0f : s.hover;
        float in = s == null ? 1f : appear(s);
        float alpha = b.getAlpha() * in * (b.isActive() ? 1f : 0.45f);
        int col = UiTheme.withAlpha(UiTheme.mix(0xFFD6D0DE, 0xFFFFFFFF, t), alpha);
        String text = b.getMessage().getString();
        float x = b.getX() - (1f - in) * 14f + 25 + 3f * t;
        String shown = MenuKit.trim(mc.font, text, MenuKit.TITLE, b.getWidth() - 32);
        MenuKit.text(g, mc.font, shown, x, MenuKit.ty(MenuKit.TITLE, b.getY() + b.getHeight() / 2f), col, MenuKit.TITLE);
    }
}
