package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.render.font.Fonts;
import ru.rooyzee.elytrixclient.client.render.font.MtsdfTextRenderer;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Инфо-панель (вотермарка) в стиле delta-26.2 (WatermarkWidget): строка сверху
 * по центру с логотипом и секциями FPS / время / координаты, разделёнными
 * вертикальными линиями. Рендер — наш MTSDF (тот же, что у остального HUD).
 */
public final class DeltaHud {
    private DeltaHud() {}

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final float SIZE = 8f, ROW = 14f, GAP = 5f, PADX = 6f;
    private static final int AC = 0xFF4FC3FF;
    private static final int FG = 0xFFFFFFFF;
    private static final int SEP = 0x33FFFFFF;

    public static void render(GuiGraphicsExtractor g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        if (mc == null || cfg == null || !cfg.hudWatermark) {
            return;
        }

        String logo = "Elytrix";
        List<String[]> secs = new ArrayList<>();
        secs.add(new String[]{"FPS", mc.getFps() + ""});
        secs.add(new String[]{"Время", LocalTime.now().format(TIME)});
        if (mc.player != null) {
            secs.add(new String[]{"XYZ",
                    (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ()});
        }

        float logoW = MtsdfTextRenderer.width(Fonts.MEDIUM, logo, SIZE);
        float w = PADX + logoW + GAP + 1f + GAP;
        for (String[] s : secs) {
            w += MtsdfTextRenderer.width(Fonts.MEDIUM, s[0], SIZE) + 3f
                    + MtsdfTextRenderer.width(Fonts.REGULAR, s[1], SIZE) + GAP + 1f + GAP;
        }
        w -= (GAP + 1f);
        if (w < 40f) {
            w = 40f;
        }
        float x = (sw - w) / 2f, y = 5f;

        UiVector.roundRect(g, x, y, w, ROW, 4f, 0xE60B0B16);
        UiVector.outline(g, x, y, w, ROW, 4f, .5f, 0x22FFFFFF);
        float ty = y + (ROW - MtsdfTextRenderer.lineHeight(Fonts.REGULAR, SIZE)) / 2f;

        float cur = x + PADX;
        MtsdfTextRenderer.draw(g, Fonts.MEDIUM, logo, cur, ty, SIZE, AC);
        cur += logoW + GAP;
        UiVector.roundRect(g, cur, y + 3f, 1f, ROW - 6f, .5f, SEP);
        cur += 1f + GAP;
        for (int i = 0; i < secs.size(); i++) {
            if (i > 0) {
                UiVector.roundRect(g, cur, y + 3f, 1f, ROW - 6f, .5f, SEP);
                cur += 1f + GAP;
            }
            String[] s = secs.get(i);
            MtsdfTextRenderer.draw(g, Fonts.MEDIUM, s[0], cur, ty, SIZE, AC);
            cur += MtsdfTextRenderer.width(Fonts.MEDIUM, s[0], SIZE) + 3f;
            MtsdfTextRenderer.draw(g, Fonts.REGULAR, s[1], cur, ty, SIZE, FG);
            cur += MtsdfTextRenderer.width(Fonts.REGULAR, s[1], SIZE) + GAP;
        }
    }
}
