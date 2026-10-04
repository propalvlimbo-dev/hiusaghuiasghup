package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;

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

    /** Известные ванильные экраны загрузки: 1 — подключение, 2 — загрузка мира. */
    private static int kindOf(Object screen) {
        Class<?> type = screen.getClass();
        while (type != null && type != Object.class) {
            String name = type.getSimpleName();
            if (name.equals("ConnectScreen")) {
                return 1;
            }
            if (name.equals("ReceivingLevelScreen") || name.equals("LevelLoadingScreen")
                    || name.equals("DownloadingTerrainScreen")) {
                return 2;
            }
            type = type.getSuperclass();
        }
        return 0;
    }

    /** Вызывается из миксина на каждом экране; рисует оверлей только на экранах загрузки. */
    public static void renderIfLoading(GuiGraphicsExtractor g, Screen screen) {
        long now = Util.getMillis();
        int kind = kindOf(screen);
        if (kind == 0) {
            shownAt = 0L;
            fade = 0f;
            lastFrame = now;
            return;
        }
        if (shownAt == 0L) {
            shownAt = now;
            lastFrame = now;
        }
        float dt = Math.max(0.0005f, Math.min(0.1f, (now - lastFrame) / 1000f));
        lastFrame = now;
        fade = Math.min(1f, fade + (UiWidget.ANIMATIONS ? dt * 5f : 1f));

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

        // затемнение и тихая сетка вместо ванильного «красного» вида
        g.fill(0, 0, w, h, UiTheme.withAlpha(0xFF05040A, 0.72f * a));
        ElytrixBackground.render(g, w, h, t, accent, 0.45f * a);

        int cx = w / 2;
        int cy = h / 2 - 26;

        // индикатор: восемь точек по кругу, «бегущая» волна
        float orbit = t * 1.6f;
        for (int i = 0; i < 8; i++) {
            double angle = orbit + i * (Math.PI / 4.0);
            float px = cx + (float) Math.cos(angle) * 17f;
            float py = cy + (float) Math.sin(angle) * 17f;
            float closeness = (float) ((Math.sin(orbit * 2.0 + i * 0.9) + 1.0) * 0.5);
            int alpha = (int) (40 + 190 * closeness);
            UiDraw.disc(g, px, py, 1.6f + 0.9f * closeness, UiTheme.withAlpha(accent, (alpha / 255f) * a));
        }

        String title = kind == 1 ? "ПОДКЛЮЧЕНИЕ" : "ЗАГРУЗКА МИРА";
        int spacing = 6;
        int titleW = UiDraw.spacedWidth(font, title, spacing, UiText.TITLE);
        UiDraw.textSpaced(g, font, title, cx - titleW / 2, cy + 16, spacing,
                UiTheme.withAlpha(0xFFFFFFFF, a), false, UiText.TITLE);

        // «неопределённая» полоса: сегмент ходит туда-сюда
        int barW = Math.min(220, w - 120);
        int barX = cx - barW / 2;
        int barY = cy + 44;
        UiDraw.roundRect(g, barX, barY, barW, 3, 2, UiTheme.withAlpha(UiTheme.TRACK, a));
        float cycle = (t * 0.7f) % 1f;
        float ping = cycle < 0.5f ? cycle * 2f : (1f - cycle) * 2f;
        int segW = Math.max(40, barW / 3);
        int segX = barX + Math.round(ping * (barW - segW));
        UiDraw.hGradient(g, segX, barY, segW, 3, 0x00000000, UiTheme.withAlpha(accent, a), 12);
        UiDraw.hGradient(g, segX, barY, segW, 3, UiTheme.withAlpha(accent, a), 0x00000000, 12);

        // строка состояния с бегущими точками
        String base = kind == 1 ? "устанавливаю соединение" : "получаю данные мира";
        int dots = (int) ((t * 2f) % 4f);
        StringBuilder sb = new StringBuilder(base);
        for (int i = 0; i < dots; i++) {
            sb.append('.');
        }
        UiDraw.textCenter(g, font, sb.toString(), cx, barY + 14,
                UiTheme.withAlpha(UiTheme.TEXT_DIM, a), UiText.MONO);
    }
}
