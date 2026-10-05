package platform.client.ui.widget;

import static platform.api.module.Interface.aM_;
import platform.client.Xivivide;
import platform.api.module.InterfaceC0020Opcode;
import platform.client.utils.render.EasingList;
import platform.client.utils.render.Fonts;
import platform.client.utils.render.Font;
import platform.client.utils.render.ColorUtil;
import platform.api.system.configs.ThemeInfo;
import platform.api.system.configs.ThemeProcessor;
import platform.api.event.GlobalEvent;
import platform.api.event.events.render.DrawEvent;
import platform.api.event.events.client.PacketEvent;
import platform.client.ui.element.Element_2;
import platform.client.utils.render.AnimationUtil;
import platform.client.ui.element.DragInfo;
import platform.api.module.setting.Setting;
import platform.client.utils.render.Draw2DProcessor;
import platform.client.utils.render.Shine;
import platform.api.module.setting.BooleanSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

public class Widget {
    private final List<Setting<?>> f = new ObjectArrayList();
    private final List<Element_2<?>> g = new ObjectArrayList();
    protected final AnimationUtil a = new AnimationUtil();
    protected final AnimationUtil b = new AnimationUtil();
    protected final AnimationUtil c = new AnimationUtil();
    private boolean h = false;
    protected float d = 12.5f;
    protected float e = 7.0f;
    private final DragInfo i;
    /** The sweeping light band over this widget's panel, switchable from the widget's own settings. */
    protected final BooleanSetting shine = new BooleanSetting("Переливание", true);

    @Generated
    public List<Setting<?>> b() { return this.f; }

    @Generated
    public List<Element_2<?>> c() { return this.g; }

    @Generated
    public AnimationUtil d() { return this.a; }

    @Generated
    public AnimationUtil e() { return this.b; }

    @Generated
    public AnimationUtil f() { return this.c; }

    @Generated
    public void setEnabled(boolean status) { this.h = status; }

    @Generated
    public boolean g() { return this.h; }

    @Generated
    public float h() { return this.d; }

    @Generated
    public float i() { return this.e; }

    @Generated
    public DragInfo j() { return this.i; }

    public Widget(DragInfo dragInfo) {
        this.i = dragInfo;
        dragInfo.a(this);
        addSettings(this.shine);
    }

    /** For widgets without a panel of their own: the shine toggle would do nothing there. */
    protected final void withoutShine() {
        this.f.remove(this.shine);
        this.g.removeIf(element -> element.e() == this.shine);
    }

    protected final void addSettings(Setting<?>... settings) {
        for (Setting<?> setting : settings) {
            this.f.add(setting);
            this.g.add(setting.d());
        }
    }

    public void a(DrawEvent event) {
        e().a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        this.c.a(this.h && (aM_.gui.screen() instanceof ChatScreen));
        this.c.a(0.0f, 1.0f, 0.3f, EasingList.g, event.g());
        if (this.c.c() > 0.0f) {
            b(event);
        }
    }

    public void a(GlobalEvent event) {
        e().a(this.i == Xivivide.h().d().s().g());
    }

    public void a(PacketEvent event) {
    }

    protected void b(DrawEvent event) {
        GuiGraphicsExtractor context = event.i();
        List<Element_2<?>> visible = this.g.stream().filter(e -> e.e().e().get().booleanValue()).toList();
        if (!visible.isEmpty()) {
            float panelWidth = 8.0f + ((Float) visible.stream().map(e2 -> Float.valueOf(19.5f + Fonts.e.a(e2.e().i(), 6.5f) + 25.0f)).reduce(Float.valueOf(0.0f), Math::max)).floatValue();
            float totalHeight = 8.0f + 15.0f * visible.size() - 3.0f;
            float anim = this.c.c() * a();
            float baseX = (this.i.b() - totalHeight) - 2.0f >= 0.0f ? (this.i.a() + (this.i.f() / 2.0f)) - (panelWidth / 2.0f) : this.i.a() + this.i.f() + 2.0f;
            float baseY = (this.i.b() - totalHeight) - 2.0f >= 0.0f ? (this.i.b() - totalHeight) - 2.0f : this.i.b();
            float baseX2 = Math.min(Math.max(baseX, 0.0f), (aM_.getWindow().getGuiScaledWidth() - panelWidth) - 2.0f);
            float baseY2 = Math.min(Math.max(baseY, 0.0f), aM_.getWindow().getGuiScaledHeight() - totalHeight);
            drawPanel(context, baseX2, baseY2, panelWidth, totalHeight, false, anim);
            event.d().a(context, baseX2, baseY2, panelWidth, totalHeight, 5.0f, 0.5f,
                    ColorUtil.a(hudText(), 0.12f * anim));
            float y = baseY2 + 4.0f;
            for (Element_2<?> element : visible) {
                float rowX = baseX2 + 4.0f;
                float rowWidth = panelWidth - 8.0f;
                element.d().set(rowX, y, rowWidth, 12.0f);
                element.a(event, rowX, y, rowWidth, anim);
                y += 15.0f;
            }
        }
    }

    protected void drawHeader(GuiGraphicsExtractor context, String icon, Object title, float width, float animation) {
        drawHeader(context, Fonts.hudIcons, icon, title, width, animation);
    }

    protected void drawHeader(GuiGraphicsExtractor context, Font iconFont, String icon, Object title, float width, float animation) {
        drawHeader(context, this.i.a(), this.i.b(), iconFont, icon, title, width, animation, Xivivide.h().d().o().a(ThemeInfo.PRIMARY).a());
    }

    protected void drawHeader(GuiGraphicsExtractor context, String icon, Object title, float width, float animation, int iconColor) {
        drawHeader(context, this.i.a(), this.i.b(), Fonts.hudIcons, icon, title, width, animation, iconColor);
    }

    protected void drawHeader(GuiGraphicsExtractor context, float x, float y, String icon, Object title, float width, float animation, int iconColor) {
        drawHeader(context, x, y, Fonts.hudIcons, icon, title, width, animation, iconColor);
    }

    protected void drawHeader(GuiGraphicsExtractor context, float x, float y, Font iconFont, String icon, Object title, float width, float animation, int iconColor) {
        if (animation > 0.0f) {
            float iconSize = this.e + 1.0f;
            drawBackground(context, x, y, width, this.d, true, animation);
            iconFont.a(context, icon, x + ((13.5f - iconFont.b(icon, iconSize)) / 2.0f), iconFont.a(icon, iconSize, y + this.d / 2.0f), iconSize, ColorUtil.a(iconColor, animation));
            drawSeparator(context, x + 13.5f, y, this.d, animation);
            Fonts.e.a(context, String.valueOf(title), x + 17.5f, (y + ((this.d - Fonts.e.a(this.e)) / 2.0f)) - 0.5f, this.e, ColorUtil.a(hudText(), animation));
        }
    }

    /** Size multiplier for the list HUD widgets (hotkeys, potions, cooldowns, staff, items). */
    protected static final float HUD_SCALE = 1.2f;

    /**
     * Starts drawing this widget scaled by {@link #HUD_SCALE} around its top-left corner. The widget keeps
     * laying itself out in unscaled units (use j().lw()/lh() for its own size); dragging uses the scaled size.
     */
    protected void pushHudScale(GuiGraphicsExtractor context) {
        this.i.setScale(HUD_SCALE);
        float x = this.i.a();
        float y = this.i.b();
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(HUD_SCALE, HUD_SCALE);
        context.pose().translate(-x, -y);
    }

    protected void popHudScale(GuiGraphicsExtractor context) {
        context.pose().popMatrix();
    }

    /** Row height for list widgets drawn with {@link #drawListHeader}. */
    protected static final float LIST_ROW_HEIGHT = 10.5f;
    protected static final float LIST_ICON_BOX = 9.5f;

    /** Width a list header needs: title on the left plus the icon box on the right. */
    protected float listHeaderWidth(String title) {
        return 6.0f + Fonts.e.a(title, this.e) + 10.0f + LIST_ICON_BOX + 3.0f;
    }

    /**
     * Panel-style list header: title on the left, accent icon in an accent-tinted box on the right,
     * and a thin divider under the header when the list has rows.
     */
    protected void drawListHeader(DrawEvent event, GuiGraphicsExtractor context, float x, float y, float width,
                                  Font iconFont, String icon, String title, boolean divider, float alpha) {
        int accent = Xivivide.h().d().o().a(ThemeInfo.PRIMARY).a();
        float iconSize = 6.5f;
        Fonts.e.a(context, title, x + 6.0f, y + (this.d - Fonts.e.a(this.e)) / 2.0f - 0.5f, this.e, ColorUtil.a(hudText(), alpha));
        float boxX = x + width - LIST_ICON_BOX - 3.0f;
        float boxY = y + (this.d - LIST_ICON_BOX) / 2.0f;
        event.d().a(context, boxX, boxY, LIST_ICON_BOX, LIST_ICON_BOX, 2.5f, ColorUtil.a(accent, 0.25f * alpha));
        iconFont.a(context, icon,
                boxX + (LIST_ICON_BOX - iconFont.b(icon, iconSize)) / 2.0f - iconFont.leftBearing(icon, iconSize),
                iconFont.a(icon, iconSize, boxY + LIST_ICON_BOX / 2.0f), iconSize, ColorUtil.a(accent, alpha));
        if (divider) {
            event.d().a(context, x + 5.0f, y + this.d, width - 10.0f, 0.5f, 0.25f, ColorUtil.a(-1, 0.15f * alpha));
        }
    }

    protected void drawBackground(GuiGraphicsExtractor context, float x, float y, float width, float height, boolean glow, float animation) {
        drawPanel(context, x, y, width, height, glow, animation);
        // The band goes on right after the panel, so the widget's text and icons still sit on top of it
        if (this.shine.c() && animation > 0.0f) {
            Shine.draw(context, x, y, width, height, 5.0f, animation);
        }
    }

    /** The panel alone, without the shine - for the settings popup, which is not the widget itself. */
    private void drawPanel(GuiGraphicsExtractor context, float x, float y, float width, float height, boolean glow, float animation) {
        if (animation > 0.0f) {
            ThemeProcessor themeProcessor = Xivivide.h().d().o();
            float alpha = themeProcessor.a(ThemeInfo.BACKGROUND_HUD).b() * animation;
            // The panel is the hud background tinted by the accent; the glow around it runs from the first
            // accent into the second, so a theme with two colours shows both
            int accent = themeProcessor.a(ThemeInfo.PRIMARY).a();
            int accentTwo = themeProcessor.a(ThemeInfo.SECONDARY).a();
            int background = ColorUtil.a(themeProcessor.a(ThemeInfo.BACKGROUND_HUD).a(), accent, themeProcessor.a(ThemeInfo.PRIMARY).b() / 6.0f);
            int glowTint = ColorUtil.a(themeProcessor.a(ThemeInfo.BACKGROUND_HUD).a(), accentTwo, themeProcessor.a(ThemeInfo.PRIMARY).b() / 6.0f);
            if (glow) {
                Xivivide.h().d().i().a(context, x, y, width, height, 5.0f + (1.0f * this.b.c()), ColorUtil.a(background, alpha), animation, ColorUtil.a(glowTint, alpha), 8.0f + (2.0f * this.b.c()));
            } else {
                Xivivide.h().d().i().b(context, x, y, width, height, 5.0f, ColorUtil.a(background, alpha), animation);
            }
        }
    }

    /** The colour hud text is written in. Its own entry in the theme, apart from the menu's text. */
    protected static int hudText() {
        return Xivivide.h().d().o().a(ThemeInfo.TEXT_HUD).a();
    }

    protected void drawSeparator(GuiGraphicsExtractor context, float x, float y, float height, float animation) {
        float separatorHeight = height / 2.0f;
        Xivivide.h().d().i().a(context, x, y + ((height - separatorHeight) / 2.0f), 0.75f, separatorHeight, ColorUtil.a(ColorUtil.a(InterfaceC0020Opcode.aN, InterfaceC0020Opcode.aN, InterfaceC0020Opcode.aN, 255), 0.5f * animation));
    }

    public float a() {
        return this.a.c() * (1.0f - (0.1f * this.b.c()));
    }
}



