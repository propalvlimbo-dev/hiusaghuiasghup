package org.xrose.menu.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.menu.pages.ConfigPage;
import org.xrose.menu.pages.SettingsPage;
import org.xrose.menu.pages.accounts.AccountPage;
import org.xrose.menu.pages.friends.FriendPage;
import org.xrose.menu.pages.modules.ModulePage;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.MenuSidebar;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.math.MathUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;

public final class MenuOverlayRenderer extends Component {
   private final MenuSidebar sidebar = new MenuSidebar();
   private final MenuHeader header = new MenuHeader();
   private final MenuOverlayRenderer.ContentFrame content = new MenuOverlayRenderer.ContentFrame();
   private final ModulePage modules = new ModulePage();
   private final SettingsPage settings = new SettingsPage();
   private final FriendPage friends = new FriendPage();
   private final AccountPage accounts = new AccountPage();
   private final ConfigPage configs = new ConfigPage();
   private MenuDimensions dimensions;
   private MenuPage displayPage = MenuPage.NONE;

   public void layout(Minecraft minecraft, MenuOverlayState state, int screenWidth, int screenHeight, int mouseX, int mouseY) {
      this.dimensions = MenuDimensions.resolve(minecraft, state);
      this.ensurePosition(state, screenWidth, screenHeight);
      float slideY = (1.0F - state.openProgress()) * Math.max(0.0F, screenHeight - state.panelY());
      this.configureFrame(state.panelX(), state.panelY() + slideY, this.dimensions.panelWidth(), this.dimensions.panelHeight());
      this.displayPage = state.displayPage();
      this.sidebar.place(this, state, minecraft, mouseX, mouseY, this.modules.selectedCategory(), this.dimensions.panelRadius());
      this.content.configureFrame(this.x() + this.px(180.0F), this.y(), this.width() - this.px(180.0F), this.height());
      this.modules.layout(this.content, state, mouseX, mouseY);
      this.settings.layout(this.content, state, mouseX, mouseY);
      this.friends.layout(this.content, state, mouseX, mouseY);
      this.accounts.layout(this.content, state, mouseX, mouseY);
      this.configs.layout(this.content, state, mouseX, mouseY);
      this.header
         .place(
            this,
            state,
            minecraft,
            mouseX,
            mouseY,
            this.modules.selectedCategory(),
            enabledCount(this.modules.selectedCategory()),
            totalCount(this.modules.selectedCategory())
         );
   }

   public boolean handleMouseButton(int mouseX, int mouseY, int button, MenuOverlayState state) {
      if (button == 0) {
         FeatureCategory category = this.sidebar.categoryAt(mouseX, mouseY);
         if (category != null) {
            if (state.page() != MenuPage.NONE) {
               state.openPage(MenuPage.NONE);
            }

            this.modules.selectCategory(category);
            this.displayPage = state.displayPage();
            return true;
         }

         MenuPage page = this.sidebar.pageAt(mouseX, mouseY);
         if (page != MenuPage.NONE) {
            state.openPage(page);
            this.displayPage = state.displayPage();
            return true;
         }

         if (this.sidebar.contains(mouseX, mouseY)) {
            return true;
         }

         if (this.header.isChevronLeftAt(mouseX, mouseY)) {
            state.goBack();
            this.displayPage = state.displayPage();
            return true;
         }

         if (this.header.isChevronRightAt(mouseX, mouseY)) {
            state.goForward();
            this.displayPage = state.displayPage();
            return true;
         }

         if (this.header.isSearchAt(mouseX, mouseY)) {
            state.openPage(state.page() == MenuPage.SEARCH ? MenuPage.NONE : MenuPage.SEARCH);
            this.displayPage = state.displayPage();
            return true;
         }
      }
      return switch (state.displayPage()) {
         case SETTINGS -> this.settings.handleMouseButton(mouseX, mouseY, button);
         case FRIENDS -> this.friends.handleClick(mouseX, mouseY);
         case ACCOUNT_SWITCHER -> this.accounts.handleClick(mouseX, mouseY);
         case CONFIGURATIONS -> this.configs.handleClick(mouseX, mouseY);
         default -> this.modules.handleMouseButton(mouseX, mouseY, button);
      };
   }

   public void drag(int mouseX, int mouseY) {
      switch (this.displayPage) {
         case SETTINGS:
            this.settings.drag(mouseX, mouseY);
            break;
         case NONE:
         case SEARCH:
            this.modules.drag(mouseX);
      }
   }

   public void releasePointer() {
      this.modules.releasePointer();
      this.settings.releasePointer();
   }

   public boolean isSearchOpen() {
      return this.modules.isSearchOpen();
   }

   public boolean isSearchFocused() {
      return this.modules.isSearchFocused();
   }

   public void focusSearch() {
      this.modules.focusSearch();
   }

   public boolean isCapturingBind() {
      return this.displayPage == MenuPage.NONE && this.modules.isCapturingBind();
   }

   public void appendSearchCodePoint(int codePoint) {
      this.modules.appendCodePoint(codePoint);
   }

   public void backspaceSearch() {
      this.modules.backspace();
   }

   public boolean handleKey(int key) {
      if (this.displayPage == MenuPage.FRIENDS) {
         return this.friends.handleKey(key);
      } else if (this.displayPage == MenuPage.ACCOUNT_SWITCHER) {
         return this.accounts.handleKey(key);
      } else {
         return this.displayPage == MenuPage.CONFIGURATIONS
            ? this.configs.handleKey(key)
            : (this.displayPage == MenuPage.NONE || this.displayPage == MenuPage.SEARCH) && this.modules.handleKey(key);
      }
   }

   public boolean handleCharacter(int codePoint) {
      if (this.displayPage == MenuPage.FRIENDS) {
         return this.friends.handleCharacter(codePoint);
      } else if (this.displayPage == MenuPage.ACCOUNT_SWITCHER) {
         return this.accounts.handleCharacter(codePoint);
      } else {
         return this.displayPage == MenuPage.CONFIGURATIONS
            ? this.configs.handleCharacter(codePoint)
            : this.displayPage == MenuPage.NONE && this.modules.handleCharacter(codePoint);
      }
   }

   public void handleScroll(int mouseX, int mouseY, double vertical) {
      if (!this.header.handleScroll(mouseX, mouseY, vertical)) {
         switch (this.displayPage) {
            case SETTINGS:
               this.settings.handleScroll(mouseX, mouseY, vertical);
               break;
            case FRIENDS:
               this.friends.handleScroll(mouseX, mouseY, vertical);
               break;
            case ACCOUNT_SWITCHER:
               this.accounts.handleScroll(mouseX, mouseY, vertical);
               break;
            case CONFIGURATIONS:
               this.configs.handleScroll(mouseX, mouseY, vertical);
               break;
            case NONE:
            case SEARCH:
               this.modules.handleScroll(mouseX, mouseY, vertical);
         }
      }
   }

   @Override
   public boolean isDragHandle(float mouseX, float mouseY) {
      return this.contains(mouseX, mouseY)
         && mouseY <= this.y() + this.px(56.0F)
         && mouseX > this.x() + this.px(180.0F)
         && !this.header.isSearchAt(mouseX, mouseY)
         && !this.header.isChevronLeftAt(mouseX, mouseY)
         && !this.header.isChevronRightAt(mouseX, mouseY);
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      this.drawBackground();
      switch (this.displayPage) {
         case SETTINGS:
            this.settings.render(minecraft, guiGraphicsExtractor);
            break;
         case FRIENDS:
            this.friends.render(minecraft, guiGraphicsExtractor);
            break;
         case ACCOUNT_SWITCHER:
            this.accounts.render(minecraft, guiGraphicsExtractor);
            break;
         case CONFIGURATIONS:
            this.configs.render(minecraft, guiGraphicsExtractor);
            break;
         default:
            this.modules.render(minecraft, guiGraphicsExtractor);
      }

      this.header.render(minecraft, guiGraphicsExtractor);
      this.sidebar.render(minecraft, guiGraphicsExtractor);
   }

   private static int totalCount(FeatureCategory category) {
      return FeatureManager.INSTANCE.getFeatures(category).size();
   }

   private static int enabledCount(FeatureCategory category) {
      int count = 0;

      for (Feature feature : FeatureManager.INSTANCE.getFeatures(category)) {
         if (feature.isEnabled()) {
            count++;
         }
      }

      return count;
   }

   private void ensurePosition(MenuOverlayState state, int screenWidth, int screenHeight) {
      if (!state.hasPosition()) {
         state.setPanelPosition((screenWidth - this.dimensions.panelWidth()) / 2.0F, (screenHeight - this.dimensions.panelHeight()) / 2.0F);
         state.markPositionInitialized();
      }

      this.clampPosition(state, screenWidth, screenHeight);
   }

   private void clampPosition(MenuOverlayState state, int screenWidth, int screenHeight) {
      float maxX = Math.max(0.0F, screenWidth - this.dimensions.panelWidth());
      float maxY = Math.max(0.0F, screenHeight - this.dimensions.panelHeight());
      state.setPanelPosition(MathUtil.clamp(state.panelX(), 0.0F, maxX), MathUtil.clamp(state.panelY(), 0.0F, maxY));
   }

   private void drawBackground() {
      float radius = this.dimensions.panelRadius();
      Render2DUtil.rect(this.x(), this.y() + this.px(8.0F), this.width(), this.height())
         .color(0)
         .radius(radius)
         .shadow(Theme.Colors.PANEL_SHADOW_STRONG, this.px(20.0F))
         .draw();
      Render2DUtil.rect(this.x(), this.y(), this.width(), this.height()).color(0).radius(radius).shadow(Theme.Colors.PANEL_SHADOW, this.px(12.0F)).draw();
      Render2DUtil.rect(this.x(), this.y(), this.width(), this.height()).color(Theme.Colors.PANEL).radius(radius).blur(this.px(26.0F)).draw();
      this.drawBackgroundShader(radius);
      Render2DUtil.rect(this.x(), this.y(), this.width(), this.height())
         .color(0)
         .radius(radius)
         .border(Math.max(this.px(1.0F), this.dimensions.panelBorder()), Theme.Colors.PANEL_BORDER)
         .draw();
      float innerRadius = Math.max(0.0F, radius - this.px(1.0F));
      Render2DUtil.rect(this.x() + this.px(1.0F), this.y() + this.px(1.0F), this.width() - this.px(2.0F), this.height() - this.px(2.0F))
         .color(0)
         .radius(innerRadius)
         .border(this.px(1.0F), ColorUtil.rgba(255, 255, 255, 22))
         .draw();
   }

   private void drawBackgroundShader(float radius) {
      MenuBackground background = MenuAppearance.background();
      if (background.isAnimated()) {
         int alpha = Math.round(216.0F);
         int primary = Theme.getAccent() & 16777215 | alpha << 24;
         int secondary = accentComplement() & 16777215 | alpha << 24;
         float headerHeight = this.px(56.0F);
         Render2DUtil.pushScissor(this.x(), this.y() + headerHeight, this.width(), this.height() - headerHeight);
         Render2DUtil.menuBackground(this.x(), this.y(), this.width(), this.height(), radius, backgroundSeconds(), background.shaderMode(), primary, secondary);
         Render2DUtil.popScissor();
         float dim = MenuAppearance.backgroundDim();
         if (dim > 0.001F) {
            Render2DUtil.rect(this.x(), this.y() + headerHeight, this.width(), this.height() - headerHeight)
               .color(ColorUtil.withAlpha(-16777216, Math.round(255.0F * dim)))
               .radius(0.0F, 0.0F, radius, radius)
               .draw();
         }

         Render2DUtil.rect(this.x(), this.y(), this.width(), this.px(56.0F)).color(Theme.Colors.PANEL).radius(radius, radius, 0.0F, 0.0F).draw();
      }
   }

   private static int accentComplement() {
      float[] hsv = ColorUtil.hsv(Theme.getAccent());
      float hue = (hsv[0] + 0.42F) % 1.0F;
      return ColorUtil.fromHsv(hue, Math.max(0.55F, hsv[1]), Math.max(0.75F, hsv[2]), 255);
   }

   private static float backgroundSeconds() {
      return (float)(System.currentTimeMillis() % 3600000L) / 1000.0F;
   }

   private static final class ContentFrame extends Component {
      @Override
      public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      }
   }
}

