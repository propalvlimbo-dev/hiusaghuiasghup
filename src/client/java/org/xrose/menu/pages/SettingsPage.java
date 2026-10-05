package org.xrose.menu.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.menu.core.MenuConfigStore;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.i18n.UiLanguage;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.PageComponent;
import org.xrose.menu.ui.SmoothScroll;
import org.xrose.menu.ui.controls.DropdownComponent;
import org.xrose.menu.ui.controls.IconButton;
import org.xrose.menu.ui.controls.MarqueeText;
import org.xrose.menu.ui.controls.ModeComponent;
import org.xrose.menu.ui.controls.SliderComponent;
import org.xrose.menu.ui.popups.DropdownPopup;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.math.Animation;
import org.xrose.utils.math.MathUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class SettingsPage extends PageComponent {
   private static final int ACCENT_SWATCH_COUNT = 10;
   private static final int ACCENT_SWATCH_Y = 256;
   private static final int ACCENT_SWATCH_SIZE = 18;
   private static final int ACCENT_SWATCH_GAP = 4;
   private static final int ACCENT_SWATCH_STEP = 22;
   private static final int ACCENT_SWATCH_WIDTH = 216;
   private static final int ACCENT_SWATCH_RIGHT = 488;
   private static final int ACCENT_SWATCH_X = 272;
   private static final int RESET_BUTTON_SIZE = 24;
   private static final int RESET_ICON_SIZE = 16;
   private static final int RESET_BUTTON_X = 968;
   private static final int RESET_BUTTON_Y = 110;
   private static final int TOP_DROPDOWN_RIGHT = 960;
   private static final String[] CORNER_MODES = new String[]{"Small", "Medium", "Large"};
   private static final int CONTENT_TOP = 200;
   private static final int CONTENT_BOTTOM = 580;
   private final List<Component> children = new ArrayList<>();
   private final Map<String, MarqueeText> descriptionLabels = new HashMap<>();
   private final SliderComponent uiScaleSlider = new SliderComponent(
         () -> this.pendingUiScaleValue, value -> this.pendingUiScaleValue = value, Theme.Colors.OUTLINES_SMALL, Theme.getAccent(), -1
      )
      .step(0.006666667F)
      .onRelease(this::applyUiScale);
   private final SliderComponent accentRSlider = new SliderComponent(() -> this.accentR, value -> {
      this.accentR = value;
      this.updateCustomAccent();
   }, Theme.Colors.OUTLINES_SMALL, ColorUtil.rgb(255, 96, 96), -1).step(0.003921569F);
   private final SliderComponent accentGSlider = new SliderComponent(() -> this.accentG, value -> {
      this.accentG = value;
      this.updateCustomAccent();
   }, Theme.Colors.OUTLINES_SMALL, ColorUtil.rgb(96, 224, 128), -1).step(0.003921569F);
   private final SliderComponent accentBSlider = new SliderComponent(() -> this.accentB, value -> {
      this.accentB = value;
      this.updateCustomAccent();
   }, Theme.Colors.OUTLINES_SMALL, ColorUtil.rgb(96, 160, 255), -1).step(0.003921569F);
   private final ModeComponent languageDropdown = new ModeComponent(() -> MenuText.option(UiLanguage.canonicalNames()[this.languageMode]));
   private final ModeComponent roundedModeDropdown = new ModeComponent(() -> MenuText.option(CORNER_MODES[this.roundedMode]));
   private final DropdownPopup languagePopup = new DropdownPopup(() -> UiLanguage.canonicalNames(), () -> this.languageMode, index -> {
      this.languageMode = index;
      UiLanguage.set(UiLanguage.byIndex(index));
   });
   private final DropdownPopup cornersPopup = new DropdownPopup(() -> CORNER_MODES, () -> this.roundedMode, index -> {
      this.roundedMode = index;
      this.applyPanelRadius();
   });
   private final List<DropdownPopup> popups = List.of(this.languagePopup, this.cornersPopup);
   private final IconButton resetButton = new IconButton(Textures.Icons.REFRESH_CCW, 16, this::resetSettings).tint(Theme.Colors.SECONDARY, -1);
   private float pendingUiScaleValue = 0.28F;
   private float accentR = 1.0F;
   private float accentG = 1.0F;
   private float accentB = 1.0F;
   private int languageMode = UiLanguage.current().ordinal();
   private int roundedMode = MathUtil.clamp(MenuConfigStore.getInt("roundedMode", 1), 0, Theme.Sizes.PANEL_RADII.length - 1);
   private final Animation accentAnimation = new Animation(180L, Animation.Easing.EASE_OUT_QUAD);
   private final SmoothScroll scroll = new SmoothScroll();
   private float scrollY;
   private int animatedAccentTarget;

   public SettingsPage() {
      this.children.add(this.uiScaleSlider);
      this.children.add(this.accentRSlider);
      this.children.add(this.accentGSlider);
      this.children.add(this.accentBSlider);
      Theme.setAccentIndex(MenuConfigStore.getInt("accentIndex", 1));
      if (MenuConfigStore.getBoolean("customAccentActive", false)) {
         Theme.setCustomAccent(MenuConfigStore.getInt("customAccent", -1));
      }

      this.accentR = ColorUtil.red(Theme.getAccent()) / 255.0F;
      this.accentG = ColorUtil.green(Theme.getAccent()) / 255.0F;
      this.accentB = ColorUtil.blue(Theme.getAccent()) / 255.0F;
      this.animatedAccentTarget = Theme.accentIndex();
      this.accentAnimation.animate(this.animatedAccentTarget, this.animatedAccentTarget, 0L, Animation.Easing.EASE_OUT_QUAD);
   }

   @Override
   protected void onLayout() {
      if (!this.uiScaleSlider.isDragging()) {
         this.pendingUiScaleValue = this.state.uiScaleValue();
      }

      this.scrollY = this.scroll.update(this.maxScroll());
      this.placeControls();
   }

   private float maxScroll() {
      float viewportBottom = this.designY(this.y() + this.height());
      return Math.max(0.0F, 580.0F - viewportBottom);
   }

   private int oy(int designY) {
      return designY >= 200 ? designY - Math.round(this.scrollY) : designY;
   }

   public boolean handleMouseButton(int mouseX, int mouseY, int button) {
      boolean consumed = this.processMouseButton(mouseX, mouseY, button);
      if (consumed) {
         this.persistSettings();
      }

      return consumed;
   }

   private boolean processMouseButton(int mouseX, int mouseY, int button) {
      if (!this.contentContains(mouseX, mouseY)) {
         return false;
      }

      if (button == 0) {
         for (DropdownPopup popup : this.popups) {
            if (popup.isOpen() && popup.handleClick(mouseX, mouseY)) {
               return true;
            }
         }
      }

      if (button != 0) {
         return true;
      }

      for (Component child : this.children) {
         if (child.handleClick(mouseX, mouseY)) {
            return true;
         }
      }

      if (this.languageDropdown.handleClick(mouseX, mouseY)) {
         this.openPopup(this.languagePopup);
         return true;
      }

      if (this.roundedModeDropdown.handleClick(mouseX, mouseY)) {
         this.openPopup(this.cornersPopup);
         return true;
      }

      if (this.resetButton.handleClick(mouseX, mouseY)) {
         return true;
      }

      if (this.hit(mouseX, mouseY, 272.0F, this.oy(256) - 6, 216.0F, 32.0F)) {
         this.setAccentIndex((int)MathUtil.clamp((this.designX(mouseX) - 272.0F) / 22.0F, 0.0F, Theme.accentCount() - 1));
      }

      return true;
   }

   private void openPopup(DropdownPopup popup) {
      for (DropdownPopup other : this.popups) {
         if (other != popup) {
            other.closeImmediately();
         }
      }

      popup.toggle();
   }

   public void drag(int mouseX, int mouseY) {
      this.uiScaleSlider.drag(mouseX);
      this.accentRSlider.drag(mouseX);
      this.accentGSlider.drag(mouseX);
      this.accentBSlider.drag(mouseX);
   }

   public void releasePointer() {
      boolean wasDragging = this.uiScaleSlider.isDragging()
         || this.accentRSlider.isDragging()
         || this.accentGSlider.isDragging()
         || this.accentBSlider.isDragging();
      this.uiScaleSlider.releasePointer();
      this.accentRSlider.releasePointer();
      this.accentGSlider.releasePointer();
      this.accentBSlider.releasePointer();
      if (wasDragging) {
         this.persistSettings();
      }
   }

   public void handleScroll(int mouseX, int mouseY, double vertical) {
      if (this.hit(mouseX, mouseY, 272.0F, this.oy(256) - 6, 216.0F, 32.0F)) {
         int next = Math.floorMod(Theme.accentIndex() + (vertical > 0.0 ? -1 : 1), Theme.accentCount());
         this.setAccentIndex(next);
         this.persistSettings();
      } else {
         if (this.contentContains(mouseX, mouseY)) {
            this.scroll.scroll(vertical, this.maxScroll());
         }
      }
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      if (!(this.progress <= 0.001F)) {
         this.pageHeader(MenuText.ui("Settings"), MenuText.ui("Customize your cheat client to meet your needs with just one click."));
         this.resetButton.render(minecraft, guiGraphicsExtractor);
         Render2DUtil.pushScissor(this.x(), this.sy(200.0F), this.width(), this.y() + this.height() - this.sy(200.0F));
         this.renderBody(minecraft, guiGraphicsExtractor);
         Render2DUtil.popScissor();

         for (DropdownPopup popup : this.popups) {
            popup.render(minecraft, guiGraphicsExtractor);
         }
      }
   }

   private void renderBody(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      this.sectionLabel(32, 214, "Theme Editor");
      this.sectionLabel(536, 214, "Interface");
      this.sectionCard(16, 234, 488, 160);
      this.sectionCard(520, 234, 488, 120);
      this.settingText(32, 246, 39, "Client Color", "Adjust the main accent color of the interface.", 260);

      for (int index = 0; index < Theme.accentCount(); index++) {
         int swatchX = 272 + index * 22;
         this.rect(swatchX, this.oy(256), 18.0F, 18.0F, Theme.accent(index), 4.0F, this.progress);
      }

      this.settingDivider(32, dividerY(246, 39, 297, 42));
      this.settingText(32, 297, 42, "Custom Accent", "Fine-tune the RGB channels of your theme accent.", 316);
      this.textRight(336.0F, this.centeredTextY(this.oy(305), 10.0F), 10.0F, "R", Theme.Colors.SECONDARY, this.progress);
      this.textRight(336.0F, this.centeredTextY(this.oy(319), 10.0F), 10.0F, "G", Theme.Colors.SECONDARY, this.progress);
      this.textRight(336.0F, this.centeredTextY(this.oy(333), 10.0F), 10.0F, "B", Theme.Colors.SECONDARY, this.progress);
      this.rect(464.0F, this.oy(316), 24.0F, 18.0F, Theme.getAccent(), 4.0F, this.progress);
      this.outline(464.0F, this.oy(316), 24.0F, 18.0F, Theme.Colors.OUTLINES_SMALL, 4.0F, 0.5F, this.progress);
      this.settingDivider(32, dividerY(297, 42, 351, 42));
      this.settingText(32, 351, 42, "Language", "Choose the language that suits you.", 364);
      this.settingText(536, 246, 39, "UI Scale", "Change the overall size of the client menus.", 810);
      this.textRight(
         859.0F,
         this.centeredTextY(this.oy(256) + 8.0F, 12.0F),
         12.0F,
         String.format(Locale.ROOT, "%.0f%%", MathUtil.lerp(50.0F, 200.0F, this.pendingUiScaleValue)),
         Theme.Colors.SECONDARY,
         this.progress
      );
      this.settingDivider(536, dividerY(246, 39, 297, 42));
      this.settingText(536, 297, 42, "Rounded Corners", "Select the corner smoothness for menu elements.", 868);

      for (Component child : this.children) {
         child.render(minecraft, guiGraphicsExtractor);
      }

      this.languageDropdown.render(minecraft, guiGraphicsExtractor);
      this.roundedModeDropdown.render(minecraft, guiGraphicsExtractor);
      float indicatorX = 272.0F + this.accentAnimation.getValue() * 22.0F;
      if (!Theme.isCustomAccent()) {
         this.outline(indicatorX, this.oy(256), 18.0F, 18.0F, -1, 4.0F, 2.0F, this.progress);
      }
   }

   private void setAccentIndex(int index) {
      float from = this.accentAnimation.getValue();
      Theme.setAccentIndex(index);
      int accent = Theme.getAccent();
      this.accentR = ColorUtil.red(accent) / 255.0F;
      this.accentG = ColorUtil.green(accent) / 255.0F;
      this.accentB = ColorUtil.blue(accent) / 255.0F;
      this.animatedAccentTarget = Theme.accentIndex();
      this.accentAnimation.animate(from, this.animatedAccentTarget, 180L, Animation.Easing.EASE_OUT_QUAD);
   }

   private void updateCustomAccent() {
      int color = ColorUtil.rgb(Math.round(this.accentR * 255.0F), Math.round(this.accentG * 255.0F), Math.round(this.accentB * 255.0F));
      Theme.setCustomAccent(color);
      this.animatedAccentTarget = Theme.accentIndex();
      this.accentAnimation.animate(this.accentAnimation.getValue(), this.animatedAccentTarget, 180L, Animation.Easing.EASE_OUT_QUAD);
   }

   private void placeControls() {
      int accent = Theme.getAccent();
      this.uiScaleSlider.place(this, 864, this.oy(256), 128, 16).style(Theme.Colors.OUTLINES_SMALL, accent, -1).alpha(this.progress);
      this.accentRSlider.place(this, 360, this.oy(305), 104, 12).style(Theme.Colors.OUTLINES_SMALL, ColorUtil.rgb(255, 96, 96), -1).alpha(this.progress);
      this.accentGSlider.place(this, 360, this.oy(319), 104, 12).style(Theme.Colors.OUTLINES_SMALL, ColorUtil.rgb(96, 224, 128), -1).alpha(this.progress);
      this.accentBSlider.place(this, 360, this.oy(333), 104, 12).style(Theme.Colors.OUTLINES_SMALL, ColorUtil.rgb(96, 160, 255), -1).alpha(this.progress);
      this.resetButton.place(this, 968.0F, 110.0F, 24, this.mouseX, this.mouseY).alpha(this.progress);
      this.placeValueRight(this.languageDropdown, 488, this.oy(centeredControlY(351, 42, 24)));
      this.placeValueRight(this.roundedModeDropdown, 992, this.oy(centeredControlY(297, 42, 24)));
      this.languagePopup.place(this, 344, this.oy(centeredControlY(351, 42, 24) + 24 + 4), this.mouseX, this.mouseY, this.progress);
      this.cornersPopup.place(this, 848, this.oy(centeredControlY(297, 42, 24) + 24 + 4), this.mouseX, this.mouseY, this.progress);
   }

   private void placeValueRight(DropdownComponent dropdown, int right, int y) {
      dropdown.place(this, right - 112, y, 112, 24).style(DropdownComponent.Style.VALUE).alpha(this.progress);
   }

   private static int centeredControlY(int rowY, int rowHeight, int controlHeight) {
      return rowY + Math.round((rowHeight - controlHeight) / 2.0F);
   }

   private void sectionLabel(int x, int y, String label) {
      this.text(x, this.oy(y), 12.0F, MenuText.ui(label), Theme.Colors.TEXT_GHOST, this.progress, UiFontStyle.REGULAR);
   }

   private void sectionCard(int x, int y, int width, int height) {
      Render2DUtil.rect(this.sx(x), this.sy(this.oy(y)), this.px(width), this.px(height))
         .color(this.alpha(Theme.Colors.BACKGROUND_SURFACE_S, this.progress))
         .radius(this.px(8.0F))
         .border(Math.max(1.0F, this.px(1.0F)), this.alpha(Theme.Colors.OUTLINES_SMALL, this.progress))
         .draw();
   }

   private void settingDivider(int x, int y) {
      this.rect(x, this.oy(y), 456.0F, 1.0F, Theme.Colors.OUTLINES_SMALL, 0.0F, this.progress);
   }

   private static int dividerY(int previousY, int previousHeight, int nextY, int nextHeight) {
      return Math.round((previousY + previousHeight + nextY) / 2.0F + (nextHeight - previousHeight) / 4.0F);
   }

   private void settingText(int x, int rowY, int rowHeight, String title, String description, int textRight) {
      this.settingText(x, rowY, rowHeight, title, description, textRight, true);
   }

   private void settingText(int x, int rowY, int rowHeight, String title, String description, int textRight, boolean enabled) {
      float titleSize = 14.0F;
      float descriptionSize = 12.0F;
      float gap = 4.0F;
      float stateAlpha = this.progress * (enabled ? 1.0F : 0.45F);
      float titleHeight = UiFonts.sfProDisplay().textHeight(this.px(titleSize));
      float descriptionHeight = UiFonts.sfProDisplay().textHeight(this.px(descriptionSize));
      float blockHeight = titleHeight + this.px(gap) + descriptionHeight;
      float titleY = this.sy(this.oy(rowY)) + (this.px(rowHeight) - blockHeight) / 2.0F;
      Render2DUtil.text(this.sx(x), titleY, this.px(titleSize), MenuText.ui(title))
         .style(UiFontStyle.MEDIUM)
         .color(this.alpha(enabled ? Theme.Colors.TEXT_TEXT : Theme.Colors.TEXT_GHOST, stateAlpha))
         .draw();
      float descriptionY = titleY + titleHeight + this.px(gap);
      MarqueeText marquee = this.descriptionLabels.computeIfAbsent(title + "\u0000" + description, ignored -> new MarqueeText(() -> MenuText.ui(description)));
      marquee.placeAt(this, this.sx(x), descriptionY, this.px(Math.max(0, textRight - x)), descriptionHeight, this.mouseX, this.mouseY)
         .style(descriptionSize, UiFontStyle.REGULAR, enabled ? Theme.Colors.SECONDARY : Theme.Colors.TEXT_GHOST, stateAlpha)
         .render(Minecraft.getInstance(), null);
   }

   private void resetSettings() {
      Theme.clearCustomAccent();
      this.setAccentIndex(0);
      this.pendingUiScaleValue = 0.28F;
      this.applyUiScale();
      this.roundedMode = 1;
      this.applyPanelRadius();
      this.languageMode = 0;
      UiLanguage.set(UiLanguage.ENGLISH);
   }

   private void persistSettings() {
      MenuConfigStore.save(data -> {
         data.addProperty("accentIndex", Theme.accentIndex());
         if (Theme.isCustomAccent()) {
            data.addProperty("customAccentActive", true);
            data.addProperty("customAccent", Theme.getAccent());
         } else {
            data.remove("customAccentActive");
            data.remove("customAccent");
         }

         data.remove("themeValue");
         data.remove("fpsMode");
         data.addProperty("language", UiLanguage.byIndex(this.languageMode).storageKey());
         data.addProperty("roundedMode", this.roundedMode);
         data.remove("lowPerformance");
         data.remove("reduceShadows");
         data.remove("cacheUi");
         data.remove("sound");
      });
   }

   private void applyUiScale() {
      if (this.state != null) {
         this.state.setUiScaleValue(this.pendingUiScaleValue);
      }
   }

   private void applyPanelRadius() {
      if (this.state != null) {
         this.state.setPanelRadius(Theme.Sizes.PANEL_RADII[this.roundedMode]);
      }
   }
}

