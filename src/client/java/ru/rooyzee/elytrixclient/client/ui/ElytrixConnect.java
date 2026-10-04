package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;
import ru.rooyzee.elytrixclient.client.ui.menu.MenuKit;

/**
 * Анимация загрузки мира и подключения к серверу.
 *
 * <p>Рисуется поверх ванильных экранов подключения ({@code ConnectScreen},
 * {@code ReceivingLevelScreen}, {@code LevelLoadingScreen} и т.п.) — см.
 * {@code mixin.client.ScreenMixin}. Никакого своего состояния «подключён/нет»:
 * как только ванильный экран сменился на игровой, оверлей исчезает сам, поэтому
 * застрять на нём нельзя.
 *
 * <p>Вид: затемнение, тихая сетка, вращающийся индикатор из точек, название
 * в разрядку и тонкая «неопределённая» полоса прогресса.
 */
public final class ElytrixConnect {

    private static long shownAt;
    private static long lastFrame;
    private static float fade;

    private ElytrixConnect() {
    }

    /**
     * Свой экран загрузки мира/подключения. Вызывается из {@code LoadingScreensMixin}
     * вместо ванильной отрисовки. {@code kind}: 1 — подключение, 2 — загрузка мира.
     */
    public static void render(GuiGraphicsExtractor g, int kind) {
        long now = Util.getMillis();
        // экран показывается заново, если между кадрами был перерыв
        if (now - lastFrame > 400L) {
            shownAt = now;
            fade = 0f;
            lastFrame = now;
        }
        float dt = Math.max(0.0005f, Math.min(0.1f, (now - lastFrame) / 1000f));
        lastFrame = now;
        fade = Math.min(1f, fade + (UiWidget.ANIMATIONS ? dt * 6f : 1f));

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        float a = fade;
        float t = ElytrixBackground.time();

        // фон непрозрачный с первого кадра — ванильный экран под ним не мелькает,
        // а содержимое (индикатор, текст) проявляется плавно
        g.fill(0, 0, w, h, 0xFF07050A);
        ElytrixBackground.render(g, w, h, t, accent, 0.55f + 0.45f * a);

        int cx = w / 2;
        int cy = h / 2;
        int spinY = cy - 26;

        // индикатор: восемь точек по кругу, «бегущая» волна
        float orbit = t * 1.6f;
        for (int i = 0; i < 8; i++) {
            double angle = orbit + i * (Math.PI / 4.0);
            float px = cx + (float) Math.cos(angle) * 11f;
            float py = spinY + (float) Math.sin(angle) * 11f;
            float closeness = (float) ((Math.sin(orbit * 2.0 + i * 0.9) + 1.0) * 0.5);
            int alpha = (int) (40 + 190 * closeness);
            UiDraw.disc(g, px, py, 1.2f + 0.7f * closeness, UiTheme.withAlpha(accent, (alpha / 255f) * a));
        }

        String title = kind == 1 ? "ПОДКЛЮЧЕНИЕ" : "ЗАГРУЗКА МИРА";
        int spacing = 2;
        int titleW = UiDraw.spacedWidth(font, title, spacing, MenuKit.TITLE);
        UiDraw.textSpaced(g, font, title, cx - titleW / 2, cy - 8, spacing,
                UiTheme.withAlpha(0xFFFFFFFF, a), false, MenuKit.TITLE);

        // «неопределённая» полоса: сегмент ходит туда-сюда
        int barW = Math.min(160, w - 80);
        int barX = cx - barW / 2;
        int barY = cy + 9;
        UiDraw.roundRect(g, barX, barY, barW, 3, 2, UiTheme.withAlpha(UiTheme.TRACK, a));
        float cycle = (t * 0.7f) % 1f;
        float ping = cycle < 0.5f ? cycle * 2f : (1f - cycle) * 2f;
        int segW = Math.max(40, barW / 3);
        int segX = barX + Math.round(ping * (barW - segW));
        int half = segW / 2;
        UiDraw.hGradient(g, segX, barY, half, 3, 0x00000000, UiTheme.withAlpha(accent, a), 12);
        UiDraw.hGradient(g, segX + half, barY, segW - half, 3, UiTheme.withAlpha(accent, a), 0x00000000, 12);

        // строка состояния с бегущими точками
        String base = kind == 1 ? "устанавливаю соединение" : "получаю данные мира";
        int dots = (int) ((t * 2f) % 4f);
        // центрируем по тексту без точек — строка не «прыгает», точки дописываются справа
        int baseW = UiText.width(font, base, MenuKit.MONO);
        int textX = cx - baseW / 2;
        int col = UiTheme.withAlpha(UiTheme.TEXT_DIM, a);
        UiText.draw(g, font, base + ".".repeat(dots), textX, barY + 9, col, MenuKit.MONO, false);
    }
}
