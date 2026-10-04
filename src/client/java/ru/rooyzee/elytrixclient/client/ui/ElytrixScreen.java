package ru.rooyzee.elytrixclient.client.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.ui.kit.UiButton;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDraw;
import ru.rooyzee.elytrixclient.client.ui.kit.UiDropdown;
import ru.rooyzee.elytrixclient.client.ui.kit.UiEmpty;
import ru.rooyzee.elytrixclient.client.ui.kit.UiInfo;
import ru.rooyzee.elytrixclient.client.ui.kit.UiSection;
import ru.rooyzee.elytrixclient.client.ui.kit.UiSlider;
import ru.rooyzee.elytrixclient.client.ui.kit.UiTheme;
import ru.rooyzee.elytrixclient.client.ui.kit.UiToggle;
import ru.rooyzee.elytrixclient.client.ui.kit.UiWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Панель ElytrixClient — полностью кастомный рендер (см. пакет {@code ui.kit}), без ванильных виджетов.
 *
 * <p>Слева — сайдбар с разделами, справа — карточки-разделы со строками.
 * Пока это только интерфейс: модули ботов подключим следующим шагом.
 *
 * <p>Открывается правым Ctrl (см. {@link ElytrixclientClient}), закрывается Esc,
 * повторным правым Ctrl или кликом вне панели.
 */
public class ElytrixScreen extends Screen {

    private static final int TAB_HOME = 0;
    private static final int TAB_BOTS = 1;
    private static final int TAB_PROXY = 2;
    private static final int TAB_CONSOLE = 3;
    private static final int TAB_SETTINGS = 4;

    private static final String[] TAB_NAMES = {"Главная", "Боты", "Прокси", "Консоль", "Настройки"};
    private static final String[] TAB_SUBS = {
            "Обзор клиента",
            "Список ботов",
            "Прокси для ботов",
            "Вывод процессов",
            "Вид и поведение"
    };
    private static final UiDraw.Icon[] TAB_ICONS = {
            UiDraw.Icon.HOME, UiDraw.Icon.BOTS, UiDraw.Icon.PROXY, UiDraw.Icon.CONSOLE, UiDraw.Icon.SETTINGS
    };

    private final Screen parent;
    private final ElytrixConfig cfg = ElytrixclientClient.CONFIG;
    private final List<List<UiSection>> tabs = new ArrayList<>();

    private int tab = TAB_HOME;
    private double scroll;
    private double scrollTarget;
    private double maxScroll;
    private boolean draggingScrollbar;

    private long openedAt;
    private long lastFrame;
    private boolean closing;
    private long closingAt;
    private boolean dirty;
    private long dirtyAt;

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
        rebuild();
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

    @Override
    public void tick() {
        long now = Util.getMillis();
        if (dirty && now - dirtyAt > 450L) {
            dirty = false;
            cfg.save();
        }
        scrollTarget = clamp(scrollTarget, 0, maxScroll);
        scroll += (scrollTarget - scroll) * (cfg.animations ? 0.35 : 1.0);
        if (closing && now - closingAt > 130L) {
            this.minecraft.gui.setScreen(parent);
        }
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

        UiSection client = new UiSection("Elytrix Client", "Minecraft 26.2 · Fabric").badge("v" + version());
        client.add(new UiInfo("Панель", () -> "правый Ctrl", UiTheme.OK));
        client.add(new UiInfo("Цель по умолчанию", cfg::target, 0));
        client.add(new UiInfo("Файл конфига", () -> String.valueOf(ElytrixConfig.file().getFileName()), 0));
        list.add(client);

        UiSection modules = new UiSection("Модули", "Пока подключён только интерфейс");
        modules.add(new UiEmpty(UiDraw.Icon.BOTS, "Здесь появятся боты", "BotMark и SoulFire сведём в один список"));
        list.add(modules);
        return list;
    }

    private List<UiSection> botsTab() {
        UiSection bots = new UiSection("Боты", "Запуск и управление — следующим шагом").badge("0");
        bots.add(new UiEmpty(UiDraw.Icon.BOTS, "Список пуст", "Пока это только каркас интерфейса"));
        return List.of(bots);
    }

    private List<UiSection> proxyTab() {
        UiSection proxy = new UiSection("Прокси", "Хранилище и проверка прокси").badge("0");
        proxy.add(new UiEmpty(UiDraw.Icon.PROXY, "Прокси пока нет", "Сюда переедет импорт и проверка прокси"));
        return List.of(proxy);
    }

    private List<UiSection> consoleTab() {
        UiSection console = new UiSection("Консоль", "Вывод запущенных процессов").badge("●");
        console.add(new UiEmpty(UiDraw.Icon.CONSOLE, "Логов пока нет", "Запустим ботов — здесь появятся строки"));
        return List.of(console);
    }

    private List<UiSection> settingsTab() {
        List<UiSection> list = new ArrayList<>();

        UiSection look = new UiSection("Внешний вид", "Настройки интерфейса применяются сразу");
        look.add(new UiDropdown("Акцент", List.of(UiTheme.ACCENT_NAMES), cfg.accentIndex, i -> {
            cfg.accentIndex = i;
            accentColor = UiTheme.accent(i);
            markDirty();
        }));
        look.add(new UiSlider("Прозрачность панели", "%", 60, 100, cfg.panelOpacity, v -> {
            cfg.panelOpacity = v;
            markDirty();
        }));
        look.add(new UiToggle("Плавные анимации", "Появление панели, переключатели, слайдеры",
                cfg.animations, v -> {
            cfg.animations = v;
            markDirty();
        }));
        look.add(new UiToggle("Размытие фона", "Блюр того, что за панелью",
                cfg.blurBackground, v -> {
            cfg.blurBackground = v;
            markDirty();
        }));
        list.add(look);

        UiSection behaviour = new UiSection("Поведение", null);
        behaviour.add(new UiToggle("Панель по правому Ctrl", cfg.panelKey, v -> {
            cfg.panelKey = v;
            markDirty();
        }));
        behaviour.add(new UiToggle("Закрывать кликом вне панели", cfg.closeOnOutsideClick, v -> {
            cfg.closeOnOutsideClick = v;
            markDirty();
        }));
        behaviour.add(new UiButton("Открыть папку конфига", UiDraw.Icon.SETTINGS, UiButton.Style.SECONDARY,
                this::openConfigFolder));
        behaviour.add(new UiButton("Сбросить настройки интерфейса", UiDraw.Icon.CLOSE, UiButton.Style.DANGER,
                this::resetInterface));
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
        cfg.accentIndex = 0;
        cfg.panelOpacity = 88;
        cfg.animations = true;
        cfg.blurBackground = true;
        cfg.closeOnOutsideClick = false;
        cfg.panelKey = true;
        cfg.save();
        rebuild();
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("elytrixclient")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Отрисовка
    // ─────────────────────────────────────────────────────────────────────

    private float fade() {
        long now = Util.getMillis();
        float appear = cfg.animations ? easeOut(Math.min(1f, (now - openedAt) / 190f)) : 1f;
        float close = closing ? 1f - Math.min(1f, (now - closingAt) / 130f) : 1f;
        return Math.max(0f, Math.min(1f, appear * close));
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private void layoutPanel() {
        panelW = Math.max(320, Math.min(600, this.width - 30));
        panelH = Math.max(200, Math.min(370, this.height - 30));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (cfg.blurBackground && this.minecraft != null
                && this.minecraft.options.getMenuBackgroundBlurriness() < 1.0F) {
            graphics.blurBeforeThisStratum();
        }
        graphics.fill(0, 0, this.width, this.height, UiTheme.withAlpha(UiTheme.SCRIM, fade()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        layoutPanel();
        long now = Util.getMillis();
        float dt = Math.max(0.001f, Math.min(0.1f, (now - lastFrame) / 1000f));
        lastFrame = now;
        float f = fade();
        var font = this.font;

        int cardX = panelX + 6;
        int cardY = panelY + 6;
        int cardW = UiTheme.SIDEBAR_W - 6;
        int cardH = panelH - 12;
        int contentX = panelX + UiTheme.SIDEBAR_W;
        int contentY = panelY + UiTheme.HEADER_H;
        int contentW = panelX + panelW - contentX - 12;
        int contentH = panelY + panelH - contentY - 10;

        // непрозрачность поверхностей панели (настройка «Прозрачность панели»)
        float fo = f * Math.max(0.35f, Math.min(1f, cfg.panelOpacity / 100f));

        // ── панель
        UiDraw.shadow(graphics, panelX, panelY, panelW, panelH, UiTheme.R_LG, 6, UiTheme.withAlpha(UiTheme.SHADOW, f));
        UiDraw.roundRectBordered(graphics, panelX, panelY, panelW, panelH, UiTheme.R_LG,
                UiTheme.withAlpha(UiTheme.PANEL, fo), UiTheme.withAlpha(UiTheme.BORDER, fo));

        // ── сайдбар
        UiDraw.roundRectBordered(graphics, cardX, cardY, cardW, cardH, UiTheme.R_MD,
                UiTheme.withAlpha(UiTheme.SIDEBAR, fo), UiTheme.withAlpha(UiTheme.BORDER_SOFT, fo));
        int navX = cardX + 5;
        int navW = cardW - 10;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int iy = cardY + 8 + i * 30;
            boolean selected = i == tab;
            boolean hover = mouseX >= navX && mouseX < navX + navW && mouseY >= iy && mouseY < iy + 26;
            if (selected) {
                UiDraw.roundRect(graphics, navX, iy, navW, 26, UiTheme.R_MD,
                        UiTheme.withAlpha(UiTheme.accentSoft(accentColor, 0.30f), f));
                UiDraw.roundRect(graphics, navX + 1, iy + 6, 2, 14, 1, UiTheme.withAlpha(accentColor, f));
            } else if (hover) {
                UiDraw.roundRect(graphics, navX, iy, navW, 26, UiTheme.R_MD, UiTheme.withAlpha(UiTheme.ROW_HOVER, f));
            }
            int iconColor = selected ? UiTheme.mix(accentColor, 0xFFFFFFFF, 0.25f) : UiTheme.TEXT_DIM;
            UiDraw.icon(graphics, TAB_ICONS[i], navX + 9, iy + 5, 16, UiTheme.withAlpha(iconColor, f));
            UiDraw.text(graphics, font, TAB_NAMES[i], navX + 32, iy + 9,
                    UiTheme.withAlpha(selected ? UiTheme.TEXT : UiTheme.TEXT_SOFT, f));
        }
        UiDraw.text(graphics, font, "правый Ctrl · Esc", cardX + 10, cardY + cardH - 16, UiTheme.withAlpha(UiTheme.TEXT_DIM, f));

        // ── заголовок раздела
        graphics.pose().pushMatrix();
        graphics.pose().translate(contentX + 14, panelY + 11);
        graphics.pose().scale(1.35f, 1.35f);
        UiDraw.text(graphics, font, TAB_NAMES[tab], 0, 0, UiTheme.withAlpha(UiTheme.TEXT, f));
        graphics.pose().popMatrix();
        UiDraw.text(graphics, font, TAB_SUBS[tab], contentX + 15, panelY + 27, UiTheme.withAlpha(UiTheme.TEXT_DIM, f));
        UiDraw.hLine(graphics, contentX + 10, panelX + panelW - 10, panelY + UiTheme.HEADER_H - 2, 1,
                UiTheme.withAlpha(UiTheme.DIVIDER, f));

        // ── кнопка закрытия
        int closeX = panelX + panelW - 34;
        int closeY = panelY + 11;
        boolean closeHover = mouseX >= closeX && mouseX < closeX + 24 && mouseY >= closeY && mouseY < closeY + 20;
        UiDraw.roundRect(graphics, closeX, closeY, 24, 20, UiTheme.R_SM,
                closeHover ? UiTheme.withAlpha(UiTheme.ERROR, 0.85f) : UiTheme.withAlpha(UiTheme.ROW, f));
        UiDraw.icon(graphics, UiDraw.Icon.CLOSE, closeX + 6, closeY + 4, 12,
                UiTheme.withAlpha(closeHover ? 0xFFFFFFFF : UiTheme.TEXT_SOFT, f));

        // ── содержимое вкладки
        int total = 0;
        for (UiSection s : tabs.get(tab)) {
            total += s.contentHeight() + 8;
        }
        maxScroll = Math.max(0, total - contentH);
        scrollTarget = clamp(scrollTarget, 0, maxScroll);
        scroll += (scrollTarget - scroll) * (cfg.animations ? 0.35 : 1.0);

        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        int y = (int) Math.round(contentY + 4 - scroll);
        for (UiSection s : tabs.get(tab)) {
            s.accent = accentColor;
            s.layoutAt(contentX + 6, y, contentW - 12);
            s.render(graphics, mouseX, mouseY, dt);
            y += s.contentHeight() + 8;
        }
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.hasPopup()) {
                    w.popupLimitTop = contentY;
                    w.popupLimitBottom = contentY + contentH;
                    w.renderPopup(graphics, mouseX, mouseY, dt);
                }
            }
        }
        graphics.disableScissor();

        // ── полоса прокрутки
        if (maxScroll > 1) {
            int barX = contentX + contentW + 3;
            int barTop = contentY + 6;
            int barH = contentH - 12;
            UiDraw.roundRect(graphics, barX, barTop, 3, barH, 1, UiTheme.withAlpha(UiTheme.TRACK, 0.7f * f));
            int thumbH = Math.max(20, (int) (barH * (contentH / (double) (contentH + maxScroll))));
            int thumbY = barTop + (int) ((barH - thumbH) * (scroll / maxScroll));
            UiDraw.roundRect(graphics, barX - 1, thumbY, 4, thumbH, 2, UiTheme.withAlpha(accentColor, 0.9f * f));
        }

        // ── подсказка
        if (mouseY > contentY && mouseY < contentY + contentH) {
            String tip = hoveredTooltip(mouseX, mouseY);
            if (tip != null) {
                int tw = font.width(tip) + 10;
                int tx = Math.min(mouseX + 8, panelX + panelW - tw - 4);
                int ty = mouseY + 10;
                UiDraw.roundRect(graphics, tx, ty, tw, 16, UiTheme.R_SM, 0xF00B0E14);
                UiDraw.text(graphics, font, tip, tx + 5, ty + 4, UiTheme.TEXT_SOFT);
            }
        }
    }

    private String hoveredTooltip(int mouseX, int mouseY) {
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.tooltip != null && w.contains(mouseX, mouseY)) {
                    return w.tooltip;
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Ввод
    // ─────────────────────────────────────────────────────────────────────

    private boolean inContent(double mx, double my) {
        int contentX = panelX + UiTheme.SIDEBAR_W;
        int contentY = panelY + UiTheme.HEADER_H;
        return mx >= contentX && mx < panelX + panelW && my >= contentY && my < panelY + panelH;
    }

    private boolean inPanel(double mx, double my) {
        return mx >= panelX && mx < panelX + panelW && my >= panelY && my < panelY + panelH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();
        layoutPanel();

        // открытые выпадающие списки перехватывают клик первыми
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.hasPopup() && w.mouseClicked(mx, my, button)) {
                    return true;
                }
            }
        }

        // кнопка закрытия
        int closeX = panelX + panelW - 34;
        int closeY = panelY + 11;
        if (mx >= closeX && mx < closeX + 24 && my >= closeY && my < closeY + 20) {
            onClose();
            return true;
        }

        // сайдбар
        int cardX = panelX + 6;
        int cardY = panelY + 6;
        int cardW = UiTheme.SIDEBAR_W - 6;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int iy = cardY + 8 + i * 30;
            if (mx >= cardX + 5 && mx < cardX + cardW - 5 && my >= iy && my < iy + 26) {
                if (i != tab) {
                    tab = i;
                    scroll = 0;
                    scrollTarget = 0;
                }
                return true;
            }
        }

        // содержимое
        if (inContent(mx, my)) {
            for (UiSection s : tabs.get(tab)) {
                for (UiWidget w : s.children()) {
                    if (w.mouseClicked(mx, my, button)) {
                        return true;
                    }
                }
            }
            return true;
        }

        // клик вне панели — закрыть
        if (!inPanel(mx, my) && cfg.closeOnOutsideClick) {
            onClose();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.mouseReleased(event.x(), event.y(), event.button())) {
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
        for (UiSection s : tabs.get(tab)) {
            for (UiWidget w : s.children()) {
                if (w.mouseDragged(event.x(), event.y(), event.button())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void scrollToMouse(double mouseY) {
        int contentY = panelY + UiTheme.HEADER_H;
        int contentH = panelY + panelH - contentY - 10;
        double t = (mouseY - contentY - 6) / Math.max(1.0, contentH - 12);
        scrollTarget = clamp(t, 0, 1) * maxScroll;
        scroll = scrollTarget;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (inContent(mx, my)) {
            scrollTarget = clamp(scrollTarget - scrollY * 26, 0, maxScroll);
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
        return super.keyPressed(event);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }
}
