package platform.client.features.modules.render;

import platform.client.ui.widget.Widget;
import platform.client.Xivivide;
import platform.api.module.Module;

import platform.api.system.configs.ThemeInfo;
import platform.api.module.Category;
import platform.api.event.interfaces.EventTarget;
import platform.api.event.GlobalEvent;
import platform.api.module.ModuleRegister;
import platform.api.event.events.render.DrawEvent;
import platform.api.event.events.client.PacketEvent;
import platform.api.module.setting.BooleanSetting;
import platform.client.ui.widget.ArmorWidget;
import platform.client.ui.widget.ArrayListWidget;
import platform.client.ui.widget.CooldownsWidget;
import platform.client.ui.widget.HotkeysWidget;
import platform.client.ui.widget.ItemsWidget;
import platform.client.ui.widget.NotificationWidget;
import platform.client.ui.widget.PotionWidget;
import platform.client.ui.widget.StaffWidget;
import platform.client.ui.widget.TargetWidget;
import platform.client.ui.widget.WatermarkWidget;

import platform.api.module.setting.ColorSetting;
import platform.api.module.setting.ModeSetting;
import platform.api.module.setting.MultiModeSetting;
import platform.api.system.configs.ThemeType;
import platform.api.system.configs.ThemePreset;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;

@ModuleRegister(a = "Interface", b = "Отображает выбранные элементы интерфейса на экране", c = Category.Render)
public class Interface_2 extends Module {
    /**
     * The theme, colour by colour. Every one of them is an ordinary colour setting, so it saves with the
     * config and opens the same picker as any other - and a preset simply fills them all at once.
     */
    private final ModeSetting preset = (ModeSetting) new ModeSetting("Готовая тема", "Xivivide", ThemePreset.titles())
            .a(title -> applyPreset(ThemePreset.byTitle(title)));
    private final ColorSetting b = new ColorSetting("1 цвет", Integer.valueOf(0xFFB14CFF));
    private final ColorSetting secondary = new ColorSetting("2 цвет", Integer.valueOf(0xFF6C63FF));
    private final ColorSetting backgroundHud = new ColorSetting("Фон худа", Integer.valueOf(0xE00B0B16));
    private final ColorSetting textHud = new ColorSetting("Текст худа", Integer.valueOf(0xFFFFFFFF));
    private final ColorSetting textHeader = new ColorSetting("Текст заголовков", Integer.valueOf(0xFFFFFFFF));
    private final ColorSetting text = new ColorSetting("Текст", Integer.valueOf(0xFFFFFFFF));
    private final ColorSetting textDisabled = new ColorSetting("Неактивный текст", Integer.valueOf(0xFF434651));
    private final ColorSetting slider = new ColorSetting("Слайдер", Integer.valueOf(0xFFB14CFF));
    private final ColorSetting sliderKnob = new ColorSetting("Круг слайдера", Integer.valueOf(0xFFFFFFFF));
    private final ColorSetting toggle = new ColorSetting("Переключатель", Integer.valueOf(0xFFB14CFF));
    private final ColorSetting toggleDisabled = new ColorSetting("Неактивный переключатель", Integer.valueOf(0xFF434651));
    private final ColorSetting button = new ColorSetting("Кнопка", Integer.valueOf(0xFFB14CFF));
    private final ColorSetting buttonDisabled = new ColorSetting("Неактивная кнопка", Integer.valueOf(0xFF434651));
    private final MultiModeSetting c = new MultiModeSetting("Элементы интерфейса", new BooleanSetting("Клавиши", true), new BooleanSetting("Таргет-худ", true), new BooleanSetting("Задержки", true), new BooleanSetting("Инфо-панель", true), new BooleanSetting("Уведомления", true), new BooleanSetting("Зелья", true), new BooleanSetting("Предметы", true), new BooleanSetting("Броня", true), new BooleanSetting("Стафф", true), new BooleanSetting("ArrayList", true), new BooleanSetting("Музыка", true));
    private final ModeSetting d = new ModeSetting("Тема", Xivivide.h().d().o().a() == ThemeType.LIGHT ? "Светлая" : "Тёмная", "Тёмная", "Светлая").a(value -> {
        Xivivide.h().d().o().a("Светлая".equalsIgnoreCase(value) ? ThemeType.LIGHT : ThemeType.DARK);
    });
    private final List<Widget> h = new ArrayList();

    @Generated
    public List<Widget> q() {
        return this.h;
    }

    public Interface_2() {
        a(this.c, this.d, this.preset, this.b, this.secondary, this.backgroundHud, this.textHud, this.textHeader,
                this.text, this.textDisabled, this.slider, this.sliderKnob, this.toggle, this.toggleDisabled,
                this.button, this.buttonDisabled);
        this.h.add(new ArmorWidget());
        this.h.add(new HotkeysWidget());
        this.h.add(new CooldownsWidget());
        this.h.add(new TargetWidget());
        this.h.add(new WatermarkWidget());
        this.h.add(new PotionWidget());
        this.h.add(new ItemsWidget());
        this.h.add(new NotificationWidget());
        this.h.add(new StaffWidget());
        this.h.add(new ArrayListWidget());
        this.h.add(new platform.client.ui.widget.MusicWidget());
    }

    @EventTarget
    public void a(DrawEvent event) {
        if (event.b()) {
            applyColors();
            Xivivide.h().d().s().c(event);
            for (Widget widget : this.h) {
                BooleanSetting toggle = this.c.a(widget.j().j());
                if (toggle != null && toggle.c().booleanValue()) {
                    // One widget failing must not take the rest of the frame with it: a throw in the middle
                    // of drawing leaves the interface scaled wrong and every click off target
                    try {
                        widget.a(event);
                    } catch (Exception exception) {
                        System.err.println("Xivivide: widget " + widget.j().j() + " failed to draw: " + exception);
                    }
                }
            }
        }
    }

    /** The colours of the theme, in the order the editor shows them. */
    public List<ColorSetting> themeColors() {
        return List.of(this.b, this.secondary, this.backgroundHud, this.textHud, this.textHeader, this.text,
                this.textDisabled, this.slider, this.sliderKnob, this.toggle, this.toggleDisabled,
                this.button, this.buttonDisabled);
    }

    /** What is set right now, ready to be saved under a name. */
    public java.util.Map<ThemeInfo, Integer> currentColors() {
        java.util.Map<ThemeInfo, Integer> colors = new java.util.LinkedHashMap<>();
        ThemeInfo[] order = {ThemeInfo.PRIMARY, ThemeInfo.SECONDARY, ThemeInfo.BACKGROUND_HUD, ThemeInfo.TEXT_HUD,
                ThemeInfo.TEXT_HEADER, ThemeInfo.TEXT, ThemeInfo.TEXT_DISABLED, ThemeInfo.SLIDER,
                ThemeInfo.SLIDER_KNOB, ThemeInfo.TOGGLE, ThemeInfo.TOGGLE_DISABLED, ThemeInfo.BUTTON,
                ThemeInfo.BUTTON_DISABLED};
        List<ColorSetting> settings = themeColors();
        for (int index = 0; index < order.length; index++) {
            colors.put(order[index], settings.get(index).c());
        }
        return colors;
    }

    /** Loads a theme by name - a ready made one or one saved in the editor. */
    public void applyTheme(String name) {
        java.util.Map<ThemeInfo, Integer> colors = platform.api.system.configs.ThemeLibrary.colors(name);
        if (colors == null) {
            return;
        }
        if (platform.api.system.configs.ThemeLibrary.isPreset(name)) {
            applyPreset(ThemePreset.byTitle(name));
            this.preset.a(name);
            return;
        }
        ThemeInfo[] order = {ThemeInfo.PRIMARY, ThemeInfo.SECONDARY, ThemeInfo.BACKGROUND_HUD, ThemeInfo.TEXT_HUD,
                ThemeInfo.TEXT_HEADER, ThemeInfo.TEXT, ThemeInfo.TEXT_DISABLED, ThemeInfo.SLIDER,
                ThemeInfo.SLIDER_KNOB, ThemeInfo.TOGGLE, ThemeInfo.TOGGLE_DISABLED, ThemeInfo.BUTTON,
                ThemeInfo.BUTTON_DISABLED};
        List<ColorSetting> settings = themeColors();
        for (int index = 0; index < order.length; index++) {
            Integer color = colors.get(order[index]);
            if (color != null) {
                settings.get(index).a(color);
            }
        }
        applyColors();
    }

    /** Saves what is set now under {@code name}, so it can be picked again later. */
    public void saveTheme(String name) {
        platform.api.system.configs.ThemeLibrary.save(name, currentColors());
    }

    /** Hands every colour of the editor to the theme, so a change shows the moment it is made. */
    private void applyColors() {
        var theme = Xivivide.h().d().o();
        theme.a(ThemeInfo.PRIMARY).a(this.b.c().intValue());
        theme.a(ThemeInfo.SECONDARY).a(this.secondary.c().intValue());
        theme.a(ThemeInfo.BACKGROUND_HUD).a(this.backgroundHud.c().intValue());
        theme.a(ThemeInfo.TEXT_HUD).a(this.textHud.c().intValue());
        theme.a(ThemeInfo.TEXT_HEADER).a(this.textHeader.c().intValue());
        theme.a(ThemeInfo.TEXT).a(this.text.c().intValue());
        theme.a(ThemeInfo.TEXT_DISABLED).a(this.textDisabled.c().intValue());
        theme.a(ThemeInfo.SLIDER).a(this.slider.c().intValue());
        theme.a(ThemeInfo.SLIDER_KNOB).a(this.sliderKnob.c().intValue());
        theme.a(ThemeInfo.TOGGLE).a(this.toggle.c().intValue());
        theme.a(ThemeInfo.TOGGLE_DISABLED).a(this.toggleDisabled.c().intValue());
        theme.a(ThemeInfo.BUTTON).a(this.button.c().intValue());
        theme.a(ThemeInfo.BUTTON_DISABLED).a(this.buttonDisabled.c().intValue());
    }

    /** Fills the editor from a ready made set, and switches the menu between its dark and light look with it. */
    private void applyPreset(ThemePreset preset) {
        var colors = preset.colors();
        this.b.a(colors.get(ThemeInfo.PRIMARY));
        this.secondary.a(colors.get(ThemeInfo.SECONDARY));
        this.backgroundHud.a(colors.get(ThemeInfo.BACKGROUND_HUD));
        this.textHud.a(colors.get(ThemeInfo.TEXT_HUD));
        this.textHeader.a(colors.get(ThemeInfo.TEXT_HEADER));
        this.text.a(colors.get(ThemeInfo.TEXT));
        this.textDisabled.a(colors.get(ThemeInfo.TEXT_DISABLED));
        this.slider.a(colors.get(ThemeInfo.SLIDER));
        this.sliderKnob.a(colors.get(ThemeInfo.SLIDER_KNOB));
        this.toggle.a(colors.get(ThemeInfo.TOGGLE));
        this.toggleDisabled.a(colors.get(ThemeInfo.TOGGLE_DISABLED));
        this.button.a(colors.get(ThemeInfo.BUTTON));
        this.buttonDisabled.a(colors.get(ThemeInfo.BUTTON_DISABLED));
        this.d.a(preset.light() ? "Светлая" : "Тёмная");
        Xivivide.h().d().o().a(preset.light() ? ThemeType.LIGHT : ThemeType.DARK);
        applyColors();
    }

    @EventTarget
    public void a(GlobalEvent event) {
        for (Widget widget : this.h) {
            BooleanSetting toggle = this.c.a(widget.j().j());
            if (toggle != null && toggle.c().booleanValue()) {
                widget.a(event);
            }
        }
    }

    @EventTarget
    public void a(PacketEvent event) {
        for (Widget widget : this.h) {
            BooleanSetting toggle = this.c.a(widget.j().j());
            if (toggle != null && toggle.c().booleanValue()) {
                widget.a(event);
            }
        }
    }
}



