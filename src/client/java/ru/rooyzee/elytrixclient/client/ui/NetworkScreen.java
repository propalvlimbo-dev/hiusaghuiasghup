package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

/**
 * Экран «Сеть» в главном меню: прокси (SOCKS5/HTTP) и желаемая версия.
 * Реальную смену версий протокола обеспечивает ViaFabric — здесь настройка
 * сохраняется и показывается; прокси применяется к Java-системным свойствам
 * (HTTP/socks-вызовы клиента: сессия, скины, сервисы).
 */
public final class NetworkScreen extends Screen {

    private final Screen parent;
    private int focus = -1; // 0 = host, 1 = port, 2 = version
    private String note = "";
    private long noteUntil = 0;

    public NetworkScreen(Screen parent) {
        super(Component.literal("Сеть"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        UiVector.rect(g, 0, 0, width, height, 0xAA000000);
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;

        try {
            org.xrose.utils.render.gui.Render2DUtil.rect(0, 0, width, height)
                    .color(0x55000000).blur(22f).draw();
            org.xrose.utils.render.gui.Render2DUtil.flush();
        } catch (Throwable ignored) {
        }

        float pw = 220f, ph = 150f;
        float px = (width - pw) / 2f, py = (height - ph) / 2f;
        UiVector.roundRect(g, px, py, pw, ph, 12f, 0xCC141218);
        UiVector.outline(g, px, py, pw, ph, 12f, .5f, 0x40FFFFFF);
        UiVector.rect(g, px + 10, py + 18f, pw - 20, 1f, 0x664FC3FF);
        MtsdfTextRenderer.draw(g, Fonts.MEDIUM, "Сеть", px + 10, py + 7, 9f, 0xFFFFFFFF);

        boolean pr = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), 0) == 1;

        // режим прокси — цикл по клику
        String modeLabel = switch (cfg.proxyMode) {
            case "socks5" -> "SOCKS5";
            case "http" -> "HTTP";
            default -> "Выкл";
        };
        rowLabel(g, "Прокси", px + 10, py + 28);
        boolean hovMode = in(mx, my, px + pw - 80, py + 26, 70, 14);
        UiVector.roundRect(g, px + pw - 80, py + 26, 70, 14, 5f, hovMode ? 0x33FFFFFF : 0x1FFFFFFF);
        center(g, modeLabel, px + pw - 80, py + 29, 70);
        if (pr && hovMode && !wasPressed) {
            cfg.proxyMode = cfg.proxyMode.equals("off") ? "socks5" : cfg.proxyMode.equals("socks5") ? "http" : "off";
        }

        field(g, "Хост", px + 10, py + 48, pw - 100, cfg.proxyHost, 0, mx, my, pr);
        field(g, "Порт", px + pw - 80, py + 48, 70, String.valueOf(cfg.proxyPort), 1, mx, my, pr);
        field(g, "Версия (ViaFabric)", px + 10, py + 74, pw - 20, cfg.protocolVersion, 2, mx, my, pr);

        // кнопки
        boolean hovApply = in(mx, my, px + 10, py + 100, 95, 16);
        boolean hovSave = in(mx, my, px + 115, py + 100, 95, 16);
        UiVector.roundRect(g, px + 10, py + 100, 95, 16, 6f, hovApply ? 0xFF4FC3FF : 0x994FC3FF);
        center(g, "Применить", px + 10, py + 104, 95);
        UiVector.roundRect(g, px + 115, py + 100, 95, 16, 6f, hovSave ? 0x33FFFFFF : 0x1FFFFFFF);
        center(g, "Сохранить", px + 115, py + 104, 95);

        if (pr && !wasPressed) {
            if (hovApply) {
                apply(cfg);
                note = "Прокси применён к клиенту";
                noteUntil = System.currentTimeMillis() + 3000;
            } else if (hovSave) {
                cfg.save();
                note = "Сохранено";
                noteUntil = System.currentTimeMillis() + 3000;
            }
        }
        if (System.currentTimeMillis() < noteUntil) {
            MtsdfTextRenderer.draw(g, Fonts.REGULAR, note, px + 10, py + 126, 7f, 0xB3FFFFFF);
        } else {
            MtsdfTextRenderer.draw(g, Fonts.REGULAR,
                    "Смена версий работает при установленном ViaFabric", px + 10, py + 126, 6.5f, 0x80FFFFFF);
        }
        wasPressed = pr;

        if (focus >= 0) {
            pollText();
        }
        if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_ESCAPE) == 1) {
            Minecraft.getInstance().gui.setScreen(parent);
        }
    }

    private boolean wasPressed;

    public static void applyFromConfig() {
        apply(ElytrixclientClient.CONFIG);
    }

    private static void apply(ElytrixConfig cfg) {
        try {
            if (cfg.proxyMode.equals("off") || cfg.proxyHost.isBlank()) {
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
            } else if (cfg.proxyMode.equals("socks5")) {
                System.setProperty("socksProxyHost", cfg.proxyHost);
                System.setProperty("socksProxyPort", String.valueOf(cfg.proxyPort));
            } else {
                System.setProperty("http.proxyHost", cfg.proxyHost);
                System.setProperty("http.proxyPort", String.valueOf(cfg.proxyPort));
                System.setProperty("https.proxyHost", cfg.proxyHost);
                System.setProperty("https.proxyPort", String.valueOf(cfg.proxyPort));
            }
        } catch (Throwable ignored) {
        }
    }

    private void rowLabel(GuiGraphicsExtractor g, String s, float x, float y) {
        MtsdfTextRenderer.draw(g, Fonts.REGULAR, s, x, y, 7f, 0xE6FFFFFF);
    }

    private void center(GuiGraphicsExtractor g, String s, float x, float y, float w) {
        MtsdfTextRenderer.draw(g, Fonts.MEDIUM, s,
                x + (w - MtsdfTextRenderer.width(Fonts.MEDIUM, s, 7f)) / 2, y, 7f, 0xF2FFFFFF);
    }

    private void field(GuiGraphicsExtractor g, String label, float x, float y, float w,
                       String value, int idx, int mx, int my, boolean pr) {
        MtsdfTextRenderer.draw(g, Fonts.REGULAR, label, x, y - 1, 6.5f, 0x99FFFFFF);
        float fy = y + 8;
        boolean hov = in(mx, my, x, fy, w, 14);
        UiVector.roundRect(g, x, fy, w, 14, 5f, focus == idx ? 0x264FC3FF : (hov ? 0x1FFFFFFF : 0x14FFFFFF));
        UiVector.outline(g, x, fy, w, 14, 5f, .5f, focus == idx ? 0x994FC3FF : 0x33FFFFFF);
        String shown = value.isEmpty() ? "—" : value;
        MtsdfTextRenderer.draw(g, Fonts.MEDIUM, shown, x + 5, fy + 3.5f, 7f, 0xE6FFFFFF);
        if (pr && !wasPressed) {
            focus = hov ? idx : focus;
        }
    }

    private void pollText() {
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        String cur = focus == 0 ? cfg.proxyHost : focus == 1 ? String.valueOf(cfg.proxyPort) : cfg.protocolVersion;
        String next = cur;
        if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_BACKSPACE) == 1 && !cur.isEmpty()) {
            next = cur.substring(0, cur.length() - 1);
        }
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
            if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 2 && cur.length() < 40) {
                next = cur + (char) ('a' + (k - GLFW.GLFW_KEY_A));
            }
        }
        for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) {
            if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), k) == 2 && cur.length() < 40) {
                next = cur + (char) ('0' + (k - GLFW.GLFW_KEY_0));
            }
        }
        if (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_PERIOD) == 2 && cur.length() < 40) {
            next = cur + '.';
        }
        if (next.equals(cur)) {
            return;
        }
        if (focus == 0) {
            cfg.proxyHost = next;
        } else if (focus == 1) {
            try {
                cfg.proxyPort = Integer.parseInt(next.isEmpty() ? "0" : next);
            } catch (NumberFormatException ignored) {
            }
        } else {
            cfg.protocolVersion = next;
        }
    }

    private static boolean in(int mx, int my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
