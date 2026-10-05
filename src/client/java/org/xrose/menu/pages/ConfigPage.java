package org.xrose.menu.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.feature.FeatureManager;
import org.xrose.menu.core.MenuConfigStore;
import org.xrose.menu.core.MenuPage;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.pages.configs.ConfigCard;
import org.xrose.menu.pages.configs.ConfigPreviewManager;
import org.xrose.menu.ui.CardGrid;
import org.xrose.menu.ui.PageComponent;
import org.xrose.menu.ui.controls.IconButton;
import org.xrose.menu.ui.controls.InputComponent;
import org.xrose.menu.ui.popups.ModalDialog;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class ConfigPage extends PageComponent {
   private static final int ROW_STEP = 73;
   private static final int DROPDOWN_WIDTH = 108;
   private static final int DROPDOWN_HEIGHT = 36;
   private static final int DROPDOWN_PADDING_LEFT = 14;
   private static final int DROPDOWN_PADDING_RIGHT = 10;
   private static final int PLUS_BOX = 24;
   private static final int PLUS_ICON = 16;
   private static final int CONTROLS_RIGHT = 992;
   private static final int CONTROLS_GAP = 8;
   private static final int CONTROLS_ROW_Y = 104;
   private static final int PLUS_X = 968;
   private static final float PLUS_Y = 110.0F;
   private static final int DROPDOWN_X = 852;
   private static final String NAME_PATTERN = "[A-Za-z0-9_-]{1,32}";
   private final List<ConfigCard> cards = new ArrayList<>();
   private final Map<String, Long> savedDates = new HashMap<>();
   private String lastDirSignature = "";
   private final CardGrid grid = new CardGrid(188, 320, 73, 624);
   private MenuPage displayedPage = MenuPage.NONE;
   private String selectedName;
   private boolean oldestFirst;
   private String pendingName = "";
   private final InputComponent nameInput = new InputComponent(() -> this.pendingName, value -> this.pendingName = value)
      .placeholder("Type Config Name")
      .filter(val -> val.length() <= 32 && val.matches("[A-Za-z0-9_-]*"));
   private final IconButton addButton = new IconButton(Textures.Icons.CIRCLE_PLUS, 16, this::openModal);
   private final ModalDialog saveModal = new ModalDialog(
      Textures.Icons.PLUS,
      "Save Config",
      new String[]{"Enter a name for this config.", "Letters, digits, - and _ only."},
      this.nameInput,
      this::saveModal,
      null
   );

   public ConfigPage() {
      this.oldestFirst = MenuConfigStore.getBoolean("configsOldestFirst", false);
      this.selectedName = MenuConfigStore.getString("selectedConfig", "");
      this.loadCards();
   }

   private void loadCards() {
      this.cards.clear();
      this.savedDates.clear();
      Set<String> favorites = this.favoriteNames();

      for (String name : FeatureManager.INSTANCE.configNames()) {
         long date = savedDateMillis(name);
         this.savedDates.put(name, date);
         ConfigCard card = new ConfigCard(name, favorites.contains(name));
         card.setDateText(ConfigPreviewManager.INSTANCE.savedDateText(name));
         this.cards.add(card);
      }

      this.lastDirSignature = this.directorySignature();
   }

   private String directorySignature() {
      try (Stream<Path> stream = Files.list(ConfigPreviewManager.configsDir())) {
         long count = 0L;
         long latest = 0L;

         for (Path path : stream.toList()) {
            if (path.getFileName().toString().endsWith(".json")) {
               count++;

               try {
                  latest = Math.max(latest, Files.getLastModifiedTime(path).toMillis());
               } catch (IOException var10) {
               }
            }
         }

         return count + ":" + latest;
      } catch (IOException exception) {
         return "";
      }
   }

   private static long savedDateMillis(String name) {
      try {
         FileTime time = Files.getLastModifiedTime(ConfigPreviewManager.jsonPath(name));
         return time.toMillis();
      } catch (IOException exception) {
         return 0L;
      }
   }

   private Set<String> favoriteNames() {
      Set<String> favorites = new HashSet<>();
      JsonArray saved = MenuConfigStore.getArray("configFavorites");
      if (saved != null) {
         for (JsonElement element : saved) {
            if (element.isJsonPrimitive()) {
               favorites.add(element.getAsString());
            }
         }
      }

      return favorites;
   }

   private void persist() {
      JsonArray favorites = new JsonArray();

      for (ConfigCard card : this.displayOrder()) {
         if (card.favorite()) {
            favorites.add(card.name());
         }
      }

      String selected = this.selectedName == null ? "" : this.selectedName;
      boolean oldest = this.oldestFirst;
      MenuConfigStore.save(data -> {
         data.add("configFavorites", favorites);
         data.addProperty("selectedConfig", selected);
         data.addProperty("configsOldestFirst", oldest);
      });
   }

   private List<ConfigCard> displayOrder() {
      List<ConfigCard> display = new ArrayList<>(this.cards);
      Comparator<ConfigCard> byDate = this.oldestFirst
         ? Comparator.comparing(card -> this.savedDates.getOrDefault(card.name(), 0L))
         : Comparator.<ConfigCard, Long>comparing(card -> this.savedDates.getOrDefault(card.name(), 0L)).reversed();
      display.sort(Comparator.comparing(ConfigCard::favorite).reversed().thenComparing(byDate));
      return display;
   }

   @Override
   protected void onLayout() {
      this.displayedPage = this.state.displayPage();
      if (this.displayedPage == MenuPage.CONFIGURATIONS) {
         String signature = this.directorySignature();
         if (!signature.equals(this.lastDirSignature)) {
            this.loadCards();
         }
      }

      if (this.displayedPage != MenuPage.CONFIGURATIONS && this.saveModal.isOpen()) {
         this.closeModal();
      }

      this.grid.update(this.cards.size());
      List<ConfigCard> display = this.displayOrder();

      for (int index = 0; index < display.size(); index++) {
         ConfigCard card = display.get(index);
         card.setSelected(card.name().equals(this.selectedName));
         card.place(this, this.grid.x(index), this.grid.y(index), this.mouseX, this.mouseY, this.progress);
      }

      this.addButton.place(this, 968.0F, 110.0F, 24, this.mouseX, this.mouseY).alpha(this.progress);
      this.saveModal.place(this, this.mouseX, this.mouseY, this.progress);
   }

   public void handleScroll(int mouseX, int mouseY, double vertical) {
      if (!this.saveModal.isOpen()) {
         if (this.contains(mouseX, mouseY) && mouseY >= this.sy(168.0F)) {
            this.grid.scroll(vertical, this.cards.size());
         }
      }
   }

   @Override
   public boolean handleClick(int mouseX, int mouseY) {
      if (!this.contentContains(mouseX, mouseY)) {
         return false;
      }

      if (this.saveModal.isOpen()) {
         return this.saveModal.handleClick(mouseX, mouseY);
      }

      if (this.hit(mouseX, mouseY, 852.0F, 104.0F, 108.0F, 36.0F)) {
         this.oldestFirst = !this.oldestFirst;
         this.persist();
         return true;
      }

      if (this.addButton.handleClick(mouseX, mouseY)) {
         return true;
      }

      if (mouseY < this.sy(168.0F)) {
         return true;
      }

      for (ConfigCard card : this.cards) {
         if (card.isPinAt(mouseX, mouseY)) {
            card.toggleFavorite();
            this.persist();
            return true;
         }

         if (card.isDeleteAt(mouseX, mouseY)) {
            this.deleteConfig(card);
            return true;
         }

         boolean loadClicked = card.isLoadAt(mouseX, mouseY) || card.handleClick(mouseX, mouseY);
         if (loadClicked) {
            this.selectConfig(card.name());
            return true;
         }
      }

      return true;
   }

   private void selectConfig(String name) {
      if (FeatureManager.INSTANCE.loadConfig(name)) {
         this.selectedName = name;
         this.persist();
      }
   }

   private void deleteConfig(ConfigCard card) {
      String name = card.name();
      FeatureManager.INSTANCE.deleteConfig(name);

      try {
         Files.deleteIfExists(ConfigPreviewManager.previewPath(name));
      } catch (IOException var4) {
      }

      ConfigPreviewManager.INSTANCE.invalidate(name);
      this.cards.remove(card);
      if (name.equals(this.selectedName)) {
         this.selectedName = null;
      }

      this.loadCards();
      this.persist();
   }

   public boolean handleKey(int key) {
      return this.saveModal.handleKey(key);
   }

   public boolean handleCharacter(int codePoint) {
      return this.saveModal.handleCharacter(codePoint);
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      if (this.displayedPage == MenuPage.CONFIGURATIONS && !(this.progress <= 0.001F)) {
         this.pageHeader("Configs", "Load, save, and manage your client presets.");
         String sortLabel = MenuText.ui(this.oldestFirst ? "Oldest" : "Recent");
         boolean sortHovered = this.hit(this.mouseX, this.mouseY, 852.0F, 104.0F, 108.0F, 36.0F);
         Render2DUtil.rect(this.sx(852.0F), this.sy(104.0F), this.px(108.0F), this.px(36.0F))
            .color(this.alpha(sortHovered ? Theme.Colors.CONTROL_HOVER : Theme.Colors.CONTROL, this.progress))
            .radius(999.0F)
            .border(Math.max(0.5F, this.px(0.5F)), this.alpha(Theme.Colors.OUTLINES_MEDIUM, this.progress))
            .draw();
         float sortTextMax = 68.0F;
         String shownSort = UiFonts.sfProDisplay().ellipsize(sortLabel, 14.0F, 14.0F * UiFontStyle.MEDIUM.letterSpacingEm(), sortTextMax);
         this.text(866.0F, this.centeredTextY(122.0F, 14.0F), 14.0F, shownSort, -1, this.progress, UiFontStyle.MEDIUM);
         this.texture(940.0F, 117.0F, 10.0F, Textures.Icons.CHEVRON_DOWN, Theme.Colors.ICON, this.progress);
         this.addButton.render(minecraft, guiGraphicsExtractor);
         if (this.cards.isEmpty()) {
            this.emptyState(468.0F, "No saved configs yet", "Save your setup with the + button or .config save <name>");
            this.saveModal.render(minecraft, guiGraphicsExtractor);
         } else {
            Render2DUtil.pushScissor(this.x(), this.sy(168.0F), this.width(), this.height() - this.px(168.0F));

            for (ConfigCard card : this.displayOrder()) {
               card.render(minecraft, guiGraphicsExtractor);
            }

            Render2DUtil.popScissor();
            this.saveModal.render(minecraft, guiGraphicsExtractor);
         }
      }
   }

   private void openModal() {
      this.pendingName = "";
      this.saveModal.open();
   }

   private void closeModal() {
      this.pendingName = "";
      this.saveModal.close();
   }

   private void saveModal() {
      String name = this.pendingName.trim();
      if (name.matches("[A-Za-z0-9_-]{1,32}")) {
         if (FeatureManager.INSTANCE.saveConfigAs(name)) {
            ConfigPreviewManager.INSTANCE.invalidate(name);
            ConfigPreviewManager.INSTANCE.scheduleCapture(name);
            this.selectedName = name;
            this.loadCards();
            this.persist();
            this.closeModal();
         }
      }
   }
}

