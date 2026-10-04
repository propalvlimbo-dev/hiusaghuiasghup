package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;
import ru.rooyzee.elytrixclient.client.ui.menu.MenuKit;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Кнопки главного меню в стиле клиента: тёмное «стекло» со скруглением, тонкая
 * рамка, при наведении — плавный переход к акцентному цвету и мягкая подсветка.
 * Подпись — шрифтом клиента вместо пиксельного. Подключается миксином
 * {@code AbstractButtonMixin} только на {@link TitleScreen}.
 */
public final class ElytrixMenuButtons {
    private ElytrixMenuButtons() {
    }

    private static final class State {
        float hover;
        long last;
    }

    private static final Map<AbstractButton, State> STATES = new WeakHashMap<>();

    public static boolean active() {
        if (!ElytrixclientClient.CONFIG.hackerBackground) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gui != null && mc.gui.screen() instanceof TitleScreen;
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
        float target = hv ? 1f : 0f;
        s.hover += (target - s.hover) * (1f - (float) Math.exp(-dt * 14f));
        return s;
    }

    public static void background(GuiGraphicsExtractor g, AbstractButton b) {
        State s = state(b);
        float t = s.hover;
        float alpha = b.getAlpha() * (b.isActive() ? 1f : 0.5f);
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        float x = b.getX();
        float y = b.getY();
        float w = b.getWidth();
        float h = b.getHeight();
        float r = Math.min(6f, h / 2f);

        if (t > 0.01f) {
            UiVector.shadow(g, x, y, w, h, r, 6f, UiTheme.withAlpha(accent, 0.35f * t * alpha), 10);
        }
        int fill = UiTheme.mix(0xB0100D14, UiTheme.withAlpha(accent, 0.30f), t);
        UiVector.roundRect(g, x, y, w, h, r, UiTheme.withAlpha(fill, alpha));
        // лёгкий блик сверху — ощущение стекла
        UiVector.roundRectGradient(g, x, y, w, h, r, UiTheme.withAlpha(0x14FFFFFF, alpha), 0x00FFFFFF);
        int edge = UiTheme.mix(0x2EFFFFFF, UiTheme.withAlpha(accent, 0.85f), t);
        UiVector.outline(g, x, y, w, h, r, 0.6f, UiTheme.withAlpha(edge, alpha));
    }

    public static void label(GuiGraphicsExtractor g, AbstractButton b) {
        Minecraft mc = Minecraft.getInstance();
        State s = STATES.get(b);
        float t = s == null ? 0f : s.hover;
        String text = b.getMessage().getString();
        float alpha = b.getAlpha() * (b.isActive() ? 1f : 0.45f);
        int col = UiTheme.withAlpha(UiTheme.mix(0xFFD9D4DE, 0xFFFFFFFF, t), alpha);
        float maxW = b.getWidth() - 8;
        String shown = MenuKit.trim(mc.font, text, MenuKit.TITLE, maxW);
        float cx = b.getX() + b.getWidth() / 2f;
        MenuKit.textCenter(g, mc.font, shown, cx, MenuKit.ty(MenuKit.TITLE, b.getY() + b.getHeight() / 2f), col,
                MenuKit.TITLE);
    }
}
