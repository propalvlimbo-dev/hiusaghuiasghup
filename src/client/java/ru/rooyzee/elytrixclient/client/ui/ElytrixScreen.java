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
import ru.rooyzee.elytrixclient.client.ui.kit.UiText;
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
 * в единицах интерфейса игры (при {@code uiScaleIndex = 0}) или как заданный процент. Поэтому
 * панель перестаёт быть «одного размера» — её размер идёт за настройкой GUI Scale игры.
 *
 * <p>Открывается правым Ctrl (см. {@link ElytrixclientClient}), закрывается Esc,
 * повторным правым Ctrl или кликом вне панели.
 */
public class ElytrixScreen extends Screen {

    // ── базовая сетка композиции (эталон) ────────────────────────────────
    private static final int BASE_W = 560;
    private static final int BASE_H = 344;
    /** Наименьший «базовый» размер: ниже него включается компактная вёрстка. */
    private static final int MIN_BASE_W = 380;
    private static final int MIN_BASE_H = 250;
    private static final int COMPACT_BELOW = 500;
    private static final int SCROLLBAR_W = 4;
    /** Место справа под «оверлейный» скроллбар, чтобы он не перекрывал карточки. */
    private static final int SCROLLBAR_SPACE = 12;
    private static final int CLOSE_SIZE = 26;
    private static final int CLOSE_Y = 16;
    private static final int[] SCALE_OPTIONS = {0, 90, 100, 115, 130};
    private static final String[] SCALE_NAMES = {"Авто (по окну)", "90%", "100%", "115%", "130%"};

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

    /** Текущий «базовый» размер композиции: 560×344 на нормальном экране, меньше — на маленьком. */
    private int baseW = BASE_W;
    private int baseH = BASE_H;
    /** Ширина сайдбара: в компактном режиме — только иконки. */
    private int sidebarW = UiTheme.SIDEBAR_W;
    private boolean compact;
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
            float baseDelay = 0.03f * i;
            section.replay(baseDelay);
            List<UiWidget> kids = section.children();
            for (int j = 0; j < kids.size(); j++) {
                kids.get(j).replay(baseDelay + 0.03f * (j + 1));
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
        // открытие/закрытие — быстро (≈110 мс), иначе панель «вылезает» слишком долго
        float k = UiWidget.ANIMATIONS ? dt * 11f : 1f;
        panelT = closing ? Math.max(0f, panelT - k) : Math.min(1f, panelT + k);
        tabT = Math.min(1f, tabT + (UiWidget.ANIMATIONS ? dt * 8f : 1f));
        scrollTarget = clamp(scrollTarget, 0, maxScroll);
        scroll += (scrollTarget - scroll) * (UiWidget.ANIMATIONS ? Math.min(1f, dt * 12f) : 1.0);
        if (closing && now - closingAt > 110L) {
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

        UiSection client = new UiSection("Elytrix Client", "Minecraft 26.2 · Fabric")
                .badge("v" + ElytrixLoader.version());
        client.icon(UiIcon.BOLT);
        client.add(new UiInfo("Панель", () -> cfg.panelKey ? "правый Ctrl" : "выключена", UiTheme.OK)
                .icon(UiIcon.KEY));
        client.add(new UiInfo("Цель по умолчанию", cfg::target, 0).icon(UiIcon.SERVER));
        client.add(new UiInfo("Конфиг", () -> String.valueOf(ElytrixConfig.file().getFileName()), 0)
                .icon(UiIcon.FOLDER));
        list.add(client);

        UiSection live = new UiSection("Состояние", "Живые данные клиента");
        live.icon(UiIcon.CHART);
        live.add(new UiInfo("FPS / эффекты",
                () -> this.minecraft.getFps() + " · " + ElytrixQuality.summary(), UiTheme.OK).icon(UiIcon.BOLT));
        live.add(new UiInfo("Тема", () -> UiTheme.PRESET_NAMES[Mth.clamp(cfg.themeIndex, 0,
                UiTheme.PRESET_NAMES.length - 1)], 0).icon(UiIcon.SETTINGS));
        live.add(new UiInfo("Размер панели", () -> SCALE_NAMES[Mth.clamp(cfg.uiScaleIndex, 0,
                SCALE_NAMES.length - 1)], 0).icon(UiIcon.LIST));
        list.add(live);

        UiSection modules = new UiSection("Модули", "Боты, прокси и стресс-тест — следующим шагом");
        modules.icon(UiIcon.SHIELD);
        modules.add(new UiEmpty(UiIcon.BOTS, "Здесь появятся боты",
                "BotMark и SoulFire сведём в один список"));
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
        look.add(new UiDropdown("Размер панели", List.of(SCALE_NAMES), cfg.uiScaleIndex, i -> {
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
        anim.add(new UiDropdown("Качество эффектов", List.of(ElytrixQuality.NAMES), cfg.effectsQuality, i -> {
            cfg.effectsQuality = i;
            ElytrixQuality.update(cfg, this.minecraft.getWindow().getWidth() * this.minecraft.getWindow().getHeight(),
                    this.minecraft.getFps());
            markDirty();
        }));
        anim.add(new UiInfo("FPS / эффекты", () -> this.minecraft.getFps() + " · " + ElytrixQuality.summary(), UiTheme.OK)
                .icon(UiIcon.CHART));
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
        cfg.effectsQuality = 0;
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

    /**
     * Адаптация под любое разрешение и масштаб интерфейса.
     *
     * <p>Панель раскладывается в тех же единицах, в которых рисует сама игра
     * ({@code this.width}/{@code this.height} уже учитывают «Масштаб интерфейса»
     * Minecraft). Раньше всё дополнительно масштабировалось матрицей на дробный
     * множитель — именно из-за этого текст и иконки выглядели «пиксельно» и
     * размыто. Теперь масштабирования нет вообще: 1 единица раскладки = 1 единица
     * интерфейса, поэтому картинка резкая, а размер панели сам следует за
     * настройкой масштаба игры. Пресет «Размер панели» меняет только габариты,
     * «Авто» подбирает их по размеру окна. На маленьких окнах включается
     * компактная вёрстка — сайдбар становится иконочным.
     */
    private void layoutPanel() {
        float k;
        if (cfg.uiScaleIndex <= 0) {
            float area = Math.max(1f, this.width) * Math.max(1f, this.height);
            k = Mth.clamp((float) Math.sqrt(area / (1920f * 1080f)) * 1.15f, 0.85f, 1.6f);
        } else {
            k = SCALE_OPTIONS[Mth.clamp(cfg.uiScaleIndex, 0, SCALE_OPTIONS.length - 1)] / 100f;
        }
        int maxW = Math.max(200, this.width - 16);
        int maxH = Math.max(150, this.height - 16);
        baseW = Math.min(Math.max(Math.round(BASE_W * k), Math.min(MIN_BASE_W, maxW)), maxW);
        baseH = Math.min(Math.max(Math.round(BASE_H * k), Math.min(MIN_BASE_H, maxH)), maxH);
        compact = baseW < COMPACT_BELOW || baseH < 300;
        sidebarW = compact ? 54 : UiTheme.SIDEBAR_W;

        panelW = baseW;
        panelH = baseH;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private double ux(double screenX) {
        return screenX - panelX;
    }

    private double uy(double screenY) {
        return screenY - panelY;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Отрисовка
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        // на больших разрешениях blur стоит дорого — режим «Авто» его выключает
        if (cfg.blurBackground && ElytrixQuality.blur() && this.minecraft != null
                && this.minecraft.options.getMenuBackgroundBlurriness() < 1.0F) {
            graphics.blurBeforeThisStratum();
        }
        float f = easeOut(Mth.clamp(panelT, 0f, 1f));
        graphics.fill(0, 0, this.width, this.height, UiTheme.withAlpha(UiTheme.SCRIM, f));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        layoutPanel();
        ElytrixQuality.updateFrom(this.minecraft, cfg);
        long now = Util.getMillis();
        float dt = frameDelta(now);
        float f = easeOut(Mth.clamp(panelT, 0f, 1f));
        if (f <= 0.01f) {
            return;
        }
        float shake = (1f - f) * -6f;
        var font = this.font;

        // ── содержимое рисуется в «базовых» единицах относительно угла панели
        graphics.pose().pushMatrix();
        graphics.pose().translate(panelX, panelY + shake);

        drawPanel(graphics, font, mouseX, mouseY, dt, f);
        drawSidebar(graphics, font, mouseX, mouseY, dt, f);
        drawHeader(graphics, font, mouseX, mouseY, f);
        drawContent(graphics, font, mouseX, mouseY, dt, f);

        graphics.pose().popMatrix();

        // ── подсказка над панелью (в экранных координатах, чтобы не масштабировалась)
        if (mouseY > panelY + UiTheme.HEADER_H && mouseY < panelY + panelH) {
            String tip = hoveredTooltip(mouseX, mouseY);
            if (tip != null) {
                int tw = UiDraw.width(font, tip) + 10;
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
        UiDraw.shadow(graphics, 0, 0, baseW, baseH, UiTheme.R_LG, 6, UiTheme.withAlpha(UiTheme.SHADOW, f));
        UiDraw.roundRectBordered(graphics, 0, 0, baseW, baseH, UiTheme.R_LG,
                UiTheme.withAlpha(UiTheme.PANEL, fo), UiTheme.withAlpha(UiTheme.BORDER, fo));
        // тонкая внутренняя подсветка сверху — «стекло», без цветной полосы
        UiDraw.hLine(graphics, UiTheme.R_LG + 8, baseW - UiTheme.R_LG - 8, 1, 1,
                UiTheme.withAlpha(0xFFFFFFFF, 0.06f * f));
    }

    // ── геометрия контента (одна на отрисовку и на ввод) ──

    private int contentX() {
        return sidebarW + 6;
    }

    private int contentY() {
        return UiTheme.HEADER_H + 2;
    }

    private int contentW() {
        return baseW - contentX() - 16 - SCROLLBAR_SPACE;
    }

    private int contentH() {
        return baseH - contentY() - 12;
    }

    private int closeX() {
        return baseW - CLOSE_SIZE - 14;
    }

    private int navItemH() {
        return compact ? 28 : 30;
    }

    private int navStep() {
        return compact ? 30 : 34;
    }

    private static final int NAV_FIRST_Y = 18;

    private void drawSidebar(GuiGraphicsExtractor graphics, Font font,
                             int mouseX, int mouseY, float dt, float f) {
        int cardX = 7;
        int cardY = 7;
        int cardW = sidebarW - 7;
        int cardH = baseH - 14;
        UiDraw.roundRectBordered(graphics, cardX, cardY, cardW, cardH, UiTheme.R_LG,
                UiTheme.withAlpha(UiTheme.SIDEBAR, f * 0.92f), UiTheme.withAlpha(UiTheme.BORDER_SOFT, f));

        int navX = cardX + 6;
        int navW = cardW - 12;
        int itemH = navItemH();
        int step = navStep();

        float targetY = NAV_FIRST_Y + tab * step;
        if (!indicatorInit) {
            indicatorY = targetY;
            indicatorInit = true;
        }
        indicatorY += (targetY - indicatorY) * (UiWidget.ANIMATIONS ? Math.min(1f, dt * 16f) : 1f);

        // выбранный пункт — аккуратная «пилюля», как в списках Apple
        UiDraw.roundRect(graphics, navX, Math.round(indicatorY), navW, itemH, UiTheme.R_MD,
                UiTheme.withAlpha(UiTheme.accentSoft(accentColor, 0.18f), f));

        double mx = ux(mouseX);
        double my = uy(mouseY);
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int iy = NAV_FIRST_Y + i * step;
            boolean selected = i == tab;
            boolean hover = mx >= navX && mx < navX + navW && my >= iy && my < iy + itemH;
            if (hover && !selected) {
                UiDraw.roundRect(graphics, navX, iy, navW, itemH, UiTheme.R_MD,
                        UiTheme.withAlpha(UiTheme.ROW_HOVER, f));
            }
            int iconColor = selected ? UiTheme.mix(accentColor, 0xFFFFFFFF, 0.35f)
                    : (hover ? UiTheme.TEXT : UiTheme.TEXT_DIM);
            if (compact) {
                TAB_ICONS[i].drawCentered(graphics, navX + navW / 2, iy + itemH / 2, 16,
                        UiTheme.withAlpha(iconColor, f));
            } else {
                TAB_ICONS[i].draw(graphics, navX + 11, iy + (itemH - 16) / 2, 16, UiTheme.withAlpha(iconColor, f));
                UiDraw.text(graphics, font, UiDraw.trim(font, TAB_NAMES[i], navW - 44), navX + 35,
                        iy + (itemH - 8) / 2, UiTheme.withAlpha(selected ? UiTheme.TEXT : UiTheme.TEXT_SOFT, f));
            }
        }

        if (!compact) {
            int footY = cardY + cardH - 36;
            UiDraw.hLine(graphics, navX, navX + navW, footY - 8, 1, UiTheme.withAlpha(UiTheme.DIVIDER, f));
            UiDraw.textSpaced(graphics, font, "ELYTRIX", navX + 2, footY, 3,
                    UiTheme.withAlpha(UiTheme.TEXT_DIM, f), false, UiText.MONO);
            UiDraw.text(graphics, font, "правый Ctrl · Esc", navX + 2, footY + 15,
                    UiTheme.withAlpha(UiTheme.TEXT_DIM, f));
        }
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Font font,
                            int mouseX, int mouseY, float f) {
        int hx = sidebarW + 6;
        // заголовок — своим крупным шрифтом 1:1 (без масштабирования матрицей)
        UiDraw.text(graphics, font, TAB_NAMES[tab], hx + 14, 15, UiTheme.withAlpha(UiTheme.TEXT, f), UiText.TITLE);
        UiDraw.text(graphics, font, TAB_SUBS[tab], hx + 15, 40, UiTheme.withAlpha(UiTheme.TEXT_DIM, f),
                UiText.MONO);

        int cx = closeX();
        double mx = ux(mouseX);
        double my = uy(mouseY);
        boolean hover = mx >= cx && mx < cx + CLOSE_SIZE && my >= CLOSE_Y && my < CLOSE_Y + CLOSE_SIZE;
        UiDraw.roundRectBordered(graphics, cx, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE, UiTheme.R_SM,
                UiTheme.withAlpha(hover ? UiTheme.ROW_HOVER : UiTheme.ROW, f),
                UiTheme.withAlpha(UiTheme.BORDER_SOFT, f));
        UiIcon.CLOSE.drawCentered(graphics, cx + CLOSE_SIZE / 2, CLOSE_Y + CLOSE_SIZE / 2, 12,
                UiTheme.withAlpha(hover ? UiTheme.TEXT : UiTheme.TEXT_DIM, f));

        UiDraw.hLine(graphics, hx + 8, baseW - 12, UiTheme.HEADER_H - 1, 1, UiTheme.withAlpha(UiTheme.DIVIDER, f));
    }

    private void drawContent(GuiGraphicsExtractor graphics, Font font,
                             int mouseX, int mouseY, float dt, float f) {
        int contentX = contentX();
        int contentY = contentY();
        int contentW = contentW();
        int contentH = contentH();
        int scrollbarSpace = SCROLLBAR_SPACE;

        List<UiSection> sections = tabs.get(tab);
        int total = 0;
        for (UiSection sect : sections) {
            total += sect.contentHeight() + 10;
        }
        maxScroll = Math.max(0, total - contentH);

        float t = UiWidget.ANIMATIONS ? easeOut(Mth.clamp(tabT, 0f, 1f)) : 1f;
        int slide = Math.round((1f - t) * 12f);

        UiDraw.scissor(graphics, contentX - 2, contentY, contentX + contentW + scrollbarSpace, contentY + contentH);

        int y = (int) Math.round(contentY - scroll) + slide;
        for (UiSection sect : sections) {
            sect.accent = accentColor;
            sect.layoutAt(contentX, y, contentW);
            sect.render(graphics, (int) ux(mouseX), (int) uy(mouseY), dt);
            y += sect.contentHeight() + 10;
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
        UiDraw.unscissor(graphics);

        // тонкий «оверлейный» скроллбар у правого края — карточки он не перекрывает
        if (maxScroll > 1) {
            int barX = contentX + contentW + 6;
            int barTop = contentY + 4;
            int barH = contentH - 8;
            int thumbH = Math.max(26, (int) (barH * (contentH / (double) (contentH + maxScroll))));
            int thumbY = barTop + (int) ((barH - thumbH) * (scroll / maxScroll));
            UiDraw.roundRect(graphics, barX, thumbY, 3, thumbH, 2, UiTheme.withAlpha(accentColor, 0.8f * f));
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
        return x >= contentX() - 4 && x < contentX() + contentW() + SCROLLBAR_SPACE
                && y >= contentY() && y < contentY() + contentH();
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
        if (lx >= closeX() && lx < closeX() + CLOSE_SIZE && ly >= CLOSE_Y && ly < CLOSE_Y + CLOSE_SIZE) {
            onClose();
            return true;
        }

        // сайдбар
        int cardX = 7;
        int cardW = sidebarW - 7;
        int itemH = navItemH();
        int step = navStep();
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int iy = NAV_FIRST_Y + i * step;
            if (lx >= cardX + 5 && lx < cardX + cardW - 5 && ly >= iy && ly < iy + itemH) {
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
        double contentY = contentY();
        double contentH = contentH();
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
