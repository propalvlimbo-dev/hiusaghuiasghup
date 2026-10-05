package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiIcon;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;
import ru.rooyzee.elytrixclient.client.ui.menu.ConsoleView;
import ru.rooyzee.elytrixclient.client.ui.menu.MenuCard;
import ru.rooyzee.elytrixclient.client.ui.menu.MenuContent;
import ru.rooyzee.elytrixclient.client.ui.menu.MenuKit;
import ru.rooyzee.elytrixclient.client.ui.menu.ThemesView;

import java.util.ArrayList;
import java.util.List;

import static ru.rooyzee.elytrixclient.client.ui.menu.MenuKit.*;

/**
 * Панель ElytrixClient (правый Ctrl).
 *
 * <p>Панель 450×350 GUI-единиц. Слева бар 100: логотип, поиск, разделы
 * с градиентной подсветкой, профиль игрока. Справа — свой фон-текстура и
 * карточки в две колонки по 160 (раскладка «кирпичом»: следующая карточка
 * ложится в более короткую колонку). Отдельные виды: «Консоль» и «Темы».
 *
 * <p>Размеры в GUI-единицах — панель сама следует за «Масштабом интерфейса»;
 * пресеты 90–130 % масштабируют её дополнительно, на маленьких окнах она
 * ужимается, чтобы целиком влезать в экран.
 */
public class ElytrixScreen extends Screen {
    private static final float W = 450;
    private static final float H = 350;
    private static final float BAR = 100;
    private static final float COL_W = 160;
    private static final float GAP = 10;
    private static final float CX = BAR + 10;
    private static final float CW = W - BAR - 20;
    private static final int[] SCALE_OPTIONS = {0, 90, 100, 115, 130};

    /** Фон панели: свой вариант под каждый акцент (лента и подсветка в цвет акцента). */
    private static final Identifier[] MENU_BG = new Identifier[UiTheme.ACCENTS.length];

    static {
        for (int i = 0; i < MENU_BG.length; i++) {
            MENU_BG[i] = Identifier.fromNamespaceAndPath("elytrixclient", "textures/gui/menu_bg_" + i + ".png");
        }
    }

    /** Плавная смена фона при переключении акцента: старый → новый. */
    private int bgCurrent = -1;
    private int bgPrevious = -1;
    private float bgFade = 1f;

    private static final int VIEW_CARDS = 0;
    private static final int VIEW_CONSOLE = 1;
    private static final int VIEW_THEMES = 2;
    /** Разделитель в сайдбаре стоит перед этим разделом. */
    private static final int SPLIT_AT = 4;

    private record Tab(String name, UiIcon icon, int view, List<MenuCard> cards) {
    }

    private final Screen parent;
    private final ElytrixConfig cfg = ElytrixclientClient.CONFIG;
    private final List<Tab> tabs = new ArrayList<>();
    private final ConsoleView console = new ConsoleView();
    private ThemesView themes;
    private float[] tabAnim = new float[0];
    private float[] tabHover = new float[0];
    /** Активная вкладка — static, чтобы при повторном открытии панели возвращаться в тот же раздел. */
    private static int current;

    private float scroll;
    private float scrollTarget;
    private float maxScroll;

    private float openT;
    private boolean closing;
    private long lastFrame;
    private boolean dirty;
    private long dirtyAt;

    private String search = "";
    private boolean searching;
    private float searchT;

    private float scale = 1f;
    private float originX;
    private float originY;

    public ElytrixScreen(Screen parent) {
        super(Component.literal("Elytrix Client"));
        this.parent = parent;
    }

    public static ElytrixScreen create(Screen parent) {
        return new ElytrixScreen(parent);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Жизненный цикл
    // ═════════════════════════════════════════════════════════════════════

    @Override
    protected void init() {
        super.init();
        lastFrame = Util.getMillis();
        UiTheme.applyPreset(cfg.themeIndex);
        UiWidget.ANIMATIONS = cfg.animations;
        if (cfg.accentIndex < 0 || cfg.accentIndex >= UiTheme.ACCENTS.length) {
            cfg.accentIndex = 0;
        }
        MenuKit.accent = UiTheme.accent(cfg.accentIndex);
        if (tabs.isEmpty()) {
            buildTabs();
            replay();
            UiSound.play(UiSound.Event.OPEN);
        }
    }

    private void buildTabs() {
        MenuContent content = new MenuContent(this::markDirty, this::select, this::resetInterface);
        themes = new ThemesView(this::markDirty);
        tabs.clear();
        tabs.add(new Tab("Главная", UiIcon.HOME, VIEW_CARDS, content.home()));
        tabs.add(new Tab("Боты", UiIcon.BOTS, VIEW_CARDS, content.bots()));
        tabs.add(new Tab("Прокси", UiIcon.PROXY, VIEW_CARDS, content.proxy()));
        tabs.add(new Tab("Консоль", UiIcon.CONSOLE, VIEW_CONSOLE, List.of()));
        tabs.add(new Tab("Визуалы", UiIcon.PALETTE, VIEW_CARDS, content.visuals()));
        tabs.add(new Tab("Misc", UiIcon.SHIELD, VIEW_CARDS, content.misc()));
        tabs.add(new Tab("Настройки", UiIcon.SETTINGS, VIEW_CARDS, content.settings()));
        current = Mth.clamp(current, 0, tabs.size() - 1);
        tabAnim = new float[tabs.size()];
        tabHover = new float[tabs.size()];
        tabAnim[current] = 1f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (dirty && Util.getMillis() - dirtyAt > 450L) {
            dirty = false;
            cfg.save();
        }
        UiWidget.ANIMATIONS = cfg.animations;
    }

    @Override
    public void onClose() {
        // Модалка настроек модуля закрывается вместе с панелью.
        ru.rooyzee.elytrixclient.client.ui.menu.ModuleModal.close();
        ru.rooyzee.elytrixclient.client.ui.menu.XroseModal.close();
        if (!closing) {
            UiSound.play(UiSound.Event.CLOSE);
        }
        closing = true;
        if (!UiWidget.ANIMATIONS) {
            finishClose();
        }
    }

    private void finishClose() {
        if (dirty) {
            dirty = false;
            cfg.save();
        }
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        // Если панель сменили другим экраном напрямую (без onClose) — модалку тоже закрыть.
        ru.rooyzee.elytrixclient.client.ui.menu.ModuleModal.close();
        ru.rooyzee.elytrixclient.client.ui.menu.XroseModal.close();
        super.removed();
    }

    private void markDirty() {
        dirty = true;
        dirtyAt = Util.getMillis();
    }

    private void select(int index) {
        if (index < 0 || index >= tabs.size() || (index == current && search.isEmpty())) {
            return;
        }
        current = index;
        UiSound.play(UiSound.Event.CLICK);
        search = "";
        searching = false;
        scroll = 0;
        scrollTarget = 0;
        replay();
    }

    private void replay() {
        List<MenuCard> cards = visibleCards();
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).replay(UiWidget.ANIMATIONS ? 0.03f * i : 0f);
        }
        if (themes != null && UiWidget.ANIMATIONS) {
            themes.replay();
        }
    }

    private void resetInterface() {
        cfg.themeIndex = 0;
        cfg.accentIndex = 0;
        cfg.accent = "#FF4FC3";
        cfg.uiScaleIndex = 0;
        cfg.effectsQuality = 0;
        cfg.panelOpacity = 88;
        cfg.animations = true;
        cfg.blurBackground = true;
        cfg.hackerBackground = true;
        cfg.customLoading = true;
        cfg.closeOnOutsideClick = false;
        cfg.panelKey = true;
        cfg.menuSounds = true;
        cfg.soundSet = 0;
        cfg.soundVolume = 70;
        cfg.hoverSounds = true;
        UiTheme.applyPreset(0);
        MenuKit.accent = UiTheme.accent(0);
        UiWidget.ANIMATIONS = true;
        cfg.save();
    }

    /** Ссылка внизу раздела «Настройки». */
    private String footerText() {
        return search.isEmpty() && "Настройки".equals(tabs.get(current).name()) ? "Сбросить интерфейс" : null;
    }

    private float footerY;
    /** Пикселей на единицу панели, если не совпадает с GUI scale (иначе 0). */
    private int pixelsPerUnit;
    private float footerHover;

    private int view() {
        return search.isEmpty() ? tabs.get(current).view() : VIEW_CARDS;
    }

    private List<MenuCard> visibleCards() {
        if (search.isEmpty()) {
            return tabs.get(current).cards();
        }
        String q = lower(search.trim());
        List<MenuCard> result = new ArrayList<>();
        for (Tab tab : tabs) {
            for (MenuCard card : tab.cards()) {
                if (card.matches(q)) {
                    result.add(card);
                }
            }
        }
        return result;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Геометрия: панель в своих единицах → экран
    // ═════════════════════════════════════════════════════════════════════

    private float targetScale() {
        float preset = cfg.uiScaleIndex <= 0 ? 1f
                : SCALE_OPTIONS[Mth.clamp(cfg.uiScaleIndex, 0, SCALE_OPTIONS.length - 1)] / 100f;
        float fit = Math.min((this.width - 12f) / W, (this.height - 12f) / H);
        return Math.max(0.4f, Math.min(preset, fit));
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private void layout() {
        float e = easeOut(Mth.clamp(openT, 0f, 1f));
        float target = targetScale();
        int gs = Math.max(1, this.minecraft != null ? this.minecraft.getWindow().getGuiScale() : 1);
        // целое число пикселей на единицу панели — текст и иконки 1:1, без размытия
        int pixels = Math.max(1, Math.round(target * gs));
        target = pixels / (float) gs;
        pixelsPerUnit = pixels == gs ? 0 : pixels;
        scale = target * (0.96f + 0.04f * e);
        originX = this.width / 2f - W * scale / 2f;
        originY = this.height / 2f - H * scale / 2f + (1f - e) * 6f;
        // привязка к сетке пикселей экрана — текст не «плывёт» между пикселями
        float k = Math.max(1, UiDraw.shapeScale());
        originX = Math.round(originX * k) / k;
        originY = Math.round(originY * k) / k;
    }

    private double lx(double sx) {
        return (sx - originX) / scale;
    }

    private double ly(double sy) {
        return (sy - originY) / scale;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Отрисовка
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (cfg.blurBackground && ElytrixQuality.blur() && this.minecraft != null
                && this.minecraft.options.getMenuBackgroundBlurriness() < 1.0F) {
            graphics.blurBeforeThisStratum();
        }
        float e = easeOut(Mth.clamp(openT, 0f, 1f));
        graphics.fill(0, 0, this.width, this.height, UiTheme.withAlpha(0x66000000, e));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float dt = Math.max(0.0005f, Math.min(0.1f, (now - lastFrame) / 1000f));
        lastFrame = now;
        ElytrixQuality.updateFrom(this.minecraft, cfg);
        UiDraw.resetScissor();

        // открытие ~140 мс, закрытие ~100 мс
        if (closing) {
            openT -= UiWidget.ANIMATIONS ? dt / 0.10f : 1f;
            if (openT <= 0f) {
                openT = 0f;
                finishClose();
                return;
            }
        } else {
            openT = Math.min(1f, openT + (UiWidget.ANIMATIONS ? dt / 0.14f : 1f));
        }
        MenuKit.alpha = easeOut(Mth.clamp(openT, 0f, 1f));
        layout();

        double mx = lx(mouseX);
        double my = ly(mouseY);
        Font font = this.font;

        // Размытие фона в стиле меню xrose — на весь экран под панелью.
        try {
            org.xrose.utils.render.gui.Render2DUtil.rect(0, 0, graphics.guiWidth(), graphics.guiHeight())
                    .color(0x55000000).blur(22f).draw();
            org.xrose.utils.render.gui.Render2DUtil.flush();
        } catch (Throwable ignored) {
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(originX, originY);
        graphics.pose().scale(scale, scale);
        UiDraw.scaleOverride = pixelsPerUnit;

        int want = Mth.clamp(cfg.accentIndex, 0, MENU_BG.length - 1);
        if (bgCurrent < 0) {
            bgCurrent = want;
        } else if (want != bgCurrent) {
            bgPrevious = bgCurrent;
            bgCurrent = want;
            bgFade = 0f;
        }
        bgFade = approach(bgFade, 1f, 5f, dt);
        drawFrame(graphics);
        drawSidebar(graphics, font, mx, my, dt);
        drawContent(graphics, font, mx, my, dt);

        graphics.pose().popMatrix();
        UiDraw.scaleOverride = 0;
        MenuKit.alpha = 1f;
    }

    private void drawFrame(GuiGraphicsExtractor g) {
        float op = Mth.clamp(cfg.panelOpacity / 100f, 0f, 1f);
        shadow(g, 0, 0, W, H, 12, 26, UiTheme.withAlpha(0x80000000, 0.3f + 0.7f * op));
        fill(g, 0, 0, BAR, H, 12, 0, 0, 12, UiTheme.withAlpha(sidebar(), op));
        fill(g, BAR, 0, W - BAR, H, 0, 12, 12, 0, UiTheme.withAlpha(content(), op));
        if (!UiTheme.isLight()) {
            if (bgFade < 1f && bgPrevious >= 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, MENU_BG[bgPrevious], (int) BAR, 0, 0f, 0f, (int) (W - BAR), (int) H,
                        1050, 1050, 1050, 1050, a(0xFFFFFFFF, op));
            }
            g.blit(RenderPipelines.GUI_TEXTURED, MENU_BG[bgCurrent], (int) BAR, 0, 0f, 0f, (int) (W - BAR), (int) H,
                    1050, 1050, 1050, 1050, a(0xFFFFFFFF, op * easeOut(bgFade)));
        }
        UiVector.rect(g, BAR, 0, 0.5f, H, a(divider()));
        outline(g, 0, 0, W, H, 12, 0.5f, UiTheme.isLight() ? 0x2414141A : 0x24FFFFFF);
    }

    // ── поиск: плавная анимация набора ───────────────────────────────────

    private String shownSearch = "";
    private float[] charIn = new float[0];
    private final List<float[]> ghostPos = new ArrayList<>();
    private final List<String> ghostText = new ArrayList<>();
    private float caretX = -1f;
    private float searchOffset;
    private float placeholderT = 1f;
    private long lastTyped;

    private void drawSearchText(GuiGraphicsExtractor g, Font font, float x0, float cy, float maxW, float dt) {
        // сверяем показанную строку с реальной: новые символы появляются, удалённые растворяются
        if (!search.equals(shownSearch)) {
            int p = 0;
            int n = Math.min(search.length(), shownSearch.length());
            while (p < n && search.charAt(p) == shownSearch.charAt(p)) {
                p++;
            }
            if (shownSearch.length() > p) {
                float gx = x0 - searchOffset + width(font, shownSearch.substring(0, p), SMALL);
                ghostText.add(shownSearch.substring(p));
                ghostPos.add(new float[] {gx, 1f});
            }
            float[] next = new float[search.length()];
            System.arraycopy(charIn, 0, next, 0, Math.min(p, charIn.length));
            charIn = next;
            shownSearch = search;
            lastTyped = Util.getMillis();
        }
        boolean anim = UiWidget.ANIMATIONS;
        placeholderT = approach(placeholderT, search.isEmpty() ? 1f : 0f, 14f, dt);
        if (placeholderT > 0.01f) {
            int pc = UiTheme.withAlpha(dim(), placeholderT * (searching ? 0.55f : 1f));
            text(g, font, "Поиск", x0 + (1f - placeholderT) * 6f, ty(SMALL, cy), pc, SMALL);
        }

        float total = width(font, search, SMALL);
        searchOffset = approach(searchOffset, Math.max(0f, total - maxW), 18f, dt);
        float baseY = ty(SMALL, cy);
        for (int i = 0; i < search.length(); i++) {
            charIn[i] = anim ? approach(charIn[i], 1f, 16f, dt) : 1f;
            float t = charIn[i];
            if (t <= 0.01f) {
                continue;
            }
            float cx = x0 - searchOffset + width(font, search.substring(0, i), SMALL);
            float e = 1f - (1f - t) * (1f - t);
            int col = UiTheme.withAlpha(UiTheme.mix(accent, text(), e), e);
            text(g, font, String.valueOf(search.charAt(i)), cx, baseY - (1f - e) * 3f, col, SMALL);
        }
        for (int i = ghostPos.size() - 1; i >= 0; i--) {
            float[] gp = ghostPos.get(i);
            gp[1] = anim ? approach(gp[1], 0f, 12f, dt) : 0f;
            if (gp[1] <= 0.02f) {
                ghostPos.remove(i);
                ghostText.remove(i);
                continue;
            }
            text(g, font, ghostText.get(i), gp[0], baseY + (1f - gp[1]) * 3f,
                    UiTheme.withAlpha(dim(), gp[1] * 0.8f), SMALL);
        }

        if (searching) {
            float target = x0 - searchOffset + total + 0.5f;
            caretX = caretX < 0 || !anim ? target : approach(caretX, target, 22f, dt);
            long since = Util.getMillis() - lastTyped;
            float pulse = since < 600L ? 1f
                    : 0.5f + 0.5f * (float) Math.cos((Util.getMillis() % 1100L) / 1100.0 * Math.PI * 2.0);
            fill(g, caretX, cy - 3.5f, 0.8f, 7, 0.4f, UiTheme.withAlpha(accent, 0.25f + 0.75f * pulse));
        } else {
            caretX = -1f;
        }
    }

    // ── сайдбар ─────────────────────────────────────────────────────────

    private float tabY(int i) {
        return 56 + i * 20 + (i >= SPLIT_AT ? 10 : 0);
    }

    private void drawSidebar(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
        // логотип
        text(g, font, "elytrix", 12, ty(TITLE, 17), text(), TITLE);
        disc(g, 12 + width(font, "elytrix", TITLE) + 2.5f, 19.5f, 1.3f, accent);

        // поиск
        searchT = approach(searchT, searching ? 1f : 0f, 16f, dt);
        float sx = 7.5f;
        float sy = 31;
        float sw = BAR - 15;
        float sh = 17;
        fill(g, sx, sy, sw, sh, 5, field());
        outline(g, sx, sy, sw, sh, 5, 0.7f, UiTheme.mix(cardEdge(), accent, searchT));
        UiIcon.SEARCH.draw(g, (int) sx + 4, (int) (sy + 3.5f), 10, a(UiTheme.mix(dim(), accent, searchT)));
        UiDraw.scissor(g, (int) sx + 15, (int) sy, (int) (sx + sw - 4), (int) (sy + sh));
        float tcy = sy + sh / 2f;
        drawSearchText(g, font, sx + 17, tcy, sw - 24, dt);
        UiDraw.unscissor(g);

        // разделы
        hline(g, 6, tabY(SPLIT_AT) - 6.5f, BAR - 12, divider());
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            float y = tabY(i);
            boolean active = i == current && search.isEmpty();
            boolean hv = inside(mx, my, 5, y, BAR - 10, 15);
            if (hv && tabHover[i] < 0.02f && !active) {
                UiSound.play(UiSound.Event.HOVER);
            }
            tabAnim[i] = approach(tabAnim[i], active ? 1f : 0f, 14f, dt);
            tabHover[i] = approach(tabHover[i], hv ? 1f : 0f, 16f, dt);
            float t = tabAnim[i];
            if (tabHover[i] > 0.01f && t < 0.99f) {
                fill(g, 5, y, BAR - 10, 15, 4, UiTheme.withAlpha(text(), 0.05f * tabHover[i] * (1f - t)));
            }
            if (t > 0.01f) {
                // выбранный пункт: мягкая подложка + тонкая полоска акцента слева
                fill(g, 5, y, BAR - 10, 15, 4, UiTheme.withAlpha(text(), 0.07f * t));
            }
            int col = UiTheme.mix(UiTheme.mix(dim(), soft(), tabHover[i]), text(), t);
            int iconCol = UiTheme.mix(UiTheme.mix(dim(), soft(), tabHover[i]), accent, t);
            tab.icon().draw(g, 11, (int) y + 2, 10, a(iconCol));
            text(g, font, tab.name(), 25, ty(BODY, y + 7.5f), col, BODY);
        }

        drawProfile(g, font);
    }

    private void drawProfile(GuiGraphicsExtractor g, Font font) {
        float top = H - 35;
        hline(g, 6, top, BAR - 12, divider());
        float fx = 6;
        float fy = H - 29;
        fill(g, fx, fy, 23, 23, 5, field());
        var player = this.minecraft.player;
        String name = this.minecraft.getUser().getName();
        if (player != null) {
            PlayerFaceExtractor.extractRenderState(g, player.getSkin(), (int) fx + 2, (int) fy + 2, 19, a(0xFFFFFFFF));
        } else {
            hgrad(g, fx + 2, fy + 2, 19, 19, 4, accent, accent2());
            String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
            textCenter(g, font, letter, fx + 11.5f, ty(BODY, fy + 11.5f), 0xFFFFFFFF, BODY);
        }
        boolean online = this.minecraft.level != null;
        disc(g, fx + 21, fy + 21, 3f, sidebar());
        disc(g, fx + 21, fy + 21, 2f, online ? UiTheme.OK : dim());

        text(g, font, trim(font, name, BODY, BAR - 40), 34, ty(BODY, fy + 7), text(), BODY);
        text(g, font, "v" + ElytrixLoader.version(), 34, ty(MONO, fy + 17), dim(), MONO);
    }

    // ── содержимое ─────────────────────────────────────────────────────

    private void drawContent(GuiGraphicsExtractor g, Font font, double mx, double my, float dt) {
        int view = view();
        if (view == VIEW_CONSOLE) {
            console.layout(CX, 10, CW, H - 20);
            console.render(g, font, mx, my, dt);
            return;
        }
        if (view == VIEW_THEMES) {
            themes.layout(CX, 10, CW);
            themes.render(g, font, mx, my, dt);
            return;
        }

        scroll = approach(scroll, scrollTarget, 16f, dt);
        List<MenuCard> cards = visibleCards();

        float top = 10;
        if (!search.isEmpty()) {
            text(g, font, cards.isEmpty() ? "Ничего не найдено" : "Найдено: " + cards.size(),
                    CX, ty(SMALL, top + 5), dim(), SMALL);
            top += 16;
        }

        // «кирпич»: каждая карточка — в более короткую колонку
        float[] colY = {top - scroll, top - scroll};
        float[] colX = {CX, CX + COL_W + GAP};
        for (MenuCard card : cards) {
            int c = colY[0] <= colY[1] ? 0 : 1;
            float h = card.layout(font, colX[c], colY[c], COL_W);
            colY[c] += h + 8;
        }
        String footer = footerText();
        footerY = Math.max(colY[0], colY[1]) + 4;
        float total = Math.max(colY[0], colY[1]) + scroll + 2 + (footer != null ? 22 : 0);
        maxScroll = Math.max(0f, total - H);
        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll);

        UiDraw.scissor(g, (int) BAR + 1, 1, (int) W - 1, (int) H - 1);
        for (MenuCard card : cards) {
            card.render(g, font, mx, my, dt);
        }
        ru.rooyzee.elytrixclient.client.ui.menu.ModuleModal.render(g, (int) W, (int) H, (int) mx, (int) my, dt);
        ru.rooyzee.elytrixclient.client.ui.menu.XroseModal.render(g, (int) W, (int) H, (int) mx, (int) my, dt);
        ru.rooyzee.elytrixclient.client.ui.menu.MenuRow.drawTooltip(g);
        if (footer != null) {
            // неприметная текстовая ссылка внизу раздела
            float fw = width(font, footer, SMALL);
            float fx = CX + (CW - fw) / 2f;
            boolean fh = inside(mx, my, fx - 4, footerY, fw + 8, 14);
            if (fh && footerHover < 0.02f) {
                UiSound.play(UiSound.Event.HOVER);
            }
            footerHover = approach(footerHover, fh ? 1f : 0f, 14f, dt);
            int fc = UiTheme.mix(UiTheme.withAlpha(dim(), 0.8f), UiTheme.ERROR, footerHover);
            text(g, font, footer, fx, ty(SMALL, footerY + 7), fc, SMALL);
            if (footerHover > 0.01f) {
                hline(g, fx, footerY + 11, fw, UiTheme.withAlpha(UiTheme.ERROR, 0.6f * footerHover));
            }
        }
        UiDraw.unscissor(g);

        if (maxScroll > 0.5f) {
            float trackH = H - 20;
            float thumbH = Math.max(20, trackH * H / (H + maxScroll));
            float t = scroll / maxScroll;
            fill(g, W - 5, 10 + (trackH - thumbH) * t, 2, thumbH, 1, UiTheme.withAlpha(text(), 0.16f));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Ввод
    // ═════════════════════════════════════════════════════════════════════

    private boolean anyCapturing() {
        if (searching || console.capturing()) {
            return true;
        }
        for (MenuCard card : visibleCards()) {
            if (card.capturing()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layout();
        double mx = lx(event.x());
        double my = ly(event.y());
        int button = event.button();

        // Открытая модалка настроек модуля перехватывает весь ввод.
        if (ru.rooyzee.elytrixclient.client.ui.menu.ModuleModal.isOpen()
                || ru.rooyzee.elytrixclient.client.ui.menu.XroseModal.isOpen()) {
            return true;
        }

        if (!inside(mx, my, 0, 0, W, H)) {
            searching = false;
            console.blur();
            if (cfg.closeOnOutsideClick) {
                onClose();
            }
            return true;
        }

        // поиск
        boolean inSearch = inside(mx, my, 7.5f, 31, BAR - 15, 17);
        searching = inSearch && button == 0 || (inSearch && searching);
        if (inSearch) {
            if (button == 1) {
                search = "";
                replay();
            }
            return true;
        }

        // разделы
        for (int i = 0; i < tabs.size(); i++) {
            if (inside(mx, my, 5, tabY(i), BAR - 10, 15)) {
                select(i);
                return true;
            }
        }

        int view = view();
        if (view == VIEW_CONSOLE) {
            console.mouseClicked(mx, my, button);
            return true;
        }
        if (view == VIEW_THEMES) {
            themes.mouseClicked(mx, my, button);
            return true;
        }
        String footer = footerText();
        if (footer != null && button == 0) {
            float fw = width(this.font, footer, SMALL);
            float fx = CX + (CW - fw) / 2f;
            if (inside(mx, my, fx - 4, footerY, fw + 8, 14)) {
                UiSound.play(UiSound.Event.OFF);
                resetInterface();
                return true;
            }
        }
        if (mx >= BAR) {
            for (MenuCard card : visibleCards()) {
                card.mouseClicked(mx, my, button);
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mx = lx(event.x());
        double my = ly(event.y());
        for (MenuCard card : visibleCards()) {
            card.mouseReleased(mx, my, event.button());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = lx(mouseX);
        double my = ly(mouseY);
        if (view() == VIEW_CONSOLE) {
            return console.mouseScrolled(mx, my, scrollY);
        }
        if (inside(mx, my, BAR, 0, W - BAR, H)) {
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 24f, 0f, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (searching) {
            if (event.isEscape() || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                searching = false;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = event.hasControlDown() ? "" : search.substring(0, search.length() - 1);
                scrollTarget = 0;
                replay();
            } else if (event.isPaste()) {
                String clip = this.minecraft.keyboardHandler.getClipboard();
                if (clip != null) {
                    search = (search + clip.replaceAll("[\\r\\n\\t]", "")).trim();
                    replay();
                }
            }
            return true;
        }
        if (view() == VIEW_CONSOLE && console.keyPressed(event)) {
            return true;
        }
        for (MenuCard card : visibleCards()) {
            if (card.keyPressed(event)) {
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_RIGHT_CONTROL || event.isEscape()) {
            onClose();
            return true;
        }
        if (event.hasControlDown() && key == GLFW.GLFW_KEY_F) {
            searching = true;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searching) {
            if (event.isAllowedChatCharacter() && search.length() < 40) {
                search += event.codepointAsString();
                scrollTarget = 0;
                replay();
            }
            return true;
        }
        if (view() == VIEW_CONSOLE && console.charTyped(event)) {
            return true;
        }
        for (MenuCard card : visibleCards()) {
            if (card.charTyped(event)) {
                return true;
            }
        }
        return super.charTyped(event);
    }
}
