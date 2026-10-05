package org.xrose.menu.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.xrose.feature.FeatureCategory;
import org.xrose.menu.core.MenuOverlayState;
import org.xrose.menu.core.MenuPage;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;
import org.xrose.utils.text.NameProtectUtil;
import org.xrose.utils.text.StringUtil;

public final class MenuSidebar extends Component {
   private static final int PADDING_X = 12;
   private static final int ROW_WIDTH = 156;
   private static final int ROW_RADIUS = 8;
   private static final int NAV_ROW_HEIGHT = 34;
   private static final int NAV_GAP = 4;
   private static final int GROUP_TOP = 56;
   private static final int GROUP2_TOP = 400;
   private static final int NAV_ICON_SIZE = 16;
   private static final int NAV_TEXT_SIZE = 12;
   private static final int NAV_ICON_TEXT_GAP = 10;
   private static final int CATEGORY_LABEL_Y = 184;
   private static final int CATEGORY_ROW_HEIGHT = 28;
   private static final int CATEGORY_GAP = 2;
   private static final int CATEGORY_TOP = 206;
   private static final int CATEGORY_ICON_SIZE = 14;
   private static final int CATEGORY_TEXT_SIZE = 11;
   private static final int PROFILE_HEIGHT = 52;
   private static final int PROFILE_BOTTOM = 16;
   private static final int PROFILE_RADIUS = 10;
   private static final int PROFILE_AVATAR_SIZE = 24;
   private static final MenuPage[] MAIN_PAGES = new MenuPage[]{MenuPage.NONE, MenuPage.SETTINGS, MenuPage.CONFIGURATIONS};
   private static final MenuPage[] UTILITY_PAGES = new MenuPage[]{MenuPage.SEARCH, MenuPage.FRIENDS, MenuPage.ACCOUNT_SWITCHER};
   private static final Identifier[] MAIN_ICONS = new Identifier[]{Textures.Icons.BOXES, Textures.Header.SETTINGS, Textures.Header.DOCUMENT};
   private static final String[] MAIN_LABELS = new String[]{"Modules", "Settings", "Configs"};
   private static final Identifier[] UTILITY_ICONS = new Identifier[]{Textures.Header.SEARCH, Textures.Header.FRIENDS, Textures.Header.PROFILE_ADD};
   private static final String[] UTILITY_LABELS = new String[]{"Search", "Friends", "Accounts"};
   private static final Identifier[] CATEGORY_ICONS = new Identifier[]{
      Textures.Icons.SWORDS, Textures.Icons.PERSON_STANDING, Textures.Icons.EYE, Textures.Icons.USER_ROUND, Textures.Icons.BOXES, Textures.Icons.BRAIN
   };
   private int mouseX;
   private int mouseY;
   private MenuPage activePage = MenuPage.NONE;
   private FeatureCategory activeCategory = FeatureCategory.COMBAT;
   private String username = "";
   private float panelRadius;

   public void place(Component frame, MenuOverlayState state, Minecraft minecraft, int mouseX, int mouseY, FeatureCategory activeCategory, float panelRadius) {
      this.attach(frame, frame.x(), frame.y(), frame.px(180.0F), frame.height());
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.activePage = state.page();
      this.activeCategory = activeCategory;
      this.username = StringUtil.abbreviate(NameProtectUtil.protect(minecraft.getUser().getName()), 14);
      this.panelRadius = panelRadius;
   }

   public MenuPage pageAt(float mouseX, float mouseY) {
      if (this.isProfileAt(mouseX, mouseY)) {
         return MenuPage.SETTINGS;
      }

      for (int index = 0; index < MAIN_PAGES.length; index++) {
         if (this.hit(mouseX, mouseY, 12.0F, mainRowY(index), 156.0F, 34.0F)) {
            return MAIN_PAGES[index];
         }
      }

      for (int index = 0; index < UTILITY_PAGES.length; index++) {
         if (this.hit(mouseX, mouseY, 12.0F, utilityRowY(index), 156.0F, 34.0F)) {
            return UTILITY_PAGES[index];
         }
      }

      return MenuPage.NONE;
   }

   public FeatureCategory categoryAt(float mouseX, float mouseY) {
      FeatureCategory[] categories = FeatureCategory.values();

      for (int index = 0; index < categories.length && index < CATEGORY_ICONS.length; index++) {
         if (this.hit(mouseX, mouseY, 12.0F, categoryRowY(index), 156.0F, 28.0F)) {
            return categories[index];
         }
      }

      return null;
   }

   public boolean isProfileAt(float mouseX, float mouseY) {
      return mouseX >= this.sx(12.0F) && mouseX <= this.sx(168.0F) && mouseY >= this.sy(profileY()) && mouseY <= this.sy(profileY() + 52.0F);
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      this.drawBackground();
      this.drawBrand();
      this.drawGroup(MAIN_PAGES, MAIN_ICONS, MAIN_LABELS, MenuSidebar::mainRowY, 34);
      this.drawDivider(174.0F);
      this.drawCategories();
      this.drawDivider(392.0F);
      this.drawGroup(UTILITY_PAGES, UTILITY_ICONS, UTILITY_LABELS, MenuSidebar::utilityRowY, 34);
      this.drawProfile();
   }

   private void drawBackground() {
      Render2DUtil.rect(this.x(), this.y(), this.width(), this.height())
         .color(ColorUtil.rgba(10, 12, 18, 120))
         .radius(this.panelRadius, 0.0F, 0.0F, this.panelRadius)
         .blur(this.px(8.0F))
         .draw();
      Render2DUtil.rect(this.x() + this.width() - Math.max(1.0F, this.px(1.0F)), this.y(), Math.max(1.0F, this.px(1.0F)), this.height())
         .color(ColorUtil.rgba(0, 0, 0, 110))
         .draw();
   }

   private void drawBrand() {
      Render2DUtil.texture(this.sx(14.0F), this.sy(13.0F), this.px(22.0F), this.px(22.0F), Textures.Logos.WATERMARK)
         .color(ColorUtil.multiplyAlpha(-1, 0.92F))
         .draw();
      Render2DUtil.text(this.sx(44.0F), UiFonts.sfProDisplay().centeredTextY(this.sy(24.0F), this.px(13.0F)), this.px(13.0F), "XRose")
         .style(UiFontStyle.SEMIBOLD)
         .color(-1)
         .draw();
   }

   private void drawGroup(MenuPage[] pages, Identifier[] icons, String[] labels, MenuSidebar.YMapper yMapper, int rowHeight) {
      for (int index = 0; index < pages.length; index++) {
         boolean active = this.activePage == pages[index];
         boolean hovered = this.hit(this.mouseX, this.mouseY, 12.0F, yMapper.y(index), 156.0F, rowHeight);
         this.drawRow(12.0F, yMapper.y(index), rowHeight, active, hovered);
         float iconX = 24.0F;
         float iconY = yMapper.y(index) + (rowHeight - 16) / 2.0F;
         int color = active ? Theme.getAccent() : (hovered ? -1 : Theme.Colors.ICON);
         this.texture(iconX, iconY, 16.0F, icons[index], color);
         this.text(
            50.0F,
            this.centeredTextY(yMapper.y(index) + rowHeight / 2.0F, 12.0F),
            12.0F,
            labels[index],
            active ? -1 : (hovered ? Theme.Colors.TEXT : Theme.Colors.TEXT_TEXT),
            UiFontStyle.MEDIUM
         );
      }
   }

   private void drawCategories() {
      Render2DUtil.text(this.sx(16.0F), this.sy(184.0F), this.px(9.0F), "CATEGORIES").style(UiFontStyle.MEDIUM).color(Theme.Colors.SECONDARY_DARK).draw();
      FeatureCategory[] categories = FeatureCategory.values();

      for (int index = 0; index < categories.length && index < CATEGORY_ICONS.length; index++) {
         boolean active = categories[index] == this.activeCategory;
         boolean hovered = this.hit(this.mouseX, this.mouseY, 12.0F, categoryRowY(index), 156.0F, 28.0F);
         this.drawRow(12.0F, categoryRowY(index), 28.0F, active, hovered);
         float iconX = 22.0F;
         float iconY = categoryRowY(index) + 7.0F;
         int color = active ? Theme.getAccent() : (hovered ? -1 : Theme.Colors.ICON);
         this.texture(iconX, iconY, 14.0F, CATEGORY_ICONS[index], color);
         this.text(
            46.0F,
            this.centeredTextY(categoryRowY(index) + 14.0F, 11.0F),
            11.0F,
            categories[index].getDisplayName(),
            active ? -1 : (hovered ? Theme.Colors.TEXT : Theme.Colors.TEXT_TEXT),
            UiFontStyle.REGULAR
         );
      }
   }

   private void drawProfile() {
      float y = profileY();
      boolean hovered = this.isProfileAt(this.mouseX, this.mouseY);
      Render2DUtil.rect(this.sx(12.0F), this.sy(y), this.px(156.0F), this.px(52.0F))
         .color(hovered ? ColorUtil.rgba(255, 255, 255, 16) : ColorUtil.rgba(10, 12, 18, 90))
         .radius(this.px(10.0F))
         .border(Math.max(0.5F, this.px(0.5F)), hovered ? ColorUtil.withAlpha(Theme.getAccent(), 90) : ColorUtil.rgba(0, 0, 0, 90))
         .draw();
      float avatarSize = this.px(24.0F);
      float avatarY = this.sy(y + 14.0F);
      Render2DUtil.texture(this.sx(22.0F), avatarY, avatarSize, avatarSize, Textures.Header.PROFILE_AVATAR).draw();
      float nameX = this.sx(54.0F);
      float nameCenterY = this.sy(y + 26.0F);
      Render2DUtil.text(nameX, UiFonts.sfProDisplay().centeredTextY(nameCenterY - this.px(6.0F), this.px(11.0F)), this.px(11.0F), this.username)
         .style(UiFontStyle.SEMIBOLD)
         .color(-1)
         .draw();
      Render2DUtil.text(nameX, UiFonts.sfProDisplay().centeredTextY(nameCenterY + this.px(6.0F), this.px(8.0F)), this.px(8.0F), "Open settings")
         .style(UiFontStyle.MEDIUM)
         .color(Theme.Colors.SECONDARY_DARK)
         .draw();
   }

   private void drawRow(float x, float y, float height, boolean active, boolean hovered) {
      if (active) {
         Render2DUtil.rect(this.sx(x), this.sy(y), this.px(156.0F), this.px(height))
            .color(ColorUtil.rgba(255, 255, 255, 22))
            .radius(this.px(8.0F))
            .border(Math.max(0.5F, this.px(0.5F)), ColorUtil.rgba(0, 0, 0, 110))
            .draw();
      } else {
         if (hovered) {
            Render2DUtil.rect(this.sx(x), this.sy(y), this.px(156.0F), this.px(height)).color(ColorUtil.rgba(255, 255, 255, 10)).radius(this.px(8.0F)).draw();
         }
      }
   }

   private void drawDivider(float y) {
      Render2DUtil.rect(this.sx(18.0F), this.sy(y), this.px(144.0F), Math.max(1.0F, this.px(1.0F))).color(Theme.Colors.DIVIDER_SUBTLE).draw();
   }

   private static float mainRowY(int index) {
      return 56 + index * 38;
   }

   private static float utilityRowY(int index) {
      return 400 + index * 38;
   }

   private static float categoryRowY(int index) {
      return 206 + index * 30;
   }

   private static float profileY() {
      return 572.0F;
   }

   @FunctionalInterface
   private interface YMapper {
      float y(int var1);
   }
}

