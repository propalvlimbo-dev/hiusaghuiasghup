package ru.rooyzee.elytrixclient.client.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

import java.util.List;

import static ru.rooyzee.elytrixclient.client.ui.menu.MenuKit.*;

/**
 * Вкладка «Консоль»: аккуратное окно терминала. Строки берутся из общего
 * {@code LogBuffer}; снизу поле команды — Enter отправляет её в SoulFire CLI.
 */
public final class ConsoleView {
    private static final float BAR_H = 20;
    private static final float INPUT_H = 20;
    private static final float LINE = 9;

    private float x;
    private float y;
    private float w;
    private float h;
    private int scrollLines;
    private String input = "";
    private boolean focused;
    private float focusT;
    private final float[] btnHover = new float[2];

    public void layout(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    private float bodyTop() {
        return y + BAR_H + 4;
    }

    private float bodyBottom() {
        return y + h - INPUT_H - 4;
    }

    private int visibleLines() {
        return Math.max(1, (int) ((bodyBottom() - bodyTop()) / LINE));
    }

    private static int lineColor(String s) {
        String l = lower(s);
        if (l.contains("error") || l.contains("ошибк") || l.contains("exception") || l.contains("failed")) {
            return UiTheme.ERROR;
        }
        if (l.contains("warn") || l.contains("предупр")) {
            return UiTheme.WARN;
        }
        if (s.startsWith("[Elytrix]")) {
            return accent2();
        }
        if (s.startsWith("> ")) {
            return accent;
        }
        return soft();
    }

    private float btnX(int i) {
        return x + w - 8 - (i + 1) * 54 - i * 4;
    }

    public void render(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
        focusT = approach(focusT, focused ? 1f : 0f, 16f, dt);

        // окно
        shadow(g, x, y, w, h, 7, 8, 0x66000000);
        card(g, x, y, w, h, 7, UiTheme.isLight() ? 0xFFFDFCFE : 0xF20B0A0E, cardEdge());
        fill(g, x + 1, y + 1, w - 2, BAR_H - 1, 6, 6, 0, 0, UiTheme.isLight() ? 0xFFF1EFF4 : 0xFF141117);
        hline(g, x + 1, y + BAR_H, w - 2, divider());

        // «светофор» в цветах темы
        float cy = y + BAR_H / 2f;
        disc(g, x + 11, cy, 3f, accent);
        disc(g, x + 20, cy, 3f, accent2());
        disc(g, x + 29, cy, 3f, UiTheme.withAlpha(soft(), 0.5f));

        List<String> lines = ElytrixclientClient.LOG.snapshot();
        textCenter(g, font, "elytrix — console", x + w / 2f - 30, ty(MONO, cy), dim(), MONO);

        String[] labels = {"Очистить", "Копировать"};
        for (int i = 0; i < 2; i++) {
            float bx = btnX(i);
            float by = cy - 6;
            boolean hv = inside(mx, my, bx, by, 54, 12);
            btnHover[i] = approach(btnHover[i], hv ? 1f : 0f, 16f, dt);
            fill(g, bx, by, 54, 12, 4, UiTheme.mix(field(), text(), 0.06f * btnHover[i]));
            textCenter(g, font, labels[i], bx + 27, ty(SMALL, cy), UiTheme.mix(dim(), text(), btnHover[i]), SMALL);
        }

        // тело
        int vis = visibleLines();
        int max = Math.max(0, lines.size() - vis);
        scrollLines = Math.max(0, Math.min(scrollLines, max));
        int start = Math.max(0, lines.size() - vis - scrollLines);
        int end = Math.min(lines.size(), start + vis);

        UiDraw.scissor(g, (int) x + 2, (int) bodyTop(), (int) (x + w - 2), (int) bodyBottom());
        if (lines.isEmpty()) {
            textCenter(g, font, "логов пока нет — запустите ботов", x + w / 2f,
                    ty(SMALL, (bodyTop() + bodyBottom()) / 2f), dim(), SMALL);
        }
        float ly = bodyTop();
        for (int i = start; i < end; i++) {
            String s = lines.get(i);
            textRight(g, font, String.valueOf(i + 1), x + 26, ty(MONO, ly + LINE / 2f), UiTheme.withAlpha(dim(), 0.55f), MONO);
            text(g, font, trim(font, s, MONO, w - 44), x + 32, ty(MONO, ly + LINE / 2f), lineColor(s), MONO);
            ly += LINE;
        }
        UiDraw.unscissor(g);

        // полоса прокрутки
        if (max > 0) {
            float trackH = bodyBottom() - bodyTop();
            float thumbH = Math.max(14, trackH * vis / (float) lines.size());
            float t = 1f - scrollLines / (float) max;
            fill(g, x + w - 5, bodyTop() + (trackH - thumbH) * t, 2, thumbH, 1, UiTheme.withAlpha(text(), 0.18f));
        }

        // строка ввода
        float iy = y + h - INPUT_H - 4;
        float ih = INPUT_H;
        fill(g, x + 6, iy, w - 12, ih, 5, field());
        outline(g, x + 6, iy, w - 12, ih, 5, 0.7f, UiTheme.mix(cardEdge(), accent, focusT));
        float icy = iy + ih / 2f;
        text(g, font, "›", x + 13, ty(MONO, icy), accent, MONO);
        String shown = input;
        while (!shown.isEmpty() && width(font, shown, MONO) > w - 44) {
            shown = shown.substring(1);
        }
        if (input.isEmpty() && !focused) {
            text(g, font, "команда SoulFire, Enter — отправить", x + 22, ty(MONO, icy), dim(), MONO);
        } else {
            text(g, font, shown, x + 22, ty(MONO, icy), text(), MONO);
        }
        if (focused && (Util.getMillis() / 530L) % 2L == 0L) {
            fill(g, x + 22 + width(font, shown, MONO) + 0.5f, icy - 3.5f, 0.8f, 7, 0, accent);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!inside(mx, my, x, y, w, h)) {
            focused = false;
            return false;
        }
        float cy = y + BAR_H / 2f;
        if (button == 0 && inside(mx, my, btnX(0), cy - 6, 54, 12)) {
            ElytrixclientClient.LOG.clear();
            scrollLines = 0;
            return true;
        }
        if (button == 0 && inside(mx, my, btnX(1), cy - 6, 54, 12)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(String.join("\n", ElytrixclientClient.LOG.snapshot()));
            ElytrixclientClient.LOG.add("[Elytrix] Лог скопирован в буфер обмена");
            return true;
        }
        focused = inside(mx, my, x + 6, y + h - INPUT_H - 4, w - 12, INPUT_H);
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my, x, y, w, h)) {
            return false;
        }
        scrollLines += (int) Math.signum(amount) * 3;
        return true;
    }

    public boolean capturing() {
        return focused;
    }

    public void blur() {
        focused = false;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!focused) {
            return false;
        }
        int key = event.key();
        if (event.isEscape()) {
            focused = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!input.isEmpty()) {
                input = event.hasControlDown() ? "" : input.substring(0, input.length() - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            String cmd = input.trim();
            if (!cmd.isEmpty()) {
                ElytrixclientClient.LOG.add("> " + cmd);
                ElytrixclientClient.CONFIG.soulfireCommand = cmd;
                ElytrixclientClient.SOULFIRE.send(cmd);
                input = "";
                scrollLines = 0;
            }
            return true;
        }
        if (event.isPaste()) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                input = (input + clip.replaceAll("[\\r\\n\\t]", " ")).substring(0,
                        Math.min(256, input.length() + clip.length()));
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP && input.isEmpty()) {
            input = ElytrixclientClient.CONFIG.soulfireCommand == null ? "" : ElytrixclientClient.CONFIG.soulfireCommand;
            return true;
        }
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!focused) {
            return false;
        }
        if (event.isAllowedChatCharacter() && input.length() < 256) {
            input += event.codepointAsString();
        }
        return true;
    }
}
