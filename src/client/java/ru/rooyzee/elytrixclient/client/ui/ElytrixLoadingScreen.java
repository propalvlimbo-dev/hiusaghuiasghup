package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;

/**
 * Красивый экран загрузки клиента: тёмный градиент, пульсирующий логотип,
 * плавный прогресс-бар с градиентом и «степпер» из шагов.
 *
 * <p>Показывается один раз при запуске игры (перехватывает первый показ главного меню),
 * затем отдаёт управление ванильному {@link TitleScreen} — само главное меню мы не трогаем.
 */
public class ElytrixLoadingScreen extends Screen {

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("elytrixclient", "textures/gui/logo.png");

    private static final String[] STEPS = {
            "Инициализация клиента",
            "Загрузка ресурсов",
            "Подготовка интерфейса",
            "Почти готово"
    };

    private static final long DURATION_MS = 2400L;
    private static final long FADE_MS = 260L;

    private final long startedAt = Util.getMillis();
    private boolean switched;

    public ElytrixLoadingScreen() {
        super(Component.literal("Elytrix Client"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        long elapsed = Util.getMillis() - startedAt;
        if (!switched && elapsed >= DURATION_MS) {
            switched = true;
            this.minecraft.gui.setScreen(new TitleScreen());
        }
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        long elapsed = now - startedAt;
        float raw = Math.min(1f, elapsed / (float) DURATION_MS);
        float progress = easeOut(raw);
        float fade = 1f;
        if (elapsed > DURATION_MS - FADE_MS) {
            fade = 1f - Math.min(1f, (elapsed - (DURATION_MS - FADE_MS)) / (float) FADE_MS);
        }

        int w = this.width;
        int h = this.height;
        int accent = UiTheme.accent(0);

        // ── фон
        graphics.fillGradient(0, 0, w, h, 0xFF090B11, 0xFF141827);
        int glow = (int) (28 + 22 * Math.sin(now / 420.0));
        for (int i = 0; i < 26; i++) {
            int bandY = h / 2 - 130 + i * 10;
            float d = Math.abs(i - 13) / 13f;
            int alpha = (int) ((1f - d) * 0.06f * 255);
            if (alpha > 2) {
                graphics.fill(0, bandY, w, bandY + 10, (alpha << 24) | (accent & 0x00FFFFFF));
            }
        }

        // ── логотип с пульсацией
        int size = (int) (54 + glow * 0.18f);
        int cx = w / 2;
        int cy = (int) (h * 0.40f);
        UiDraw.ring(graphics, cx, cy, size * 0.78f, 1.4f, UiTheme.withAlpha(accent, 0.35f * fade));
        UiDraw.ring(graphics, cx, cy, size * 0.95f, 1f, UiTheme.withAlpha(0xFFFFFFFF, 0.06f * fade));
        graphics.blit(RenderPipelines.GUI_TEXTURED, LOGO, cx - size / 2, cy - size / 2, 0f, 0f, size, size, 256, 256,
                UiTheme.withAlpha(0xFFFFFFFF, fade));

        // ── прогресс
        int barW = Math.min(260, w - 80);
        int barX = cx - barW / 2;
        int barY = cy + 64;
        UiDraw.progress(graphics, barX, barY, barW, 6, progress, accent);

        // бегущий блик по полосе
        float shimmer = (now % 1400L) / 1400f;
        int shimmerX = barX + (int) (barW * shimmer);
        graphics.fill(shimmerX - 18, barY, shimmerX + 18, barY + 6, UiTheme.withAlpha(0xFFFFFFFF, 0.07f * fade));

        int step = Math.min(STEPS.length - 1, (int) (raw * STEPS.length));
        var font = this.font;
        UiDraw.text(graphics, font, STEPS[step], barX, barY + 14, UiTheme.withAlpha(UiTheme.TEXT_SOFT, fade));
        int percent = Math.round(progress * 100f);
        UiDraw.textRight(graphics, font, percent + "%", barX + barW, barY + 14, UiTheme.withAlpha(UiTheme.TEXT_DIM, fade));

        // ── степпер шагов
        int dots = STEPS.length;
        int dotGap = 6;
        int dotW = 26;
        int totalW = dots * dotW + (dots - 1) * dotGap;
        int dotX = cx - totalW / 2;
        int dotY = barY + 34;
        for (int i = 0; i < dots; i++) {
            int x = dotX + i * (dotW + dotGap);
            boolean active = i <= step;
            int color = active ? UiTheme.withAlpha(accent, 0.9f * fade) : UiTheme.withAlpha(UiTheme.TRACK, fade);
            UiDraw.roundRect(graphics, x, dotY, dotW, 3, 2, color);
        }
    }
}
