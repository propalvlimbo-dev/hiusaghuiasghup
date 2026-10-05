package ru.rooyzee.elytrixclient.client.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * Свой экран загрузки ElytrixClient — рисуется вместо ванильного красного лоадера
 * (см. {@code mixin.client.LoadingOverlayMixin}) на всё время загрузки ресурсов.
 *
 * <p>Композиция намеренно сдержанная («Apple + терминал»): тёмный фон с тихой
 * сеткой, знак клиента с мягким ореолом, название в разрядку, тонкая линия
 * прогресса и одна строка состояния шрифтом Onest — без «терминального
 * лога» и мигающих курсоров.
 */
public final class ElytrixLoader {

    /** Размер, под который сгенерированы спрайты знака (logo_88_xN). */
    private static final int LOGO_SIZE = 88;

    /** Короткие подписи по прогрессу (одна строка, без списка шагов). */
    private static final String[] STATUS = {
            "загружаю ресурсы",
            "собираю реестры",
            "готовлю интерфейс и шрифты",
            "почти готово"
    };

    private ElytrixLoader() {
    }

    public static void render(GuiGraphicsExtractor g, float progress, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int accent = UiTheme.accent(ElytrixclientClient.CONFIG.accentIndex);
        float t = ElytrixBackground.time();
        float p = Mth.clamp(progress, 0f, 1f);

        MenuBackgrounds.render(g, w, h, t, accent, 1.0f * alpha);

        int cx = w / 2;
        int cy = h / 2 - 34;

        // ── только знак клиента (без названия/версии и без плашки-ореола)
        int mark = 80;
        int k = UiDraw.shapeScale();
        UiDraw.icon(g, UiDraw.shapeTexture("logo_" + LOGO_SIZE), cx - mark / 2, cy - mark / 2, mark, mark,
                LOGO_SIZE * k, LOGO_SIZE * k, UiTheme.withAlpha(0xFFFFFFFF, alpha));
        int lineY = cy + mark / 2;

        // ── тонкая линия прогресса
        int barW = Math.min(260, w - 120);
        int barX = cx - barW / 2;
        int barY = lineY + 26;
        UiDraw.roundRect(g, barX, barY, barW, 3, 2, UiTheme.withAlpha(UiTheme.TRACK, alpha));
        int fillW = Math.round(barW * p);
        if (fillW > 2) {
            UiDraw.roundRect(g, barX, barY, fillW, 3, 2, UiTheme.withAlpha(accent, alpha));
        }
        int shineW = Math.max(16, barW / 7);
        int sx = barX + Math.round((t * 0.3f % 1f) * Math.max(1, barW - shineW));
        UiDraw.hGradient(g, sx, barY, shineW, 3, 0x00000000,
                UiTheme.withAlpha(0xFFFFFFFF, 0.25f * alpha), 10);

        // ── одна строка состояния
        int idx = Mth.clamp((int) (p * STATUS.length), 0, STATUS.length - 1);
        UiDraw.textCenter(g, font, STATUS[idx], cx, barY + 16,
                UiTheme.withAlpha(UiTheme.TEXT_SOFT, alpha), UiText.MONO);
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
