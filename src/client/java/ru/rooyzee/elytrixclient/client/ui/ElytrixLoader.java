package ru.rooyzee.elytrixclient.client.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * Свой экран загрузки ElytrixClient — рисуется вместо ванильного красного лоадера
 * (см. {@code mixin.client.LoadingOverlayMixin}) на всё время загрузки ресурсов.
 *
 * <p>Композиция: «хакерский» фон, логотип с пульсацией и орбитальной точкой,
 * заголовок в разрядку, полоса прогресса с бликом, «терминальный» список шагов
 * и мигающий курсор. {@code alpha} отвечает за плавный вход/выход.
 */
public final class ElytrixLoader {

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("elytrixclient", "textures/gui/logo.png");

    private static final String[] STEPS = {
            "инициализация клиента",
            "ресурсы и текстуры",
            "реестры и модели",
            "звуковая система",
            "интерфейс и шрифты",
            "запуск главного меню"
    };

    private ElytrixLoader() {
    }

    public static void render(GuiGraphicsExtractor g, float progress, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        float t = ElytrixBackground.time();
        float p = Mth.clamp(progress, 0f, 1f);

        ElytrixBackground.render(g, w, h, t, accent, alpha);

        var font = mc.font;
        int cx = w / 2;
        int cy = (int) (h * 0.40f);

        // ── логотип с пульсацией и орбитальной точкой
        float pulse = 1f + 0.035f * (float) Math.sin(t * 2.4f);
        int size = Math.round(88 * pulse);
        int glowR = (int) (size * 0.78f);
        UiDraw.disc(g, cx, cy, glowR, UiTheme.withAlpha(accent, 0.16f * alpha));
        UiDraw.disc(g, cx, cy, size * 0.60f, UiTheme.withAlpha(accent, 0.10f * alpha));
        UiDraw.ring(g, cx, cy, size * 0.62f, 1.4f, UiTheme.withAlpha(accent, 0.55f * alpha));
        UiDraw.icon(g, LOGO, cx - size / 2, cy - size / 2, size, size, 256, 256,
                UiTheme.withAlpha(0xFFFFFFFF, alpha));
        double orbit = t * 1.9;
        float ox = cx + (float) Math.cos(orbit) * size * 0.62f;
        float oy = cy + (float) Math.sin(orbit) * size * 0.62f;
        UiDraw.disc(g, ox, oy, 3.2f, UiTheme.withAlpha(0xFFFFFFFF, alpha));
        UiDraw.disc(g, ox, oy, 6.5f, UiTheme.withAlpha(accent, 0.35f * alpha));

        // ── заголовок в разрядку
        int titleY = cy + (int) (size * 0.62f) + 16;
        g.pose().pushMatrix();
        g.pose().translate(cx, titleY);
        g.pose().scale(1.7f, 1.7f);
        String title = "ELYTRIX";
        UiDraw.textSpaced(g, font, title, -UiDraw.spacedWidth(font, title, 3) / 2, 0, 3,
                UiTheme.withAlpha(0xFFFFFFFF, alpha), true);
        g.pose().popMatrix();
        String sub = "client · minecraft " + mcVersion();
        UiDraw.textCenter(g, font, sub, cx, titleY + 20, UiTheme.withAlpha(accent, 0.85f * alpha));

        // ── полоса прогресса
        int barW = Math.min(360, w - 80);
        int barX = cx - barW / 2;
        int barY = (int) (h * 0.72f);
        UiDraw.roundRect(g, barX, barY, barW, 6, 3, UiTheme.withAlpha(UiTheme.TRACK, 0.85f * alpha));
        int fillW = Math.round(barW * p);
        if (fillW > 2) {
            UiDraw.roundRectGradient(g, barX, barY, fillW, 6, 3,
                    UiTheme.withAlpha(UiTheme.accentLight(accent), alpha),
                    UiTheme.withAlpha(accent, alpha));
            float shimmer = (t * 0.45f) % 1f;
            int sx = barX + Math.round(shimmer * Math.max(1, fillW - 24));
            UiDraw.hGradient(g, sx, barY, Math.min(24, fillW), 6, 0x00000000,
                    UiTheme.withAlpha(0xFFFFFFFF, 0.35f * alpha), 12);
        }
        String pct = Math.round(p * 100f) + "%";
        UiDraw.text(g, font, pct, barX + barW + 10, barY - 1, UiTheme.withAlpha(0xFFFFFFFF, alpha));

        // ── «терминальные» шаги
        int step = Mth.clamp((int) (p * STEPS.length), 0, STEPS.length - 1);
        int linesY = barY + 20;
        for (int i = Math.max(0, step - 3); i <= step; i++) {
            boolean done = i < step;
            String mark = done ? "[ ok ]" : "[ >> ]";
            int color = done ? UiTheme.OK : accent;
            int ly = linesY + (i - Math.max(0, step - 3)) * 11;
            UiDraw.text(g, font, mark, barX, ly, UiTheme.withAlpha(color, 0.85f * alpha));
            UiDraw.text(g, font, STEPS[i], barX + 40, ly, UiTheme.withAlpha(UiTheme.TEXT_SOFT, alpha));
        }

        // ── нижняя строка + мигающий курсор
        UiDraw.text(g, font, "elytrix:~$ boot", 12, h - 22, UiTheme.withAlpha(accent, 0.9f * alpha));
        if ((int) (t * 2f) % 2 == 0) {
            UiDraw.roundRect(g, 12 + font.width("elytrix:~$ boot") + 3, h - 21, 5, 9, 1,
                    UiTheme.withAlpha(0xFFFFFFFF, 0.9f * alpha));
        }
        String right = "Elytrix Client v" + version();
        UiDraw.textRight(g, font, right, w - 12, h - 22, UiTheme.withAlpha(UiTheme.TEXT_DIM, alpha));
        UiDraw.hLine(g, 12, w - 12, h - 26, 1, UiTheme.withAlpha(accent, 0.20f * alpha));
    }

    private static String mcVersion() {
        return "26.2";
    }

    public static String version() {
        return FabricLoader.getInstance().getModContainer("elytrixclient")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }
}
