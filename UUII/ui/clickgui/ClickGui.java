package wtf.expensive.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.impl.render.ClickGuiFunction;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.Type;
import wtf.expensive.client.scripts.ScriptManager;
import wtf.expensive.client.ui.clickgui.object.ModuleObject;
import wtf.expensive.client.ui.clickgui.theme.ThemeRenderUtil;
import wtf.expensive.client.ui.theme.Style;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ClickGui extends Screen {
    public static final int DARK = new Color(18, 19, 25).getRGB();
    public static final int MEDIUM = new Color(18, 19, 25).brighter().getRGB();
    public static final int LIGHT = new Color(129, 134, 153).getRGB();

    private static final float WIDTH = 450;
    private static final float HEIGHT = 350;
    private static final float BAR = 100;

    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/ui/menu_rounded.png");
    private static final Identifier THEME_CARD_PREVIEW =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/rect.png");
    private static final Identifier THEME_TAB =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/corner.png");
    private static final Identifier THEME_CUSTOM_PREVIEW =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/rect2.png");

    private static final net.minecraft.network.chat.Style THEME_SEMIBOLD_16 = Fonts.SEMIBOLD_16;
    private static final net.minecraft.network.chat.Style THEME_SEMIBOLD_18 = Fonts.SEMIBOLD_18;
    private static final net.minecraft.network.chat.Style THEME_LIGHT_10 = Fonts.LIGHT_10;

    private final List<ModuleObject> objects = new ArrayList<>();

    private float posX;
    private float posY;
    private Type current = Type.Combat;

    public static float scrolling;
    private static float scrollingOut;

    private boolean searching;
    private String searchText = "";
    private boolean customColorOpen;
    private boolean draggingHue;
    private boolean draggingSaturation;
    private float customColorAnimation;
    private float customThemeAnimation;
    private int customColorIndex;
    private float customHue;
    private float customSaturation;
    private float customBrightness;
    private final Map<Style, Float> themeAnimations = new IdentityHashMap<>();

    public ClickGui() {
        super(Component.literal("Expensive"));
        scrolling = 0;
        for (Function function : Managment.FUNCTION_MANAGER.getFunctions()) {
            objects.add(new ModuleObject(function));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void selectCategory(Type type) {
        current = type;
        scrolling = 0;
    }

    public void openCustomPicker() {
        if (!customColorOpen) {
            customColorOpen = true;
            loadCustomColor(0);
        }
    }

    @Override
    protected void init() {
        super.init();
        posX = width / 2f - WIDTH / 2f;
        posY = height / 2f - HEIGHT / 2f;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor graphics) {
        if (Managment.FUNCTION_MANAGER.get("Click Gui") instanceof ClickGuiFunction settings
                && settings.blur.get()) {
            graphics.blurBeforeThisStratum();
        }
        RenderUtil.roundedRect(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0,
                ColorUtil.rgba(0, 0, 0, 80));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        ClickGuiFunction settings = Managment.FUNCTION_MANAGER.get("Click Gui") instanceof ClickGuiFunction value
                ? value : null;

        boolean glow = settings != null && settings.glow.get();
        if (glow) {
            RenderUtil.shadow(graphics, posX, posY, WIDTH, HEIGHT, 8,
                    RenderUtil.withAlpha(Managment.STYLE_MANAGER.getColor(0), 200));
        }

        RenderUtil.shadow(graphics, posX, posY, WIDTH, HEIGHT, 8,
                ColorUtil.rgba(0, 0, 0, 120));
        RenderUtil.roundedRect(graphics, posX + BAR, posY, WIDTH - BAR, HEIGHT, 0, 8, 8, 0, DARK, DARK);
        RenderUtil.texture(graphics, BACKGROUND, posX + BAR, posY, WIDTH - BAR, HEIGHT, -1);

        RenderUtil.roundedRect(graphics, posX, posY, BAR, HEIGHT, 8, 0, 0, 8,
                RenderUtil.withAlpha(MEDIUM, 200), RenderUtil.withAlpha(MEDIUM, 200));
        RenderUtil.roundedRect(graphics, posX + BAR, posY, 0.5f, HEIGHT, 0, RenderUtil.withAlpha(LIGHT, 50));

        renderSearch(graphics);
        renderLogo(graphics);
        renderCategories(graphics, mouseX, mouseY);
        renderProfile(graphics);

        scrollingOut = AnimationMath.fast(scrollingOut, scrolling, 15f);
        renderObjects(graphics, mouseX, mouseY);

        if (glow) {
            ThemeRenderUtil.gradientOutline(graphics, posX, posY, WIDTH, HEIGHT, 8, 1.5f,
                    Managment.STYLE_MANAGER.getColor(0), Managment.STYLE_MANAGER.getColor(90),
                    Managment.STYLE_MANAGER.getColor(180), Managment.STYLE_MANAGER.getColor(270));
        }
    }

    private void renderLogo(GuiGraphicsExtractor graphics) {
        Component label = Component.literal("expensive").withStyle(Fonts.SORA_18);
        float textWidth = StyledFontRenderer.width(font, label);
        StyledFontRenderer.draw(graphics, font, Component.literal("H").withStyle(Fonts.ICONS_15),
                Math.round(posX + BAR / 2 - textWidth / 2f - 8), Math.round(posY + 15f), -1);
        StyledFontRenderer.drawCentered(graphics, font, label,
                Math.round(posX + BAR / 2 + 4), Math.round(posY + 13), -1);
    }

    private void renderSearch(GuiGraphicsExtractor graphics) {
        RenderUtil.roundedRect(graphics, posX + 7.5f, posY + 30, BAR - 15, 17, 5, DARK);

        RenderUtil.scissor(graphics, posX + 7.5f, posY + 30, BAR - 15, 17);
        String text = !searching && searchText.isEmpty()
                ? "Поиск"
                : searchText + (searching && System.currentTimeMillis() % 1000 > 500 ? "_" : "");
        int color = (!searching && searchText.isEmpty()) ? RenderUtil.withAlpha(LIGHT, 150) : -1;

        StyledFontRenderer.draw(graphics, font, Component.literal(text).withStyle(Fonts.GILROY_14),
                Math.round(posX + 22.5f), Math.round(posY + 34.5f), color);
        RenderUtil.unscissor(graphics);

        StyledFontRenderer.draw(graphics, font, Component.literal("I").withStyle(Fonts.ICONS_12),
                Math.round(posX + 13f), Math.round(posY + 35f), RenderUtil.withAlpha(LIGHT, 200));
    }

    private void renderCategories(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Type type : Type.values()) {
            if (type == Type.Theme) {
                RenderUtil.roundedRect(graphics, posX + 5, posY + 55 + type.ordinal() * 20, BAR - 10, 0.5f, 0,
                        RenderUtil.withAlpha(LIGHT, 50));
            }

            float len = categoryOffset(type);

            type.anim = AnimationMath.fast(type.anim, type == current ? 1f : 0f, 10f);
            if (type.anim > 0.001f) {
                int alpha = (int) (150 * type.anim);
                int start = Managment.STYLE_MANAGER.getColor(0);
                int end = Managment.STYLE_MANAGER.getColor(180);
                if (glowEnabled()) {
                    RenderUtil.shadow(graphics, posX + 5f, posY + 55 + len, BAR - 10, 15, 5,
                            RenderUtil.withAlpha(start, alpha));
                }
                RenderUtil.roundedRect(graphics, posX + 5f, posY + 55 + len, BAR - 10, 15, 3, 3, 3, 3,
                        RenderUtil.withAlpha(start, alpha),
                        RenderUtil.withAlpha(end, alpha));
            }

            int textColor = ColorUtil.interpolate(LIGHT, -1, type.anim);
            StyledFontRenderer.draw(graphics, font, Component.literal(type.image).withStyle(Fonts.ICONS_15),
                    (int) (posX + 9f), (int) (posY + 61 + len), textColor);
            StyledFontRenderer.draw(graphics, font, Component.literal(type.name()).withStyle(Fonts.SORA_15),
                    (int) (posX + 21f), (int) (posY + 60 + len), textColor);
        }
    }

    private boolean glowEnabled() {
        return Managment.FUNCTION_MANAGER != null
                && Managment.FUNCTION_MANAGER.clickGui != null
                && Managment.FUNCTION_MANAGER.clickGui.glow.get();
    }

    private float categoryOffset(Type type) {
        float len = type.ordinal() * 20;

        if (type == Type.Theme || type == Type.Configs) {
            len += 10;
        }
        return len;
    }

    private static boolean isSpecial(Type type) {
        return type == Type.Scripts || type == Type.Theme || type == Type.Configs;
    }

    private void renderProfile(GuiGraphicsExtractor graphics) {
        RenderUtil.roundedRect(graphics, posX + 5, posY + HEIGHT - 35, BAR - 10, 0.5f, 0,
                RenderUtil.withAlpha(LIGHT, 50));

        RenderUtil.roundedRect(graphics, posX + 5, posY + HEIGHT - 30, 25, 25, 5,
                ColorUtil.rgba(0, 0, 0, 64));

        if (minecraft != null && minecraft.player != null) {
            PlayerFaceExtractor.extractRenderState(graphics, minecraft.player.getSkin(),
                    Math.round(posX + 6), Math.round(posY + HEIGHT - 29), 23);
        }

        StyledFontRenderer.draw(graphics, font, Component.literal(Managment.USER_PROFILE.getName()).withStyle(Fonts.GILROY_BOLD_15),
                (int) (posX + 35), (int) (posY + HEIGHT - 25), -1);
        StyledFontRenderer.draw(graphics, font, Component.literal("UID: " + Managment.USER_PROFILE.getUid()).withStyle(Fonts.GILROY_13),
                (int) (posX + 35), (int) (posY + HEIGHT - 16), LIGHT);
    }

    private List<ModuleObject> column(int parity) {
        List<ModuleObject> result = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            ModuleObject object = objects.get(i);
            boolean matches = searchText.isEmpty()
                    ? object.function.category == current
                    : object.function.name.toLowerCase().contains(searchText.toLowerCase());
            if (matches && i % 2 == parity) {
                result.add(object);
            }
        }
        return result;
    }

    private void renderObjects(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isSpecial(current)) {
            renderExtraTab(graphics, mouseX, mouseY);
            return;
        }

        RenderUtil.scissor(graphics, posX, posY, WIDTH, HEIGHT - 1);

        float first = layoutColumn(graphics, column(0), posX + 110, mouseX, mouseY);
        float second = layoutColumn(graphics, column(1), posX + 280, mouseX, mouseY);

        RenderUtil.unscissor(graphics);

        float max = Math.max(first, second) + 20;
        scrolling = max < HEIGHT ? 0 : Mth.clamp(scrolling, -(max - HEIGHT), 0);
    }

    private float layoutColumn(GuiGraphicsExtractor graphics, List<ModuleObject> column,
                               float x, int mouseX, int mouseY) {
        float offset = scrollingOut;
        float total = 0;
        for (ModuleObject object : column) {
            object.setPosition(x, posY + 10 + offset, 160);
            object.render(graphics, mouseX, mouseY);
            float step = object.getHeight() + 5;
            offset += step;
            total += step;
        }
        return total;
    }

    private void renderExtraTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (current == Type.Theme) {
            renderThemes(graphics, mouseX, mouseY);
        } else if (current == Type.Scripts) {
            renderScripts(graphics, mouseX, mouseY);
        } else {
            renderConfigs(graphics, mouseX, mouseY);
        }
    }

    private void renderScripts(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float contentX = posX + BAR;
        float contentWidth = WIDTH - BAR;
        RenderUtil.roundedRect(graphics, contentX, posY + 35, contentWidth, 0.5f, 0,
                ColorUtil.rgba(42, 47, 67, 220));

        boolean hovered = RenderUtil.isHovered(mouseX, mouseY, contentX + 10, posY + 10, 15, 15);
        RenderUtil.roundedRect(graphics, contentX + 10, posY + 10, 15, 15, 4,
                ColorUtil.rgba(10, 11, 15, hovered ? 235 : 190));
        StyledFontRenderer.drawCentered(graphics, font,
                Component.literal("L").withStyle(Fonts.CONFIG_ICONS_16),
                contentX + 17.5f, posY + 14, hovered ? -1 : RenderUtil.withAlpha(LIGHT, 220));

        boolean reloadHovered = RenderUtil.isHovered(mouseX, mouseY, contentX + 30, posY + 10, 15, 15);
        RenderUtil.roundedRect(graphics, contentX + 30, posY + 10, 15, 15, 4,
                ColorUtil.rgba(10, 11, 15, reloadHovered ? 235 : 190));
        StyledFontRenderer.drawCentered(graphics, font,
                Component.literal("R").withStyle(Fonts.SEMIBOLD_15),
                contentX + 37.5f, posY + 14, reloadHovered ? -1 : RenderUtil.withAlpha(LIGHT, 220));
        StyledFontRenderer.draw(graphics, font,
                Component.literal("Скрипты").withStyle(Fonts.SEMIBOLD_15),
                Math.round(contentX + 53), Math.round(posY + 14), -1);

        List<ScriptManager.Entry> scripts = Managment.SCRIPT_MANAGER == null
                ? List.of() : Managment.SCRIPT_MANAGER.getEntries();
        if (scripts.isEmpty()) {
            StyledFontRenderer.drawCentered(graphics, font,
                    Component.literal("Папка scripts пуста").withStyle(Fonts.MEDIUM_14),
                    contentX + contentWidth / 2f, posY + HEIGHT / 2f - 4,
                    RenderUtil.withAlpha(LIGHT, 200));
            return;
        }

        float cardX = contentX + 10;
        float cardY = posY + 51;
        float cardWidth = 158;
        for (ScriptManager.Entry script : scripts) {
            boolean cardHovered = RenderUtil.isHovered(mouseX, mouseY, cardX, cardY, cardWidth, 30);
            RenderUtil.roundedRect(graphics, cardX, cardY, cardWidth, 30, 5,
                    script.loaded() && script.function().isState()
                            ? RenderUtil.withAlpha(Managment.STYLE_MANAGER.getColor(0), 220)
                            : ColorUtil.rgba(42, 47, 67, cardHovered ? 255 : 230));
            RenderUtil.roundedRect(graphics, cardX + 1, cardY + 1, cardWidth - 2, 28, 4,
                    ColorUtil.rgba(18, 19, 25, 245));
            String title = script.moduleName().length() > 20
                    ? script.moduleName().substring(0, 19) + "…" : script.moduleName();
            StyledFontRenderer.draw(graphics, font,
                    Component.literal(title).withStyle(Fonts.SEMIBOLD_15),
                    Math.round(cardX + 8), Math.round(cardY + 6), -1);
            String status = script.loaded()
                    ? (script.function().isState() ? "Включён" : "Выключен")
                    : "Ошибка: " + script.error();
            if (status.length() > 28) {
                status = status.substring(0, 27) + "…";
            }
            StyledFontRenderer.draw(graphics, font,
                    Component.literal(status).withStyle(Fonts.LIGHT_11),
                    Math.round(cardX + 8), Math.round(cardY + 18), LIGHT);
            cardX += cardWidth + 8;
            if (cardX + cardWidth > contentX + contentWidth - 10) {
                cardX = contentX + 10;
                cardY += 38;
            }
        }
    }

    private static java.nio.file.Path scriptDirectory() {
        return Managment.SCRIPT_MANAGER == null
                ? net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("scripts")
                : Managment.SCRIPT_MANAGER.getDirectory();
    }

    private void renderThemes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float areaX = posX + BAR + 10;
        float areaY = posY + 6;
        float areaWidth = WIDTH - BAR - 20;
        float cardWidth = 77;
        float cardHeight = 50;

        var styles = Managment.STYLE_MANAGER.styles;
        int presetCount = Math.max(0, styles.size() - 1);
        for (int i = 0; i < presetCount; i++) {
            Style style = styles.get(i);

            float cx = areaX + (i % 4) * 83.5f;
            float cy = areaY + (i / 4) * 55f;
            drawThemeCard(graphics, style, cx, cy, mouseX, mouseY);
        }

        if (!styles.isEmpty()) {
            Style custom = styles.getLast();
            float customY = posY + HEIGHT - 65;
            drawCustomTheme(graphics, custom, areaX, areaWidth, customY, mouseX, mouseY);
        }

        renderCustomThemePicker(graphics, mouseX, mouseY);
    }

    private void renderCustomThemePicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (customColorAnimation < 0.01f || Managment.STYLE_MANAGER.styles.isEmpty()) {
            return;
        }

        if (customColorOpen && (draggingHue || draggingSaturation)) {
            updateCustomColor(mouseX, mouseY);
        }

        Style custom = Managment.STYLE_MANAGER.styles.getLast();
        float pickerX = posX + WIDTH + 20;
        float pickerY = posY + HEIGHT - 157.5f;
        float svX = pickerX + 10;
        float svY = pickerY + 16;
        float svWidth = 111;
        float svHeight = 105;

        float anchorX = posX + BAR + 10 + (WIDTH - BAR - 20) - 15 + 3.5f;
        float anchorY = posY + HEIGHT - 65 + 20 + 3.5f;
        graphics.pose().pushMatrix();
        graphics.pose().scaleAround(customColorAnimation, customColorAnimation, anchorX, anchorY);

        RenderUtil.shadow(graphics, pickerX, pickerY, 130, 157.5f, 15,
                ColorUtil.rgba(0, 0, 0, 160));
        RenderUtil.roundedRect(graphics, pickerX, pickerY, 130, 157.5f, 15,
                ColorUtil.rgba(15, 15, 15, 255));
        RenderUtil.roundedOutline(graphics, pickerX, pickerY, 130, 157.5f, 15, 1,
                ColorUtil.rgba(48, 50, 60, 255));

        for (int slot = 1; slot >= 0; slot--) {
            boolean active = customColorIndex == slot;
            float tabX = pickerX + 8 + slot * 25;
            float tabY = pickerY + 7 + (active ? -2 : 0);
            ThemeRenderUtil.gradientTexture(graphics, THEME_TAB, tabX, tabY, 34, 23,
                    custom.colors[slot], custom.colors[slot],
                    custom.colors[slot], custom.colors[slot]);
        }

        int hueColor = Color.HSBtoRGB(customHue, 1f, 1f);

        int svSteps = 32;
        float cell = svWidth / svSteps;
        for (int i = 0; i < svSteps; i++) {
            float saturation = i / (float) svSteps;
            float nextSaturation = (i + 1) / (float) svSteps;
            RenderUtil.gradientRect(graphics, svX + i * cell, svY, cell + 0.5f, svHeight,
                    ColorUtil.interpolate(-1, hueColor, saturation),
                    0xFF000000,
                    0xFF000000,
                    ColorUtil.interpolate(-1, hueColor, nextSaturation));
        }
        RenderUtil.roundedOutline(graphics, svX - 1, svY - 1, svWidth + 2, svHeight + 2,
                5, 1, ColorUtil.rgba(0, 0, 0, 220));

        float hueY = svY + svHeight + 13;

        int steps = 60;
        float stepWidth = svWidth / steps;
        for (int i = 0; i < steps; i++) {
            float hue = i / (float) steps;
            float nextHue = (i + 1) / (float) steps;
            RenderUtil.roundedRect(graphics, svX + i * stepWidth, hueY - 3.5f,
                    stepWidth + 0.5f, 7,
                    i == 0 ? 3.5f : 0, i == steps - 1 ? 3.5f : 0,
                    i == steps - 1 ? 3.5f : 0, i == 0 ? 3.5f : 0,
                    Color.HSBtoRGB(hue, 1f, 1f), Color.HSBtoRGB(nextHue, 1f, 1f));
        }

        float valueX = svX + customSaturation * svWidth;
        float valueY = svY + (1f - customBrightness) * svHeight;
        RenderUtil.circle(graphics, valueX, valueY, 4f, Color.BLACK.getRGB());
        RenderUtil.circle(graphics, valueX, valueY, 3f, -1);
        RenderUtil.circle(graphics, svX + customHue * svWidth, hueY, 5f, -1);

        custom.colors[customColorIndex] = Color.HSBtoRGB(customHue, customSaturation, customBrightness);
        graphics.pose().popMatrix();
    }

    private void drawThemeCard(GuiGraphicsExtractor graphics, Style style,
                               float x, float y, int mouseX, int mouseY) {
        int[] colors = themeColors(style);
        boolean hover = RenderUtil.isHovered(mouseX, mouseY, x, y, 77, 50);
        float target = style == Managment.STYLE_MANAGER.getCurrentStyle() ? 1f : hover ? .7f : 0f;
        float animation = AnimationMath.lerp(themeAnimations.getOrDefault(style, 0f), target, 5f);
        themeAnimations.put(style, animation);

        RenderUtil.roundedRect(graphics, x, y, 77, 50, 5, ColorUtil.rgba(25, 26, 33, 255));
        ThemeRenderUtil.gradientOutline(graphics, x, y, 77, 50, 5, 1,
                ColorUtil.rgba(63, 72, 103, 255), ColorUtil.rgba(19, 23, 39, 255),
                ColorUtil.rgba(63, 72, 103, 255), ColorUtil.rgba(19, 23, 39, 255));

        float off = 0;
        for (String word : style.name.split(" ")) {
            StyledFontRenderer.draw(graphics, font, Component.literal(word).withStyle(THEME_SEMIBOLD_16),
                    x + 7, y + 7 + off, -1);
            off += 10;
        }
        String[] hexes = hexLabels(style);
        StyledFontRenderer.draw(graphics, font, Component.literal(hexes[0]).withStyle(THEME_LIGHT_10),
                x + 7, y + 36, -1);
        StyledFontRenderer.draw(graphics, font, Component.literal(hexes[1]).withStyle(THEME_LIGHT_10),
                x + 7, y + 43, -1);

        float previewX = x + 34.5f;
        float previewY = y + 33.5f;

        if (animation > .001f) {
            for (int i = 3; i >= 1; i--) {
                int alpha = Math.round(255 * animation * 0.18f / i);
                float grow = i * 2f;
                ThemeRenderUtil.gradientTexture(graphics, THEME_CARD_PREVIEW,
                        previewX - grow, previewY - grow, 42 + grow * 2, 16 + grow * 2,
                        RenderUtil.withAlpha(colors[0], alpha), RenderUtil.withAlpha(colors[0], alpha),
                        RenderUtil.withAlpha(colors[1], alpha), RenderUtil.withAlpha(colors[1], alpha));
            }
        }
        ThemeRenderUtil.gradientTexture(graphics, THEME_CARD_PREVIEW, previewX, previewY, 42, 16,
                colors[0], colors[0], colors[1], colors[1]);
    }

    private void drawCustomTheme(GuiGraphicsExtractor graphics, Style custom,
                                 float areaX, float areaWidth, float y,
                                 int mouseX, int mouseY) {
        boolean hover = RenderUtil.isHovered(mouseX, mouseY, areaX, y, areaWidth, 50);
        float target = custom == Managment.STYLE_MANAGER.getCurrentStyle() ? 1f : hover ? .5f : 0f;
        customColorAnimation = AnimationMath.lerp(customColorAnimation, customColorOpen ? 1f : 0f, 15f);
        customThemeAnimation = AnimationMath.lerp(customThemeAnimation, target, 5f);
        int[] colors = themeColors(custom);

        RenderUtil.roundedRect(graphics, areaX, y, areaWidth, 50, 5, ColorUtil.rgba(25, 26, 33, 255));
        ThemeRenderUtil.gradientOutline(graphics, areaX, y, areaWidth, 50, 5, 1,
                ColorUtil.rgba(63, 72, 103, 255), ColorUtil.rgba(19, 23, 39, 255),
                ColorUtil.rgba(63, 72, 103, 255), ColorUtil.rgba(19, 23, 39, 255));

        StyledFontRenderer.draw(graphics, font, Component.literal(custom.name).withStyle(THEME_SEMIBOLD_18),
                areaX + 7, y + 7, -1);
        String[] customHexes = hexLabels(custom);
        StyledFontRenderer.draw(graphics, font, Component.literal(customHexes[0]).withStyle(THEME_LIGHT_10),
                areaX + 7, y + 35, -1);
        StyledFontRenderer.draw(graphics, font, Component.literal(customHexes[1]).withStyle(THEME_LIGHT_10),
                areaX + 7, y + 42, -1);

        float previewX = areaX + 157;
        float previewY = y + 15.5f;
        if (customThemeAnimation > .001f) {
            for (int i = 3; i >= 1; i--) {
                int alpha = Math.round(255 * customThemeAnimation * 0.18f / i);
                float grow = i * 3f;
                ThemeRenderUtil.gradientTexture(graphics, THEME_CUSTOM_PREVIEW,
                        previewX - grow, previewY - grow, 172.5f + grow * 2, 34.5f + grow * 2,
                        RenderUtil.withAlpha(colors[0], alpha), RenderUtil.withAlpha(colors[0], alpha),
                        RenderUtil.withAlpha(colors[1], alpha), RenderUtil.withAlpha(colors[1], alpha));
            }
        }
        ThemeRenderUtil.gradientTexture(graphics, THEME_CUSTOM_PREVIEW, previewX, previewY,
                172.5f, 34.5f, colors[0], colors[0], colors[1], colors[1]);

        float dotX = areaX + areaWidth - 15;
        float dotY = y + 20;
        RenderUtil.roundedOutline(graphics, dotX, dotY, 7, 7, 3.5f, 1,
                ColorUtil.rgba(255, 255, 255, 255));

        float openX = posX + WIDTH + 30;
        float openY = y + 34;
        float travelX = dotX + (openX - dotX) * customColorAnimation;
        float travelY = dotY + (openY - dotY) * customColorAnimation;
        themeLine(graphics, dotX + 3.5f, dotY + 3.5f, travelX + 3.5f, travelY + 3.5f, -1);
        RenderUtil.roundedOutline(graphics, travelX, travelY, 7, 7, 3.5f, 1,
                ColorUtil.rgba(255, 255, 255, 255));
    }

    private void themeLine(GuiGraphicsExtractor graphics, float x0, float y0,
                           float x1, float y1, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < .01f) {
            return;
        }
        float angle = (float) Math.atan2(dy, dx);
        graphics.pose().pushMatrix();
        graphics.pose().rotateAbout(angle, x0, y0);
        RenderUtil.roundedRect(graphics, x0, y0 - .25f, length, .5f, .25f, color);
        graphics.pose().popMatrix();
    }

    private int[] themeColors(Style style) {
        if (style.isRainbow()) {
            return new int[]{style.getColor(0), style.getColor(90)};
        }
        return new int[]{style.colors[0], style.colors[1]};
    }

    private String[] hexLabels(Style style) {
        int[] source = style.isRainbow() ? style.colors : themeColors(style);
        return new String[]{hex(source[0]), hex(source[1])};
    }

    private String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private void renderConfigs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float contentX = posX + BAR;
        float contentWidth = WIDTH - BAR;
        float headerHeight = 35;
        RenderUtil.roundedRect(graphics, contentX, posY + headerHeight, contentWidth, 0.5f, 0,
                ColorUtil.rgba(42, 47, 67, 220));

        for (int i = 0; i < 2; i++) {
            float bx = headerButtonX(contentX, i);
            boolean hovered = RenderUtil.isHovered(mouseX, mouseY, bx, posY + 10, 15, 15);
            RenderUtil.roundedRect(graphics, bx, posY + 10, 15, 15, 4,
                    ColorUtil.rgba(10, 11, 15, hovered ? 235 : 190));
            StyledFontRenderer.drawCentered(graphics, font,
                    Component.literal(i == 0 ? "H" : "L").withStyle(Fonts.CONFIG_ICONS_16),
                    bx + 7.5f, posY + 14, hovered ? -1 : RenderUtil.withAlpha(LIGHT, 220));
        }

        float nameX = headerButtonX(contentX, 2);
        float nameWidth = 130;
        if (configTyping) {
            RenderUtil.roundedRect(graphics, nameX, posY + 10, nameWidth, 15, 4,
                    ColorUtil.rgba(10, 11, 15, 210));
            String input = configName.isEmpty() ? "Имя конфига" : configName;
            if (System.currentTimeMillis() % 1000 > 500) {
                input += "_";
            }
            RenderUtil.scissor(graphics, nameX, posY + 10, nameWidth - 40, 15);
            StyledFontRenderer.draw(graphics, font, Component.literal(input).withStyle(Fonts.MEDIUM_14),
                    Math.round(nameX + 5), Math.round(posY + 14),
                    configName.isEmpty() ? RenderUtil.withAlpha(LIGHT, 190) : -1);
            RenderUtil.unscissor(graphics);

            boolean canCreate = !configName.isBlank();
            boolean createHover = RenderUtil.isHovered(mouseX, mouseY, nameX + nameWidth - 38, posY + 10, 38, 15);
            RenderUtil.roundedRect(graphics, nameX + nameWidth - 38, posY + 10, 38, 15, 4,
                    RenderUtil.withAlpha(Managment.STYLE_MANAGER.getColor(0),
                            canCreate ? (createHover ? 235 : 190) : 70));
            StyledFontRenderer.drawCentered(graphics, font,
                    Component.literal("Создать").withStyle(Fonts.MEDIUM_12),
                    nameX + nameWidth - 19, posY + 14,
                    canCreate ? -1 : RenderUtil.withAlpha(LIGHT, 160));
        }

        float searchWidth = 150;
        float searchX = contentX + contentWidth - searchWidth - 10;
        RenderUtil.roundedRect(graphics, searchX, posY + 10, searchWidth, 15, 4,
                ColorUtil.rgba(10, 11, 15, 210));
        boolean emptySearch = configSearch.isEmpty() && !configSearching;
        String search = emptySearch
                ? "Поиск конфига"
                : configSearch + (configSearching && System.currentTimeMillis() % 1000 > 500 ? "_" : "");

        RenderUtil.scissor(graphics, searchX, posY + 10, searchWidth, 15);
        StyledFontRenderer.drawCentered(graphics, font, Component.literal(search).withStyle(Fonts.MEDIUM_14),
                Math.round(searchX + searchWidth / 2), Math.round(posY + 14),
                emptySearch ? RenderUtil.withAlpha(LIGHT, 190) : -1);
        RenderUtil.unscissor(graphics);

        float cardX = contentX + 10;
        float cardY = posY + 51;
        float cardWidth = 158;
        float cardHeight = 50;
        String author = Managment.USER_PROFILE.getName();
        for (String config : Managment.CONFIG_MANAGER.list()) {
            if (!configSearch.isEmpty() && !config.toLowerCase().contains(configSearch.toLowerCase())) {
                continue;
            }
            RenderUtil.roundedRect(graphics, cardX, cardY, cardWidth, cardHeight, 5,
                    ColorUtil.rgba(42, 47, 67, 230));
            RenderUtil.roundedRect(graphics, cardX + 1, cardY + 1, cardWidth - 2, cardHeight - 2, 4,
                    ColorUtil.rgba(18, 19, 25, 245));
            StyledFontRenderer.draw(graphics, font, Component.literal(config).withStyle(Fonts.SEMIBOLD_15),
                    Math.round(cardX + 8), Math.round(cardY + 7), -1);

            String created = java.time.Instant.ofEpochMilli(Managment.CONFIG_MANAGER.lastModified(config))
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
            StyledFontRenderer.draw(graphics, font, Component.literal("Created: " + created).withStyle(Fonts.LIGHT_11),
                    Math.round(cardX + 8), Math.round(cardY + 25), LIGHT);
            StyledFontRenderer.draw(graphics, font, Component.literal("Author: " + author).withStyle(Fonts.LIGHT_11),
                    Math.round(cardX + 8), Math.round(cardY + 35), LIGHT);

            String[] icons = {"J", "M", "I"};
            for (int i = 0; i < icons.length; i++) {
                float buttonX = configButtonX(cardX, cardWidth, i);
                float buttonY = cardY + cardHeight - 20;
                RenderUtil.roundedRect(graphics, buttonX, buttonY, 14, 14, 4,
                        ColorUtil.rgba(0, 0, 0, 84));
                StyledFontRenderer.drawCentered(graphics, font,
                        Component.literal(icons[i]).withStyle(Fonts.CONFIG_ICONS_16),
                        buttonX + 7, buttonY + 3, -1);
            }

            cardX += cardWidth + 8;
            if (cardX + cardWidth > contentX + contentWidth - 10) {
                cardX = contentX + 10;
                cardY += cardHeight + 8;
            }
        }
    }

    private String configName = "";
    private boolean configTyping;
    private String configSearch = "";
    private boolean configSearching;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (current == Type.Theme) {
            scrolling = 0;
            return true;
        }
        scrolling += (float) scrollY * 30f;
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        if (RenderUtil.isHovered(mx, my, posX + 7.5f, posY + 30, BAR - 15, 17)) {
            searching = true;
            return true;
        }
        searching = false;

        for (Type type : Type.values()) {
            float len = categoryOffset(type);
            if (RenderUtil.isHovered(mx, my, posX + 5f, posY + 55 + len, BAR - 10, 15)) {
                current = type;
                scrolling = 0;
                return true;
            }
        }

        if (current == Type.Configs) {
            return handleConfigClick(mx, my);
        }
        if (current == Type.Theme) {
            return handleThemeClick(mx, my, event.button());
        }
        if (current == Type.Scripts) {
            if (RenderUtil.isHovered(mx, my, posX + BAR + 10, posY + 10, 15, 15)) {
                java.nio.file.Path dir = scriptDirectory();
                try {
                    java.nio.file.Files.createDirectories(dir);
                } catch (java.io.IOException ignored) {
                }
                Util.getPlatform().openPath(dir);
                return true;
            }
            if (RenderUtil.isHovered(mx, my, posX + BAR + 30, posY + 10, 15, 15)) {
                if (Managment.SCRIPT_MANAGER != null) {
                    Managment.SCRIPT_MANAGER.reload();
                }
                return true;
            }
            if (Managment.SCRIPT_MANAGER != null && event.button() == 0) {
                float cardX = posX + BAR + 10;
                float cardY = posY + 51;
                float cardWidth = 158;
                for (ScriptManager.Entry script : Managment.SCRIPT_MANAGER.getEntries()) {
                    if (RenderUtil.isHovered(mx, my, cardX, cardY, cardWidth, 30)) {
                        if (script.loaded()) {
                            script.function().toggle();
                        }
                        return true;
                    }
                    cardX += cardWidth + 8;
                    if (cardX + cardWidth > posX + WIDTH - 10) {
                        cardX = posX + BAR + 10;
                        cardY += 38;
                    }
                }
            }
            return true;
        }

        for (ModuleObject object : visibleObjects()) {
            if (object.mouseClicked(mx, my, event.button())) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private List<ModuleObject> visibleObjects() {
        List<ModuleObject> result = new ArrayList<>(column(0));
        result.addAll(column(1));
        return result;
    }

    private boolean handleThemeClick(double mx, double my, int button) {
        float areaX = posX + BAR + 10;
        float areaY = posY + 6;
        float areaWidth = WIDTH - BAR - 20;
        float cardWidth = 77;
        float cardHeight = 50;

        var styles = Managment.STYLE_MANAGER.styles;
        if (customColorOpen && handleCustomPickerClick(mx, my)) {
            return true;
        }
        int presetCount = Math.max(0, styles.size() - 1);
        for (int i = 0; i < presetCount; i++) {
            float cx = areaX + (i % 4) * 83.5f;
            float cy = areaY + (i / 4) * 55f;
            if (RenderUtil.isHovered(mx, my, cx, cy, cardWidth, cardHeight)) {
                Managment.STYLE_MANAGER.setCurrentStyle(styles.get(i));
                return true;
            }
        }
        if (!styles.isEmpty()) {
            float customY = posY + HEIGHT - 65;
            if (RenderUtil.isHovered(mx, my, areaX, customY, areaWidth, 50)) {
                if (button == 1) {
                    customColorOpen = !customColorOpen;
                    if (customColorOpen) {
                        loadCustomColor(0);
                    }
                } else if (button == 0) {
                    Managment.STYLE_MANAGER.setCurrentStyle(styles.getLast());
                }
                return true;
            }
        }
        return true;
    }

    private boolean handleCustomPickerClick(double mx, double my) {
        float pickerX = posX + WIDTH + 20;
        float pickerY = posY + HEIGHT - 157.5f;

        for (int slot = 0; slot < 2; slot++) {
            if (RenderUtil.isHovered(mx, my, pickerX + 8 + slot * 25, pickerY + 5, 25, 12)) {
                loadCustomColor(slot);
                return true;
            }
        }
        return updateCustomColor(mx, my);
    }

    private boolean updateCustomColor(double mx, double my) {
        float pickerX = posX + WIDTH + 20;
        float pickerY = posY + HEIGHT - 157.5f;
        float svX = pickerX + 10;
        float svY = pickerY + 16;

        if (draggingSaturation) {
            customSaturation = Mth.clamp((float) (mx - svX) / 111f, 0f, 1f);
            customBrightness = Mth.clamp(1f - (float) (my - svY) / 105f, 0f, 1f);
            return true;
        }
        if (draggingHue) {
            customHue = Mth.clamp((float) (mx - svX) / 111f, 0f, 1f);
            return true;
        }
        if (RenderUtil.isHovered(mx, my, svX, svY, 111, 105)) {
            draggingSaturation = true;
            customSaturation = Mth.clamp((float) (mx - svX) / 111f, 0f, 1f);
            customBrightness = Mth.clamp(1f - (float) (my - svY) / 105f, 0f, 1f);
            return true;
        }
        float hueY = svY + 105 + 13;
        if (RenderUtil.isHovered(mx, my, svX - 4, hueY - 6, 119, 12)) {
            draggingHue = true;
            customHue = Mth.clamp((float) (mx - svX) / 111f, 0f, 1f);
            return true;
        }
        return false;
    }

    private void loadCustomColor(int index) {
        customColorIndex = index;
        int color = Managment.STYLE_MANAGER.styles.getLast().colors[index];
        float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        customHue = hsb[0];
        customSaturation = hsb[1];
        customBrightness = hsb[2];
    }

    private boolean handleConfigClick(double mx, double my) {
        float contentX = posX + BAR;
        float contentWidth = WIDTH - BAR;

        if (RenderUtil.isHovered(mx, my, headerButtonX(contentX, 0), posY + 10, 15, 15)) {
            configTyping = !configTyping;
            configSearching = false;
            return true;
        }

        if (RenderUtil.isHovered(mx, my, headerButtonX(contentX, 1), posY + 10, 15, 15)) {
            Util.getPlatform().openPath(Managment.CONFIG_MANAGER.directory());
            return true;
        }

        float nameX = headerButtonX(contentX, 2);
        if (configTyping) {
            if (RenderUtil.isHovered(mx, my, nameX + 130 - 38, posY + 10, 38, 15)) {
                if (!configName.isBlank()) {
                    Managment.CONFIG_MANAGER.save(configName);
                    configName = "";
                    configTyping = false;
                }
                return true;
            }
            if (RenderUtil.isHovered(mx, my, nameX, posY + 10, 130, 15)) {
                return true;
            }
        }

        float searchWidth = 150;
        float searchX = contentX + contentWidth - searchWidth - 10;
        if (RenderUtil.isHovered(mx, my, searchX, posY + 10, searchWidth, 15)) {
            configSearching = true;
            configTyping = false;
            return true;
        }
        configSearching = false;

        float cardX = contentX + 10;
        float cardY = posY + 51;
        float cardWidth = 158;
        float cardHeight = 50;
        for (String config : Managment.CONFIG_MANAGER.list()) {
            if (!configSearch.isEmpty() && !config.toLowerCase().contains(configSearch.toLowerCase())) {
                continue;
            }
            float buttonY = cardY + cardHeight - 20;
            if (RenderUtil.isHovered(mx, my, configButtonX(cardX, cardWidth, 0), buttonY, 14, 14)) {
                Managment.CONFIG_MANAGER.delete(config);
                return true;
            }
            if (RenderUtil.isHovered(mx, my, configButtonX(cardX, cardWidth, 1), buttonY, 14, 14)) {
                Managment.CONFIG_MANAGER.save(config);
                return true;
            }
            if (RenderUtil.isHovered(mx, my, configButtonX(cardX, cardWidth, 2), buttonY, 14, 14)
                    || RenderUtil.isHovered(mx, my, cardX, cardY, cardWidth, 24)) {
                Managment.CONFIG_MANAGER.load(config);
                return true;
            }
            cardX += cardWidth + 8;
            if (cardX + cardWidth > contentX + contentWidth - 10) {
                cardX = contentX + 10;
                cardY += cardHeight + 8;
            }
        }
        return true;
    }

    private float configButtonX(float cardX, float cardWidth, int index) {
        return cardX + cardWidth - 20 - (2 - index) * 20;
    }

    private float headerButtonX(float contentX, int index) {
        return contentX + 10 + index * 20;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingHue = false;
        draggingSaturation = false;
        for (ModuleObject object : objects) {
            object.mouseReleased(event.x(), event.y(), event.button());
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (ModuleObject object : objects) {
            if (object.isCapturingInput()) {
                object.keyPressed(event);
                return true;
            }
        }

        if (searching && event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            }
            return true;
        }
        if (configTyping && event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!configName.isEmpty()) {
                configName = configName.substring(0, configName.length() - 1);
            }
            return true;
        }
        if (configSearching && event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!configSearch.isEmpty()) {
                configSearch = configSearch.substring(0, configSearch.length() - 1);
            }
            return true;
        }

        for (ModuleObject object : visibleObjects()) {
            object.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!event.isAllowedChatCharacter()) {
            return super.charTyped(event);
        }
        if (configTyping && configName.length() < 23) {
            configName += event.codepointAsString();
            return true;
        }
        if (configSearching && configSearch.length() < 23) {
            configSearch += event.codepointAsString();
            return true;
        }
        if (searching && searchText.length() < 13) {
            searchText += event.codepointAsString();
            return true;
        }
        for (ModuleObject object : visibleObjects()) {
            object.charTyped(event);
        }
        return super.charTyped(event);
    }
}
