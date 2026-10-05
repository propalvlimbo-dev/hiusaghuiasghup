package org.xrose.menu.pages.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.Setting;
import org.xrose.menu.core.MenuOverlayState;
import org.xrose.menu.core.MenuPage;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.SmoothScroll;
import org.xrose.menu.ui.controls.MenuClipboard;
import org.xrose.menu.ui.controls.SearchInputComponent;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.math.Animation;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;

public final class ModulePage extends Component {
   private static final int PAGE_PADDING = 16;
   private static final int COLUMN_GAP = 16;
   private static final int CARD_GAP = 12;
   private static final int SEARCH_FIELD_X = 32;
   private static final int SEARCH_FIELD_Y = 170;
   private static final int SEARCH_FIELD_WIDTH = 960;
   private static final int SEARCH_FIELD_HEIGHT = 44;
   private static final int SEARCH_RESULTS_TOP = 238;
   private static final int SEARCH_ROW_HEIGHT = 48;
   private static final int SEARCH_ROW_GAP = 6;
   private static final int SEARCH_MAX_ROWS = 5;
   private static final int SEARCH_ROW_PADDING_X = 10;
   private static final Identifier[] CATEGORY_ICONS = new Identifier[]{
      Textures.Icons.SWORDS, Textures.Icons.PERSON_STANDING, Textures.Icons.EYE, Textures.Icons.USER_ROUND, Textures.Icons.BOXES, Textures.Icons.BRAIN
   };
   private final List<Component> children = new ArrayList<>();
   private final List<ModuleCard> cards = new ArrayList<>();
   private final StringBuilder searchQuery = new StringBuilder();
   private final SearchInputComponent searchInput = new SearchInputComponent(this.searchQuery::toString, this::searchSuggestionSuffix);
   private boolean searchAllSelected;
   private final Animation categoryAnimation = new Animation(150L, Animation.Easing.EASE_OUT_QUAD);
   private final Animation categoryIndicatorAnimation = new Animation(180L, Animation.Easing.EASE_OUT_QUAD);
   private final Animation[] searchRowHover = new Animation[5];
   private MenuOverlayState state;
   private MenuPage displayedPage = MenuPage.NONE;
   private FeatureCategory selectedCategory = FeatureCategory.COMBAT;
   private FeatureCategory cardsCategory;
   private boolean searchFocused;
   private int mouseX;
   private int mouseY;
   private float transitionProgress;
   private int visualOffsetX;
   private float visualAlpha = 1.0F;
   private final SmoothScroll scroll = new SmoothScroll();
   private float scrollOffset;
   private int maxContentBottom = 560;
   private int[] cardColumns;
   private Feature triggeredFeature;

   public ModulePage() {
      this.categoryAnimation.animate(1.0F, 1.0F, 0L, Animation.Easing.EASE_OUT_QUAD);
      float index = categoryIndex(this.selectedCategory);
      this.categoryIndicatorAnimation.animate(index, index, 0L, Animation.Easing.EASE_OUT_QUAD);

      for (int i = 0; i < this.searchRowHover.length; i++) {
         this.searchRowHover[i] = new Animation(140L, Animation.Easing.EASE_OUT_QUAD);
         this.searchRowHover[i].animate(0.0F, 0.0F, 0L, Animation.Easing.EASE_OUT_QUAD);
      }
   }

   public void layout(Component frame, MenuOverlayState state, int mouseX, int mouseY) {
      this.attach(frame, frame.x(), frame.y(), frame.width(), frame.height());
      this.state = state;
      this.displayedPage = state.displayPage();
      this.transitionProgress = state.contentProgress();
      if (state.page() != MenuPage.SEARCH) {
         this.searchFocused = false;
         this.searchAllSelected = false;
      }

      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.scrollOffset = this.scroll.update(Math.max(0, this.maxContentBottom - 560));
      this.ensureCards();
      this.layoutCards();
      this.searchInput
         .place(this, 32, 170, 960, 44, mouseX, mouseY)
         .alpha(this.transitionProgress)
         .focused(this.searchFocused)
         .selected(this.searchAllSelected)
         .cornerRadius(10.0F);
   }

   public boolean handleMouseButton(int mouseX, int mouseY, int button) {
      if (this.displayedPage == MenuPage.SEARCH && this.transitionProgress > 0.05F) {
         Feature result = this.resultAt(mouseX, mouseY);
         if (result != null) {
            this.openSearchResult(result);
            return true;
         } else if (button == 0 && this.searchInput.contains(mouseX, mouseY)) {
            this.searchFocused = !this.searchFocused;
            this.searchAllSelected = false;
            return true;
         } else {
            return this.contains(mouseX, mouseY) && mouseY >= this.y() + this.px(56.0F);
         }
      } else if (this.displayedPage == MenuPage.NONE && !(mouseY < this.y() + this.px(56.0F))) {
         for (ModuleCard card : this.cards) {
            if (card.handleBindPopupClick(mouseX, mouseY, button)) {
               return true;
            }
         }

         if (!this.contains(mouseX, mouseY)) {
            return false;
         }

         if (button == 2) {
            for (ModuleCard card : this.cards) {
               if (card.handleMiddleClick(mouseX, mouseY)) {
                  this.closeBindPopupsExcept(card);
                  return true;
               }
            }

            return this.contains(mouseX, mouseY);
         } else if (button == 1) {
            for (ModuleCard card : this.cards) {
               if (card.handleRightClick(mouseX, mouseY)) {
                  return true;
               }
            }

            return this.contains(mouseX, mouseY);
         } else {
            if (button != 0) {
               return this.contains(mouseX, mouseY);
            }

            for (ModuleCard card : this.cards) {
               if (card.handleDropdownPopupClick(mouseX, mouseY)) {
                  return true;
               }
            }

            for (ModuleCard card : this.cards) {
               if (card.handleClick(mouseX, mouseY)) {
                  return true;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public void drag(int mouseX) {
      for (ModuleCard card : this.cards) {
         card.drag(mouseX);
      }
   }

   public void releasePointer() {
      for (ModuleCard card : this.cards) {
         card.releasePointer();
      }
   }

   public boolean handleKey(int key) {
      if (this.displayedPage == MenuPage.SEARCH) {
         if (this.searchFocused && MenuClipboard.shortcutDown()) {
            if (key == 65) {
               this.searchAllSelected = !this.searchQuery.isEmpty();
               return true;
            }

            if (key == 67) {
               if (this.searchAllSelected) {
                  MenuClipboard.set(this.searchQuery.toString());
               }

               return true;
            }

            if (key == 88) {
               if (this.searchAllSelected) {
                  MenuClipboard.set(this.searchQuery.toString());
                  this.replaceSearchText("");
               }

               return true;
            }

            if (key == 86) {
               String base = this.searchAllSelected ? "" : this.searchQuery.toString();
               this.replaceSearchText(base + MenuClipboard.get());
               return true;
            }
         }

         if (this.searchFocused && key == 258) {
            this.acceptSearchSuggestion();
            return true;
         }

         if (this.searchFocused && key == 257) {
            List<Feature> results = this.searchResults();
            if (!results.isEmpty()) {
               this.openSearchResult(results.getFirst());
            }

            return true;
         } else if (this.searchFocused && key == 259) {
            this.backspace();
            return true;
         } else {
            return false;
         }
      } else {
         if (this.displayedPage != MenuPage.NONE) {
            return false;
         }

         for (ModuleCard card : this.cards) {
            if (card.handleKey(key)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isCapturingBind() {
      if (this.displayedPage != MenuPage.NONE) {
         return false;
      }

      for (ModuleCard card : this.cards) {
         if (card.isCapturingBind()) {
            return true;
         }
      }

      return false;
   }

   public boolean handleCharacter(int codePoint) {
      if (this.displayedPage != MenuPage.NONE) {
         return false;
      }

      for (ModuleCard card : this.cards) {
         if (card.handleCharacter(codePoint)) {
            return true;
         }
      }

      return false;
   }

   public void handleScroll(int mouseX, int mouseY, double vertical) {
      if (this.displayedPage == MenuPage.NONE && this.contains(mouseX, mouseY) && mouseY > this.y() + this.px(56.0F)) {
         for (ModuleCard card : this.cards) {
            if (card.handleScroll(mouseX, mouseY, vertical)) {
               return;
            }
         }

         this.scroll.scroll(vertical, Math.max(0, this.maxContentBottom - 560));

         for (ModuleCard card : this.cards) {
            card.closeOverlays();
         }
      }
   }

   public boolean isSearchOpen() {
      return this.state != null && this.state.page() == MenuPage.SEARCH;
   }

   public boolean isSearchFocused() {
      return this.isSearchOpen() && this.searchFocused;
   }

   public void appendCodePoint(int codePoint) {
      if (this.isSearchFocused() && !MenuClipboard.shortcutDown() && !Character.isISOControl(codePoint)) {
         if (this.searchAllSelected) {
            this.searchQuery.setLength(0);
            this.searchAllSelected = false;
         }

         if (this.searchQuery.codePointCount(0, this.searchQuery.length()) < 40) {
            this.searchQuery.appendCodePoint(codePoint);
         }
      }
   }

   public void backspace() {
      if (this.isSearchFocused() && !this.searchQuery.isEmpty()) {
         if (this.searchAllSelected) {
            this.replaceSearchText("");
         } else {
            int lastCodePoint = this.searchQuery.codePointBefore(this.searchQuery.length());
            this.searchQuery.delete(this.searchQuery.length() - Character.charCount(lastCodePoint), this.searchQuery.length());
         }
      }
   }

   private void replaceSearchText(String value) {
      String resolved = value == null ? "" : value.replaceAll("\\R", " ");
      int codePoints = resolved.codePointCount(0, resolved.length());
      if (codePoints > 40) {
         resolved = resolved.substring(0, resolved.offsetByCodePoints(0, 40));
      }

      this.searchQuery.setLength(0);
      this.searchQuery.append(resolved);
      this.searchAllSelected = false;
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      if (this.displayedPage == MenuPage.NONE || this.displayedPage == MenuPage.SEARCH) {
         this.updateCategoryVisuals();
         float headerH = this.px(56.0F);
         Render2DUtil.pushScissor(
            this.x() + this.px(1.0F), this.y() + headerH, this.width() - this.px(2.0F), Math.max(0.0F, this.height() - headerH - this.px(6.0F))
         );

         for (Component child : this.children) {
            child.render(minecraft, guiGraphicsExtractor);
         }

         if (this.children.isEmpty()) {
            this.textCentered(512.0F, 290.0F, 17.0F, MenuText.ui("No modules in this category yet"), Theme.Colors.PRIMARY);
            this.textCentered(
               512.0F, 320.0F, 11.0F, MenuText.ui("Modules registered in this category will appear here automatically."), Theme.Colors.SECONDARY_DARK
            );
         }

         Render2DUtil.popScissor();

         for (ModuleCard card : this.cards) {
            card.renderOverlay(minecraft, guiGraphicsExtractor);
         }

         this.visualAlpha = 1.0F;
         this.visualOffsetX = 0;
         if (this.displayedPage == MenuPage.SEARCH && this.transitionProgress > 0.001F) {
            Render2DUtil.flush();
            guiGraphicsExtractor.nextStratum();
            this.renderSearch(minecraft, guiGraphicsExtractor);
         }
      }
   }

   private void ensureCards() {
      if (this.cardsCategory != this.selectedCategory) {
         this.rebuildCards();
      }
   }

   private void rebuildCards() {
      this.cards.clear();

      for (Feature feature : FeatureManager.INSTANCE.getFeatures(this.selectedCategory)) {
         this.cards.add(new ModuleCard(feature));
      }

      this.cardsCategory = this.selectedCategory;
      int[] tops = this.computeMasonry();
      if (this.triggeredFeature != null) {
         for (int index = 0; index < this.cards.size(); index++) {
            ModuleCard card = this.cards.get(index);
            if (card.feature() == this.triggeredFeature) {
               card.flashHighlight();
               float maxScroll = Math.max(0.0F, this.maxContentBottom - 560.0F);
               float scrollTarget = tops[index] - 68.0F - (560.0F - card.designHeight()) / 2.0F;
               this.scroll.setTarget(Mth.clamp(scrollTarget, 0.0F, maxScroll));
               break;
            }
         }

         this.triggeredFeature = null;
      }
   }

   private int[] computeMasonry() {
      int[] tops = new int[this.cards.size()];
      if (this.cardColumns == null || this.cardColumns.length != this.cards.size()) {
         this.cardColumns = new int[this.cards.size()];
      }

      int[] columnBottom = new int[]{68, 68, 68};

      for (int index = 0; index < this.cards.size(); index++) {
         int column = shortestColumn(columnBottom);
         this.cardColumns[index] = column;
         tops[index] = columnBottom[column];
         columnBottom[column] += this.cards.get(index).designHeight() + 12;
      }

      this.maxContentBottom = Math.max(columnBottom[0], Math.max(columnBottom[1], columnBottom[2]));
      return tops;
   }

   private void closeBindPopupsExcept(ModuleCard except) {
      for (ModuleCard card : this.cards) {
         if (card != except) {
            card.closeBindPopup();
         }
      }
   }

   private void layoutCards() {
      this.children.clear();
      int columnWidth = 320;
      int[] tops = this.computeMasonry();

      for (int index = 0; index < this.cards.size(); index++) {
         ModuleCard card = this.cards.get(index);
         int x = 16 + this.cardColumns[index] * (columnWidth + 16);
         int y = tops[index] - Math.round(this.scrollOffset);
         card.place(this, x, y, columnWidth, this.visualAlpha, this.mouseX, this.mouseY, this.visualOffsetX, 640);
         this.children.add(card);
      }
   }

   public FeatureCategory selectedCategory() {
      return this.selectedCategory;
   }

   public void selectCategory(FeatureCategory category) {
      if (category != null && category != this.selectedCategory) {
         float from = this.categoryIndicatorAnimation.getValue();
         this.selectedCategory = category;
         this.categoryIndicatorAnimation.animate(from, categoryIndex(category), 180L, Animation.Easing.EASE_OUT_QUAD);
         this.categoryAnimation.animate(0.0F, 1.0F, 150L, Animation.Easing.EASE_OUT_QUAD);
         this.rebuildCards();
      }
   }

   public void focusSearch() {
      this.searchFocused = true;
      this.searchAllSelected = true;
   }

   private void renderSearch(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      Render2DUtil.rect(this.x(), this.y(), this.width(), this.height())
         .color(0)
         .radius(this.px(12.0F))
         .shadow(Theme.Colors.PANEL_SHADOW, this.px(8.0F))
         .draw();
      Render2DUtil.rect(this.x(), this.y() + this.px(56.0F), this.width(), this.height() - this.px(56.0F))
         .color(ColorUtil.withAlpha(Theme.Colors.OVERLAY, Math.round(218.0F * this.transitionProgress)))
         .radius(0.0F, 0.0F, this.px(12.0F), this.px(12.0F))
         .blur(this.px(16.0F * this.transitionProgress))
         .draw();
      this.text(32.0F, 96.0F, 24.0F, MenuText.ui("Search"), -1, this.transitionProgress, UiFontStyle.SEMIBOLD);
      this.text(
         32.0F, 134.0F, 16.0F, MenuText.ui("Start typing to find a module or setting."), Theme.Colors.SECONDARY, this.transitionProgress, UiFontStyle.MEDIUM
      );
      this.searchInput.render(minecraft, guiGraphicsExtractor);
      List<Feature> results = this.searchResults();
      if (results.isEmpty()) {
         if (!this.searchQuery.isEmpty() && this.transitionProgress > 0.9F) {
            this.textCentered(512.0F, 272.0F, 12.0F, MenuText.ui("Nothing found"), Theme.Colors.SECONDARY_DARK, this.transitionProgress);
         }
      } else {
         float rowX = 42.0F;
         float rowW = 940.0F;

         for (int i = 0; i < Math.min(results.size(), 5); i++) {
            Feature feature = results.get(i);
            float staggerProgress = Math.clamp((this.transitionProgress - i * 0.05F) / 0.65F, 0.0F, 1.0F);
            float slide = (1.0F - Animation.Easing.EASE_OUT_CUBIC.ease(staggerProgress)) * 10.0F;
            float rowY = 238 + i * 54 + slide;
            boolean hovered = this.hit(this.mouseX, this.mouseY, rowX, rowY, rowW, 48.0F);
            Animation hoverAnimation = this.searchRowHover[i];
            hoverAnimation.animate(hoverAnimation.getValue(), hovered ? 1.0F : 0.0F, 140L, Animation.Easing.EASE_OUT_QUAD);
            this.renderSearchRow(feature, rowX, rowY, rowW, hoverAnimation.getValue(), staggerProgress);
         }
      }
   }

   private void renderSearchRow(Feature feature, float rowX, float rowY, float rowW, float hover, float rowAlpha) {
      if (!(rowAlpha < 0.01F)) {
         if (hover > 0.01F) {
            this.rect(rowX, rowY, rowW, 48.0F, Theme.Colors.SURFACE_HOVER, 8.0F, rowAlpha * hover);
         }

         boolean active = hover > 0.5F;
         this.texture(rowX + 10.0F, rowY + 17.5F, 13.0F, categoryIcon(feature.getCategory()), active ? -1 : Theme.Colors.ICON_MUTED, rowAlpha);
         float centerY = rowY + 24.0F;
         this.text(
            rowX + 10.0F + 23.0F,
            this.centeredTextY(centerY, 13.0F),
            13.0F,
            feature.getName(),
            active ? Theme.Colors.PRIMARY : Theme.Colors.TEXT_TEXT,
            rowAlpha,
            UiFontStyle.MEDIUM
         );
         this.textRight(
            rowX + rowW - 14.0F,
            this.centeredTextY(centerY, 11.0F),
            11.0F,
            MenuText.ui(categoryLabel(feature.getCategory())),
            Theme.Colors.SECONDARY_DARK,
            rowAlpha * 0.85F
         );
      }
   }

   private List<Feature> searchResults() {
      String query = this.searchQuery.toString().trim().toLowerCase(Locale.ROOT);
      return query.isEmpty()
         ? List.of()
         : FeatureManager.INSTANCE
            .getFeatures()
            .stream()
            .filter(feature -> matches(feature, query))
            .sorted(Comparator.<Feature>comparingInt(feature -> matchPriority(feature, query)).thenComparing(Feature::getName, String.CASE_INSENSITIVE_ORDER))
            .limit(5L)
            .toList();
   }

   private String searchSuggestionSuffix() {
      String query = this.searchQuery.toString();
      if (!query.isEmpty() && query.equals(query.trim())) {
         String normalizedQuery = query.toLowerCase(Locale.ROOT);
         return FeatureManager.INSTANCE
            .getFeatures()
            .stream()
            .map(Feature::getName)
            .filter(name -> name.length() > query.length() && name.toLowerCase(Locale.ROOT).startsWith(normalizedQuery))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .map(name -> name.substring(query.length()))
            .findFirst()
            .orElse("");
      } else {
         return "";
      }
   }

   private void acceptSearchSuggestion() {
      String suffix = this.searchSuggestionSuffix();
      if (!suffix.isEmpty()) {
         this.searchQuery.append(suffix);
      }
   }

   private void openSearchResult(Feature result) {
      if (result.getCategory() != this.selectedCategory) {
         float from = this.categoryIndicatorAnimation.getValue();
         this.selectedCategory = result.getCategory();
         this.categoryIndicatorAnimation.animate(from, categoryIndex(this.selectedCategory), 180L, Animation.Easing.EASE_OUT_QUAD);
      }

      this.triggeredFeature = result;
      this.searchQuery.setLength(0);
      this.searchFocused = false;
      this.searchAllSelected = false;
      this.state.openPage(MenuPage.NONE);
      this.rebuildCards();
   }

   private Feature resultAt(int mouseX, int mouseY) {
      List<Feature> results = this.searchResults();
      int visibleRows = Math.min(results.size(), 5);
      float rowX = 42.0F;
      float rowW = 940.0F;

      for (int i = 0; i < visibleRows; i++) {
         float rowY = 238 + i * 54;
         if (this.hit(mouseX, mouseY, rowX, rowY, rowW, 48.0F)) {
            return results.get(i);
         }
      }

      return null;
   }

   private static String categoryLabel(FeatureCategory category) {
      return switch (category) {
         case COMBAT -> "Combat";
         case MOVEMENT -> "Movement";
         case VISUAL -> "Visual";
         case PLAYER -> "Player";
         case MISC -> "Misc";
         case PVE -> "PvE";
      };
   }

   private static Identifier categoryIcon(FeatureCategory category) {
      int index = category.ordinal();
      return index >= 0 && index < CATEGORY_ICONS.length ? CATEGORY_ICONS[index] : CATEGORY_ICONS[0];
   }

   private static boolean matches(Feature feature, String query) {
      return matchPriority(feature, query) < Integer.MAX_VALUE;
   }

   private static int matchPriority(Feature feature, String query) {
      String name = feature.getName().toLowerCase(Locale.ROOT);
      if (name.startsWith(query)) {
         return 0;
      }

      if (name.contains(query)) {
         return 1;
      }

      for (Setting<?> setting : feature.getSettings()) {
         String canonicalSettingName = setting.getName().toLowerCase(Locale.ROOT);
         String settingName = MenuText.setting(feature.getName(), setting.getName()).toLowerCase(Locale.ROOT);
         if (!settingName.startsWith(query) && !canonicalSettingName.startsWith(query)) {
            if (!settingName.contains(query) && !canonicalSettingName.contains(query)) {
               for (String option : setting instanceof ModeSetting mode
                  ? mode.getModes()
                  : (setting instanceof MultiSelectSetting multi ? multi.getOptions() : List.<String>of())) {
                  if (option.toLowerCase(Locale.ROOT).contains(query) || MenuText.option(option).toLowerCase(Locale.ROOT).contains(query)) {
                     return 3;
                  }
               }
               continue;
            }

            return 3;
         }

         return 2;
      }

      return !feature.getDescription().toLowerCase(Locale.ROOT).contains(query)
            && !MenuText.featureDescription(feature.getDescription()).toLowerCase(Locale.ROOT).contains(query)
         ? Integer.MAX_VALUE
         : 4;
   }

   private static int shortestColumn(int[] values) {
      int column = 0;

      for (int index = 1; index < values.length; index++) {
         if (values[index] < values[column]) {
            column = index;
         }
      }

      return column;
   }

   private void updateCategoryVisuals() {
      float categoryProgress = this.categoryAnimation.getValue();
      this.visualAlpha = categoryProgress;
      this.visualOffsetX = Math.round(14.0F * (1.0F - categoryProgress));
   }

   private static float categoryIndex(FeatureCategory category) {
      FeatureCategory[] categories = FeatureCategory.values();

      for (int index = 0; index < categories.length; index++) {
         if (categories[index] == category) {
            return index;
         }
      }

      return 0.0F;
   }
}

