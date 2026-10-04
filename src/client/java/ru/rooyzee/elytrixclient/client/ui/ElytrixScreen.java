package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.ui.kit.UiButton;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDropdown;
import ru.rooyzee.elytrixclient.client.ui.kit.UiEmpty;
import ru.rooyzee.elytrixclient.client.ui.kit.UiIcon;
import ru.rooyzee.elytrixclient.client.ui.kit.UiInfo;
import ru.rooyzee.elytrixclient.client.ui.kit.UiSection;
import ru.rooyzee.elytrixclient.client.ui.kit.UiSlider;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiToggle;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Панель ElytrixClient — полностью кастомный рендер ({@code ui.kit}), без ванильных виджетов.
 *
 * <p>Слева — сайдбар с разделами, справа — карточки со строками. Вся композиция описана
 * в «базовых» единицах (560×344) и затем масштабируется: либо как масштаб интерфейса самой
 * игры (при {@code uiScaleIndex = 0}), либо как заданный в настройках процент. Из-за этого
 * панель перестаёт быть «одного размера» — её размер идёт за настройкой GUI Scale игры.
 *
 * <p>Открывается правым Ctrl (см. {@link ElytrixclientClient}), закрывается Esc,
 * повторным правым Ctrl или кликом вне панели.
 */
public class ElytrixScreen extends Screen {

    // ── базовая сетка композиции ─────────────────────────────────────────
    private static final int BASE_W = 560;
    private static final int BASE_H = 344;
    private static final int SCROLLBAR_W = 4;
    private static final int[] SCALE_OPTIONS = {0, 90, 100, 115, 130};
    private static final String[] SCALE_NAMES = {"Как в игре", "90%", "100%", "115%", "130%"};

    private static final int TAB_HOME = 0;
    private static final int TAB_BOTS = 1;
    private static final int TAB_PROXY = 2;
    private static final int TAB_CONSOLE = 3;
    private static final int TAB_SETTINGS = 4;

    private static final String[] TAB_NAMES = {"Главная", "Боты", "Прокси", "Консоль", "Настройки"};
    private static final String[] TAB_SUBS = {"Обзор клиента", "Список ботов", "Прокси для ботов", "Вывод процессов", "Вид и поведение"};
    private static final UiIcon[] TAB_ICONS = {
            UiIcon.HOME, UiIcon.BOTS, UiIcon.PROXY, UiIcon.CONSOLE, UiIcon.SETTINGS
    };

    private final Screen parent;
    private final ElytrixConfig cfg = ElytrixclientClient.CONFIG;
    private final List<List<UiSection>> tabs = new ArrayList<>();

    private int tab = TAB_HOME;
    private int prevTab = TAB_HOME;
    private double scroll;
    private double scrollTarget;
    private double maxScroll;
    private boolean draggingScrollbar;

    private long openedAt;
    private long lastFrame;
    private float panelT;
    private float tabT = 1f;
    private float indicatorY;
    private boolean indicatorInit;
    private boolean closing;
    private long closingAt;
    private boolean dirty;
    private long dirtyAt;

    private float uiScale = 1f;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int accentColor = UiTheme.ACCENTS[0];

    public ElytrixScreen(Screen parent) {
        super(Component.literal("Elytrix Client"));
        this.parent = parent;
    }

    public static ElytrixScreen create(Screen parent) {
        return new ElytrixScreen(parent);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Жизненный цикл
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        long now = Util.getMillis();
        if (openedAt == 0L) {
            openedAt = now;
        }
        lastFrame = now;
        UiTheme.applyPreset(cfg.themeIndex);
        UiWidget.ANIMATIONS = cfg.animations;
        rebuild();
        replayCurrent();
    }

    private void rebuild() {
        accentColor = UiTheme.accent(cfg.accentIndex);
        tabs.clear();
        tabs.add(homeTab());
        tabs.add(botsTab());
        tabs.add(proxyTab());
        tabs.add(consoleTab());
        tabs.add(settingsTab());
    }

    /** Перезапускает анимацию входа у карточек текущей вкладки (каскадом). */
    private void replayCurrent() {
        tabT = UiWidget.ANIMATIONS ? 0f : 1f;
        List<UiSection> sections = tabs.get(tab);
        for (int i = 0; i < sections.size(); i++) {
            UiSection section = sections.get(i);
            float baseDelay = 0.05f * i;
            section.replay(baseDelay);
            List<UiWidget> kids = section.children();
            for (int j = 0; j < kids.size(); j++) {
                kids.get(j).replay(baseDelay + 0.05f * (j + 1));
            }
        }
        indicatorInit = false;
    }

    @Override
    public void tick() {
        long now = Util.getMillis();
        if (dirty && now - dirtyAt > 450L) {
            dirty = false;
            cfg.save();
        }
        UiWidget.ANIMATIONS = cfg.animations;

        float dt = frameDelta(now);
        float k = UiWidget.ANIMATIONS ? dt * 6f : 1f;
        panelT = closing ? Math.max(0f, panelT - k) : Math.min(1f, panelT + k);
        tabT = Math.min(1f, tabT + (UiWidget.ANIMATIONS ? dt * 4.5f : 1f));
        scrollTarget = clamp(scrollTarget, 0, maxScroll);
        scroll += (scrollTarget - scroll) * (UiWidget.ANIMATIONS ? Math.min(1f, dt * 12f) : 1.0);
        if (closing && now - closingAt > 150L) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private float frameDelta(long now) {
        float dt = (now - lastFrame) / 1000f;
        lastFrame = now;
        return Math.max(0.0005f, Math.min(0.1f, dt));
    }

    @Override
    public void onClose() {
        if (closing) {
            return;
        }
        closing = true;
        closingAt = Util.getMillis();
    }

    private void markDirty() {
        dirty = true;
        dirtyAt = Util.getMillis();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Наполнение вкладок
    // ─────────────────────────────────────────────────────────────────────

    private List<UiSection> homeTab() {
        List<UiSection> list = new ArrayList<>();

        UiSection client = new UiSection("Elytrix Client", "Minecraft 26.2 · Fabric").badge("v" + ElytrixLoader.version());
        client.icon(UiIcon.BOLT);
        client.add(new UiInfo("Панель", () -> cfg.panelKey ? "правый Ctrl" : "выключена", UiTheme.OK).icon(UiIcon.KEY));
        client.add(new UiInfo("Цель по умолчанию", cfg::target, 0).icon(UiIcon.SERVER));
        client.add(new UiInfo("Конфиг", () -> String.valueOf(ElytrixConfig.file().getFileName()), 0).icon(UiIcon.FOLDER));
        list.add(client);

        UiSection modules = new UiSection("Модули", "Пока подключён только интерфейс");
        modules.icon(UiIcon.SHIELD);
        modules.add(new UiEmpty(UiIcon.BOTS, "Здесь появятся боты", "BotMark и SoulFire сведём в один список"));
        list.add(modules);
        return list;
    }

    private List<UiSection> botsTab() {
        UiSection bots = new UiSection("Боты", "Запуск и управление — следующим шагом").badge("0");
        bots.icon(UiIcon.BOTS);
        bots.add(new UiEmpty(UiIcon.BOTS, "Список пуст", "Пока это только каркас интерфейса"));
        return List.of(bots);
    }

    private List<UiSection> proxyTab() {
        UiSection proxy = new UiSection("Прокси", "Хранилище и проверка прокси").badge("0");
        proxy.icon(UiIcon.PROXY);
        proxy.add(new UiEmpty(UiIcon.GLOBE, "Прокси пока нет", "Сюда переедет импорт и проверка прокси"));
        return List.of(proxy);
    }

    private List<UiSection> consoleTab() {
        UiSection console = new UiSection("Консоль", "Вывод запущенных процессов").badge("●");
        console.icon(UiIcon.CONSOLE);
        console.add(new UiEmpty(UiIcon.CONSOLE, "Логов пока нет", "Запустим ботов — здесь появятся строки"));
        return List.of(console);
    }

    private List<UiSection> settingsTab() {
        List<UiSection> list = new ArrayList<>();

        UiSection look = new UiSection("Внешний вид", "Применяется сразу, сохраняется автоматически");
        look.icon(UiIcon.SETTINGS);
        look.add(new UiDropdown("Тема", List.of(UiTheme.PRESET_NAMES), cfg.themeIndex, i -> {
            cfg.themeIndex = i;
            UiTheme.applyPreset(i);
            markDirty();
        }));
        look.add(new UiDropdown("Акцент", List.of(UiTheme.ACCENT_NAMES), cfg.accentIndex, i -> {
            cfg.accentIndex = i;
            accentColor = UiTheme.accent(i);
            cfg.accent = String.format("#%06X", accentColor & 0xFFFFFF);
            markDirty();
        }));
        look.add(new UiDropdown("Масштаб панели", List.of(SCALE_NAMES), cfg.uiScaleIndex, i -> {
            cfg.uiScaleIndex = i;
            markDirty();
        }));
        look.add(new UiSlider("Прозрачность панели", "%", 60, 100, cfg.panelOpacity, v -> {
            cfg.panelOpacity = v;
            markDirty();
        }));
        list.add(look);

        UiSection anim = new UiSection("Анимации", "Появление, переключение вкладок, тумблеры");
        anim.icon(UiIcon.PLAY);
        anim.add(new UiToggle("Плавные анимации", cfg.animations, v -> {
            cfg.animations = v;
            UiWidget.ANIMATIONS = v;
            markDirty();
        }));
        anim.add(new UiToggle("Размытие фона", "Размывает мир за панелью", cfg.blurBackground, v -> {
            cfg.blurBackground = v;
            markDirty();
        }));
        list.add(anim);

        UiSection menu = new UiSection("Главное меню и загрузка", null);
        menu.icon(UiIcon.SHIELD);
        menu.add(new UiToggle("Хакерский фон меню", "Свой анимированный фон вместо панорамы", cfg.hackerBackground, v -> {
            cfg.hackerBackground = v;
            markDirty();
        }));
        menu.add(new UiToggle("Свой экран загрузки", "Вместо ванильного красного лоадера", cfg.customLoading, v -> {
            cfg.customLoading = v;
            markDirty();
        }));
        list.add(menu);

        UiSection behaviour = new UiSection("Поведение", null);
        behaviour.icon(UiIcon.GLOBE);
        behaviour.add(new UiToggle("Панель по правому Ctrl", cfg.panelKey, v -> {
            cfg.panelKey = v;
            markDirty();
        }));
        behaviour.add(new UiToggle("Закрывать кликом вне панели", cfg.closeOnOutsideClick, v -> {
            cfg.closeOnOutsideClick = v;
            markDirty();
        }));
        behaviour.add(new UiButton("Открыть папку конфига", UiIcon.FOLDER, UiButton.Style.SECONDARY, this::openConfigFolder));
        behaviour.add(new UiButton("Сбросить настройки интерфейса", UiIcon.REFRESH, UiButton.Style.DANGER, this::resetInterface));
        list.add(behaviour);

        return list;
    }

    private void openConfigFolder() {
        try {
            Util.getPlatform().openFile(ElytrixConfig.file().getParent().toFile());
        } catch (Throwable t) {
            ElytrixclientClient.LOG.add("[Elytrix] Не удалось открыть папку конфига: " + t);
        }
    }

    private void resetInterface() {
        cfg.themeIndex = 0;
        cfg.accentIndex = 0;
        cfg.uiScaleIndex = 0;
        cfg.panelOpacity = 88;
        cfg.animations = true;
        cfg.blurBackground = true;
        cfg.hackerBackground = true;
        cfg.customLoading = true;
        cfg.closeOnOutsideClick = false;
        cfg.panelKey = true;
        UiTheme.applyPreset(cfg.themeIndex);
        UiWidget.ANIMATIONS = cfg.animations;
        cfg.save();
        accentColor = UiTheme.accent(cfg.accentIndex);
        rebuild();
        replayCurrent();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Раскладка и анимация панели
    // ─────────────────────────────────────────────────────────────────────

    private void layoutPanel() {
        float user = cfg.uiScaleIndex <= 0 ? 0f
                : SCALE_OPTIONS[Mth.clamp(cfg.uiScaleIndex, 0, SCALE_OPTIONS.length - 1)] / 100f;
        float auto = Math.max(1f, this.minecraft.getWindow().getGuiScale()) / 2f;
        float base = user > 0f ? user : auto;
        float fit = Math.min((this.width - 16f) / BASE_W, (this.height - 16f) / BASE_H);
        uiScale = Mth.clamp(Math.min(base, fit), 0.5f, 2.5f);
        panelW = Math.round(BASE_W * uiScale);
        panelH = Math.round(BASE_H * uiScale);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private double ux(double screenX) {
        return (screenX - panelX) / uiScale;
    }

    private double uy(double screenY) {
        return (screenY - panelY) / uiScale;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Отрисовка
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (cfg.blurBackground && this.minecraft != null
                && this.minecraft.options.getMenuBackgroundBlurriness() < 1.0F) {
            graphics.blurBeforeThisStratum();
        }
        float f = easeOut(Mth.clamp(panelT, 0f, 1f));
        graphics.fill(0, 0, this.width, this.height, UiTheme.withAlpha(UiTheme.SCRIM, f));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        layoutPanel();
        long now = Util.getMillis();
        float dt = frameDelta(now);
        float f = easeOut(Mth.clamp(panelT, 0f, 1f));
        if (f <= 0.01f) {
            return;
        }
        float s = uiScale * (0.97f + 0.03f * f);
        float shake = (1f - f) * -10f;
        var font = this.font;

        // ── содержимое рисуется в «базовых» единицах относительно угла панели
        graphics.pose().pushMatrix();
        graphics.pose().translate(panelX, panelY + shake);
        graphics.pose().scale(s, s);

        drawPanel(graphics, font, mouseX, mouseY, dt, f);
        drawSidebar(graphics, font, mouseX, mouseY, dt, f);
        drawHeader(graphics, font, mouseX, mouseY, f);
        drawContent(graphics, font, mouseX, mouseY, dt, f);

        graphics.pose().popMatrix();

        // ── подсказка над панелью (в экранных координатах, чтобы не масштабировалась)
        if (mouseY > panelY + UiTheme.HEADER_H * uiScale && mouseY < panelY + panelH) {
            String tip = hoveredTooltip(mouseX, mouseY);
            if (tip != null) {
                int tw = font.width(tip) + 10;
                int tx = Mth.clamp(mouseX + 10, 2, this.width - tw - 2);
                int ty = mouseY + 12;
                UiDraw.roundRect(graphics, tx, ty, tw, 16, UiTheme.R_SM, UiTheme.withAlpha(UiTheme.POPUP, 0.96f));
                UiDraw.text(graphics, font, tip, tx + 5, ty + 4, UiTheme.TEXT_SOFT);
            }
        }
    }

    private void drawPanel(GuiGraphicsExtractor graphics, Font font,
                           int mouseX, int mouseY, float dt, float f) {
        float fo = f * Mth.clamp(cfg.panelOpacity / 100f, 0.35f, 1f);
        UiDraw.shadow(graphics, 0, 0, BASE_W, BASE_H, UiTheme.R_LG, 7, UiTheme.withAlpha(UiTheme.SHADOW, f));
        UiDraw.roundRectBordered(graphics, 0, 0, BASE_W, BASE_H, UiTheme.R_LG,
                UiTheme.withAlpha(UiTheme.PANEL, fo), UiTheme.withAlpha(UiTheme.BORDER, fo));
        // акцентная кромка сверху — «подсветка» панели
        int edgeW = BASE_W - 2 * UiTheme.R_LG;
        int edgeHalf = edgeW / 2;
        UiDraw.hGradient(graphics, UiTheme.R_LG, 0, edgeHalf, 2,
                0x00000000, UiTheme.withAlpha(accentColor, 0.8f * f), 40);
        UiDraw.hGradient(graphics, UiTheme.R_LG + edgeHalf, 0, edgeW - edgeHalf, 2,
                UiTheme.withAlpha(accentColor, 0.8f * f), 0x00000000, 40);
    }

    private void drawSidebar(GuiGraphicsExtractor graphics, Font font,
                             int mouseX, int mouseY, float dt, float f) {
        int cardX = 6;
        int cardY = 6;
        int cardW = UiTheme.SIDEBAR_W - 6;
        int cardH = BASE_H - 12;
        UiDraw.roundRectBordered(graphics, cardX, cardY, cardW, cardH, UiTheme.R_MD,
                UiTheme.withAlpha(UiTheme.SIDEBAR, f * 0.96f), UiTheme.withAlpha(UiTheme.BORDER_SOFT, f));

        int navX = cardX + 5;
        int navW = cardW - 10;
        int itemH = 30;
        int step = 34;
        int firstY = cardY + 12;

        // плавно переезжающий индикатор выбранного пункта
        float targetY = firstY + tab * step;
        if (!indicatorInit) {
            indicatorY = targetY;
            indicatorInit = true;
        }
        indicatorY += (targetY - indicatorY) * (UiWidget.ANIMATIONS ? Math.min(1f, dt * 14f) : 1f);
        UiDraw.roundRect(graphics, navX, Math.round(indicatorY), navW, itemH, UiTheme.R_MD,
                UiTheme.withAlpha(UiTheme.accentSoft(accentColor, 0.30f), f));
        UiDraw.roundRect(graphics, navX + 1, Math.round(indicatorY) + 7, 3, itemH - 14, 2,
                UiTheme.withAlpha(accentColor, f));

        double mx = ux(mouseX);
        double my = uy(mouseY);
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int iy = firstY + i * step;
            boolean selected = i == tab;
            boolean hover = mx >= navX && mx < navX + navW && my >= iy && my < iy + itemH;
            if (hover && !selected) {
                UiDraw.roundRect(graphics, navX, iy, navW, itemH, UiTheme.R_MD, UiTheme.withAlpha(UiTheme.ROW_HOVER, f));
            }
            int iconColor = selected ? UiTheme.mix(accentColor, 0xFFFFFFFF, 0.25f)
                    : (hover ? UiTheme.TEXT : UiTheme.TEXT_DIM);
            TAB_ICONS[i].draw(graphics, navX + 9, iy + 7, 16, UiTheme.withAlpha(iconColor, f));
            UiDraw.text(graphics, font, TAB_NAMES[i], navX + 33, iy + 11,
                    UiTheme.withAlpha(selected ? UiTheme.TEXT : UiTheme.TEXT_SOFT, f));
            if (selected) {
                UiDraw.disc(graphics, navX + navW - 9, iy + itemH / 2f, 2.4f, UiTheme.withAlpha(accentColor, f));
            }
        }

        UiDraw.text(graphics, font, "правый Ctrl · Esc", cardX + 12, cardY + cardH - 18,
                UiTheme.withAlpha(UiTheme.TEXT_DIM, f));
        UiDraw.textSpaced(graphics, font, "ELYTRIX", cardX + 12, cardY + cardH - 32, 2,
                UiTheme.withAlpha(UiTheme.mix(accentColor, UiTheme.TEXT, 0.35f), f), false);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Font font,
                            int mouseX, int mouseY, float f) {
        int hx = UiTheme.SIDEBAR_W + 4;
        graphics.pose().pushMatrix();
        graphics.pose().translate(hx + 12, 12);
        graphics.pose().scale(1.4f, 1.4f);
        UiDraw.text(graphics, font, TAB_NAMES[tab], 0, 0, UiTheme.withAlpha(UiTheme.TEXT, f));
        graphics.pose().popMatrix();
        UiDraw.text(graphics, font, TAB_SUBS[tab], hx + 13, 30, UiTheme.withAlpha(UiTheme.TEXT_DIM, f));
        UiDraw.hLine(graphics, hx + 10, BASE_W - 12, UiTheme.HEADER_H - 3, 1,
                UiTheme.withAlpha(UiTheme.DIVIDER, f));

        // кнопка закрытия
        int closeX = BASE_W - 38;
        int closeY = 13;
        double mx = ux(mouseX);
        double my = uy(mouseY);
        boolean hover = mx >= closeX && mx < closeX + 26 && my >= closeY && my < closeY + 20;
        UiDraw.roundRect(graphics, closeX, closeY, 26, 20, UiTheme.R_SM,
                UiTheme.withAlpha(hover ? UiTheme.ERROR : UiTheme.ROW, f));
        UiIcon.CLOSE.drawCentered(graphics, closeX + 13, closeY + 10, 12,
                UiTheme.withAlpha(hover ? 0xFFFFFFFF : UiTheme.TEXT_SOFT, f));
    }

    private void drawContent(GuiGraphicsExtractor graphics, Font font,
                             int mouseX, int mouseY, float dt, float f) {
        int contentX = UiTheme.SIDEBAR_W + 4;
        int contentY = UiTheme.HEADER_H;
        int contentW = BASE_W - contentX - 22;
        int contentH = BASE_H - contentY - 10;

        List<UiSection> sections = tabs.get(tab);
        int total = 0;
        for (UiSection sect : sections) {
            total += sect.contentHeight() + 8;
        }
        maxScroll = Math.max(0, total - contentH);

        // сдвиг при переключении вкладки (проявление делает анимация самих виджетов)
        float t = UiWidget.ANIMATIONS ? easeOut(Mth.clamp(tabT, 0f, 1f)) : 1f;
        int slide = Math.round((1f - t) * (tab >= prevTab ? 20f : -20f));

        // scissor применяется в текущей позе — координаты тоже в «базовых» единицах
        graphics.enableScissor(contentX, contentY, contentX + contentW + SCROLLBAR_W + 6, contentY + contentH);

        int y = (int) Math.round(contentY + 4 - scroll);
        for (UiSection sect : sections) {
            sect.accent = accentColor;
            sect.layoutAt(contentX + 6 + slide, y, contentW - 12);
            sect.render(graphics, (int) ux(mouseX), (int) uy(mouseY), dt);
            y += sect.contentHeight() + 8;
        }
        for (UiSection sect : sections) {
            for (UiWidget wd : sect.children()) {
                if (wd.hasPopup()) {
                    wd.popupLimitTop = contentY;
                    wd.popupLimitBottom = contentY + contentH;
                    wd.renderPopup(graphics, (int) ux(mouseX), (int) uy(mouseY), dt);
                }
            }
        }
        graphics.disableScissor();

        // полоса прокрутки
        if (maxScroll > 1) {
            int barX = contentX + contentW - 2;
            int barTop = contentY + 6;
            int barH = contentH - 12;
            UiDraw.roundRect(graphics, barX, barTop, 3, barH, 2, UiTheme.withAlpha(UiTheme.TRACK, 0.75f * f));
            int thumbH = Math.max(24, (int) (barH * (contentH / (double) (contentH + maxScroll))));
            int thumbY = barTop + (int) ((barH - thumbH) * (scroll / maxScroll));
            UiDraw.roundRect(graphics, barX - 1, thumbY, 5, thumbH, 2, UiTheme.withAlpha(accentColor, 0.9f * f));
        }
    }

    private String hoveredTooltip(int mouseX, int mouseY) {
        int mx = (int) ux(mouseX);
        int my = (int) uy(mouseY);
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.tooltip != null && w.contains(mx, my)) {
                    return w.tooltip;
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Ввод
    // ─────────────────────────────────────────────────────────────────────

    private boolean inPanel(double mx, double my) {
        return mx >= panelX && mx < panelX + panelW && my >= panelY && my < panelY + panelH;
    }

    private boolean inContent(double mx, double my) {
        double x = ux(mx);
        double y = uy(my);
        return x >= UiTheme.SIDEBAR_W + 4 && x < BASE_W && y >= UiTheme.HEADER_H && y < BASE_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layoutPanel();
        double mx = event.x();
        double my = event.y();
        int button = event.button();
        double lx = ux(mx);
        double ly = uy(my);

        // открытые выпадающие списки перехватывают клик первыми
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.hasPopup() && w.mouseClicked(lx, ly, button)) {
                    return true;
                }
            }
        }

        // кнопка закрытия
        if (lx >= BASE_W - 38 && lx < BASE_W - 12 && ly >= 13 && ly < 33) {
            onClose();
            return true;
        }

        // сайдбар
        int cardX = 6;
        int cardW = UiTheme.SIDEBAR_W - 6;
        int firstY = 18;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int iy = firstY + i * 34;
            if (lx >= cardX + 5 && lx < cardX + cardW - 5 && ly >= iy && ly < iy + 30) {
                if (i != tab) {
                    prevTab = tab;
                    tab = i;
                    scroll = 0;
                    scrollTarget = 0;
                    replayCurrent();
                }
                return true;
            }
        }

        // содержимое
        if (inContent(mx, my)) {
            for (UiSection s : tabs.get(tab)) {
                for (UiWidget w : s.children()) {
                    if (w.mouseClicked(lx, ly, button)) {
                        return true;
                    }
                }
            }
            return true;
        }

        if (!inPanel(mx, my) && cfg.closeOnOutsideClick) {
            onClose();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        double lx = ux(event.x());
        double ly = uy(event.y());
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.mouseReleased(lx, ly, event.button())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar) {
            scrollToMouse(event.y());
            return true;
        }
        double lx = ux(event.x());
        double ly = uy(event.y());
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.mouseDragged(lx, ly, event.button())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void scrollToMouse(double mouseY) {
        double contentY = UiTheme.HEADER_H;
        double contentH = BASE_H - contentY - 10;
        double t = (uy(mouseY) - contentY - 6) / Math.max(1.0, contentH - 12);
        scrollTarget = clamp(t, 0, 1) * maxScroll;
        scroll = scrollTarget;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (inContent(mx, my)) {
            scrollTarget = clamp(scrollTarget - scrollY * 28, 0, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            onClose();
            return true;
        }
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.keyPressed(event.key(), event.scancode(), event.modifiers())) {
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }
}
