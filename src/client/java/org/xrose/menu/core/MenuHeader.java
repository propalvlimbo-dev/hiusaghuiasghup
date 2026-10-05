package org.xrose.menu.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.feature.FeatureCategory;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.controls.IconButton;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.TextAlign;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class MenuHeader extends Component {
   private static final int CONTENT_PADDING_X = 24;
   private static final int BUTTON_GAP = 8;
   private static final int TITLE_SIZE = 16;
   private static final int COUNTER_PILL_HEIGHT = 22;
   private static final int COUNTER_PILL_TEXT_SIZE = 10;
   private static final int COUNTER_PILL_PADDING_X = 8;
   private static final int TITLE_COUNTER_GAP = 10;
   private final IconButton searchButton = new IconButton(Textures.Header.SEARCH, 0, null)
      .radius(8)
      .tint(Theme.Colors.ICON, -1)
      .activeTint(-1)
      .hoverBackground(ColorUtil.rgba(255, 255, 255, 12))
      .activeBackground(ColorUtil.rgba(255, 255, 255, 24), ColorUtil.rgba(255, 255, 255, 32));
   private final IconButton chevronLeftButton = new IconButton(Textures.Header.CHEVRON_LEFT, 0, null).tint(Theme.Colors.ICON, Theme.Colors.ICON);
   private final IconButton chevronRightButton = new IconButton(Textures.Header.CHEVRON_RIGHT, 0, null).tint(Theme.Colors.ICON, Theme.Colors.ICON);
   private MenuOverlayState state;
   private int mouseX;
   private int mouseY;
   private MenuPage activePage = MenuPage.NONE;
   private FeatureCategory activeCategory = FeatureCategory.COMBAT;
   private int categoryEnabled;
   private int categoryTotal;

   public void place(Component frame, MenuOverlayState state, Minecraft minecraft, int mouseX, int mouseY, FeatureCategory category, int enabled, int total) {
      this.attach(frame, frame.x() + frame.px(180.0F), frame.y(), frame.width() - frame.px(180.0F), frame.px(56.0F));
      this.state = state;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.activePage = state.page();
      this.activeCategory = category;
      this.categoryEnabled = enabled;
      this.categoryTotal = total;
   }

   public MenuPage pageAt(float mouseX, float mouseY) {
      return MenuPage.NONE;
   }

   public boolean isSearchAt(float mouseX, float mouseY) {
      return this.searchButton.contains(mouseX, mouseY);
   }

   public boolean isChevronLeftAt(float mouseX, float mouseY) {
      return this.chevronLeftButton.contains(mouseX, mouseY);
   }

   public boolean isChevronRightAt(float mouseX, float mouseY) {
      return this.chevronRightButton.contains(mouseX, mouseY);
   }

   public boolean handleMouseButton(float mouseX, float mouseY, int button) {
      return false;
   }

   public boolean handleScroll(float mouseX, float mouseY, double vertical) {
      return false;
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      this.drawLabel();
      this.drawCounterPill();
      this.drawControls(minecraft);
      Render2DUtil.rect(this.x(), this.y() + this.height() - Math.max(1.0F, this.px(1.0F)), this.width(), Math.max(1.0F, this.px(1.0F)))
         .color(Theme.Colors.DIVIDER_HEADER)
         .draw();
   }

   private void drawLabel() {
      float titleSize = this.px(16.0F);
      float titleX = this.x() + this.px(24.0F);
      float titleY = UiFonts.sfProDisplay().centeredTextY(this.y() + this.height() / 2.0F, titleSize);
      Render2DUtil.text(titleX, titleY, titleSize, this.title()).style(UiFontStyle.SEMIBOLD).color(-1).draw();
   }

   private void drawCounterPill() {
      if (this.activePage == MenuPage.NONE && this.categoryTotal > 0) {
         MsdfFont font = UiFonts.sfProDisplay();
         float titleSize = this.px(16.0F);
         float letterSpacing = titleSize * UiFontStyle.SEMIBOLD.letterSpacingEm();
         float titleWidth = font.measureWidth(this.title(), titleSize, letterSpacing);
         float pillX = this.x() + this.px(24.0F) + titleWidth + this.px(10.0F);
         String label = this.categoryEnabled + "/" + this.categoryTotal;
         float textSize = this.px(10.0F);
         float textSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
         float textWidth = font.measureWidth(label, textSize, textSpacing);
         float pillHeight = this.px(22.0F);
         float pillY = this.y() + (this.height() - pillHeight) / 2.0F;
         float pillWidth = textWidth + this.px(16.0F);
         Render2DUtil.rect(pillX, pillY, pillWidth, pillHeight)
            .color(ColorUtil.rgba(255, 255, 255, 14))
            .radius(pillHeight / 2.0F)
            .border(Math.max(0.5F, this.px(0.5F)), ColorUtil.rgba(0, 0, 0, 110))
            .draw();
         Render2DUtil.text(pillX + pillWidth / 2.0F, UiFonts.sfProDisplay().centeredTextY(pillY + pillHeight / 2.0F, textSize), textSize, label)
            .style(UiFontStyle.MEDIUM)
            .align(TextAlign.CENTER)
            .color(Theme.getAccent())
            .draw();
      }
   }

   private void drawControls(Minecraft minecraft) {
      float buttonSize = this.px(24.0F);
      float buttonY = this.y() + (this.height() - buttonSize) / 2.0F;
      int iconSize = 16;
      float searchX = this.x() + this.width() - this.px(16.0F) - buttonSize;
      float rightChevronX = searchX - this.px(8.0F) - buttonSize;
      float leftChevronX = rightChevronX - this.px(8.0F) - buttonSize;
      this.searchButton
         .placeAt(this, searchX, buttonY, buttonSize, this.mouseX, this.mouseY)
         .iconSize(iconSize)
         .active(this.activePage == MenuPage.SEARCH)
         .render(minecraft, null);
      this.chevronRightButton
         .placeAt(this, rightChevronX, buttonY, buttonSize, this.mouseX, this.mouseY)
         .iconSize(iconSize)
         .enabled(this.state != null && this.state.canGoForward())
         .render(minecraft, null);
      this.chevronLeftButton
         .placeAt(this, leftChevronX, buttonY, buttonSize, this.mouseX, this.mouseY)
         .iconSize(iconSize)
         .enabled(this.state != null && this.state.canGoBack())
         .render(minecraft, null);
   }

   private String title() {
      return switch (this.activePage) {
         case SEARCH -> MenuText.ui("Search");
         case FRIENDS -> MenuText.ui("Friends");
         case ACCOUNT_SWITCHER -> MenuText.ui("Accounts");
         case SETTINGS -> MenuText.ui("Settings");
         case CONFIGURATIONS -> MenuText.ui("Configs");
         default -> this.activeCategory.getDisplayName();
      };
   }
}

